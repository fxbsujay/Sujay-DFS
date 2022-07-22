package com.susu.dfs.common.netty;

import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.netty.msg.NetSyncRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.SnowFlakeUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>Description: 用来处理客户端端的消息处理器</p>
 *
 * @author sujay
 * @version 17:45 2022/7/1
 */
@Slf4j
public class ClientChannelHandle extends AbstractChannelHandler {


    /**
     *  Channel Handler Context
     */
    public volatile SocketChannel socketChannel;

    /**
     * 是否有其他处理器
     */
    private volatile boolean hasOtherHandlers = false;

    /**
     * 网络包响应监听器
     */
    private List<NetPacketListener> packetListeners = new CopyOnWriteArrayList<>();

    /**
     * 网络连接状态监听器
     */
    private List<NetConnectListener> connectListeners = new CopyOnWriteArrayList<>();

    /**
     * 同步请求
     */
    private NetSyncRequest netSyncRequest;

    /**
     * 生成请求序列号的工具
     */
    private final SnowFlakeUtils snowFlakeUtils = new SnowFlakeUtils(1,1);


    public ClientChannelHandle(TaskScheduler taskScheduler) {
        netSyncRequest = new NetSyncRequest(taskScheduler);
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setHasOtherHandlers(boolean hasOtherHandlers) {
        this.hasOtherHandlers = hasOtherHandlers;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        socketChannel = (SocketChannel) ctx.channel();
        netSyncRequest.setSocketChannel(socketChannel);
        invokeConnectListener(true);
        log.info("Socket channel is connected. {}", socketChannel);
        ctx.fireChannelInactive();

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        socketChannel = null;
        invokeConnectListener(false);
        log.debug("Socket channel is disconnected！{}", ctx.channel().id().asLongText().replaceAll("-",""));
        ctx.fireChannelInactive();
    }

    /**
     * 发送消息，异步转同步获取响应
     *
     * @param packet 网络包
     * @return 响应
     * @throws IllegalStateException 网络异常
     */
    public NetPacket sendSync(NetPacket packet) {
        packet.setSequence(snowFlakeUtils.nextId());
        return netSyncRequest.sendRequest(packet);
    }

    /**
     * 发送消息
     * @param packet 数据包
     */
    public void send(NetPacket packet) {
        packet.setSequence(snowFlakeUtils.nextId());
        socketChannel.writeAndFlush(packet);
    }

    /**
     * <p>Description: 是否已经建立链接</p>
     * <p>Description: Has a link been established</p>
     *
     * @return 是否已建立了链接
     */
    public boolean isConnected() {
        return socketChannel != null;
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        synchronized (this) {
            boolean ret = netSyncRequest.onResponse(packet);
            NetRequest request = new NetRequest(ctx, packet);
            for (NetPacketListener listener : packetListeners) {
                try {
                    listener.onMessage(request);
                } catch (Exception e) {
                    log.error("Exception occur on invoke listener :", e);
                }
            }
            return !hasOtherHandlers || ret;
        }
    }

    /**
     * 回调连接监听器
     *
     * @param isConnected 是否连接上
     */
    private void invokeConnectListener(boolean isConnected) {
        for (NetConnectListener listener : connectListeners) {
            try {
                listener.onConnectStatusChanged(isConnected);
            } catch (Exception e) {
                log.error("Exception occur on invoke listener :", e);
            }
        }
    }

    @Override
    protected Set<Integer> interestPackageTypes() {
        return Collections.emptySet();
    }

    /**
     * <p>Description: 添加消息监听器</p>
     * <p>Description: Add package listener</p>
     *
     * @param listener 监听器
     */
    public void addNetPackageListener(NetPacketListener listener) {
        packetListeners.add(listener);
    }

    /**
     * <p>Description: 清空消息监听器</p>
     * <p>Description: Clear package listener</p>
     */
    public void clearNetPackageListener() {
        packetListeners.clear();
    }

    /**
     * <p>Description: 添加网络连接状态监听器</p>
     * <p>Description: Add Connect listener</p>
     */
    public void addConnectListener(NetConnectListener listener) {
        connectListeners.add(listener);
    }

    /**
     * <p>Description: 清空网络连接状态监听器</p>
     * <p>Description: Clear Connect listener</p>
     */
    public void clearConnectListener() {
        connectListeners.clear();
    }

}
