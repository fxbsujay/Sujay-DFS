package com.susu.dfs.tracker.cluster;

import com.susu.dfs.common.TrackerInfo;

/**
 * @author sujay
 * <p>Description: 抽象的集群管理服务API</p>
 *
 * @version 11:52 2022/7/22
 */
public abstract class AbstractTrackerCluster implements TrackerCluster{

    private TrackerInfo tracker;

    /**
     * 当前节点下标
     */
    protected int currentIndex;

    /**
     * 目标节点下标
     */
    private int targetIndex;

    public AbstractTrackerCluster(int currentIndex, int targetIndex, TrackerInfo tracker) {
        this.currentIndex = currentIndex;
        this.targetIndex = targetIndex;
        this.tracker = tracker;
    }

    @Override
    public int getTargetIndex() {
        return targetIndex;
    }

    @Override
    public TrackerInfo getServer() {
        return tracker;
    }
}
