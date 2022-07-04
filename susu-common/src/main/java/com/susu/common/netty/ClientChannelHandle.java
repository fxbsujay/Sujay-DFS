package com.susu.common.netty;


import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

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
       /* for (int i = 0; i < 2; i++) {
            ByteBuf buffer = ctx.alloc().buffer();
            buffer.writeBytes(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,12});
            ctx.writeAndFlush(buffer);
        }*/
    }

    public void send(String msg) {
        socketChannel.writeAndFlush(msg);
    }
}
