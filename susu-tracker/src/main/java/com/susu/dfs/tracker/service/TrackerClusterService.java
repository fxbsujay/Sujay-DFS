package com.susu.dfs.tracker.service;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.ClusterInfo;
import com.susu.dfs.tracker.cluster.TrackerCluster;
import com.susu.dfs.tracker.cluster.TrackerClusterClient;
import com.susu.dfs.tracker.server.TrackerChannelHandle;
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

    private final List<ClusterInfo> clusters;

    private TrackerChannelHandle trackerChannelHandle;

    public TrackerClusterService(Node node,List<ClusterInfo> clusters, TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.node = node;
        this.clusters = clusters;
    }

    public void setTrackerChannelHandle(TrackerChannelHandle trackerChannelHandle) {
        this.trackerChannelHandle = trackerChannelHandle;
    }

    /**
     * 启动服务，连接Tracker
     */
    public void start() {
        for (ClusterInfo cluster : clusters) {
            connect(cluster,false);
        }
    }

    /**
     * <p>Description: 连接Tracker节点</p>
     *
     * @param cluster   Tracker节点
     * @param force     是否强制重新连接
     */
    public void connect(ClusterInfo cluster,boolean force) {
        if (Objects.equals(node.getIndex(), cluster.getIndex())) {
            return;
        }
        synchronized (this) {
            TrackerCluster trackerCluster = clusterServerMap.get(cluster.getIndex());
            if (force || trackerCluster == null) {
                if (trackerCluster != null) {
                    trackerCluster.close();
                }
                NetClient netClient = new NetClient("Cluster-Client-" + cluster.getHostname(), taskScheduler);
                netClient.addHandler(trackerChannelHandle);
                TrackerCluster newTrackerCluster = new TrackerClusterClient(netClient,node.getIndex(),cluster.getIndex(),cluster);
                clusterServerMap.put(cluster.getIndex(),newTrackerCluster);
                netClient.addConnectListener(connected -> {
                    if (connected) {
                        // TODO
                    }
                });
                netClient.start(cluster.getHostname(),cluster.getPort());
                log.info("Tracker Cluster Service connect：[hostname={}, port={}, nameNodeId={}]", cluster.getHostname(), cluster.getPort(), cluster.getIndex());
            }
        }
    }
}
