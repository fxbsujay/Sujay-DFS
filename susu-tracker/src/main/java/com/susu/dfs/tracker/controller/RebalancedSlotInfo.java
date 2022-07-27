package com.susu.dfs.tracker.controller;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sujay
 * <p>Description: 重平衡Slots信息</p>
 * @version 17:51 2022/7/27
 */
public class RebalancedSlotInfo {

    /**
     * 申请发起重平衡的节点ID
     */
    private int applyTrackerId;

    /**
     * 所有节点的ID
     */
    private List<Integer> trackerIdList;

    /**
     * 本次重平衡包含的节点ID列表
     */
    private Set<Integer> trackerIdSet;

    /**
     * Slots分配的快照
     */
    private Map<Integer, Integer> slotsOfTrackerSnapshot;

    /**
     * Slots分配的快照
     */
    private Map<Integer, Set<Integer>> trackerOfSlotsSnapshot;

    private ReentrantLock lock;

    private Set<Integer> fetchMetadataWaitTrackerSet;

    private Set<Integer> fetchMetadataCompletedTrackerSet;

    private Condition fetchMetadataCompletedCondition;

    private CountDownLatch removeMetadataCompleteCountDownLatch;

    private Set<Integer> removeMetadataCompletedTrackerSet = new HashSet<>();

    public RebalancedSlotInfo() {
        this.lock = new ReentrantLock();
        this.fetchMetadataCompletedCondition = lock.newCondition();
    }


    /**
     * <p>Description: 设置快照信息 </p>
     * <p>Description: set snapshot information </p>
     *
     * @param slotsOfTrackerSnapshot 快照
     */
    public void setSlotsOfTrackerSnapshot(Map<Integer, Integer> slotsOfTrackerSnapshot) {
        this.slotsOfTrackerSnapshot = slotsOfTrackerSnapshot;
        this.trackerOfSlotsSnapshot = new HashMap<>(2);
        for (Map.Entry<Integer, Integer> entry : slotsOfTrackerSnapshot.entrySet()) {
            Set<Integer> slots = trackerOfSlotsSnapshot.computeIfAbsent(entry.getValue(), k -> new HashSet<>());
            slots.add(entry.getKey());
        }
    }

    public Set<Integer> getSlotsFor(int nodeId) {
        return trackerOfSlotsSnapshot.getOrDefault(nodeId, new HashSet<>());
    }


    /**
     * <p>Description: 等到整个重平衡过程结束 </p>
     * <p>Description: Wait until the whole balancing process is over </p>
     * <pre>
     *     For Example:
     *
     *          比如一开始3个节点[1, 2, 3]
     *          此时新加入2个节点[1, 2, 3, 4, 5]
     *
     *          那么需要等待[4, 5]两个节点都完成了拉取元数据，
     *          并且等到 [1, 2, 3] 上报移除了不属于自己的元数据的时候，这里才算重平衡结束
     *
     * </pre>
     * @param rebalancedTrackerIndexList 本次重平衡的需要的节点数量
     * @throws InterruptedException 异常
     */
    public void waitRemoveMetadataCompleted(Set<Integer> rebalancedTrackerIndexList) throws InterruptedException {
        this.removeMetadataCompleteCountDownLatch = new CountDownLatch(trackerIdList.size() - rebalancedTrackerIndexList.size() - 1);
        this.removeMetadataCompleteCountDownLatch.await();
    }

}
