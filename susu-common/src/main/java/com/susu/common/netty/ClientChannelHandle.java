package com.susu.common.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: 用来处理客户端端的消息处理器</p>
 * @author sujay
 * @version 17:45 2022/7/1
 */
@Slf4j
public class ClientChannelHandle extends AbstractChannelHandler {

    public volatile ChannelHandlerContext socketChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        socketChannel = ctx;
        log.info("Socket channel is connected. {}", socketChannel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        socketChannel = null;
        log.debug("Socket channel is disconnected！{}", ctx.channel().id().asLongText().replaceAll("-",""));
        ctx.fireChannelInactive();
    }

    public void send(String msg) {
        socketChannel.writeAndFlush(msg);
    }

    /**
     * 是否已经建立链接
     * @return 是否已建立了链接
     */
    public boolean isConnected() {
        return socketChannel != null;
    }
}
