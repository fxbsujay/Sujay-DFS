package com.susu.common.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

/**
 * <p>Description: Netty 的 客户端端实现 网络服务</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
public class NetClient {

    private String name;

    /**
     * @param name 启动的节点名称
     */
    public NetClient(String name) {
        this.name = name;

    }

    public void start() {
        BaseChannelHandler baseChannelHandler = new BaseChannelHandler();
        ClientChannelHandle clientChannelHandle = new ClientChannelHandle();
        baseChannelHandler.addHandler(clientChannelHandle);
        try {
            ChannelFuture channelFuture = new Bootstrap()
                    .group(new NioEventLoopGroup())
                    .channel(NioSocketChannel.class)
                    .handler(baseChannelHandler)
                    // 指定要连接的服务器和端口
                    .connect(new InetSocketAddress("localhost", 8080))
                    .sync();
            // 获取 channel 对象，它即为通道抽象，可以进行数据读写操作
            Channel channel = channelFuture.channel();
            // 写入消息并清空缓冲区
            channel.writeAndFlush("hello world");
            channel.writeAndFlush("aaaaa");
            clientChannelHandle.send("bbbbb");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        NetClient netClient = new NetClient("client");
        netClient.start();
    }
}
