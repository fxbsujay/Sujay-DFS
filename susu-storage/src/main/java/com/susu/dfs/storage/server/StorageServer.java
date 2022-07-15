package com.susu.dfs.storage.server;


import com.susu.dfs.common.Node;
import com.susu.dfs.common.netty.NetServer;

/**
 * <p>Description: Storage 的 通讯服务端</p>
 *
 * @author sujay
 * @version 11:50 2022/7/15
 */
public class StorageServer {


    private NetServer netServer;

    private StorageChannelHandle storageChannelHandle;

    private final Node node;

    public StorageServer(Node node) {
        this.node = node;
        this.storageChannelHandle = new StorageChannelHandle();
    }
}
