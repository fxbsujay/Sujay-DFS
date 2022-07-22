package com.susu.dfs.tracker.cluster;


import com.susu.dfs.common.ClusterInfo;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;

/**
 * @author sujay
 * <p>Description: 集群管理客户端</p>
 * @version 13:26 2022/7/22
 */
public class TrackerClusterClient extends AbstractTrackerCluster{

    private NetClient netClient;

    public TrackerClusterClient(NetClient netClient, int currentIndex, int targetIndex, ClusterInfo cluster) {
        super(currentIndex, targetIndex, cluster);
        this.netClient = netClient;
    }

    @Override
    public void send(NetPacket packet) throws InterruptedException {
        netClient.send(packet);
    }

    @Override
    public NetPacket sendSync(NetPacket packet) throws InterruptedException {
        return netClient.sendSync(packet);
    }

    @Override
    public void close() {
        netClient.shutdown();
    }

    @Override
    public boolean isConnected() {
        return netClient.isConnected();
    }
}
