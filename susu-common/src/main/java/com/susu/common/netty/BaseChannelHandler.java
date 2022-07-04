package com.susu.common.netty;

import com.susu.common.netty.msg.MessageCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 8, 4, 0, 0));
        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        ch.pipeline().addLast(new MessageCodec());
        for (ChannelInboundHandlerAdapter handler : handlerList) {
            ch.pipeline().addLast("myHandle",handler);
        }
    }

    public void addHandler(ChannelInboundHandlerAdapter handler) {
        handlerList.add(handler);
    }
}
