package com.susu.dfs.tracker.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.common.model.RegisterRequest;
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

    public TrackerChannelHandle() {
        this.executor = new ThreadPoolExecutor(8,20,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(8));
        this.clientManager = new ClientManager();
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        switch (packetType) {
            case CLIENT_REGISTER:
                clientRegisterHandle(packet);
                break;
            case TEST:
                log.info("测试请求");
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
     * 客户端注册方法
     */
    public void clientRegisterHandle(NetPacket packet) throws InvalidProtocolBufferException {
        RegisterRequest registerRequest = RegisterRequest.parseFrom(packet.getBody());
        boolean register = clientManager.register(registerRequest);
        if (register) log.info("=========================注册成功=========================");
        else log.info("=========================注册失败=========================");
    }
}
