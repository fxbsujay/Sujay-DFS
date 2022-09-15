package com.susu.dfs.tracker.server;

import com.susu.dfs.common.model.*;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.cluster.TrackerCluster;
import com.susu.dfs.tracker.service.TrackerFileService;
import com.susu.dfs.tracker.slot.OnSlotCompletedListener;
import com.susu.dfs.tracker.slot.TrackerSlot;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.slot.TrackerSlotLocal;
import com.susu.dfs.tracker.slot.TrackerSlotRemote;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sujay
 * <p>Description: 服务端管理器 contrller</p>
 * @version 0:04 2022/7/8
 */
@Slf4j
public class ServerManager {

    private TaskScheduler taskScheduler;

    private TrackerClusterService trackerClusterService;

    private TrackerFileService trackerFileService;

    private AtomicInteger trackerSize;

    private Node node;

    private List<TrackerInfo> nodes;

    private ClientManager clientManager;

    private TrackerSlot trackerSlot;

    private AtomicInteger trackerCount = new AtomicInteger(0);

    private List<OnSlotCompletedListener> slotCompletedListeners = new ArrayList<>();

    /**
     * 1. 服务之间互相连接
     * 2. 选举主节点，负责分配slot以及同步信息
     * 3. 向主节点发送自身信息
     * 4. 主节点收到信息，开始分配slot
     * 5. 有新节点加入则开始重新分配slot
     * 6. 服务之间广播消息和接收广播消息
     * 7. 同步目录树等等
     */

    public ServerManager(Node node, List<TrackerInfo> nodes, ClientManager clientManager, TrackerClusterService trackerClusterService, TrackerFileService trackerFileService) {
        this.node = node;
        this.nodes = nodes;
        this.trackerClusterService = trackerClusterService;
        this.trackerClusterService.setServerManager(this);
        this.trackerFileService = trackerFileService;
        this.trackerSize = new AtomicInteger(nodes.size());
        this.clientManager = clientManager;

    }

    /**
     * <p>Description: 上报自身信息给其他 Tracker节点</p>
     *
     * @param trackerCluster        Tracker
     * @throws InterruptedException 发送异常
     */
    public void reportSelfInfo(TrackerCluster trackerCluster,boolean isClient) throws InterruptedException {

        TrackerNode trackerNode = TrackerNode.newBuilder()
                .setIndex(node.getIndex())
                .setHostname(node.getHost())
                .setPort(node.getPort())
                .build();
        List<TrackerNode> trackerNodes = new ArrayList<>();
        for (TrackerInfo tracker : nodes) {
            TrackerNode trackerItem = TrackerNode.newBuilder()
                    .setIndex(tracker.getIndex())
                    .setHostname(tracker.getHostname())
                    .setPort(tracker.getPort())
                    .build();
            trackerNodes.add(trackerItem);
        }
        TrackerAwareRequest awareRequest = TrackerAwareRequest.newBuilder()
                .setIndex(node.getIndex())
                .setNode(trackerNode)
                .addAllNodes(trackerNodes)
                .setIsClient(isClient)
                .build();
        NetPacket packet = NetPacket.buildPacket(awareRequest.toByteArray(), PacketType.TRACKER_SERVER_AWARE);

        trackerCluster.send(packet);
        log.info("发送自身信息：[index={}, targetIndex={},isClient={}]",node.getIndex(), trackerCluster.getTargetIndex(),isClient);
    }

    /**
     * <p>Description: 接收其他 Tracker节点 信息</p>
     *
     * @param request       请求包
     * @throws Exception    slot初始化异常
     */
    public void receiveSelfInf(TrackerAwareRequest request) throws Exception {
        trackerCount.incrementAndGet();
        trackerSize.set(Math.max(trackerSize.get(),request.getTrackerSize()));
        log.info("收到Tracker发送过来的信息：[nodeId={}, curNumOfNum={}, peerNodeNum={}]",
                request.getIndex(), trackerSize.get(), trackerClusterService.getConnectedCount());
        if (trackerClusterService.getConnectedCount() == trackerSize.get() - 1) {
            initMaster();
            initSlots();
        }
    }

    /**
     * <p>Description: Slot初始化操作</p>
     * @throws Exception    slot初始化异常
     */
    private void initSlots() throws Exception {
        Map<Integer, Integer> slots = trackerSlot.initSlots();
        for (OnSlotCompletedListener listener : slotCompletedListeners) {
            listener.onCompleted(slots);
            trackerSlot.addOnSlotCompletedListener(listener);
        }
    }

    /**
     * <p>Description: 主节点初始化操作,如果不是主节点，则申请重新分配槽位</p>
     */
    private void initMaster() throws Exception {

        if (node.getIsMaster()) {
            this.trackerSlot = new TrackerSlotLocal(node.getIndex(),clientManager,trackerClusterService,trackerFileService);
            return;
        }

        int masterIndex = node.getIndex();
        for (TrackerInfo trackerInfo : nodes) {
            if (TrackerInfo.ROLE_MASTER.equals(trackerInfo.getRole())) {
                masterIndex = trackerInfo.getIndex();
                break;
            }
        }

        if (masterIndex == node.getIndex()) {
            // TODO 发起投票，进行选举
        }

        this.trackerSlot = new TrackerSlotRemote(node.getIndex(),masterIndex,clientManager,trackerClusterService,trackerFileService);
        RebalancedSlotsRequest rebalancedSlotsRequest = RebalancedSlotsRequest.newBuilder()
                .setRebalancedNodeId(node.getIndex())
                .build();

        NetPacket packet = NetPacket.buildPacket(rebalancedSlotsRequest.toByteArray(), PacketType.TRACKER_RE_BALANCE_SLOTS);
        trackerSlot.rebalancedSlots(packet);
    }

