package com.susu.dfs.common.netty.msg;

import com.google.protobuf.MessageLite;
import com.susu.dfs.common.eum.PacketType;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p>Description: 网络请求</p>
 * <p>Description:  network Request</p>
 *
 * @author sujay
 * @version 0:31 2022/7/9
 */
public class NetRequest {


    private ChannelHandlerContext ctx;

    private NetPacket request;

    public NetRequest(ChannelHandlerContext ctx, NetPacket request) {
        this.ctx = ctx;
        this.request = request;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public NetPacket getRequest() {
        return request;
    }

    /**
     * 发送响应
     */
    public void sendResponse() {
        sendResponse(null);
    }

    /**
     * 发送响应
     *
     * @param response 响应
     */
    public void sendResponse(MessageLite response) {
        byte[] body = response == null ? new byte[0] : response.toByteArray();
        NetPacket packet = NetPacket.buildPacket(body, PacketType.getEnum(request.getType()));
        packet.setSequence(request.getSequence());
        ctx.writeAndFlush(packet);
    }

}
