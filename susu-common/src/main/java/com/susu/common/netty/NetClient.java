package com.susu.common.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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
        baseChannelHandler.addHandler(new ClientChannelHandle());
        try {
            new Bootstrap()
                    .group(new NioEventLoopGroup())
                    // 选择客户 Socket 实现类，NioSocketChannel 表示基于 NIO 的客户端实现
                    .channel(NioSocketChannel.class)
                    // ChannelInitializer 处理器（仅执行一次）
                    // 它的作用是待客户端SocketChannel建立连接后，执行initChannel以便添加更多的处理器
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            // 消息会经过通道 handler 处理，这里是将 String => ByteBuf 编码发出
                            channel.pipeline().addLast(new StringEncoder());
                        }
                    })
                    // 指定要连接的服务器和端口
                    .connect(new InetSocketAddress("localhost", 8080))
                    // Netty 中很多方法都是异步的，如 connect
                    // 这时需要使用 sync 方法等待 connect 建立连接完毕
                    .sync()
                    // 获取 channel 对象，它即为通道抽象，可以进行数据读写操作
                    .channel()
                    // 写入消息并清空缓冲区
                    .writeAndFlush("hello world");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        NetClient netClient = new NetClient("client");
        netClient.start();
    }
}
