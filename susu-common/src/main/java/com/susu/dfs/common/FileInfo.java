package com.susu.dfs.common;

import lombok.Data;

/**
 * <p>Description: 文件信息</p>
 *
 * @author sujay
 * @version 17:20 2022/7/12
 */
@Data
public class FileInfo {

    private Long clientId;

    private String fileName;

    private long fileSize;

    public FileInfo() {
    }

    public FileInfo(Long clientId, String fileName, long fileSize) {
        this.clientId = clientId;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
