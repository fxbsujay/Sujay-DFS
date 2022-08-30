package com.susu.dfs.tracker.tomcat.dto;

import lombok.Data;

@Data
public class TrackerDTO {

    /**
     * 地址
     */
    private String host;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 存储路径
     */
    private String filePath;
}
