package com.susu.dfs.server;

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

    public TrackerChannelHandle() {
        this.executor = new ThreadPoolExecutor(8,20,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(8));
    }

    @Override
    protected boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception {
        PacketType packetType = PacketType.getEnum(packet.getType());
        switch (packetType) {
            case CLIENT_REGISTER:
                log.info("收到了客户端的注册消息");
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
    public void clientRegisterHandle() {

    }
}
