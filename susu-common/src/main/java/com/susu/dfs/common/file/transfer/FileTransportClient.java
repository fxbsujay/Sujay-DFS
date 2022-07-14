package com.susu.dfs.common.file.transfer;

import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Description: File Transport Client</p>
 * <p>Description: 文件上传下载的客户端</p>
 *
 * @author sujay
 * @version 15:28 2022/7/14
 */
@Slf4j
public class FileTransportClient {

    /**
     * 网络组件
     */
    private NetClient netClient;

    private Map<String, String> filePathMap = new ConcurrentHashMap<>();

    private Map<String, OnProgressListener> listeners = new ConcurrentHashMap<>();

    public FileTransportClient(NetClient netClient) {
        this(netClient, true);
    }

    public FileTransportClient(NetClient netClient, boolean getFile) {
        this.netClient = netClient;
        if (getFile) {
            FileTransportCallback callback = new FileTransportCallback() {

                @Override
                public String getPath(String filename) {
                    return filePathMap.remove(filename);
                }

                @Override
                public void onProgress(String filename, long total, long current, float progress, int currentWriteBytes) {
                    OnProgressListener listener = listeners.get(filename);
                    if (listener != null) {
                        listener.onProgress(total, current, progress, currentWriteBytes);
                    }
                }

                @Override
                public void onCompleted(FileAttribute fileAttribute) {
                    OnProgressListener listener = listeners.remove(fileAttribute.getFilename());
                    if (listener != null) {
                        listener.onCompleted();
                    }
                }
            };
            FileReceiveHandler fileReceiveHandler = new FileReceiveHandler(callback);
            this.netClient.addPackageListener(requestWrapper -> {
                NetPacket request = requestWrapper.getRequest();
                if (request.getType() == PacketType.TRANSFER_FILE.getValue()) {
                    FilePacket filePacket = FilePacket.parseFrom(requestWrapper.getRequest().getBody());
                    fileReceiveHandler.handleRequest(filePacket);
                }
            });
        }
    }
}
