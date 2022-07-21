package com.susu.dfs.common.task;

import com.susu.common.model.HeartbeatRequest;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 往NameNode发送心跳的请求
 *
 * @author Sun Dasheng
 */
@Slf4j
public class HeartbeatTask implements Runnable {

    private Node node;

    private ChannelHandlerContext ctx;

    public HeartbeatTask(ChannelHandlerContext ctx, Node node) {
        this.ctx = ctx;
        this.node = node;
    }

    @Override
    public void run() {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setClientId(node.getId())
                .build();
        // 发送心跳请求
        NetPacket nettyPacket = NetPacket.buildPacket(request.toByteArray(), PacketType.STORAGE_HEART_BEAT);
        ctx.writeAndFlush(nettyPacket);
    }
}