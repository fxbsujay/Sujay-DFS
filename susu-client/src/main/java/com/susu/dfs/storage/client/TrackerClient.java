package com.susu.dfs.storage.client;


import com.susu.common.model.RegisterRequest;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.task.TaskScheduler;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

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
        log.info(" Tracker Client Register : {}",request.getHostname());
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

    private void onTrackerResponse(NetPacket packet) {
        PacketType packetType = PacketType.getEnum(packet.getType());
        switch (packetType) {
            case CLIENT_REGISTER:
                log.info("===============注册成功==============");
                break;
            default:
                break;
        }
    }


}
