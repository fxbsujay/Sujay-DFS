package com.susu.dfs.common;

import lombok.Data;

/**
 * <p>Description: 启动的节点信息</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Data
public class Node {

    /**
     * 实例id
     */
    private Long id;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 默认启动端口
     */
    private Integer port = 8090;

    /**
     * 服务端地址
     */
    private String host = "localhost";

    /**
     * 是否为主节点
     */
    private Boolean isMaster;


}
