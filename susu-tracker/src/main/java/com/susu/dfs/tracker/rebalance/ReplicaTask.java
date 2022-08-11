package com.susu.dfs.tracker.rebalance;

import lombok.Data;

/**
 * 副本复制任务
 */
@Data
public class ReplicaTask {

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 客户端Id
     */
    private String hostname;

    /**
     * 端口
     */
    private int port;

    public ReplicaTask(String filename, String hostname, int port) {
        this.filename = filename;
        this.hostname = hostname;
        this.port = port;
    }
}
