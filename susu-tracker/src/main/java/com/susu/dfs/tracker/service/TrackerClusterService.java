package com.susu.dfs.tracker.service;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.cluster.TrackerCluster;
import com.susu.dfs.tracker.cluster.TrackerClusterClient;
import com.susu.dfs.tracker.cluster.TrackerClusterServer;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.server.TrackerChannelHandle;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

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
     *
     * 当当前节点为master 是需要同为master节点的服务互相建立连接，但如果是slave则slave只需要连接master
     *   +----------+                +----------+                +----------+
     *   |  master  |  <------->     |  master  |   <---------   |  slave   |
     *   +----------+                +----------+                +----------+
     */
    public void start() {
        log.info("Tracker is cluster：[mode={}]", node.getIsCluster());
        if (!node.getIsCluster()) {
            return;
        }
        if (!node.getIsMaster()) {
            log.info("Start TrackerClusterService");
            for (TrackerInfo tracker : trackers) {
                connect(tracker,false);
            }
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
     * 作为客户端添加Tracker: 主动连接Tracker节点
     *
     * @param tracker 节点信息
     */
    public void connect(TrackerInfo tracker) {
        connect(tracker, false);
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
        log.info("Establish connection：[index={},hostname={},port={}]",tracker.getIndex(),tracker.getHostname(),tracker.getPort());
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
     * TODO 目前最好先启动 index 小的
     *
     * @param index         来源的tracker下标
     * @param channel       通道
     * @param tracker       节点信息
     * @param taskScheduler 任务调度器
     * @return              是否产生新的连接
     */
    public TrackerCluster addTrackerCluster(int index, ChannelHandlerContext channel, TrackerInfo tracker, TaskScheduler taskScheduler) {
        synchronized (this) {
            TrackerCluster oldCluster = clusterServerMap.get(index);
            TrackerCluster newCluster = new TrackerClusterServer(tracker,channel,node.getIndex(),index,taskScheduler);
            if (oldCluster == null) {
                log.info("Received a new Tracker connection: [trackerIndex={}]", index);
                clusterServerMap.put(index, newCluster);
                return newCluster;
            }

            if (oldCluster instanceof TrackerClusterServer && newCluster.getTargetIndex() == oldCluster.getTargetIndex()) {
                TrackerClusterServer trackerClusterServer = (TrackerClusterServer) oldCluster;
                trackerClusterServer.setSocketChannel(channel);
                log.info("Tracker Cluster reconnect, update channel: [trackerIndex={}]", oldCluster.getTargetIndex());
                return oldCluster;
            }
            clusterServerMap.put(index, newCluster);
            return oldCluster;
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

    /**
     * <p>Description: 同步广播消息给所有的Tracker节点</p>
     * <p>Description: Synchronously broadcast messages to nodes of all tracker server</p>
     *
     * @param packet        消息内容
     * @return              所进行广播消息的Tracker节点下标
     */
    public List<NetPacket> broadcastSync(NetPacket packet) {
        try {
            if (clusterServerMap.size() == 0) {
                return new ArrayList<>();
            }

            packet.setTrackerIndex(node.getIndex());

            List<NetPacket> result = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(clusterServerMap.size());

            for (TrackerCluster trackerCluster : clusterServerMap.values()) {
                taskScheduler.scheduleOnce("Broadcast TrackerCluster", () -> {
                    NetPacket response;
                    NetPacket requestCopy = NetPacket.copy(packet);
                    try {
                        response = trackerCluster.sendSync(requestCopy);
                        result.add(response);
                    } catch (Exception e) {
                        log.error("Broadcast failed, [sequence={}, e={}]", requestCopy.getSequence(), e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            return result;
        } catch (Exception e) {
            log.error("TrackerCluster#boardcast has interrupted. ", e);
            return new ArrayList<>();
        }
    }

    /**
     * <p>Description: 往其他节点发送请求</p>
     *
     * @param trackerIndex          节点标记
     * @param packet                网络包
     * @throws InterruptedException 消息发送异常
     */
    public void send(int trackerIndex,NetPacket packet) throws InterruptedException {
        TrackerCluster trackerCluster = clusterServerMap.get(trackerIndex);
        if (trackerCluster != null) {
            trackerCluster.send(packet);
        } else {
            log.warn("No tracker found by index");
        }
    }

    /**
     * 转发
     */
    public void relay(int trackerIndex, NetRequest request) throws InterruptedException {
        NetPacket packet = request.getRequest();
        long sequence = packet.getSequence();
        NetPacket response = sendSync(trackerIndex, packet);
        request.sendResponse(response,sequence);
    }

    /**
     * 同步发送
     */
    public NetPacket sendSync(int trackerIndex,NetPacket packet) throws InterruptedException {
        TrackerCluster trackerCluster = clusterServerMap.get(trackerIndex);
        if (trackerCluster != null) {
            return trackerCluster.sendSync(packet);
        } else {
            log.warn("No tracker found by index");
        }
        throw new IllegalArgumentException("Invalid TrackerIndex: " + trackerIndex);
    }

    /**
     * 收到响应结果
     */
    public boolean onResponse(NetPacket packet) {
        TrackerCluster trackerCluster = clusterServerMap.get(packet.getTrackerIndex());
        if (trackerCluster == null) {
            return false;
        }
        if (!(trackerCluster instanceof TrackerClusterServer)) {
            return false;
        }
        TrackerClusterServer server = (TrackerClusterServer) trackerCluster;
        return server.onResponse(packet);
    }

    public List<TrackerInfo> getAllTracker() {
        List<TrackerInfo> result = new ArrayList<>(clusterServerMap.size());
        for (TrackerCluster peerNameNode : clusterServerMap.values()) {
            result.add(peerNameNode.getServer());
        }
        return result;
    }

    public List<Integer> getAllTrackerIndex() {
        return new ArrayList<>(clusterServerMap.keySet());
    }

    public int getConnectedCount() {
        synchronized (this) {
            int count = 0;
            for (TrackerCluster peerNameNode : clusterServerMap.values()) {
                if (peerNameNode.isConnected()) {
                    count++;
                }
            }
            return count;
        }
    }
}
