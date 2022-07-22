package com.susu.dfs.tracker.cluster;

import com.susu.dfs.common.ClusterInfo;

/**
 * @author sujay
 * <p>Description: 抽象的集群管理服务API</p>
 * @version 11:52 2022/7/22
 */
public abstract class AbstractTrackerCluster implements TrackerCluster{

    private ClusterInfo cluster;

    /**
     * 当前节点下标
     */
    protected int currentIndex;

    /**
     * 目标节点下标
     */
    private int targetIndex;

    public AbstractTrackerCluster(int currentIndex, int targetIndex, ClusterInfo cluster) {
        this.currentIndex = currentIndex;
        this.targetIndex = targetIndex;
        this.cluster = cluster;
    }

    @Override
    public int getTargetIndex() {
        return targetIndex;
    }

    @Override
    public ClusterInfo getServer() {
        return cluster;
    }
}
