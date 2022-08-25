package com.susu.dfs.tracker.slot;


import com.susu.common.model.RebalancedFetchMetadataCompletedEvent;
import com.susu.common.model.TrackerSlots;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author sujay
 * <p>Description: 本地 Slot组件</p>
 * @version 15:37 2022/7/27
 */
@Slf4j
public class TrackerSlotLocal extends AbstractTrackerSlot {

    public TrackerSlotLocal(int trackerIndex, ClientManager clientManager, TrackerClusterService trackerClusterService, TrackerFileService trackerFileService) {
        super(trackerIndex,trackerIndex,clientManager,trackerClusterService,trackerFileService);
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
}
