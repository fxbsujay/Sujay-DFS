package com.susu.dfs.client.service.impl;

import com.susu.common.model.*;
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

    /**
     *  File upload is to upload to the Storage {@link com.susu.dfs.common.StorageInfo} node.
     *  After the client uploads to Storage, Storage will report to the Tracker node. There is a time difference.
     *
     *  In order to achieve strong consistency and ensure that the file can be read immediately after uploading,
     *  it is considered successful to upload the file only after the Tracker{@link com.susu.dfs.common.TrackerInfo}
     *  receives the information reported by Storage.
     *
     *  But this will reduce the throughput of uploading files.
     *  Because where do the threads that will occupy a thread pool of Tracker wait for 3 seconds.
     *  It is possible to keep the requests reported by Storage waiting in the queue, and eventually a timeout error occurs.
     *
     *  <pre>
     *      For Example:
     *
     *           1.The client can be configured to let the Tracker confirm whether to wait.
     *             If confirmation waiting is enabled, the throughput will decrease,
     *             but strong consistency is guaranteed.
     *             If confirmation waiting is not enabled, the throughput is relatively high.
     *             However, consistency cannot be guaranteed,
     *             which means that the file may not be read immediately after uploading
     *
     *           2.In the process of waiting in the Tracker,
     *             don't wait directly in the thread.
     *             Instead, create a task, save it in the collection,
     *             start a thread in the background,
     *             and judge whether the task is completed indefinitely.
     *             If it is completed, write back the response.
     *             This method can ensure strong consistency
     *             and will not block threads in the thread pool.
     *
     *  </pre>
     */
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

        NetPacket confirmRequest = NetPacket.buildPacket(request.toByteArray(), PacketType.UPLOAD_FILE_CONFIRM);
        trackerClient.authSendSync(confirmRequest);
    }

    @Override
    public Map<String, String> readAttr(String filename) throws Exception {
        validate(filename);
        ReadAttrRequest request = ReadAttrRequest.newBuilder()
                .setFilename(filename)
                .build();
        NetPacket packet = NetPacket.buildPacket(request.toByteArray(), PacketType.READ_ATTR);
        NetPacket resp = trackerClient.authSendSync(packet);
        ReadAttrResponse response = ReadAttrResponse.parseFrom(resp.getBody());
        return response.getAttrMap();
    }

    @Override
    public void get(String filename, String absolutePath) throws Exception {

    }

    @Override
    public void get(String filename, String absolutePath, OnProgressListener listener) throws Exception {

    }

    @Override
    public void remove(String filename) throws Exception {
        validate(filename);

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
