package com.susu.dfs.common;

import lombok.Data;

import java.util.List;

/**
 * <p>Description: Storage 节点信息</p>
 *
 * @author sujay
 * @version 13:38 2022/7/19
 */
@Data
public class StorageInfo {


    /**
     * 文件信息
     */
    private List<FileInfo> files;

    /**
     * 已用空间
     */
    private long storageSize;

    /**
     * 可用空间
     */
    private long freeSpace;

    public StorageInfo() {
        this.storageSize = 0L;
    }
}
