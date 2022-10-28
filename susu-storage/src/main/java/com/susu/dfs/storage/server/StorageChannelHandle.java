package com.susu.dfs.storage.server;

import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.file.transfer.DefaultFileSendTask;
import com.susu.dfs.common.file.transfer.FilePacket;
import com.susu.dfs.common.file.transfer.FileReceiveHandler;
import com.susu.dfs.common.model.GetFileRequest;
import com.susu.dfs.common.netty.AbstractChannelHandler;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Description: Storage 的 通讯服务</p>
 *
 * @author sujay
 * @version 13:32 2022/7/15
 */
@Slf4j
public class StorageChannelHandle extends AbstractChannelHandler {

    private StorageTransportCallback callback;

    private FileReceiveHandler fileReceiveHandler;

    public StorageChannelHandle(StorageTransportCallback callback) {
        this.callback = callback;
        this.fileReceiveHandler = new FileReceiveHandler(callback);
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        NetRequest request = new NetRequest(ctx, packet);
        switch (packetType) {
            case UPLOAD_FILE:
                clientUploadFile(request);
                break;
            case GET_FILE:
                clientGetFile(request);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected Set<Integer> interestPackageTypes() {
        return new HashSet<>();
    }

    /**
     * <p>Description: 客户端上传文件</p>
     */
    private void clientUploadFile(NetRequest request) {
        FilePacket filePacket = FilePacket.parseFrom(request.getRequest().getBody());

        if (filePacket.getType() == FilePacket.HEAD) {
            log.info("Received the uploaded file from the client.....");
        }
        fileReceiveHandler.handleRequest(filePacket);
    }

    private void clientGetFile(NetRequest request) throws IOException {
        GetFileRequest packet = GetFileRequest.parseFrom(request.getRequest().getBody());
        String filename = packet.getFilename();
        String path = callback.getPath(filename);
        File file = new File(path);
        log.info("收到下载文件请求：{}", filename);
        DefaultFileSendTask fileSender = new DefaultFileSendTask(file, filename,
                (SocketChannel) request.getCtx().channel(),
                (total, current, progress, currentReadBytes) -> log.info("正在发送文件：filename={}",filename));
        fileSender.execute(false);
    }
}
