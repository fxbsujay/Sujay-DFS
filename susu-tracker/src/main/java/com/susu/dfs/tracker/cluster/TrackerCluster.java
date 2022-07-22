package com.susu.dfs.tracker.cluster;

import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.netty.msg.NetPacket;

/**
 * @author sujay
 * <p>Description: 集群管理服务API</p>
 * @version 11:52 2022/7/22
 */
public interface TrackerCluster {

    /**
     * <p>Description: 往 Tracker Cluster 发送网络包, 如果连接断开了，会同步等待连接重新建立</p>
     *
     * @param packet 网络包
     * @throws InterruptedException 中断异常
     */
    void send(NetPacket packet) throws InterruptedException;

    /**
     * <p>Description: 往 Tracker Cluster 发送网络包, 同步发送</p>
     *
     * @param packet 网络包
     * @return 响应
     * @throws InterruptedException    中断异常
     */
    NetPacket sendSync(NetPacket packet) throws InterruptedException;

    /**
     * 关闭连接
     */
    void close();

    /**
     * 获取节点下标
     */
    int getTargetIndex();

    /**
     * 获取服务连接的IP和端口号
     */
    TrackerInfo getServer();

    /**
     * 是否连接上
     */
    boolean isConnected();

}
