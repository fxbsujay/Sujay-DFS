package com.susu.dfs.storage.client;


import com.susu.common.model.RegisterRequest;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.HeartbeatTask;
import com.susu.dfs.common.task.TaskScheduler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * <p>Description: Tracker 的 通讯客户端端</p>
 *
 * @author sujay
 * @version 14:01 2022/7/8
 */
@Slf4j
public class TrackerClient {

    private final Node node;

    private NetClient netClient;

    private final TaskScheduler taskScheduler;

    /**
     * 用来客户端发送心跳
     */
    private ScheduledFuture<?> scheduledFuture;

    private static final int HEARTBEAT_INTERVAL = 30000;

    public TrackerClient(Node node, TaskScheduler taskScheduler) {
        this.node = node;
        this.netClient = new NetClient(node.getName(), taskScheduler);
        this.taskScheduler = taskScheduler;
    }

    /**
     * 启动服务
     */
    public void start() {
        this.netClient.addPackageListener(this::onTrackerResponse);
        this.netClient.addConnectListener( isConnected -> {
            log.info("Tracker Client Connect Start : {}",isConnected);
            if (isConnected) {
                register();
            } else {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                    scheduledFuture = null;
                }
            }
        });
        this.netClient.addClientFailListener(() -> {
            log.info("Tracker Server Down !!");
        });
        this.netClient.start(node.getHost(),node.getPort());
    }

    /**
     * 客户端向 Tracker 服务端注册
     */
    private void register() throws InterruptedException {
        RegisterRequest request = RegisterRequest.newBuilder()
                .setClientId(1111)
                .setHostname(node.getHost())
                .setName(node.getName())
                .setPort(node.getPort()).build();
        NetPacket packet = NetPacket.buildPacket(request.toByteArray(),PacketType.CLIENT_REGISTER);
        log.info("Tracker Client Register : {}",request.getHostname());
        netClient.send(packet);
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        if (netClient != null) {
            netClient.shutdown();
        }
    }

    /**
     * <p>Description: 处理 Tracker Server 返回的信息</p>
     * <p>Description: Processing requests returned by Tracker Server</p>
     *
     * @param request NetWork Request 网络请求
     */
    private void onTrackerResponse(NetRequest request) {
        PacketType packetType = PacketType.getEnum(request.getRequest().getType());
        switch (packetType) {
            case CLIENT_REGISTER:
                clientRegisterResponse(request);
                break;
            default:
                break;
        }
    }

    /**
     * <p>Description: 处理 注册 返回的消息</p>
     * <p>Description: Processing messages returned from registration</p>
     *
     * @param request NetWork Request 网络请求
     */
    private void clientRegisterResponse(NetRequest request) {
        ChannelHandlerContext ctx = request.getCtx();
        if (scheduledFuture == null) {
            log.info("Start the scheduled task to send heartbeat, heartbeat interval: [interval={}ms]", HEARTBEAT_INTERVAL);
            scheduledFuture = ctx.executor().scheduleAtFixedRate(new HeartbeatTask(ctx, node),
                    0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }



}
