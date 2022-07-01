package com.susu.common.netty;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: 用来处理客户端端的消息处理器</p>
 * @author sujay
 * @version 17:45 2022/7/1
 */
@Slf4j
public class ClientChannelHandle extends ChannelInboundHandlerAdapter {

    private volatile SocketChannel socketChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        socketChannel = (SocketChannel) ctx.channel();
        log.info("Socket channel is connected. {}", socketChannel.id());
        socketChannel.writeAndFlush("AAAAAAAAAAA");
        ctx.fireChannelActive();
    }


    public void send(String msg) {
        log.info("发送消息：{}",msg);
        log.info("A----------{}",socketChannel);
        socketChannel.writeAndFlush(msg);
    }
}
