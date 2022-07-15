package com.susu.dfs.common.file.transfer;

import com.susu.common.model.GetFileRequest;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.task.TaskScheduler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
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
                if (request.getType() == PacketType.UPLOAD_FILE.getValue()) {
                    FilePacket filePacket = FilePacket.parseFrom(request.getBody());
                    fileReceiveHandler.handleRequest(filePacket);
                }
            });
        }
    }


    /**
     * 上传文件
     *
     * @param absolutePath 本地文件绝对路径
     * @throws Exception 文件不存在
     */
    public void sendFile(String absolutePath) throws Exception {
        sendFile(absolutePath, absolutePath, null, false);
    }

    /**
     * 上传文件
     *
     * @param filename     服务器文件名称
     * @param absolutePath 本地文件绝对路径
     * @throws Exception 文件不存在
     */
    public void sendFile(String filename, String absolutePath, OnProgressListener listener, boolean force) throws Exception {
        File file = new File(absolutePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在：" + absolutePath);
        }
        DefaultFileSendTask fileSender = new DefaultFileSendTask(file, filename, netClient.socketChannel(), listener);
        fileSender.execute(force);
    }

    /**
     * 下载文件
     *
     * @param filename     文件名
     * @param absolutePath 本地文件绝对路径
     * @param listener     进度监听器
     */
    public void readFile(String filename, String absolutePath, OnProgressListener listener) throws InterruptedException {
        if (listener != null) {
            listeners.put(filename, listener);
        }
        filePathMap.put(filename, absolutePath);
        GetFileRequest request = GetFileRequest.newBuilder()
                .setFilename(filename)
                .build();
        NetPacket nettyPacket = NetPacket.buildPacket(request.toByteArray(), PacketType.GET_FILE);
        netClient.send(nettyPacket);
    }

    /**
     * 优雅关闭
     */
    public void shutdown() {
        listeners.clear();
        filePathMap.clear();
        netClient.shutdown();
    }
}
