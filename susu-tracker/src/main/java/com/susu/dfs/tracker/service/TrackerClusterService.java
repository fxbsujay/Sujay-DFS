package com.susu.dfs.tracker.service;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.cluster.TrackerCluster;
import com.susu.dfs.tracker.cluster.TrackerClusterClient;
import com.susu.dfs.tracker.cluster.TrackerClusterServer;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.server.TrackerChannelHandle;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sujay
 * <p>Description: 集群服务</p>
 * @version 13:26 2022/7/22
 */
@Slf4j
public class TrackerClusterService {

    private final TaskScheduler taskScheduler;

    private final Map<Integer, TrackerCluster> clusterServerMap = new ConcurrentHashMap<>();

    private final Node node;

    private final List<TrackerInfo> trackers;

    private TrackerChannelHandle trackerChannelHandle;

    private ServerManager serverManager;

    public TrackerClusterService(Node node, List<TrackerInfo> trackers, TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.node = node;
        this.trackers = trackers;
    }

    public void setTrackerChannelHandle(TrackerChannelHandle trackerChannelHandle) {
        this.trackerChannelHandle = trackerChannelHandle;
    }

    public void setServerManager(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /**
     * 启动服务，连接Tracker
     */
    public void start() {
        log.info("Tracker is cluster：[mode={}]", node.getIsCluster());
        if (!node.getIsCluster()) {
            return;
        }
        log.info("Start TrackerClusterService");
        for (TrackerInfo tracker : trackers) {
            connect(tracker,false);
        }
    }

    /**
     * 优雅停止
     */
    public void shutdown() {
        log.info("Shutdown TrackerClusterService");
        for (TrackerCluster cluster : clusterServerMap.values()) {
            cluster.close();
        }
    }

    /**
     * <p>Description: 连接Tracker节点</p>
     *
     * @param tracker   Tracker节点
     * @param force     是否强制重新连接
     */
    public void connect(TrackerInfo tracker,boolean force) {
        if (Objects.equals(node.getIndex(), tracker.getIndex())) {
            return;
        }
        synchronized (this) {
            TrackerCluster trackerCluster = clusterServerMap.get(tracker.getIndex());
            if (force || trackerCluster == null) {
                if (trackerCluster != null) {
                    trackerCluster.close();
                }
                NetClient netClient = new NetClient("Cluster-Client-" + tracker.getHostname(), taskScheduler);
                netClient.addHandler(trackerChannelHandle);
                TrackerCluster newTrackerCluster = new TrackerClusterClient(netClient,node.getIndex(),tracker.getIndex(),tracker);
                clusterServerMap.put(tracker.getIndex(),newTrackerCluster);
                netClient.addConnectListener(connected -> {
                    if (connected) {
                        serverManager.reportSelfInfo(newTrackerCluster,true);
                    }
                });
                netClient.start(tracker.getHostname(),tracker.getPort());
                log.info("Tracker Cluster Service connect：[hostname={}, port={}, nameNodeId={}]", tracker.getHostname(), tracker.getPort(), tracker.getIndex());
            }
        }
    }

    /**
     * <p>Description: 收到其他Tracker节点发来的请求，添加tracker节点</p>
     *
     * @param index         来源的tracker下标
     * @param selfIndex     自己的下标
     * @param channel       通道
     * @param tracker       节点信息
     * @param taskScheduler 任务调度器
     * @return              是否产生新的连接
     */
    public TrackerCluster addTrackerCluster(int index,int selfIndex, SocketChannel channel,TrackerInfo tracker,TaskScheduler taskScheduler) {
        synchronized (this) {
            TrackerCluster oldCluster = clusterServerMap.get(index);
            TrackerCluster newCluster = new TrackerClusterServer(tracker,channel,node.getIndex(),index,taskScheduler);
            if (oldCluster == null) {
                log.info("收到新的Tracker的通知网络包, 保存连接以便下一次使用: [index={}]", index);
                clusterServerMap.put(index, newCluster);
                return newCluster;
            }

            if (oldCluster instanceof TrackerClusterServer && newCluster.getTargetIndex() == oldCluster.getTargetIndex()) {
                TrackerClusterServer peerNameNodeServer = (TrackerClusterServer) oldCluster;
                peerNameNodeServer.setSocketChannel(channel);
                log.info("TrackerCluster断线重连，更新channel: [index={}]", oldCluster.getTargetIndex());
                return oldCluster;
            }

            if (selfIndex > index) {
                newCluster.close();
                connect(tracker, true);
                log.info("新的连接Tracker比较小，关闭新的连接, 并主动往小index的节点发起连接: [index={}]", newCluster.getTargetIndex());
                return null;
            }

            clusterServerMap.put(index, newCluster);
            oldCluster.close();
            log.info("新的连接Tracker index比较大，则关闭旧的连接, 并替换链接: [index={}]", oldCluster.getTargetIndex());
            return newCluster;
        }
    }
    /**
     * <p>Description: 广播消息给所有的Tracker节点</p>
     *
     * @param packet 消息内容
     */
    public List<Integer> broadcast(NetPacket packet) {
        return broadcast(packet, -1);
    }


    /**
     * <p>Description: 广播消息给所有的Tracker节点</p>
     *
     * @param packet       消息内容
     * @param excludeIndex 不给该节点发送请求
     */
    public List<Integer> broadcast(NetPacket packet, int excludeIndex) {
        return broadcast(packet, new HashSet<>(Collections.singletonList(excludeIndex)));
    }

    /**
     * <p>Description: 广播消息给所有的Tracker节点</p>
     * <p>Description: Broadcast messages to nodes of all tracker server</p>
     *
     * @param packet        消息内容
     * @param excludeIndex  排除在外的Tracker节点
     * @return              所进行广播消息的Tracker节点下标
     */
    public  List<Integer> broadcast(NetPacket packet,Set<Integer> excludeIndex) {
        try {
            List<Integer> result = new ArrayList<>();
            for (TrackerCluster trackerCluster : clusterServerMap.values()) {
                if (excludeIndex.contains(trackerCluster.getTargetIndex())) {
                    continue;
                }
                trackerCluster.send(packet);
                result.add(trackerCluster.getTargetIndex());
            }
            return result;
        } catch (Exception e) {
            log.error("Tracker Cluster Service broadcast has interrupted. ", e);
            return new ArrayList<>();
        }
    }
}
