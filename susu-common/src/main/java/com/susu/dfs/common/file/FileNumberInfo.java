package com.susu.dfs.common.file;

import lombok.Data;

/**
 * <p>Description: Number of file</p>
 * <p>Description: 文件数量</p>
 *
 * @author sujay
 * @version 9:44 2022/7/13
 */
@Data
public class FileNumberInfo {

    /**
     * 文件计数
     */
    private int fileCount;

    /**
     * 文件总大小
     */
    private long totalSize;

    public void addFileCount() {
        this.fileCount++;
    }

    public void addTotalSize(long size) {
        this.totalSize += size;
    }

    public void addFileCount(int fileCount) {
        this.fileCount += fileCount;
    }
}
