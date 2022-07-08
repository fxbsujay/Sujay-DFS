package com.susu.dfs.tracker.client;

import lombok.Data;

import java.util.Objects;

/**
 * @author sujay
 * <p>Description: 客户端信息</p>
 * @version 13:14 2022/7/8
 */
@Data
public class ClientInfo {

    /**
     * 心跳检测初始化
     */
    public static final int STATUS_INIT = 1;

    /**
     * 心跳检测就绪
     */
    public static final int STATUS_READY = 2;

    /**
     * 客户端Id
     */
    private Integer clientId;

    /**
     * 节点地址
     */
    private String hostname;

    /**
     * 客户端端口号
     */
    private int port;

    /**
     * 客户端状态
     */
    private int status;

    /**
     * 最近一次心跳时间
     */
    private long latestHeartbeatTime;


    public ClientInfo(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.latestHeartbeatTime = System.currentTimeMillis();
        this.status = STATUS_INIT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientInfo that = (ClientInfo) o;
        return port == that.port &&
                Objects.equals(hostname, that.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port);
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                '}';
    }
}
