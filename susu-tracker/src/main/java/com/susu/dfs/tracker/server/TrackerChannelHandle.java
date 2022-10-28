package com.susu.dfs.tracker.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.dfs.common.model.*;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.eum.CommandType;
import com.susu.dfs.common.file.FileNode;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.NetUtils;
import com.susu.dfs.common.utils.SnowFlakeUtils;
import com.susu.dfs.common.utils.StopWatch;
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
import com.susu.dfs.tracker.service.TrackerUserService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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

    private final String DEFAULT_BASE_FILE_PATH;

    private final Node node;

    private final ThreadPoolExecutor executor;

    private final TaskScheduler taskScheduler;

    /**
     *  Tracker 客户端管理器 保存注册进来的客户端信息
     */
    private final ClientManager clientManager;

    private final ServerManager serverManager;

    private final TrackerFileService trackerFileService;

    private final TrackerUserService trackerUserService;

    private final TrackerClusterService trackerClusterService;

    /**
     * 给注册进来的客户端分配id
     */
    private final SnowFlakeUtils snowFlakeUtils = new SnowFlakeUtils(1,1);

    public TrackerChannelHandle(SysConfig config, TaskScheduler taskScheduler,
                                ClientManager clientManager, ServerManager serverManager,
                                TrackerUserService trackerUserService, TrackerFileService trackerFileService,
                                TrackerClusterService trackerClusterService) {
        this.node = config.getNode();
        this.DEFAULT_BASE_FILE_PATH = config.DEFAULT_BASE_FILE_PATH;
        this.taskScheduler = taskScheduler;
        this.executor = new ThreadPoolExecutor(Constants.HANDLE_THREAD_EXECUTOR_CORE_SIZE,Constants.HANDLE_THREAD_EXECUTOR_CORE_SIZE_MAX,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(Constants.HANDLE_THREAD_EXECUTOR_QUEUE_SIZE_MAX));
        this.clientManager = clientManager;
        this.serverManager = serverManager;
        this.trackerUserService = trackerUserService;
        this.trackerFileService = trackerFileService;
        this.trackerClusterService = trackerClusterService;
        this.trackerClusterService.setTrackerChannelHandle(this);

        this.serverManager.addOnSlotCompletedListener(slots -> {
            log.info("Slot initialization is complete");
        });
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        boolean consumedMsg = trackerClusterService.onResponse(packet);
        if (consumedMsg) {
            return true;
        }
        PacketType packetType = PacketType.getEnum(packet.getType());
        NetRequest request = new NetRequest(ctx, packet, node.getIndex());
        switch (packetType) {
            case STORAGE_REGISTER:
                storageRegisterHandle(request);
                break;
            case STORAGE_REPORT_INFO:
                storageReportInfoHandle(request);
                break;
            case STORAGE_HEART_BEAT:
                storageHeartbeatHandle(request);
                break;
            case AUTH_INFO:
                clientAuthHandel(request);
                break;
            case MKDIR:
                clientMkdirHandel(request);
                break;
            case CREATE_FILE:
                clientCreateFileHandle(request);
                break;
            case UPLOAD_FILE_COMPLETE:
                storageUploadFileComplete(request);
                break;
            case UPLOAD_FILE_CONFIRM:
                clientUploadFileConfirm(request);
                break;
            case READ_ATTR:
                clientReadAttrHandle(request);
                break;
            case REMOVE_FILE:
                clientRemoveFile(request);
            case REMOVE_FILE_COMPLETE:
                storageRemoveFileComplete(request);
                break;
            case GET_STORAGE_FOR_FILE:
                clientGetStorageForFile(request);
                break;
            case TRACKER_SERVER_AWARE:
                trackerServerAware(request);
                break;
            case TRACKER_SLOT_BROADCAST:
                serverManager.onReceiveSlots(request);
                break;
            case NEW_TRACKER_INFO:
                trackerNewTrackerInfoRequest(request);
                break;
            case TRACKER_RE_BALANCE_SLOTS:
                serverManager.onRebalancedSlots(request);
                break;
            case TRACKER_FETCH_SLOT_META_DATA:
                serverManager.writeMetadataToTracker(request);
                break;
            case TRACKER_FETCH_SLOT_META_DATA_RESPONSE:
                serverManager.onFetchMetadata(request);
                break;
            case TRACKER_FETCH_SLOT_META_DATA_COMPLETED:
                serverManager.onLocalControllerFetchSlotMetadataCompleted(request);
                break;
            case TRACKER_FETCH_SLOT_META_DATA_COMPLETED_BROADCAST:
                serverManager.onRemoteControllerFetchSlotMetadataCompleted(request);
                break;
            case TRACKER_REMOVE_META_DATA_COMPLETED:
                serverManager.onRemoveMetadataCompleted(request);
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
        boolean broadcast = broadcast(request.getRequest());
        RegisterRequest registerRequest = RegisterRequest.parseFrom(request.getRequest().getBody());
        boolean register = clientManager.register(registerRequest,clientId,broadcast ? null : request.getCtx());
        if (register && !broadcast) {
            RegisterResponse response = RegisterResponse.newBuilder().setClientId(clientId).build();
            request.sendResponse(response);
        }
    }

    /**
     * <p>Description: Storage上报自身的存储信息</p>
     * <p>Description: Storage reports its own storage information </p>
     *
     * @param request NetWork Request 网络请求
     */
    private void storageReportInfoHandle(NetRequest request) throws InvalidProtocolBufferException {
        NetPacket packet = request.getRequest();
        broadcast(packet);
        ReportStorageInfoRequest reportStorageInfoRequest = ReportStorageInfoRequest.parseFrom(packet.getBody());
        log.info("Report storage information：[hostname={}, files={}]", reportStorageInfoRequest.getHostname(), reportStorageInfoRequest.getFileInfosCount());
        for (FileMetaInfo file : reportStorageInfoRequest.getFileInfosList()) {
            int trackerIndex = serverManager.getTrackerIndexByFilename(file.getFilename());
            if (serverManager.isCurrentTracker(trackerIndex)) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(file.getFilename());
                fileInfo.setFileSize(file.getFileSize());
                fileInfo.setHostname(reportStorageInfoRequest.getHostname());
                clientManager.addFile(fileInfo);
            }
        }
        if (reportStorageInfoRequest.getFinished()) {
            clientManager.setStorageReady(reportStorageInfoRequest.getHostname());
            log.info("Completion of reporting storage information：[hostname={}]", reportStorageInfoRequest.getHostname());
        }
    }

    /**
     * <p>Description: Storage心跳请求处理</p>
     * <p>Description: Storage heartbeat request processing </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void storageHeartbeatHandle(NetRequest request) throws InvalidProtocolBufferException {
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

        List<NetPacket> nettyPackets = broadcastSync(request);

        for (NetPacket nettyPacket : nettyPackets) {
            HeartbeatResponse heartbeatResponse = HeartbeatResponse.parseFrom(nettyPacket.getBody());
            commandList.addAll(heartbeatResponse.getCommandsList());
        }

        request.getRequest().getBroadcast();
        HeartbeatResponse response = HeartbeatResponse.newBuilder()
                .setIsSuccess(isSuccess)
                .addAllCommands(commandList)
                .build();
        request.sendResponse(response);
    }

    private void clientAuthHandel(NetRequest request) throws InvalidProtocolBufferException {
        NetPacket packet = request.getRequest();
        AuthenticateInfoRequest authRequest = AuthenticateInfoRequest.parseFrom(packet.getBody());

        if (!packet.getBroadcast()) {
            Channel channel = request.getCtx().channel();
            boolean authenticate = trackerUserService.login(channel, authRequest.getUsername(), authRequest.getPassword());
            if (!authenticate) {
                log.info("Authentication failed ！！");
                return;
            }
            String token = trackerUserService.getToken(channel);
            packet.setToken(token);
            broadcastSync(request);
            AuthenticateInfoResponse response = AuthenticateInfoResponse.newBuilder()
                    .setToken(token)
                    .build();
            request.sendResponse(response);
        } else {
            trackerUserService.setToken(authRequest.getUsername(),packet.getToken());
            request.sendResponse();
            log.info("Receive broadcast authentication information: [username={},token={}]",authRequest.getUsername(),packet.getToken());
        }
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
        verifyToken(request);
        NetPacket packet = request.getRequest();
        MkdirRequest mkdirRequest = MkdirRequest.parseFrom(packet.getBody());
        String realFilename =  DEFAULT_BASE_FILE_PATH + mkdirRequest.getPath();
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
    private void clientCreateFileHandle(NetRequest request) throws InvalidProtocolBufferException{
        verifyToken(request);
        NetPacket packet = request.getRequest();
        CreateFileRequest createFileRequest = CreateFileRequest.parseFrom(packet.getBody());
        String filename = DEFAULT_BASE_FILE_PATH + createFileRequest.getFilename();
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
        verifyToken(request);
        NetPacket packet = request.getRequest();
        CreateFileRequest createFileRequest = CreateFileRequest.parseFrom(packet.getBody());
        String filename =  DEFAULT_BASE_FILE_PATH  + createFileRequest.getFilename();
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
    private void storageUploadFileComplete(NetRequest request) throws InvalidProtocolBufferException{
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
        if (awareRequest.getIsClient()) {
            TrackerCluster trackerCluster = trackerClusterService.addTrackerCluster(
                    awareRequest.getIndex(),
                    ctx,
                    new TrackerInfo(awareRequest.getNode()),
                    taskScheduler);
            if (trackerCluster != null) {
                serverManager.reportSelfInfo(trackerCluster,false);
            }
        }
        serverManager.receiveSelfInf(awareRequest);
    }

    /**
     * 处理同步过来的storage信息请求
     */
    private void trackerNewTrackerInfoRequest(NetRequest request) throws InvalidProtocolBufferException {
        NetPacket packet = request.getRequest();
        NewTrackerInfo newPeerDataNodeInfo = NewTrackerInfo.parseFrom(packet.getBody());
        List<RegisterRequest> requestsList = newPeerDataNodeInfo.getRequestsList();
        for (RegisterRequest registerRequest : requestsList) {
            clientManager.register(registerRequest,registerRequest.getNodeId(),null);
            ClientInfo client = clientManager.getClientByHost(registerRequest.getHostname());
            client.setStatus(ClientInfo.STATUS_READY);
        }
    }
    /**
     * <p>Description: Client 读取文件属性请求</p>
     *
     * @param request   网络包
     */
    private void clientReadAttrHandle(NetRequest request) throws InvalidProtocolBufferException, InterruptedException {
        verifyToken(request);
        NetPacket packet = request.getRequest();
        ReadAttrRequest readAttrRequest = ReadAttrRequest.parseFrom(packet.getBody());
        String readFilename = DEFAULT_BASE_FILE_PATH + readAttrRequest.getFilename();
        int trackerIndex = serverManager.getTrackerIndexByFilename(readFilename);
        if (serverManager.isCurrentTracker(trackerIndex)) {
            Map<String, String> attr = trackerFileService.getAttr(readFilename);
            if (attr == null) {
                throw new RuntimeException("file does not exist !!：" + readFilename);
            }
            ReadAttrResponse response = ReadAttrResponse.newBuilder()
                    .putAllAttr(attr)
                    .build();
            request.sendResponse(response);
        }else {
            trackerClusterService.relay(trackerIndex,request);
        }
    }

    /**
     * <p>Description: Client 删除文件的请求处理</p>
     *
     * @param request   网络包
     */
    private void clientRemoveFile(NetRequest request) throws InvalidProtocolBufferException, InterruptedException {
        verifyToken(request);
        NetPacket packet = request.getRequest();
        RemoveFileRequest removeFileRequest = RemoveFileRequest.parseFrom(packet.getBody());
        String filename =  DEFAULT_BASE_FILE_PATH + removeFileRequest.getFilename();
        int trackerIndex = serverManager.getTrackerIndexByFilename(filename);
        if (serverManager.isCurrentTracker(trackerIndex)) {
            removeFile(filename);
        } else {
            trackerClusterService.relay(trackerIndex,request);
        }
    }

    public void removeFile(String filename) {
        FileNode fileNode = trackerFileService.listFiles(filename);
        if (fileNode == null) {
            throw new RuntimeException("file does not exist !!：" + filename);
        }
        Map<String, String> attr = new HashMap<>(Constants.MAP_SIZE);
        attr.put(Constants.ATTR_FILE_DEL_TIME, String.valueOf(System.currentTimeMillis()));
        if (fileNode.getChildren().isEmpty()) {
            trackerFileService.deleteFile(filename);
            String destFilename = DEFAULT_BASE_FILE_PATH + File.separator + Constants.TRASH_DIR + filename;
            Map<String, String> currentAttr = fileNode.getAttr();
            currentAttr.putAll(attr);
            trackerFileService.createFile(destFilename,currentAttr);
            log.debug("Delete files and move to trash：[src={}, target={}]", filename, destFilename);
        } else {
            throw new RuntimeException("file does not exist !!：" + filename);
        }
    }

    /**
     * <p>Description: Storage 删除文件完成后上报给Tracker的请求</p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void storageRemoveFileComplete(NetRequest request) throws InvalidProtocolBufferException {
        NetPacket packet = request.getRequest();
        boolean broadcast = broadcast(packet);
        RemoveCompletionRequest removeCompletionRequest = RemoveCompletionRequest.parseFrom(packet.getBody());
        log.info("Receive the Storage information reported by the client：[hostname={}, filename={}]", removeCompletionRequest.getHostname(), removeCompletionRequest.getFilename());
        ClientInfo client = clientManager.getClientByHost(removeCompletionRequest.getHostname());
        client.addStoredDataSize(-removeCompletionRequest.getFileSize());

        if (!broadcast) {
            request.sendResponse();
        }
    }

    /**
     * <p>Description: Client 下载文件完获取文件所在的一个storage节点</p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientGetStorageForFile(NetRequest request) throws InvalidProtocolBufferException, InterruptedException {
        verifyToken(request);
        NetPacket packet = request.getRequest();
        GetStorageForFileRequest getStorageForFileRequest = GetStorageForFileRequest.parseFrom(packet.getBody());
        String realFilename = DEFAULT_BASE_FILE_PATH + getStorageForFileRequest.getFilename();

        int trackerIndex = serverManager.getTrackerIndexByFilename(realFilename);
        if (serverManager.isCurrentTracker(trackerIndex)) {

            ClientInfo client = clientManager.chooseReadableClientByFileName(realFilename);

            if (client == null)  throw new RuntimeException("file does not exist !!：" + realFilename);

            StorageNode storageNode = StorageNode.newBuilder()
                    .setHostname(client.getHostname())
                    .setPort(client.getPort())
                    .build();

            GetStorageForFileResponse response = GetStorageForFileResponse.newBuilder()
                    .setStorage(storageNode)
                    .setRealFileName(realFilename)
                    .build();

            request.sendResponse(response);
        } else {
            trackerClusterService.relay(trackerIndex,request);
        }
    }

    /**
     * 验证Client端的消息是否合法
     */
    private void verifyToken(NetRequest request) {
        NetPacket packet = request.getRequest();
        String token = packet.getToken();

        if (StringUtils.isEmpty(token)) {
            log.warn("An unauthenticated request was received and has been blocked：[channel={}, packetType={}]", NetUtils.getChannelId(request.getCtx().channel()), packet.getType());
            throw new RuntimeException("Illegal token !!");
        }

        if (!trackerUserService.isLogin(request.getCtx().channel(), token)) {
            log.warn("An unauthenticated request was received and has been blocked：[channel={}, packetType={}]", NetUtils.getChannelId(request.getCtx().channel()), packet.getType());
            throw new RuntimeException("Illegal token !!");
        }

    }

    /**
     * 广播请求给所有的Tracker节点
     *
     * @param packet 请求
     * @return 该请求是否是别的NameNode广播过来的
     */
    private boolean broadcast(NetPacket packet) {
        if (!node.getIsCluster()) {
            return false;
        }
        boolean isBroadcastRequest = packet.getBroadcast();
        if (!isBroadcastRequest) {
            packet.setBroadcast(true);
            List<Integer> broadcast = trackerClusterService.broadcast(packet);
            if (!broadcast.isEmpty()) {
                log.debug("Broadcast request to all Tracker: [sequence={}, broadcast={}, packetType={}]",
                        packet.getSequence(), broadcast, PacketType.getEnum(packet.getType()).getDescription());
            }
        }
        return isBroadcastRequest;
    }

    /**
     * 广播请求给所有的Tracker节点, 同时获取所有节点的响应
     *
     * @param request 请求
     * @return 请求结果
     */
    private List<NetPacket> broadcastSync(NetRequest request) {

        if (!node.getIsCluster()) {
            return new ArrayList<>();
        }

        NetPacket packet = request.getRequest();
        boolean isBroadcastRequest = packet.getBroadcast();

        if (!isBroadcastRequest) {

            packet.setBroadcast(true);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<NetPacket> nettyPackets = new ArrayList<>(trackerClusterService.broadcastSync(packet));
            stopWatch.stop();

            if (!nettyPackets.isEmpty()) {

                log.debug("Broadcast request to all Tracker and obtained the response: [sequence={}, broadcast={}, packetType={}, cost={} s]",
                        packet.getSequence(), trackerClusterService.getAllTrackerIndex(),
                        PacketType.getEnum(packet.getType()).getDescription(),
                        stopWatch.getTime() / 1000.0D);
            }

            return nettyPackets;
        }
        return new ArrayList<>();
    }
}
