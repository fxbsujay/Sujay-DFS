package com.susu.dfs.common.netty.msg;

import com.susu.dfs.common.eum.PacketType;
import lombok.extern.slf4j.Slf4j;


/**
 * <p>Description: 网络同步请求获取结果</p>
 * <p>Description: network Sync Request</p>
 *
 * @author sujay
 * @version 1:04 2022/7/10
 */
@Slf4j
public class NetSyncRequestPromise {

    /**
     * 请求
     */
    private NetPacket request;

    /**
     * 响应
     */
    private NetPacket response;

    /**
     * 请求时间
     */
    private final long startTime;

    /**
     * 请求超时时间
     */
    private final long TIME_OUT = 60 * 1000;

    /**
     * 是否超时
     */
    private boolean isTimeout;

    /**
     * 结果是否成功返回
     */
    private volatile boolean receiveResponseCompleted = false;

    public NetSyncRequestPromise(NetPacket request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 获取响应结果
     *
     * @return 获取响应结果
     */
    public NetPacket getResult() throws RuntimeException {
        waitForResult();
        return response;
    }

    /**
     * 结果返回
     *
     * @param nettyPacket nettyPacket
     */
    public void setResult(NetPacket nettyPacket) {
        synchronized (this) {
            this.response = nettyPacket;
            this.receiveResponseCompleted = true;
            notifyAll();
        }
    }

    /**
     * 等到结果返回
     */
    protected void waitForResult() throws RuntimeException {
        synchronized (this) {
            try {
                while (!receiveResponseCompleted && !isTimeout) {
                    wait(10);
                }
                if (isTimeout) {
                    if (log.isDebugEnabled()) {
                        log.debug("Synchronization request timed out：[cost={} s, request={}, sequence={}]", (System.currentTimeMillis() - startTime) / 1000.0D,
                                PacketType.getEnum(request.getType()).getDescription(), request.getSequence());
                    }
                    throw new RuntimeException("Synchronization request timed out: " +
                            PacketType.getEnum(request.getType()).getDescription());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Synchronization request succeeded：[cost={} s, request={}, sequence={}]",
                                (System.currentTimeMillis() - startTime) / 1000.0D,
                                PacketType.getEnum(response.getType()).getDescription(),
                                response.getSequence());
                    }
                }
            } catch (InterruptedException e) {
                log.info("NetSyncRequestPromise waitForResult exception!!");
            }
        }
    }

    /**
     * 判断是否超时
     *
     * @return 结果
     */
    public boolean isTimeout() {
        return isTimeout(TIME_OUT);
    }

    /**
     * 判断是否超时
     *
     * @param timeout 超时
     * @return 结果
     */
    public boolean isTimeout(long timeout) {
        if (this.isTimeout) {
            return true;
        }
        long now = System.currentTimeMillis();
        return startTime + timeout < now;
    }


    /**
     * 标记为超时
     */
    public void markTimeout() {
        if (this.isTimeout) {
            return;
        }
        isTimeout = true;
        synchronized (this) {
            notifyAll();
        }
    }
}