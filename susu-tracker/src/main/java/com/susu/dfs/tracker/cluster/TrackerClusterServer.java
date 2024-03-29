package com.susu.dfs.tracker.cluster;

import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetSyncRequest;
import com.susu.dfs.common.task.TaskScheduler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author sujay
 * <p>Description: 集群管理服务端</p>
 * @version 11:52 2022/7/22
 */
@Slf4j
public class TrackerClusterServer extends AbstractTrackerCluster{

    private final String name;

    private volatile ChannelHandlerContext socketChannel;

    private final NetSyncRequest syncRequest;

    public TrackerClusterServer(TrackerInfo tracker, ChannelHandlerContext socketChannel, int currentIndex, int targetIndex, TaskScheduler taskScheduler) {
        super(currentIndex, targetIndex, tracker);
        this.name = "Tracker-Cluster-" + currentIndex + "-" + targetIndex;
        this.syncRequest = new NetSyncRequest(taskScheduler);
        this.setSocketChannel(socketChannel);
    }

    public void setSocketChannel(ChannelHandlerContext socketChannel) {
        synchronized (this) {
            this.socketChannel = socketChannel;
            this.syncRequest.setSocketChannel((SocketChannel) socketChannel.channel());
            notifyAll();
        }
    }

    @Override
    public void send(NetPacket packet) throws InterruptedException {
        synchronized (this) {
            while (!isConnected()) {
                try {
                    wait(10);
                } catch (InterruptedException e) {
                    log.error("Tracker Cluster Server send has Interrupted !!");
                }
            }
        }
        socketChannel.writeAndFlush(packet);
    }

    @Override
    public NetPacket sendSync(NetPacket packet) throws InterruptedException {
       return syncRequest.sendRequest(packet);
    }

    public boolean onResponse(NetPacket request) {
        return syncRequest.onResponse(request);
    }

    @Override
    public void close() {
        socketChannel.close();
    }

    @Override
    public boolean isConnected() {
        return socketChannel != null && socketChannel.channel().isActive();
    }
}
