package com.susu.dfs.tracker.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.common.model.HeartbeatRequest;
import com.susu.common.model.RegisterRequest;
import com.susu.common.model.RegisterResponse;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.utils.SnowFlakeUtils;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.common.netty.AbstractChannelHandler;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.eum.PacketType;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: Tracker 的 通讯服务</p>
 *
 * @author sujay
 * @version 15:19 2022/7/7
 */
@Slf4j
public class TrackerChannelHandle extends AbstractChannelHandler {

    private final ThreadPoolExecutor executor;

    /**
     *  Tracker 客户端管理器 保存注册进来的客户端信息
     */
    private final ClientManager clientManager;

    /**
     * 给注册进来的客户端分配id
     */
    private final SnowFlakeUtils snowFlakeUtils = new SnowFlakeUtils(1,1);

    public TrackerChannelHandle() {
        this.executor = new ThreadPoolExecutor(8,20,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(8));
        this.clientManager = new ClientManager();
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        NetRequest request = new NetRequest(ctx, packet);
        switch (packetType) {
            case CLIENT_REGISTER:
                clientRegisterHandle(request);
                break;
            case CLIENT_HEART_BEAT:
                clientHeartbeatHandel(request);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    protected Set<Integer> interestPackageTypes() {
        return new HashSet<>();
    }

    @Override
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * <p>Description: 客户端注册请求处理</p>
     * <p>Description: Client registration request processing </p>
     *
     * @param request NetWork Request 网络请求
     */
    private void clientRegisterHandle(NetRequest request) throws InvalidProtocolBufferException {
        long clientId = snowFlakeUtils.nextId();
        RegisterRequest registerRequest = RegisterRequest.parseFrom(request.getRequest().getBody());
        boolean register = clientManager.register(registerRequest,clientId);
        if (register) {
            RegisterResponse response = RegisterResponse.newBuilder().setClientId(clientId).build();
            request.sendResponse(response);
        }
    }

    /**
     * <p>Description: 客户端心跳请求处理</p>
     * <p>Description: Client heartbeat request processing </p>
     *
     * @param request NetWork Request 网络请求
     * @throws InvalidProtocolBufferException protobuf error
     */
    private void clientHeartbeatHandel(NetRequest request) throws InvalidProtocolBufferException {
        HeartbeatRequest heartbeatRequest = HeartbeatRequest.parseFrom(request.getRequest().getBody());
        Boolean isSuccess = clientManager.heartbeat(heartbeatRequest.getClientId());
        if (!isSuccess) {
            throw new RuntimeException("Client heartbeat update failed");
        }
    }

}
