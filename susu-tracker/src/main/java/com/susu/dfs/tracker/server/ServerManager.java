package com.susu.dfs.tracker.server;

import com.susu.common.model.TrackerAwareRequest;
import com.susu.common.model.TrackerNode;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.cluster.TrackerCluster;
import com.susu.dfs.tracker.service.TrackerClusterService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sujay
 * <p>Description: 服务端管理器</p>
 * @version 0:04 2022/7/8
 */
@Slf4j
public class ServerManager {

    private TaskScheduler taskScheduler;

    private TrackerClusterService trackerClusterService;

    private AtomicInteger numOfNode;

    private Node node;

    private List<TrackerInfo> nodes;

    public ServerManager(Node node, List<TrackerInfo> nodes, TrackerClusterService trackerClusterService) {
        this.node = node;
        this.nodes = nodes;
        this.trackerClusterService = trackerClusterService;
        this.trackerClusterService.setServerManager(this);
    }

    /**
     * <p>Description: 上报信息给其他Tracker节点</p>
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
        log.info("建立了Tracker的连接, 发送自身信息：[index={}, targetIndex={}]",node.getIndex(), trackerCluster.getTargetIndex());
    }




}
