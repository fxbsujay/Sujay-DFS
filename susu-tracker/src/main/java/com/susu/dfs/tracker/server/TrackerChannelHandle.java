package com.susu.dfs.tracker.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.common.model.*;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.eum.CommandType;
import com.susu.dfs.common.file.FileNode;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.SnowFlakeUtils;
import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.common.netty.AbstractChannelHandler;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.tracker.cluster.TrackerCluster;
import com.susu.dfs.tracker.rebalance.RemoveReplicaTask;
import com.susu.dfs.tracker.rebalance.ReplicaTask;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Description: Tracker 的 通讯服务</p>
 *
 * @author sujay
 * @version 15:19 2022/7/7
 */
@Slf4j
public class TrackerChannelHandle extends AbstractChannelHandler {

    private final ThreadPoolExecutor executor;

    private final TaskScheduler taskScheduler;

    /**
     *  Tracker 客户端管理器 保存注册进来的客户端信息
     */
    private final ClientManager clientManager;

    private final ServerManager serverManager;

    private final TrackerFileService trackerFileService;

    private final TrackerClusterService trackerClusterService;

    /**
     * 给注册进来的客户端分配id
     */
    private final SnowFlakeUtils snowFlakeUtils = new SnowFlakeUtils(1,1);

    public TrackerChannelHandle(TaskScheduler taskScheduler,
                                ClientManager clientManager, ServerManager serverManager,
                                TrackerFileService trackerFileService, TrackerClusterService trackerClusterService) {
        this.taskScheduler = taskScheduler;
        this.executor = new ThreadPoolExecutor(Constants.HANDLE_THREAD_EXECUTOR_CORE_SIZE,Constants.HANDLE_THREAD_EXECUTOR_CORE_SIZE_MAX,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(Constants.HANDLE_THREAD_EXECUTOR_QUEUE_SIZE_MAX));
        this.clientManager = clientManager;
        this.serverManager = serverManager;

        this.trackerFileService = trackerFileService;
        this.trackerClusterService = trackerClusterService;
        this.serverManager.addOnSlotCompletedListener(slots -> {
            log.info("slot 初始化已经完成了");
        });
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        NetRequest request = new NetRequest(ctx, packet);
        switch (packetType) {
            case STORAGE_REGISTER:
                storageRegisterHandle(request);
                break;
            case STORAGE_HEART_BEAT:
                storageHeartbeatHandel(request);
                break;
            case MKDIR:
                clientMkdirHandel(request);
                break;
            case CREATE_FILE:
                clientCreateFileHandel(request);
                break;
            case UPLOAD_FILE_COMPLETE:
                clientUploadFileComplete(request);
                break;
            case UPLOAD_FILE_CONFIRM:
                clientUploadFileConfirm(request);
                break;
            case READ_ATTR:
                clientReadAttrHandel(request);
                break;
            case TRACKER_SERVER_AWARE:
                trackerServerAware(request);
                break;
            default:
                break;
        }
        return false;
    }


    @Override
    protected Set<Integer> interestPackageTypes() {
        return new HashSet<>();
    }

    @Override
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * <p>Description: Storage注册请求处理</p>
     * <p>Description: Storage registration request processing </p>
     *
     * @param request NetWork Request 网络请求
     */
    private void storageRegisterHandle(NetRequest request) throws InvalidProtocolBufferException {
        long clientId = snowFlakeUtils.nextId();
        RegisterRequest registerRequest = RegisterRequest.parseFrom(request.getRequest().getBody());
        boolean register = clientManager.register(registerRequest,clientId);
        if (register) {
            RegisterResponse response = RegisterResponse.newBuilder().setClientId(clientId).build();
            request.sendResponse(response);
        }
    }

