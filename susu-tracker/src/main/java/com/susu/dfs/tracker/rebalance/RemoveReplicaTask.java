package com.susu.dfs.tracker.rebalance;

import lombok.Data;

/**
 * @author sujay
 * <p>Description: 移除副本任务</p>
 * @version 17:22 2022/7/12
 */
@Data
public class RemoveReplicaTask {

    /**
     * 客户端
     */
    private String hostname;

    /**
     * 文件名称
     */
    private String filename;

    public RemoveReplicaTask(String hostname, String filename) {
        this.hostname = hostname;
        this.filename = filename;
    }
}

