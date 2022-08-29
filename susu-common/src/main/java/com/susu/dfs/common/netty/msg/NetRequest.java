package com.susu.dfs.common.netty.msg;

import com.google.protobuf.MessageLite;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.PacketType;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <p>Description: 网络请求</p>
 * <p>Description:  network Request</p>
 *
 * @author sujay
 * @version 0:31 2022/7/9
 */
@Slf4j
public class NetRequest {


    private ChannelHandlerContext ctx;

    private long requestSequence;

    private int trackerIndex;

    private NetPacket request;

    public NetRequest(ChannelHandlerContext ctx, NetPacket request) {
       this(ctx,request,-1);
    }


    public NetRequest(ChannelHandlerContext ctx, NetPacket request,int trackerIndex) {
        this.ctx = ctx;
        this.requestSequence = request.getSequence();
        this.request = request;
        this.trackerIndex = trackerIndex;
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
        List<NetPacket> responses = packet.partitionChunk(request.isSupportChunked(), Constants.CHUNKED_SIZE);
        if (responses.size() > 1) {
            log.info("返回响应通过chunked方式，共拆分为{}个包", responses.size());
        }
        for (NetPacket res : responses) {
            sendResponse(res, requestSequence);
        }
    }

    public void sendResponse(NetPacket response, Long sequence) {
        response.setTrackerIndex(trackerIndex);
        if (sequence != null) {
            response.setSequence(sequence);
        }
        ctx.writeAndFlush(response);
    }
}
