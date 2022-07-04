package com.susu.common.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Description: Client NetWork</p>
 * <p>Description: Netty 的 客户端端实现 网络服务</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Slf4j
public class NetClient {

    /**
     * 服务器节点名称
     * <p>Description: Client Name</p>
     */
    private String name;

    /**
     * 允许重连的阈值，小于 0 则一直尝试重连
     * <p>Description: maximum number of reconnect</p>
     */
    private int retryTime;

    /**
     * 消息管理器
     */
    private BaseChannelHandler baseChannelHandler;

    private ClientChannelHandle clientChannelHandle;

    private EventLoopGroup loopGroup;

    /**
     * 服务状态的检测，是否重连，是否停机
     */
    private AtomicBoolean started = new AtomicBoolean(true);

    /**
     * @param name 启动的节点名称
     */
    public NetClient(String name) {
        this.name = name;
        this.retryTime = -1;
        loopGroup = new NioEventLoopGroup();
        baseChannelHandler = new BaseChannelHandler();
        clientChannelHandle = new ClientChannelHandle();
        baseChannelHandler.addHandler(clientChannelHandle);
    }

    /**
     * 启动一个客户端
     * @param host 地址
     * @param port 端口号
     */
    public void start(String host, int port) {
        start(host,port,1);
    }

    /**
     * 启动一个客户端
     * @param host 地址
     * @param port 端口号
     * @param connectTimes 当前重连次数
     */
    private void start(String host, int port, final int connectTimes) {
        Bootstrap client = new Bootstrap()
                .group(loopGroup)
                .channel(NioSocketChannel.class)
                .handler(baseChannelHandler);
        try {
            ChannelFuture channelFuture = client.connect(new InetSocketAddress(host, port)).sync();
            Scanner scanner = new Scanner(System.in);
            while(scanner.hasNextLine()) {
                String msg = scanner.nextLine();
                log.debug("用户输入：{}",msg);
                clientChannelHandle.send(msg);
            }
            channelFuture.channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            log.error("连接异常：[ex={}, started={}, name={}]", e.getMessage(), started.get(), name);
        } finally {
            int curConnectTimes = connectTimes + 1;
            reStart(host,port,curConnectTimes);
        }
    }

    /**
     * 尝试重启客户端
     * @param host 地址
     * @param port 端口号
     * @param connectTimes 当前重连次数
     */
    private void reStart(String host, int port, int connectTimes) {
        if (started.get()) {
            boolean retry = retryTime < 0 || connectTimes <= retryTime;
            if (retry) {
                log.error("重新发起连接：[started={}, name={}]", started.get(), name);
                start(host, port, connectTimes);
            } else {
                shutdown();
                log.info("重试次数超出阈值，不再进行重试：[retryTime={}]", retryTime);
            }
        }
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        if(log.isDebugEnabled()) {
            log.debug("Shutdown NetClient : [name={}]", name);
        }
        started.set(false);

        if (loopGroup != null) loopGroup.shutdownGracefully();
    }

    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }

    public static void main(String[] args) {
        NetClient netClient = new NetClient("client");
        netClient.start("localhost",8080);
        System.out.println("====================");
    }
}
