package com.susu.dfs.storage.server;

import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.AbstractChannelHandler;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>Description: Storage 的 通讯服务</p>
 *
 * @author sujay
 * @version 13:32 2022/7/15
 */
public class StorageChannelHandle extends AbstractChannelHandler {

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        NetRequest request = new NetRequest(ctx, packet);
        switch (packetType) {
            case UPLOAD_FILE:
                clientUploadFile(request);
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

    }
}
