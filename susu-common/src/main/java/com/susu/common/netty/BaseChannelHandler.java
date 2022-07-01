package com.susu.common.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Description: 一个简单的不做任何处理的消息处理器</p>
 * @author sujay
 * @version 16:02 2022/7/1
 */
public class BaseChannelHandler extends ChannelInitializer<Channel> {

    private List<ChannelInboundHandlerAdapter> handlerList = new LinkedList<>();

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new StringDecoder());
        for (ChannelInboundHandlerAdapter handler : handlerList) {
            ch.pipeline().addLast("myHandle",handler);
        }
    }

    public void addHandler(ChannelInboundHandlerAdapter handler) {
        handlerList.add(handler);
    }
}
