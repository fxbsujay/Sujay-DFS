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
        log.info("创建文件夹成功：[filename={}]", path);
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
           throw new RuntimeException("不合法的副本数量：" + replicaNum);
       }
        for (String key : Constants.KEYS_ATTR_SET) {
            if (attr.containsKey(key)) {
                log.warn("文件属性包含关键属性：[key={}]", key);
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
        List<StorageNode> storagesList = response.getStoragesList();
        log.info("===============storagesList={}",storagesList);


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
            throw new RuntimeException("不合法的文件名：" + filename);
        }
    }


}
