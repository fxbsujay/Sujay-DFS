package com.susu.dfs.tracker.client;

import com.susu.dfs.tracker.rebalance.RemoveReplicaTask;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private Long clientId;

    /**
     * 客户端Name
     */
    private String name;

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

    /**
     * 当前存储量
     */
    private volatile long storedSize;

    private volatile long freeSpace;

    /**
     * 删除副本的任务
     */
    private ConcurrentLinkedQueue<RemoveReplicaTask> removeReplicaTasks = new ConcurrentLinkedQueue<>();


    public ClientInfo(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.latestHeartbeatTime = System.currentTimeMillis();
        this.storedSize = 0L;
        this.status = STATUS_INIT;
    }

    public List<RemoveReplicaTask> pollRemoveReplicaTask(int maxNum) {
        List<RemoveReplicaTask> result = new LinkedList<>();

        for (int i = 0; i < maxNum; i++) {
            RemoveReplicaTask task = removeReplicaTasks.poll();
            if (task == null) {
                break;
            }
            result.add(task);
        }
        return result;
    }

    /**
     * 增加存储信息
     *
     * @param fileSize 文件大小
     */
    public void addStoredDataSize(long fileSize) {
        synchronized (this) {
            this.storedSize += fileSize;
            this.freeSpace -= fileSize;
        }
    }

    public void addRemoveReplicaTask(RemoveReplicaTask task) {
        removeReplicaTasks.add(task);
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
