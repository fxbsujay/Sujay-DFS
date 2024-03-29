package com.susu.dfs.common.netty;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.netty.msg.NetPacketDecoder;
import com.susu.dfs.common.netty.msg.NetPacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Description: 一个简单的不做任何处理的消息处理器</p>
 * @author sujay
 * @version 16:02 2022/7/1
 */
@ChannelHandler.Sharable
public class BaseChannelHandler extends ChannelInitializer<SocketChannel> {

    /**
     * 添加其他处理器的数组
     */
    private List<AbstractChannelHandler> handlerList = new LinkedList<>();

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new NetPacketDecoder(Constants.MAX_BYTES),
                new LengthFieldPrepender(3),
                new NetPacketEncoder()
        );
        for (AbstractChannelHandler handler : handlerList) {
            ch.pipeline().addLast(handler);
        }
    }

    public void addHandler(AbstractChannelHandler handler) {
        handlerList.add(handler);
    }
}
