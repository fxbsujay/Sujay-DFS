package com.susu.dfs.common;

import lombok.Data;

/**
 * <p>Description: Tracker 节点信息</p>
 *
 * @author sujay
 * @version 13:38 2022/7/19
 */
@Data
public class TrackerInfo {

    public static final String ROLE_MASTER = "master";

    public static final String ROLE_SLAVE = "slave";

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

    public TrackerInfo(){};

    public TrackerInfo(Integer index,String role,String hostname,int port) {
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
