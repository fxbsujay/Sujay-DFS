package com.susu.dfs.tracker.slot;

import lombok.Data;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sujay
 * <p>Description: 重平衡Slots信息</p>
 * @version 17:51 2022/7/27
 */
@Data
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

    /**
     * 需要等待数据同步的节点
     */
    private Set<Integer> fetchMetadataWaitTrackerSet;

    /**
     * 数据同步完成的节点
     */
    private Set<Integer> fetchMetadataCompletedTrackerSet;

    private Condition fetchMetadataCompletedCondition;

    private CountDownLatch removeMetadataCompleteCountDownLatch;

    /**
     * 删除数据完成的节点
     */
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

    public boolean onFetchSlotMetadataCompleted(int rebalancedTrackerIndex) {
        synchronized (this) {
            removeMetadataCompletedTrackerSet.add(rebalancedTrackerIndex);
            return isAllCompleted(trackerIdSet, removeMetadataCompletedTrackerSet);
        }
    }

    /**
     * 某个节点完成所有的元数据删除
     */
    public boolean onCompletedRemoveMetadata() {
        removeMetadataCompleteCountDownLatch.countDown();
        return removeMetadataCompleteCountDownLatch.getCount() == 0;
    }


    /**
     * <p>Description: 是否全部完成 </p>
     *
     * @param waitSet           等待完成的id
     * @param completedSet      已经完成的
     * @return                  是否全部完成
     */
    private boolean isAllCompleted(Set<Integer> waitSet, Set<Integer> completedSet) {
        boolean allCompleted = true;
        for (Integer nodeId : waitSet) {
            if (!completedSet.contains(nodeId)) {
                allCompleted = false;
                break;
            }
        }
        return allCompleted;
    }


    /**
     * <p>Description: 作为新加入的节点等待其他 tracker 所有的元数据发送过来 </p>
     *
     * @param waitMetadataNodeSet 目标列表
     * @throws InterruptedException 异常
     */
    public void waitFetchMetadataCompleted(Set<Integer> waitMetadataNodeSet) throws InterruptedException {
        this.fetchMetadataCompletedTrackerSet = new HashSet<>();
        this.fetchMetadataWaitTrackerSet = waitMetadataNodeSet;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            fetchMetadataCompletedCondition.await();
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>Description: 在这个节点完成数据同步 </p>
     *
     * @param trackerIndex  节点标记
     */
    public void onCompletedFetchMetadata(int trackerIndex) {
        synchronized (this) {
            fetchMetadataCompletedTrackerSet.add(trackerIndex);
            boolean allCompleted = isAllCompleted(fetchMetadataWaitTrackerSet, fetchMetadataCompletedTrackerSet);
            if (allCompleted) {
                lock.lock();
                try {
                    fetchMetadataCompletedCondition.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
