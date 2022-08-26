package com.susu.dfs.tracker.slot;


import com.susu.common.model.*;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sujay
 * <p>Description: 本地 Slot组件</p>
 * @version 15:37 2022/7/27
 */
@Slf4j
public class TrackerSlotLocal extends AbstractTrackerSlot {

    private RebalancedManager rebalancedManager;

    public TrackerSlotLocal(int trackerIndex, ClientManager clientManager, TrackerClusterService trackerClusterService, TrackerFileService trackerFileService) {
        super(trackerIndex,trackerIndex,clientManager,trackerClusterService,trackerFileService);

        this.rebalancedManager = new RebalancedManager(this);
    }

    @Override
    public Map<Integer, Integer> initSlots() throws Exception {
        if (!initCompleted.get()) {
            List<Integer> trackerIndexList = trackerClusterService.getAllTrackerIndex();
            trackerIndexList.add(trackerIndex);
            HashMap<Integer, Integer> slotTrackerMap = new HashMap<>(Constants.SLOTS_COUNT);
            HashMap<Integer, Set<Integer>> trackerSlotMap = new HashMap<>(trackerClusterService.getAllTracker().size());
            for (int i = 0; i < Constants.SLOTS_COUNT; i++) {
                int index = i % trackerIndexList.size();
                Integer trackerIndex = trackerIndexList.get(index);
                slotTrackerMap.put(i,trackerIndex);
                Set<Integer> slots = trackerSlotMap.computeIfAbsent(trackerIndex, k -> new HashSet<>());
                slots.add(i);
            }
            replaceSlots(slotTrackerMap,trackerSlotMap);
            log.info("作为主分配好槽位信息, 发送广播给所有的Tracker：[nodeId={}]", trackerIndex);
            TrackerSlots trackerSlots = TrackerSlots.newBuilder()
                    .putAllOldSlots(slotTrackerMap)
                    .putAllNewSlots(slotTrackerMap)
                    .setRebalanced(false)
                    .build();
            NetPacket packet = NetPacket.buildPacket(trackerSlots.toByteArray(), PacketType.TRACKER_SLOT_BROADCAST);
            trackerClusterService.broadcast(packet);
        }
        Set<Integer> slots = trackerOfSlots.get(trackerIndex);
        log.info("Initialize slot information: [trackerIndex={},slots size={}]",trackerIndex,slots.size());
        return Collections.unmodifiableMap(slotsOfTracker);

    }

    @Override
    public void rebalancedSlots(NetPacket packet) throws Exception {
        log.info("rebalanced Slots Start");
        synchronized (this) {
            List<Integer> allTrackerIndex = trackerClusterService.getAllTrackerIndex();
            allTrackerIndex.add(trackerIndex);
            int numOfNode = allTrackerIndex.size();
            int oldNumOfNode = trackerOfSlots.size();
            if (numOfNode <= oldNumOfNode) {
                log.info("当前节点数量不需要重新分配slots [allTrackerSize={},oldTrackerSize={}]",numOfNode,oldNumOfNode);
                return;
            }
            RebalancedSlotsRequest rebalancedSlotsRequest = RebalancedSlotsRequest.parseFrom(packet.getBody());
            RebalancedSlotInfo info = new RebalancedSlotInfo();
            info.setApplyTrackerId(rebalancedSlotsRequest.getRebalancedNodeId());
            info.setTrackerIdList(allTrackerIndex);

            HashMap<Integer, Integer> slotNodeMapSnapshot = new HashMap<>(slotsOfTracker);
            info.setSlotsOfTrackerSnapshot(slotNodeMapSnapshot);
            rebalancedManager.add(info);
        }
    }

    @Override
    public void onFetchSlotMetadataCompleted(int rebalancedNodeId) throws Exception {
        boolean isAllCompleted = rebalancedSlotInfo.onFetchSlotMetadataCompleted(rebalancedNodeId);
        if (isAllCompleted) {

            RebalancedFetchMetadataCompletedEvent event = RebalancedFetchMetadataCompletedEvent.newBuilder()
                    .setRebalancedNodeId(rebalancedSlotInfo.getApplyTrackerId())
                    .build();

            NetPacket nettyPacket = NetPacket.buildPacket(event.toByteArray(), PacketType.TRACKER_FETCH_SLOT_META_DATA_COMPLETED_BROADCAST);
            List<Integer> otherNodeIds = trackerClusterService.broadcast(nettyPacket, rebalancedSlotInfo.getTrackerIdSet());

            log.info("发送广播给所有的Tracker节点，让他们移除所有的元数据: [applyRebalancedNodeId={}, rebalancedNodeId={}, otherNodeIds={}]",
                    rebalancedSlotInfo.getApplyTrackerId(), rebalancedNodeId, otherNodeIds == null ? 0 : otherNodeIds.size());
        }
    }

    /**
     * 重平衡结果下发
     */
    public void setRebalancedInfo(RebalancedSlotInfo rebalancedSlotInfo) throws InterruptedException {
        synchronized (this) {
            this.rebalancedSlotInfo = rebalancedSlotInfo;
            TrackerSlots trackerSlots = TrackerSlots.newBuilder()
                    .putAllOldSlots(slotsOfTracker)
                    .putAllNewSlots(rebalancedSlotInfo.getSlotsOfTrackerSnapshot())
                    .setRebalanced(true)
                    .setRebalancedNodeId(rebalancedSlotInfo.getApplyTrackerId())
                    .build();
            NetPacket packet = NetPacket.buildPacket(trackerSlots.toByteArray(), PacketType.TRACKER_SLOT_BROADCAST);
            trackerClusterService.broadcast(packet);
            log.info("重平衡Slot之后，将最新的Slots分配信息广播给所有的NameNode.");

            Set<Integer> rebalancedNodeIdSet = rebalancedSlotInfo.getTrackerIdSet();
            List<ClientInfo> clients = clientManager.getClientList();
            NewTrackerInfo.Builder builder = NewTrackerInfo.newBuilder();
            for (ClientInfo client : clients) {
                RegisterRequest request = RegisterRequest.newBuilder()
                        .setHostname(client.getHostname())
                        .setPort(client.getPort())
                        .setStoredDataSize(client.getStoredSize())
                        .setFreeSpace(client.getFreeSpace())
                        .setNodeId(client.getClientId())
                        .build();
                builder.addRequests(request);
            }

            NewTrackerInfo newPeerDataNodeInfo = builder.build();
            NetPacket request = NetPacket.buildPacket(newPeerDataNodeInfo.toByteArray(), PacketType.NEW_TRACKER_INFO);
            for (Integer nodeId : rebalancedNodeIdSet) {
                trackerClusterService.send(nodeId, request);
            }
            log.info("下发所有Storage信息给本次重平衡包含的节点：[nodeIds={}]", rebalancedNodeIdSet);
        }

    }
}
