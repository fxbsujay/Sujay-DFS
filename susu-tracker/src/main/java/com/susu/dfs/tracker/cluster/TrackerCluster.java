package com.susu.dfs.tracker.cluster;

import com.susu.dfs.common.ClusterInfo;
import com.susu.dfs.common.netty.msg.NetPacket;

/**
 * @author sujay
 * <p>Description: 集群管理服务API</p>
 * @version 11:52 2022/7/22
 */
public interface TrackerCluster {

    /**
     * 往 PeerNameNode发送网络包, 如果连接断开了，会同步等待连接重新建立
     *
     * @param packet 网络包
     * @throws InterruptedException 中断异常
     */
    void send(NetPacket packet) throws InterruptedException;

    /**
     * 往 PeerNameNode发送网络包, 同步发送
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
     *
     * @return index
     */
    int getTargetIndex();

    /**
     * 获取服务连接的IP和端口号
     *
     * @return IP 和端口号
     */
    ClusterInfo getServer();

    /**
     * 是否连接上
     *
     * @return 是否连接上
     */
    boolean isConnected();

}
