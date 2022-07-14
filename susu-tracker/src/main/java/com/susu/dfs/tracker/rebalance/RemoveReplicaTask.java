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
     * 客户端ID
     */
    private Long clientId;

    /**
     * 文件名称
     */
    private String fileName;

    public RemoveReplicaTask(Long clientId, String fileName) {
        this.clientId = clientId;
        this.fileName = fileName;
    }
}