    /**
     * <p>Description: 根据文件名来分配又哪里 Tracker 节点处理请求</p>
     *
     * @param filename  文件名
     * @return          Tracker 节点
     */
    public int getTrackerIndexByFilename(String filename) {
        if (node.getIsCluster()) {
            int slot = StringUtils.hash(filename, Constants.SLOTS_COUNT);
            return trackerSlot.getTrackerIndexBySlot(slot);
        }
        return node.getIndex();
    }

    /**
     * 是否上传文件在当前节点
     */
    public boolean isCurrentTracker(int index) {
        return index == node.getIndex();
    }

    /**
     * <p>Description: 添加Slot分配成功监听器</p>
     */
    public void addOnSlotCompletedListener(OnSlotCompletedListener listener) {
        this.slotCompletedListeners.add(listener);
    }

    /**
     * 收到重新分配Slots的请求
     */
    public void onRebalancedSlots(NetRequest request) throws Exception {
        if (trackerSlot == null) {
            log.info("没有找到主节点");
            return;
        }
        if (trackerSlot instanceof TrackerSlotRemote) {
            log.info("我不是主节点, 要重新分配Slots别找我");
            return;
        }
        trackerSlot.rebalancedSlots(request.getRequest());
    }

    /**
     * 收到Slot响应
     */
    public void onReceiveSlots(NetRequest request) throws Exception {
        TrackerSlots slots = TrackerSlots.parseFrom(request.getRequest().getBody());
        ensureControllerExists();
        trackerSlot.onReceiveSlots(slots);
    }

    /**
     * 等待主节点的选举成功
     */
    private void ensureControllerExists() throws InterruptedException {
        while (trackerSlot == null) {
            synchronized (this) {
                wait(10);
            }
        }
    }

    /**
     * 将元数据发送给其他Tracker
     */
    public void writeMetadataToTracker(NetRequest request) throws Exception {
        FetchMetaDataRequest fetchMetaDataRequest = FetchMetaDataRequest.parseFrom(request.getRequest().getBody());
        List<Integer> slotsList = fetchMetaDataRequest.getSlotsList();
        FetchMetaDataResponse.Builder builder = FetchMetaDataResponse.newBuilder()
                .setNodeId(node.getIndex());
        for (Integer slot : slotsList) {
            Set<Metadata> filesBySlot = trackerFileService.getFilesBySlot(slot);
            for (Metadata metadata : filesBySlot) {
                FileInfo fileStorage = clientManager.getFileStorage(metadata.getFileName());
                if (fileStorage != null) {
                    metadata = Metadata.newBuilder(metadata)
                            .setHostname(fileStorage.getHostname())
                            .setFileSize(fileStorage.getFileSize())
                            .build();
                    builder.addFiles(metadata);
                }
                if (builder.getFilesCount() >= 500) {
                    builder.setCompleted(false);
                    NetPacket response = NetPacket.buildPacket(builder.build().toByteArray(), PacketType.TRACKER_FETCH_SLOT_META_DATA_RESPONSE);
                    request.sendResponse(response, null);
                    log.info("往别的NameNode发送不属于自己Slot的元数据：[targetNodeId={}, size={}]",
                            fetchMetaDataRequest.getNodeId(), builder.getFilesCount());
                    builder = FetchMetaDataResponse.newBuilder().setNodeId(node.getIndex());
                }
            }
        }
        builder.setCompleted(true);
        NetPacket response = NetPacket.buildPacket(builder.build().toByteArray(), PacketType.TRACKER_FETCH_SLOT_META_DATA_RESPONSE);
        request.sendResponse(response, null);
        log.info("往别的NameNode发送不属于自己Slot的元数据：[size={}]", builder.getFilesCount());
    }

    /**
     * 抓取到其他节点的元数据信息
     */
    public void onFetchMetadata(NetRequest request) throws Exception {
        trackerSlot.onFetchMetadata(request);
    }

    /**
     * 主节点收到申请重平衡的节点已经完成重平衡的通知，
     */
    public void onLocalControllerFetchSlotMetadataCompleted(NetRequest request) throws Exception {
        RebalancedFetchMetadataCompletedEvent rebalancedFetchMetadataCompletedEvent =
                RebalancedFetchMetadataCompletedEvent.parseFrom(request.getRequest().getBody());
        trackerSlot.onFetchSlotMetadataCompleted(rebalancedFetchMetadataCompletedEvent.getRebalancedNodeId());
    }

    /**
     * 普通节点收到主节点节点的完成拉取元数据广播
     */
    public void onRemoteControllerFetchSlotMetadataCompleted(NetRequest request) throws Exception {
        RebalancedFetchMetadataCompletedEvent event = RebalancedFetchMetadataCompletedEvent.parseFrom(request.getRequest().getBody());
        trackerSlot.onFetchSlotMetadataCompleted(event.getRebalancedNodeId());
    }

    /**
     * 主节点节点收到其他所有的Tracker节点的删除元数据完成的请求
     *
     * @param request 网络包
     */
    public void onRemoveMetadataCompleted(NetRequest request) throws Exception {
        trackerSlot.onRemoveMetadataCompleted(request.getRequest());
    }
}
