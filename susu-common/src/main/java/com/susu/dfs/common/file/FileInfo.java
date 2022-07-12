package com.susu.dfs.common.file;

import lombok.Data;

/**
 * <p>Description: 文件信息</p>
 *
 * @author sujay
 * @version 17:20 2022/7/12
 */
@Data
public class FileInfo {

    private String hostname;

    private String fileName;

    private long fileSize;

    public FileInfo() {
    }

    public FileInfo(String hostname, String fileName, long fileSize) {
        this.hostname = hostname;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