    /**
     * <p>Description: Storage心跳请求处理</p>
     * <p>Description: Storage heartbeat request processing </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void storageHeartbeatHandel(NetRequest request) throws InvalidProtocolBufferException {
        HeartbeatRequest heartbeatRequest = HeartbeatRequest.parseFrom(request.getRequest().getBody());
        Boolean isSuccess = clientManager.heartbeat(heartbeatRequest.getHostname());
        if (!isSuccess) {
            throw new RuntimeException("Client heartbeat update failed");
        }
        ClientInfo client = clientManager.getClientByHost(heartbeatRequest.getHostname());
        List<ReplicaTask> replicaTask = client.pollReplicaTask(100);
        List<NetPacketCommand> commandList = new LinkedList<>();
        if (!replicaTask.isEmpty()) {
            List<NetPacketCommand> commands = replicaTask.stream()
                    .map(item -> {
                        ClientInfo clientInfo = clientManager.getClientByHost(item.getHostname());
                        return NetPacketCommand.newBuilder()
                                .setFilename(item.getFilename())
                                .setHostname(clientInfo.getHostname())
                                .setPort(clientInfo.getPort())
                                .setCommand(CommandType.FILE_COPY.getValue())
                                .build();
                    })
                    .collect(Collectors.toList());
            commandList.addAll(commands);
        }
        List<RemoveReplicaTask> removeReplicaTasks = client.pollRemoveReplicaTask(100);

        if (!removeReplicaTasks.isEmpty()) {
            List<NetPacketCommand> commands = removeReplicaTasks.stream()
                    .map(item -> {
                        ClientInfo clientInfo = clientManager.getClientByHost(item.getHostname());
                        return NetPacketCommand.newBuilder()
                                .setFilename(item.getFilename())
                                .setHostname(clientInfo.getHostname())
                                .setPort(clientInfo.getPort())
                                .setCommand(CommandType.FILE_REMOVE.getValue())
                                .build();
                    })
                    .collect(Collectors.toList());
            commandList.addAll(commands);
        }

        HeartbeatResponse response = HeartbeatResponse.newBuilder()
                .setIsSuccess(isSuccess)
                .addAllCommands(commandList)
                .build();
        request.sendResponse(response);
    }

    /**
     * <p>Description: 客户端的创建文件夹请求</p>
     * <p>Description: Create folder request from client </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     * @throws InterruptedException           消息转发异常
     */
    private void clientMkdirHandel(NetRequest request) throws InvalidProtocolBufferException, InterruptedException {
        NetPacket packet = request.getRequest();
        MkdirRequest mkdirRequest = MkdirRequest.parseFrom(packet.getBody());
        String realFilename =  "/susu" + mkdirRequest.getPath();
        int trackerIndex = serverManager.getTrackerIndexByFilename(realFilename);
        if (serverManager.isCurrentTracker(trackerIndex)) {
            trackerFileService.mkdir(realFilename, mkdirRequest.getAttrMap());
            request.sendResponse();
        }else {
            trackerClusterService.relay(trackerIndex,request);
        }
    }

