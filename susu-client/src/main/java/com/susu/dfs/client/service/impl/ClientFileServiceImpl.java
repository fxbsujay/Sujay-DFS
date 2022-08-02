package com.susu.dfs.client.service.impl;

import com.susu.common.model.CreateFileRequest;
import com.susu.common.model.CreateFileResponse;
import com.susu.common.model.MkdirRequest;
import com.susu.common.model.StorageNode;
import com.susu.dfs.client.OnMultiFileProgressListener;
import com.susu.dfs.client.TrackerClient;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.file.transfer.FileTransportClient;
import com.susu.dfs.common.file.transfer.OnProgressListener;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.client.service.ClientFileService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: DFS 客户端的文件API服务</p>
 *
 * @author sujay
 * @version 10:45 2022/7/14
 */
@Slf4j
public class ClientFileServiceImpl implements ClientFileService {

    private TrackerClient trackerClient;

    private TaskScheduler taskScheduler;

    public ClientFileServiceImpl(TrackerClient trackerClient, TaskScheduler taskScheduler) {
        this.trackerClient = trackerClient;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void mkdir(String path) throws Exception {
        mkdir(path, new HashMap<>(Constants.MAP_SIZE));
    }

    @Override
    public void mkdir(String path, Map<String, String> attr) throws Exception {
        validate(path);
        MkdirRequest request = MkdirRequest.newBuilder()
                .setPath(path)
                .putAllAttr(attr)
                .build();
        NetPacket packet = NetPacket.buildPacket(request.toByteArray(), PacketType.MKDIR);
        trackerClient.authSendSync(packet);
        log.info("mkdir success：[filename={}]", path);
    }

    @Override
    public void put(String filename, File file) throws Exception {
        put(filename, file, -1, new HashMap<>(Constants.MAP_SIZE));
    }

    @Override
    public void put(String filename, File file, int replicaNum) throws Exception {
        put(filename, file, replicaNum, new HashMap<>(Constants.MAP_SIZE));
    }

    @Override
    public void put(String filename, File file, int replicaNum, Map<String, String> attr) throws Exception {
        put(filename, file, replicaNum, attr, null);
    }

    @Override
    public void put(String filename, File file, int replicaNum, Map<String, String> attr, OnProgressListener listener) throws Exception {
       validate(filename);
       if (replicaNum > Constants.MAX_REPLICA_NUM) {
           throw new RuntimeException("Number of illegal copies error：" + replicaNum);
       }
        for (String key : Constants.KEYS_ATTR_SET) {
            if (attr.containsKey(key)) {
                log.warn("File attributes contain key attributes：[key={}]", key);
            }
        }
        if (replicaNum > 0) {
            attr.put(Constants.ATTR_REPLICA_NUM, String.valueOf(replicaNum));
        }
        CreateFileRequest request = CreateFileRequest.newBuilder()
                .setFilename(filename)
                .setFileSize(file.length())
                .putAllAttr(attr)
                .build();
        NetPacket packet = NetPacket.buildPacket(request.toByteArray(), PacketType.CREATE_FILE);
        NetPacket resp = trackerClient.authSendSync(packet);
        CreateFileResponse response = CreateFileResponse.parseFrom(resp.getBody());
        OnMultiFileProgressListener onMultiFileProgressListener = new OnMultiFileProgressListener(listener,response.getStoragesList().size());
        for (int i = 0; i < response.getStoragesList().size(); i++) {
            StorageNode storage = response.getStorages(i);
            String hostname = storage.getHostname();
            int port = storage.getPort();
            NetClient netClient = new NetClient("SUSU-Client-" + hostname, taskScheduler);
            FileTransportClient fileTransportClient = new FileTransportClient(netClient);
            netClient.start(hostname, port);
            netClient.ensureStart();
            log.debug("Start uploading files to：[node={}:{}, filename={}]", hostname, port, filename);
            fileTransportClient.sendFile(response.getFilename(), file.getAbsolutePath(), onMultiFileProgressListener, true);
            fileTransportClient.shutdown();
            log.debug("Finish uploading files to：[node={}:{}, filename={}]", hostname, port, filename);
        }
        /*
         * 文件上传是上传到 Storage 节点，客户端上传到 Storage 之后，Storage 再上报给 Tracker 节点中间有一个时间差
         * 为了达到强一致性，保证文件上传后，立马是可以读取文件的，需要等待 Tracker 收到 Storage 上报的信息，才认为是上传成功的。
         * 但是这样一来会降低上传文件的吞吐量。 因为会占用 Tracker 一个线程池的线程在哪里hang住等待3秒，
         * 有可能让 Storage 上报的请求在队列里面一直等待，最终出现超时错误。这里有两种方案可以选择：
         *
         * 1、 客户端可以配置让 Tracker 确认是否等待，如果开启确认等待，则吞吐量会下降，但是保证强一致性。如果不开启确认等待，则吞吐量比较高，
         *     但是一致性不能保证，就是说上传完毕后有可能立即读文件读不到
         *
         * 2、 在 Tracker 那边等待的过程，不要直接在线程里面等待，而是建立一个任务Task，保存在集合中，后台起一个线程，就无限循环的去判断
         *     这个Task是否完成，如果完成才写回响应。这种方式可以保证强一致性，并且不会阻塞线程池中的线程。
         *
         * 目前我们先采用第一种方式实现，第二种后面可以考虑优化实现。
         *
         */
        NetPacket confirmRequest = NetPacket.buildPacket(request.toByteArray(), PacketType.UPLOAD_FILE_CONFIRM);
        trackerClient.authSendSync(confirmRequest);
    }

    @Override
    public void get(String filename, String absolutePath) throws Exception {

    }

    @Override
    public void get(String filename, String absolutePath, OnProgressListener listener) throws Exception {

    }

    @Override
    public void remove(String filename) throws Exception {

    }

    /**
     * 验证文件名称合法,校验连接已经认证通过
     *
     * @param filename 文件名称
     */
    private void validate(String filename) throws Exception {
        boolean ret = FileUtils.validateFileName(filename);
        if (!ret) {
            throw new RuntimeException("Exception: Illegal file name !! [filename=" + filename + "]");
        }
    }


}
