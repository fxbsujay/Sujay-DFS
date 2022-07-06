package com.susu.common.netty;

import com.susu.common.Node;
import com.susu.common.config.NodeConfig;
import com.susu.common.eum.PacketType;
import com.susu.common.model.NodeTest;
import com.susu.common.netty.msg.NetPacket;
import com.susu.common.task.TaskScheduler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
     * 任务调度器
     */
    private TaskScheduler taskScheduler;

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

    public NetClient(String name, TaskScheduler taskScheduler) {
        this(name,taskScheduler,1);
    }

    /**
     * @param name 启动的节点名称
     */
    public NetClient(String name, TaskScheduler taskScheduler, int retryTime) {
        this.name = name;
        this.retryTime = retryTime;
        loopGroup = new NioEventLoopGroup();
        baseChannelHandler = new BaseChannelHandler();
        clientChannelHandle = new ClientChannelHandle();
        baseChannelHandler.addHandler(clientChannelHandle);
        this.taskScheduler = taskScheduler;
    }

    /**
     * 启动一个客户端
     * <p>Description: Netty Client Start </p>
     * @param host 地址
     * @param port 端口号
     */
    public void start(String host, int port) {
        start(host,port,1,0);
    }

    /**
     * 启动一个客户端
     * <p>Description: Netty Client Start </p>
     * @param host 地址
     * @param port 端口号
     * @param connectTimes 当前重连次数
     * @param delay 任务启动延时时间
     */
    private void start(String host, int port, final int connectTimes,long delay) {

        taskScheduler.scheduleOnce("Netty Client Start",() -> {
            Bootstrap client = new Bootstrap()
                    .group(loopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(baseChannelHandler);
            try {
                ChannelFuture channelFuture = client.connect(new InetSocketAddress(host, port)).sync();
                ensureStart();
                if (isConnected()) {
                    Scanner scanner = new Scanner(System.in);
                    while(scanner.hasNextLine()) {
                        String msg = scanner.nextLine();
                        log.debug("user input：{}",msg);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = null;
                        try {
                            oos = new ObjectOutputStream(bos);
                            oos.writeObject(msg);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        byte[] bytes = bos.toByteArray();
                        clientChannelHandle.send(NetPacket.buildPacket(bytes, PacketType.TEST));
                    }
                }else {
                    log.info("没有连接成功");
                }
                channelFuture.channel()
                        .closeFuture()
                        .sync();
            } catch (InterruptedException e) {
                log.error("connect exception：[ex={}, started={}, name={}]", e.getMessage(), started.get(), name);
            } finally {
                int curConnectTimes = connectTimes + 1;
                reStart(host,port,curConnectTimes);
            }
        },delay);
    }

    /**
     * 尝试重启客户端
     * <p>Description: restart client </p>
     *
     * @param host 地址
     * @param port 端口号
     * @param connectTimes 当前重连次数
     */
    private void reStart(String host, int port, int connectTimes) {
        if (started.get()) {
            boolean retry = retryTime < 0 || connectTimes <= retryTime;
            if (retry) {
                log.error("client restart：[started={}, name={}]", started.get(), name);
                start(host, port, connectTimes,3000);
            } else {
                shutdown();
                log.info("The number of retry time exceeds the maximum，not longer retry：[retryTime={}]", retryTime);
            }
        }
    }

    /**
     * 同步等待确保连接已经建立。
     * 如果连接断开了，会阻塞直到连接重新建立
     * <p>
     *     Description: Sync wait to make sure the connection has been established.
     *                 If the connection is disconnected, it will block until the connection is re established
     *     Example:    timeout < 0
     * </p>
     */
    public void ensureStart() throws InterruptedException {
        ensureStart(-1);
    }

    /**
     * 确保连接成功
     * <p>Description: Ensure successful connection</p>
     *
     * @param timeout 超时时间
     * @exception InterruptedException 连接失败
     */
    public void ensureStart(int timeout) throws InterruptedException {
        int remainTimeout = timeout;
        synchronized (this) {
            while (!isConnected()) {
                if (!started.get()) {
                    throw new InterruptedException("无法连接上服务器：" + name);
                }
                if (timeout > 0) {
                    if (remainTimeout <= 0) {
                        throw new InterruptedException("无法连接上服务器：" + name);
                    }
                    wait(10);
                    remainTimeout -= 10;
                } else {
                    wait(10);
                }
            }
        }
    }

    /**
     * 是否连接上
     * @return 是否已建立了链接
     */
    public boolean isConnected() {
        return clientChannelHandle.isConnected();
    }

    /**
     * 关闭客户端
     * <p>Description: shutdown client </p>
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
        TaskScheduler taskScheduler = new TaskScheduler("Client-Scheduler",1,false);
        Node node = NodeConfig.getNode("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\config.json");
        NetClient netClient = new NetClient(node.getName(),taskScheduler);
        netClient.start(node.getHost(),node.getPort());

    }
}
