package com.susu.dfs.tracker.rebalance;

import lombok.Data;

/**
 * 副本复制任务
 *
 * @author Sun Dasheng
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
    private Long clientId;

    /**
     * 端口
     */
    private int port;

    public ReplicaTask(String filename, Long clientId, int port) {
        this.filename = filename;
        this.clientId = clientId;
        this.port = port;
    }
}
