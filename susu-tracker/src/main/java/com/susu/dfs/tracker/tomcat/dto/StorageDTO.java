package com.susu.dfs.tracker.tomcat.dto;

import lombok.Data;

@Data
public class StorageDTO {

    /**
     * 地址
     */
    private String host;

    /**
     * 状态
     */
    private Boolean status;

    /**
     * 存储大写
     */
    private Long storedSize;

    /**
     * 存储路径
     */
    private String filePath;
}