    /**
     * <p>Description: 客户端的创建文件请求</p>
     * <p>Description: Create file request from client </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientCreateFileHandel(NetRequest request) throws InvalidProtocolBufferException{
        NetPacket packet = request.getRequest();
        CreateFileRequest createFileRequest = CreateFileRequest.parseFrom(packet.getBody());
        String filename =  "/susu" + createFileRequest.getFilename();
        Map<String, String> attrMap = new HashMap<>(createFileRequest.getAttrMap());
        String replicaNumStr = attrMap.get(Constants.ATTR_REPLICA_NUM);
        attrMap.put(Constants.ATTR_FILE_SIZE, String.valueOf(createFileRequest.getFileSize()));

        FileNode node = trackerFileService.listFiles(filename);
        if (node != null) {
            throw new RuntimeException("file already exist：" + createFileRequest.getFilename());
        }
        List<ClientInfo> clientInfoList = clientManager.selectAllClientsByFile(StringUtils.isNotBlank(replicaNumStr) ? Integer.parseInt(replicaNumStr) : 1, filename);
        List<StorageNode> storages = new ArrayList<>();
        for (ClientInfo client : clientInfoList) {
            StorageNode storage = StorageNode.newBuilder()
                    .setHostname(client.getHostname())
                    .setPort(client.getPort())
                    .build();
            storages.add(storage);
        }
        trackerFileService.createFile(filename,attrMap);
        log.info("create a file：[ filename={},storages={}]",filename,storages);
        CreateFileResponse response = CreateFileResponse.newBuilder()
                .setFilename(filename)
                .addAllStorages(storages)
                .build();
        request.sendResponse(response);
    }

    /**
     * <p>Description: 客户端的上传文件的确认请求</p>
     * <p>Description: Confirmation request for uploading files from the client </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientUploadFileConfirm(NetRequest request) throws InvalidProtocolBufferException, InterruptedException {
        NetPacket packet = request.getRequest();
        CreateFileRequest createFileRequest = CreateFileRequest.parseFrom(packet.getBody());
        String filename =  "/susu"  + createFileRequest.getFilename();
        clientManager.waitUploadFile(filename,3000);
        CreateFileResponse response = CreateFileResponse.newBuilder().build();
        request.sendResponse(response);
    }

    /**
     * <p>Description: Storage 接收文件完成后上报给Tracker的请求</p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientUploadFileComplete(NetRequest request) throws InvalidProtocolBufferException{
        NetPacket packet = request.getRequest();
        UploadCompletionRequest uploadCompletionRequest = UploadCompletionRequest.parseFrom(packet.getBody());
        log.info("Receive the Storage information reported by the client：[hostname={}, filename={}]", uploadCompletionRequest.getHostname(), uploadCompletionRequest.getFilename());
        FileInfo fileInfo = new FileInfo(uploadCompletionRequest.getHostname(), uploadCompletionRequest.getFilename(), uploadCompletionRequest.getFileSize());
        clientManager.addFile(fileInfo);
        ClientInfo client = clientManager.getClientByHost(uploadCompletionRequest.getHostname());
        client.addStoredDataSize(uploadCompletionRequest.getFileSize());
        request.sendResponse();
    }

    /**
     * <p>Description: Tracker 服务集群之间的感知请求</p>
     * <p>
     *     只有作为服务端的时候，才会保存新增的链接
     * </p>
     * <p>For example: tracker-01 -> tracker-02 </p>
     * <ul>
     *     <li>当 tracker-01 建立好连接之后，会主动发送一个  {@link PacketType } TRACKER_SERVER_AWARE 请求 </li>
     *     <li>当 tracker-02 收到后保存连接，并发送一个 {@link PacketType } TRACKER_SERVER_AWARE 请求给 tracker-01 </li>
     *     <li>当 tracker-01 收到 tracker-02 的返回消息后就不再保存连接了 </li>
     * </ul>
     *
     * @param request NetWork Request 网络请求
     * @throws Exception protobuf error || 发送异常
     */
    private void trackerServerAware(NetRequest request) throws Exception {
        NetPacket packet = request.getRequest();
        ChannelHandlerContext ctx = request.getCtx();
        TrackerAwareRequest awareRequest = TrackerAwareRequest.parseFrom(packet.getBody());
        log.info("收到 Track信息:[index={},isClient={}]",awareRequest.getIndex(),awareRequest.getIsClient());
        if (awareRequest.getIsClient()) {
            TrackerCluster trackerCluster = trackerClusterService.addTrackerCluster(
                    awareRequest.getIndex(),
                    (SocketChannel) ctx.channel(),
                    new TrackerInfo(awareRequest.getNode()),
                    taskScheduler);
            if (trackerCluster != null) {
                serverManager.reportSelfInfo(trackerCluster,false);
            }
        }
        serverManager.receiveSelfInf(awareRequest);
    }

    /**
     * <p>Description: Client 读取文件属性请求</p>
     *
     * @param request   网络包
     */
    private void clientReadAttrHandel(NetRequest request) throws InvalidProtocolBufferException, InterruptedException {
        NetPacket packet = request.getRequest();
        ReadAttrRequest readAttrRequest = ReadAttrRequest.parseFrom(packet.getBody());
        String readFilename = "/susu" + readAttrRequest.getFilename();
        int trackerIndex = serverManager.getTrackerIndexByFilename(readFilename);
        if (serverManager.isCurrentTracker(trackerIndex)) {
            Map<String, String> attr = trackerFileService.getAttr(readFilename);
            if (attr == null) {
                throw new RuntimeException("文件不存在：" + readFilename);
            }
            ReadAttrResponse response = ReadAttrResponse.newBuilder()
                    .putAllAttr(attr)
                    .build();
            request.sendResponse(response);
        }else {
            trackerClusterService.relay(trackerIndex,request);
        }

    }

}
