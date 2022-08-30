package com.susu.dfs.storage.server;


import com.susu.dfs.common.Node;
import com.susu.dfs.common.netty.NetServer;
import com.susu.dfs.common.task.TaskScheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * <p>Description: Storage 的 通讯服务端</p>
 *
 * @author sujay
 * @version 11:50 2022/7/15
 */
@Slf4j
public class StorageServer {

    private final Node node;

    private NetServer netServer;

    private StorageChannelHandle storageChannelHandle;

    private StorageManager storageManager;

    public StorageServer(Node node, TaskScheduler taskScheduler, StorageManager storageManager, StorageTransportCallback callback) {
        this.node = node;
        this.storageChannelHandle = new StorageChannelHandle(callback);
        this.storageManager = storageManager;
        this.netServer = new NetServer("STORAGE-SERVER",taskScheduler);
    }

    public void start() throws InterruptedException {
        log.info("Shutdown Storage Server");
        MultiPortBaseChannelHandle baseChannelHandle = new MultiPortBaseChannelHandle(node,storageManager);
        baseChannelHandle.addHandler(storageChannelHandle);
        netServer.setBaseChannelHandler(baseChannelHandle);
        netServer.start(Arrays.asList(node.getPort(),node.getHttpPort()));
    }

    public void shutdown() {
        log.info("Shutdown Storage Server");
        this.netServer.shutdown();
    }
}
