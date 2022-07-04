package com.susu.common.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * <p>Description: Netty 的 客户端端实现 网络服务</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Slf4j
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
            Scanner scanner = new Scanner(System.in);
            while(scanner.hasNextLine()) {
                String msg = scanner.nextLine();
                log.debug("用户输入：{}",msg);
                clientChannelHandle.send("");
            }

            channelFuture.channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        NetClient netClient = new NetClient("client");
        netClient.start();
        System.out.println("====================");
    }
}
