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
public class ClientChannelHandle extends ChannelInboundHandlerAdapter {

    public volatile ChannelHandlerContext socketChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        socketChannel = ctx;
        log.info("Socket channel is connected. {}", socketChannel);
    }

    public void send(String msg) {
        socketChannel.writeAndFlush(msg);
    }
}
