package com.susu.dfs.storage.server;

import com.susu.dfs.common.file.transfer.FileAttribute;
import com.susu.dfs.common.file.transfer.FileTransportCallback;
import com.susu.dfs.storage.client.TrackerClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * <p>Description: Storage 的 文件上传的回调</p>
 *
 * @author sujay
 * @version 14:03 2022/7/15
 */
@Slf4j
public class StorageTransportCallback implements FileTransportCallback {

    private TrackerClient trackerClient;

    private StorageManager storageManager;

    public StorageTransportCallback(StorageManager storageManager, TrackerClient trackerClient) {
        this.storageManager = storageManager;
        this.trackerClient = trackerClient;
    }

    @Override
    public String getPath(String filename) {
        String localFileName = storageManager.getAbsolutePathByFileName(filename);
        log.info("获取文件路径文件：[filename={}, location={}]", filename, localFileName);
        return localFileName;
    }

    @Override
    public void onProgress(String filename, long total, long current, float progress, int currentWriteBytes) {
       // TODO 上传文件进度监听
    }

    @Override
    public void onCompleted(FileAttribute attr) throws InterruptedException, IOException {
        storageManager.recordReplicaReceive(attr.getFilename(), attr.getAbsolutePath(), attr.getSize());
        trackerClient.clientUploadCompletionRequest(attr.getFilename(),attr.getSize());
    }
}
