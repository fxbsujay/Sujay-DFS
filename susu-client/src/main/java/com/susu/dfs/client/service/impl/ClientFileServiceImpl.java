package com.susu.dfs.client.service.impl;

import com.susu.common.model.MkdirRequest;
import com.susu.dfs.client.TrackerClient;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.file.OnProgressListener;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.client.service.ClientFileService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
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

    public ClientFileServiceImpl(TrackerClient trackerClient) {
        this.trackerClient = trackerClient;
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

    }

    @Override
    public void put(String filename, File file, int numOfReplica) throws Exception {

    }

    @Override
    public void put(String filename, File file, int numOfReplica, Map<String, String> attr) throws Exception {

    }

    @Override
    public void put(String filename, File file, int numOfReplica, Map<String, String> attr, OnProgressListener listener) throws Exception {

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
