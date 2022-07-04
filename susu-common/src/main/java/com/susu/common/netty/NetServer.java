package com.susu.common.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: Server NetWork</p>
 * <p>Description: Netty 的 服务端实现 网络服务</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Slf4j
public class NetServer {

    private String name;

    /**
     * @param name 启动的节点名称
     */
    public NetServer(String name) {
       this.name = name;

    }

    /**
     * 启动服务
     */
    public void start() {
        BaseChannelHandler baseChannelHandler = new BaseChannelHandler();
        ServerChannelHandler serverChannelHandler = new ServerChannelHandler();
        baseChannelHandler.addHandler(serverChannelHandler);
        try {
            new ServerBootstrap()
                    .group(new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .childHandler(baseChannelHandler).bind(8080).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        NetServer netServer = new NetServer("server");
        netServer.start();
    }
}
