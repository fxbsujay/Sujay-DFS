package com.susu.common.netty;

import com.susu.common.Constants;
import com.susu.common.netty.msg.NetPacketDecoder;
import com.susu.common.netty.msg.NetPacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
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

    /**
     * 添加其他处理器的数组
     */
    private List<AbstractChannelHandler> handlerList = new LinkedList<>();

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline().addLast(new NetPacketDecoder(Constants.MAX_BYTES));
        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        ch.pipeline().addLast(new NetPacketEncoder());
        for (AbstractChannelHandler handler : handlerList) {
            ch.pipeline().addLast("myHandle",handler);
        }
    }

    public void addHandler(AbstractChannelHandler handler) {
        handlerList.add(handler);
    }
}
