package com.susu.dfs.tracker.server;

import com.susu.common.model.TrackerAwareRequest;
import com.susu.common.model.TrackerNode;
import com.susu.dfs.common.Constants;
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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private AtomicInteger trackerSize;

    private Node node;

    private List<TrackerInfo> nodes;

    private TrackerSlot trackerSlot;

    private AtomicInteger trackerCount = new AtomicInteger(0);

    private AtomicBoolean startSlotElection = new AtomicBoolean(false);

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
        this.trackerSize = new AtomicInteger(nodes.size());
        this.trackerSlot = new TrackerSlotLocal(node.getIndex(),clientManager,trackerClusterService,trackerFileService);
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

    public void onRebalancedSlots(NetRequest request) throws Exception {
        if (trackerSlot == null) {
            log.info("没有找到主节点");
            return;
        }
        if (trackerSlot instanceof TrackerSlotRemote) {
            log.info("我不是主节点, 要重新分配Slots别找我");
            return;
        }
        // trackerSlot.rebalanceSlots(request.getRequest());
    }

}
