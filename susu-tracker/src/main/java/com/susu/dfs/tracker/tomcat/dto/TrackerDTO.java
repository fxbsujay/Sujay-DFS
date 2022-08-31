package com.susu.dfs.tracker.tomcat.dto;

import lombok.Data;

@Data
public class TrackerDTO {

    /**
     * 地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 端口
     */
    private Integer httpPort;

    /**
     * 目录树路径
     */
    private String baseDir;

    /**
     * 操作记录文件路径
     */
    private String logBaseDir;

    /**
     * 总存储大写
     */
    private Long totalStoredSize;

    /**
     * 总存储大写
     */
    private Long fileCount;


}
