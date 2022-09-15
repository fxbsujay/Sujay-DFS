package com.susu.dfs.tracker.slot;

import com.susu.dfs.common.model.*;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.eum.FileNodeType;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * @author sujay
 * <p>Description: 远程 Slot组件</p>
 * @version 13:34 2022/8/25
 */
@Slf4j
public class TrackerSlotRemote extends AbstractTrackerSlot{

    public TrackerSlotRemote(int trackerIndex, int trackerMasterIndex, ClientManager clientManager, TrackerClusterService trackerClusterService, TrackerFileService trackerFileService) {
        super(trackerIndex,trackerMasterIndex,clientManager,trackerClusterService,trackerFileService);
    }

    @Override
    public Map<Integer, Integer> initSlots() throws Exception {
        while (!initCompleted.get()) {
            lock.lock();
            try {
                initCompletedCondition.await();
            } finally {
                lock.unlock();
            }
        }
        Set<Integer> currentSlots = trackerOfSlots.get(trackerIndex);
        log.info("初始化槽位信息：[trackerIndex={}, slots size={}]", trackerIndex, currentSlots.size());
        return Collections.unmodifiableMap(slotsOfTracker);
    }

    @Override
    protected void replaceSlots(Map<Integer, Integer> slotOfTracker, Map<Integer, Set<Integer>> trackerOfSlots) throws Exception {
        super.replaceSlots(slotOfTracker, trackerOfSlots);
        lock.lock();
        this.initCompleted.set(true);
        try {
            initCompletedCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void rebalancedSlots(NetPacket packet) throws Exception {
        if (initCompleted.get()) {
            log.info("从磁盘中读取到Slots信息，不需要重新分配了. [trackerIndex={}]", trackerIndex);
        } else {
            trackerClusterService.send(trackerMasterIndex, packet);
            log.info("发送请求到主节点申请重平衡. [trackerMasterIndex={}]", trackerMasterIndex);
        }
    }

    @Override
    public void onReceiveSlots(TrackerSlots slots) throws Exception {
        synchronized (this) {

            if (slots.getRebalanced()) {
                maybeFetchMetadata(slots.getOldSlotsMap(), slots.getNewSlotsMap(), slots.getRebalancedNodeId());
                return;
            }

            Map<Integer, Integer> replaceSlotsNodeMap = new HashMap<>(slots.getNewSlotsMap());
            replaceSlots(replaceSlotsNodeMap,  map(replaceSlotsNodeMap));
        }
    }

    @Override
    public void onFetchMetadata(NetRequest request) throws Exception {
        FetchMetaDataResponse response = FetchMetaDataResponse.parseFrom(request.getRequest().getBody());
        log.info("收到元数据信息：[peerNodeId={}, isCompleted={}, fileSize={}]",
                response.getNodeId(), response.getCompleted(), response.getFilesCount());
        if (rebalancedSlotInfo.getApplyTrackerId() != this.trackerIndex) {
            log.warn("提示：本轮重平衡不是自己发起的，别人发起的重平衡把我的Slot信息也分配好了。");
        }
        List<Metadata> filesList = response.getFilesList();

        for (Metadata metadata : filesList) {
            if (FileNodeType.FILE.getValue() == metadata.getType()) {
                fileService.createFile(metadata.getFileName(), metadata.getAttrMap());
                FileInfo fileInfo = new FileInfo();
                fileInfo.setHostname(metadata.getHostname());
                fileInfo.setFileSize(metadata.getFileSize());
                fileInfo.setFileName(metadata.getFileName());
                clientManager.addFile(fileInfo);
            } else if (FileNodeType.DIRECTORY.getValue() == metadata.getType()) {
                fileService.mkdir(metadata.getFileName(), metadata.getAttrMap());
            }
        }

        if (response.getCompleted()) {
            rebalancedSlotInfo.onCompletedFetchMetadata(response.getNodeId());
        }
    }

    @Override
    public void onFetchSlotMetadataCompleted(int rebalancedNodeId) throws Exception {
        removeMetadata(rebalancedNodeId);
        RebalancedRemoveMetadataCompletedEvent event = RebalancedRemoveMetadataCompletedEvent.newBuilder()
                .setCurrentNodeId(this.trackerIndex)
                .setRebalancedNodeId(rebalancedNodeId)
                .build();
        NetPacket packet = NetPacket.buildPacket(event.toByteArray(), PacketType.TRACKER_REMOVE_META_DATA_COMPLETED);
        trackerClusterService.send(trackerMasterIndex, packet);
        rebalancedSlotInfo = null;
        log.info("已经移除了内存中不属于自己Slots的元数据，发送通知告诉Controller: [controllerNodeId={}]", trackerMasterIndex);
    }

    /**
     * <p>Description: 可以需要同步数据 </p>
     *
     * @param oldSlotsMap        旧的Slots分配信息
     * @param newSlotsMap        新的Slots分配信息
     * @param rebalancedNodeId   需要重平衡的Tracker节点ID
     * @throws Exception         中断异常
     */
    private void maybeFetchMetadata(Map<Integer, Integer> oldSlotsMap, Map<Integer, Integer> newSlotsMap, int rebalancedNodeId) throws Exception {
        /*
         * 对于新上线节点来说：
         *
         *    oldSlot = []
         *    newSlot = [1, 2, 3]
         *
         * 则需要将[1, 2, 3]从别的节点中获取过来
         *
         * 对于旧节点来说：
         *
         *    oldSlot = [1, 2, 3, 4, 5, 6]
         *    newSlot = [4, 5, 6]
         *
         * 则不需要做任何事情
         *
         */

        log.info("收到重平衡后的Slots信息广播，当次重平衡发起人为：[rebalancedNodeId={}]", rebalancedNodeId);

        rebalancedSlotInfo = new RebalancedSlotInfo();
        rebalancedSlotInfo.setApplyTrackerId(rebalancedNodeId);
        rebalancedSlotInfo.setSlotsOfTrackerSnapshot(newSlotsMap);

        Set<Integer> currentSlots = rebalancedSlotInfo.getSlotsFor(trackerIndex);
        Map<Integer, Set<Integer>> otherNodeSlots = new HashMap<>(trackerClusterService.getAllTracker().size());

        for (Integer slotIndex : currentSlots) {
            Integer oldSlotNameNodeId = oldSlotsMap.get(slotIndex);
            if (oldSlotNameNodeId != trackerIndex) {
                // 这部分槽的元数据在别的节点
                Set<Integer> slots = otherNodeSlots.computeIfAbsent(oldSlotNameNodeId, k -> new HashSet<>());
                slots.add(slotIndex);
            }
        }
        if (otherNodeSlots.isEmpty()) {
            log.info("本次重平衡，我不需要从别的节点获取元数据：[rebalancedNodeId={}]", rebalancedNodeId);
            return;
        }
        log.info("本次重平衡，我需要从别的节点获取元数据：[targetNodeId={}]", otherNodeSlots.keySet());

        for (Map.Entry<Integer, Set<Integer>> entry : otherNodeSlots.entrySet()) {
            Integer targetNodeId = entry.getKey();
            FetchMetaDataRequest fetchDataBySlotRequest = FetchMetaDataRequest.newBuilder()
                    .addAllSlots(entry.getValue())
                    .setNodeId(trackerIndex)
                    .build();
            NetPacket nettyPacket = NetPacket.buildPacket(fetchDataBySlotRequest.toByteArray(), PacketType.TRACKER_FETCH_SLOT_META_DATA);
            trackerClusterService.send(targetNodeId, nettyPacket);
            // 这里发送后会走到 ServerManger#writeMetadataToTracker方法，接着会走到自身节点的onFetchMetadata方法
            log.info("发送请求从别的NameNode获取元数据：[targetNodeId={}, slotsSize={}]", targetNodeId, entry.getValue().size());
        }
        rebalancedSlotInfo.waitFetchMetadataCompleted(otherNodeSlots.keySet());
        replaceSlots(rebalancedSlotInfo.getSlotsOfTrackerSnapshot(), rebalancedSlotInfo.getTrackerOfSlotsSnapshot());
        // 发送个请求给主节点, 告诉它我已经完事了，可以对外工作了。
        RebalancedFetchMetadataCompletedEvent event = RebalancedFetchMetadataCompletedEvent.newBuilder()
                .setRebalancedNodeId(trackerIndex)
                .build();
        NetPacket nettyPacket = NetPacket.buildPacket(event.toByteArray(), PacketType.TRACKER_FETCH_SLOT_META_DATA_COMPLETED);
        trackerClusterService.send(trackerMasterIndex, nettyPacket);
        log.info("恭喜，所有节点的元数据都拉取回来了，自己可以对外工作了，同时发送请求给Controller表示自己已经完成了: [targetNodeId={}]",
                otherNodeSlots.keySet());

    }


}

