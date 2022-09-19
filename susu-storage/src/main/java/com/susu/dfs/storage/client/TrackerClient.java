package com.susu.dfs.storage.client;

import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.dfs.common.model.*;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.file.transfer.FilePacket;
import com.susu.dfs.common.file.transfer.FileReceiveHandler;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.HeartbeatTask;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.storage.server.StorageManager;
import com.susu.dfs.storage.server.StorageTransportCallback;
import com.susu.dfs.storage.task.CommandTask;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Description: Tracker 的 通讯客户端端</p>
 *
 * @author sujay
 * @version 14:01 2022/7/8
 */
@Slf4j
public class TrackerClient {

    /**
     * 客户端心跳时间
     */
    private final int HEARTBEAT_INTERVAL;

    private final Node node;

    private NetClient netClient;

    private final TaskScheduler taskScheduler;

    private final CommandTask commandTask;

    /**
     * 用来客户端发送心跳
     */
    private ScheduledFuture<?> scheduledFuture;

    private StorageManager storageManager;

    private FileReceiveHandler fileReceiveHandler;

    public TrackerClient(SysConfig config, TaskScheduler taskScheduler, StorageManager storageManager) {
        this.node = config.getNode();
        this.HEARTBEAT_INTERVAL = config.HEARTBEAT_INTERVAL;
        this.netClient = new NetClient(node.getName(), taskScheduler);
        this.taskScheduler = taskScheduler;
        this.storageManager = storageManager;
        this.commandTask = new CommandTask(taskScheduler,this,storageManager);
    }

    public void setFileReceiveHandler(StorageTransportCallback callback) {
        this.fileReceiveHandler = new FileReceiveHandler(callback);
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
                .setHttpPort(node.getHttpPort())
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
            case UPLOAD_FILE:
                clientUploadFile(request);
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
            log.info("Start the scheduled task to send heartbeat, heartbeat interval: [interval={}ms]", HEARTBEAT_INTERVAL);
            scheduledFuture = ctx.executor().scheduleAtFixedRate(new HeartbeatTask(ctx, node),
                    0,HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        }
        storageReportInfo(request.getCtx());
    }

    /**
     * <p>Description: 注册成功后上报自身存储信息,每次上报最多1 00个文件</p>
     * <p>Description: After successful registration, report its own storage information
     *                 Up to 100 files are reported each time
     * </p>
     */
    private void storageReportInfo(ChannelHandlerContext ctx) {
        List<FileInfo> files = storageManager.getStorageInfo().getFiles();

        if (files.isEmpty()) {
            ReportStorageInfoRequest request = ReportStorageInfoRequest.newBuilder()
                    .setHostname(node.getHost())
                    .setFinished(true)
                    .build();
            NetPacket packet = NetPacket.buildPacket(request.toByteArray(), PacketType.STORAGE_REPORT_INFO);
            ctx.writeAndFlush(packet);
            return;
        }

        List<List<FileInfo>> partition = Lists.partition(files, 100);
        for (int i = 0; i < partition.size(); i++) {
            List<FileInfo> fileInfos = partition.get(i);
            List<FileMetaInfo> fileMetaInfos = fileInfos.stream()
                    .map(e -> FileMetaInfo.newBuilder()
                            .setFilename(e.getFileName())
                            .setFileSize(e.getFileSize())
                            .build())
                    .collect(Collectors.toList());
            boolean isFinish = i == partition.size() - 1;
            ReportStorageInfoRequest.Builder builder = ReportStorageInfoRequest.newBuilder()
                    .setHostname(node.getHost())
                    .addAllFileInfos(fileMetaInfos)
                    .setFinished(isFinish);
            ReportStorageInfoRequest request = builder.build();
            NetPacket packet = NetPacket.buildPacket(request.toByteArray(), PacketType.STORAGE_REPORT_INFO);
            ctx.writeAndFlush(packet);
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
        if (response.getCommandsList().isEmpty()) {
            return;
        }
        List<NetPacketCommand> commands = response.getCommandsList();
        commandTask.addCommand(commands);
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
                .setHostname(node.getHost())
                .setFilename(filename)
                .setFileSize(fileSize)
                .build();
        NetPacket packet = NetPacket.buildPacket(uploadCompletionRequest.toByteArray(), PacketType.UPLOAD_FILE_COMPLETE);
        netClient.send(packet);
    }

    public void clientRemoveCompletionRequest(String filename, long fileSize) throws InterruptedException {
        RemoveCompletionRequest removeCompletionRequest = RemoveCompletionRequest.newBuilder()
                .setHostname(node.getHost())
                .setFilename(filename)
                .setFileSize(fileSize)
                .build();
        NetPacket packet = NetPacket.buildPacket(removeCompletionRequest.toByteArray(), PacketType.REMOVE_FILE_COMPLETE);
        netClient.send(packet);
    }

    /**
     * <p>Description: 客户端上传文件</p>
     */
    private void clientUploadFile(NetRequest request) {
       FilePacket filePacket = FilePacket.parseFrom(request.getRequest().getBody());
        if (filePacket.getType() == FilePacket.HEAD) {
            log.info("Received the uploaded file from the client.....");
        }
        fileReceiveHandler.handleRequest(filePacket);
    }

}
