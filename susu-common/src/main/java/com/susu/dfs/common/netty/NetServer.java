package com.susu.dfs.common.netty;

import com.susu.dfs.common.task.TaskScheduler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Description: Server NetWork</p>
 * <p>Description: Netty 的 服务端实现 网络服务</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Slf4j
public class NetServer {

    /**
     * 服务名称
     */
    private String name;

    /**
     * 任务调度器
     */
    private TaskScheduler taskScheduler;

    /**
     * 消息管理器
     */
    private BaseChannelHandler baseChannelHandler;

    private EventLoopGroup boss;

    private EventLoopGroup worker;

    /**
     * @param name 启动的节点名称
     */
    public NetServer(String name, TaskScheduler taskScheduler) {
        this.name = name;
        this.boss = new NioEventLoopGroup();
        this.worker = new NioEventLoopGroup();
        this.baseChannelHandler = new BaseChannelHandler();
        this.taskScheduler = taskScheduler;
    }

    /**
     * 异步绑定端口
     * <p>Description: start server </p>
     *
     * @param port 端口
     */
    public void startAsync(int port) {
        taskScheduler.scheduleOnce("NettyServer", () -> {
            try {
                start(Collections.singletonList(port));
            } catch (InterruptedException e) {
                log.info("NetServer internalBind is Interrupted !!");
            }
        }, 0);
    }

    /**
     * 启动服务
     * <p>Description: start server </p>
     *
     * @param ports 端口
     * @exception InterruptedException 绑定端口异常
     */
    public void start(int ports) throws InterruptedException {
        start(Collections.singletonList(ports));
    }

    /**
     * 启动服务
     * <p>Description: start server </p>
     *
     * @param ports 端口
     * @exception InterruptedException 绑定端口异常
     */
    public void start(List<Integer> ports) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(baseChannelHandler);
        List<ChannelFuture> channelFeature = new ArrayList<>();
        try {
            for (Integer port : ports) {
                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("Netty Server started on port ：{}", port);
                channelFeature.add(future);
            }
            for (ChannelFuture future : channelFeature) {
                future.channel().closeFuture().addListener((ChannelFutureListener) future1 -> future1.channel().close());
            }
            for (ChannelFuture future : channelFeature) {
                future.channel().closeFuture().sync();
            }
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    /**
     * 添加自定义的handler
     */
    public void addHandler(AbstractChannelHandler handlers) {

        baseChannelHandler.addHandler(handlers);
    }

    /**
     * 关闭服务端
     * <p>Description: shutdown server </p>
     */
    public void shutdown() {
        log.info("Shutdown NetServer : [name={}]", name);
        if (boss != null && worker != null) {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
