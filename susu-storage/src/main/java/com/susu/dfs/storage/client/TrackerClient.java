package com.susu.dfs.storage.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.common.model.HeartbeatResponse;
import com.susu.common.model.RegisterRequest;
import com.susu.common.model.RegisterResponse;
import com.susu.common.model.UploadCompletionRequest;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.HeartbeatTask;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.storage.server.StorageManager;
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

    private StorageManager storageManager;

    public TrackerClient(Node node, TaskScheduler taskScheduler, StorageManager storageManager) {
        this.node = node;
        this.netClient = new NetClient(node.getName(), taskScheduler);
        this.taskScheduler = taskScheduler;
        this.storageManager = storageManager;
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
        this.netClient.start(node.getTrackerHost(),node.getTrackerPort());
    }

    /**
     * 客户端向 Tracker 服务端注册
     */
    private void register() throws InterruptedException {
        RegisterRequest request = RegisterRequest.newBuilder()
                .setName(node.getName())
                .setHostname(node.getHost())
                .setPort(node.getPort()).build();
        NetPacket packet = NetPacket.buildPacket(request.toByteArray(),PacketType.STORAGE_REGISTER);
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
    private void onTrackerResponse(NetRequest request) throws Exception {
        PacketType packetType = PacketType.getEnum(request.getRequest().getType());
        switch (packetType) {
            case STORAGE_REGISTER:
                storageRegisterResponse(request);
                break;
            case STORAGE_HEART_BEAT:
                storageHeartbeatResponse(request);
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
    private void storageRegisterResponse(NetRequest request) throws InvalidProtocolBufferException {
        ChannelHandlerContext ctx = request.getCtx();
        RegisterResponse response = RegisterResponse.parseFrom(request.getRequest().getBody());
        node.setId(response.getClientId());
        if (scheduledFuture == null) {
            log.info("Start the scheduled task to send heartbeat, heartbeat interval: [interval={}ms]", Constants.HEARTBEAT_INTERVAL);
            scheduledFuture = ctx.executor().scheduleAtFixedRate(new HeartbeatTask(ctx, node),
                    0, Constants.HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * <p>Description: 处理 心跳请求 返回的消息</p>
     * <p>Description: Processing messages returned from heartbeat</p>
     *
     * @param request NetWork Request 网络请求
     */
    private void storageHeartbeatResponse(NetRequest request) throws Exception {
        HeartbeatResponse response = HeartbeatResponse.parseFrom(request.getRequest().getBody());
        if (!response.getIsSuccess()) {
            log.warn("Client heartbeat fail!! ReRegister");
            register();
        }
    }


    /**
     * <p>Description: 客户端上传文件完成，Storage将信息上报给 Tracker</p>
     * <p>Description: Processing messages returned from heartbeat</p>
     *
     * @param filename 文件名称
     * @param fileSize 文件大小
     */
    public void clientUploadCompletionRequest(String filename, long fileSize) throws InterruptedException {
        UploadCompletionRequest uploadCompletionRequest = UploadCompletionRequest.newBuilder()
                .setClientId(node.getId())
                .setFilename(filename)
                .setFileSize(fileSize)
                .build();
        NetPacket packet = NetPacket.buildPacket(uploadCompletionRequest.toByteArray(), PacketType.UPLOAD_FILE_COMPLETE);
        netClient.send(packet);
    }

}
