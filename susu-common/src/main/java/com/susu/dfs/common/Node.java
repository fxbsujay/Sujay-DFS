package com.susu.dfs.common;

import lombok.Data;
import java.util.List;

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
    private boolean isMaster;

    /**
     * 调度器端口
     */
    private Integer trackerPort;

    /**
     * 调度器地址
     */
    private String trackerHost;

    /**
     * 是否为集群模式
     */
    private boolean isCluster;

    /**
     * 当前节点所在集群下标
     */
    private Integer index;

    /**
     * 集群配置，key为数组的index
     */
    private List<String> servers;


}
