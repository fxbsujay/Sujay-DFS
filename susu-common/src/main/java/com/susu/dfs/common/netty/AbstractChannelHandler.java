package com.susu.dfs.common.netty;

import com.susu.dfs.common.netty.msg.NetPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * <p>Description: 消息处理器的顶级抽象类，其他消息处理器需继承该抽象类</p>
 * @author sujay
 * @version 11:04 2022/7/7
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractChannelHandler extends ChannelInboundHandlerAdapter {


    private Set<Integer> interestPackageTypes;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("AbstractChannelHandler exception caught：", cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("消息内容 channelRead： {}",msg);
        Executor executor = getExecutor();
        if (executor != null) {
            executor.execute(() -> channelReadInternal(ctx, msg));
        }else {
            channelReadInternal(ctx, msg);
        }
    }

    private void channelReadInternal(ChannelHandlerContext ctx, Object msg) {
        try {
            String loggerId = String.valueOf(System.nanoTime() + new Random().nextInt());
            MDC.put("logger_id", loggerId);
            NetPacket nettyPacket = (NetPacket) msg;
            boolean consumedMsg = false;
            if (getPackageTypes().isEmpty() || getPackageTypes().contains(nettyPacket.getType())) {
                try {
                    consumedMsg = handlePackage(ctx, nettyPacket);
                } catch (Exception e) {
                    log.info("处理请求发生异常：", e);
                }
            }
            if (!consumedMsg) {
                ctx.fireChannelRead(msg);
            }
        } finally {
            MDC.remove("logger_id");
        }
    }

    /**
     * <p>Description: 获取执行器，如果返回执行器，则表示请求逻辑在执行器中执行</p>
     * <p>Description: Get the actuator. If the actuator is returned, it means that the request logic is executed in the actuator</p>
     *
     * @return Executor
     */
    protected Executor getExecutor() {
        return null;
    }

    /**
     * <p>Description: 处理网络包</p>
     *
     * @param ctx          上下文
     * @param packet 网络包
     * @return 是否消费了该消息
     * @throws Exception 序列化异常
     */
    protected abstract boolean handlePackage(ChannelHandlerContext ctx, NetPacket packet) throws Exception;

    /**
     * <p>Description: 获取消息类型集合</p>
     *
     * @return Set<PacketType>
     */
    private Set<Integer> getPackageTypes() {
        if (interestPackageTypes == null) {
            interestPackageTypes = interestPackageTypes();
        }
        return interestPackageTypes;
    }

    /**
     * <p>Description: 需要处理的消息类型集合</p>
     *
     * @return 需要处理的消息类型集合, 返回空集合表示所有信息都感兴趣
     */
    protected abstract Set<Integer> interestPackageTypes();

}
