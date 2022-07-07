package com.susu.dfs.common.netty;

import com.susu.dfs.common.netty.msg.NetPacket;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>Description: 用来处理客户端端的消息处理器</p>
 * @author sujay
 * @version 17:45 2022/7/1
 */
@Slf4j
public class ClientChannelHandle extends AbstractChannelHandler {

    public volatile ChannelHandlerContext socketChannel;

    /**
     * 网络包响应监听器
     */
    private List<NetPacketListener> packetListeners = new CopyOnWriteArrayList<>();

    /**
     * 网络连接状态监听器
     */
    private List<NetConnectListener> connectListeners = new CopyOnWriteArrayList<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        socketChannel = ctx;
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
     * 发送消息
     * @param packet 数据包
     */
    public void send(NetPacket packet) {
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
            for (NetPacketListener listener : packetListeners) {
                try {
                    listener.onMessage(packet);
                } catch (Exception e) {
                    log.error("Exception occur on invoke listener :", e);
                }
            }
        }
        return true;
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