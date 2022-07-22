package com.susu.dfs.common;

import lombok.Data;

/**
 * @author sujay
 * <p>Description: 集群节点信息</p>
 * @version 11:52 2022/7/22
 */
@Data
public class ClusterInfo {

    private static final String ROLE_MASTER = "master";

    private static final String ROLE_SLAVE = "slave";

    /**
     * 角色
     */
    private String role;

    private String hostname;

    private int port;

    /**
     * 索引下标，根据集群数组而来，下标越大，权重越大
     */
    private Integer index;

    public ClusterInfo(){};

    public ClusterInfo(Integer index,String role,String hostname,int port) {
        this.index = index;
        this.setRole(role);
        this.hostname = hostname;
        this.port = port;
    }

    public void setRole(String role) {
        if (ROLE_MASTER.equals(role)) {
            this.role = ROLE_MASTER;
            return;
        }
        this.role = ROLE_SLAVE;
    }
}
