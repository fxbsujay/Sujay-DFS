package com.susu.dfs.tracker.controller;

import com.google.common.collect.Sets;
import com.susu.common.model.Metadata;
import com.susu.common.model.TrackerSlots;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.service.TrackerFileService;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sujay
 * <p>Description: 数据分片的控制器服务组件抽象类</p>
 * @version 17:12 2022/7/27
 */
@Slf4j
public abstract class AbstractController implements Controller{

    protected int trackerIndex;

    protected ClientManager clientManager;

    protected TrackerFileService fileService;

    protected Map<Integer, Integer> slotsOfTracker;

    protected Map<Integer, Set<Integer>> trackerOfSlots;

    /**
     * 数据平衡组件
     */
    protected RebalancedSlotInfo rebalancedSlotInfo;

    protected AtomicBoolean initCompleted;

    protected ReentrantLock lock;

    protected Condition initCompletedCondition;

    /**
     * 数据槽位分配成功监听调用
     */
    private final List<OnSlotAllocateCompletedListener> slotAllocateCompletedListeners = new ArrayList<>();

    public AbstractController(int trackerIndex) {
        this.trackerIndex = trackerIndex;
        this.initCompleted = new AtomicBoolean(false);
        this.lock = new ReentrantLock();
        this.initCompletedCondition = lock.newCondition();
        loadReadySlots();
    }

    /**
     * <p>Description: Persist to local disk </p>
     * <p>Description: 应用新的槽位配置，持久化到本地磁盘 </p>
     *
     * @param slotOfTracker     槽位对应节点
     * @param trackerOfSlots    节点对应槽位
     * @throws Exception        IO异常
     */
    protected void writeSlots(Map<Integer, Integer> slotOfTracker, Map<Integer, Set<Integer>> trackerOfSlots) throws Exception {
        this.slotsOfTracker = slotOfTracker;
        this.trackerOfSlots = trackerOfSlots;

        TrackerSlots slots = TrackerSlots.newBuilder()
                .putAllNewSlots(this.slotsOfTracker)
                .build();

        ByteBuffer buffer = ByteBuffer.wrap(slots.toByteArray());
        FileUtils.writeFile(fileService.getBaseDir(), true, buffer);
        invokeSlotAllocateCompleted();

        log.info("保存槽位信息到磁盘中：[TrackerIndex={}]", trackerIndex);
    }


    /**
     * <p>Description: Load all slots file information from disk </p>
     * <p>Description: 从磁盘中加载所有的slots文件信息，恢复槽位 </p>
     */
    protected void loadReadySlots() {
        try {
            String baseDir = fileService.getBaseDir();
            File file = new File(baseDir);
            if (!file.exists()) {
                return;
            }

            try ( RandomAccessFile raf = new RandomAccessFile(baseDir, "r");
                  FileInputStream fis = new FileInputStream(raf.getFD());
                  FileChannel channel = fis.getChannel()) {

                ByteBuffer buffer = ByteBuffer.allocate((int) raf.length());
                channel.read(buffer);
                buffer.flip();

                TrackerSlots nameNodeSlots = TrackerSlots.parseFrom(buffer);
                Map<Integer, Integer> slotNodeMap = nameNodeSlots.getNewSlotsMap();
                Map<Integer, Set<Integer>> nodeSlotsMap = map(slotNodeMap);

                this.slotsOfTracker = slotNodeMap;
                this.trackerOfSlots = nodeSlotsMap;
                this.initCompleted.set(true);
                invokeSlotAllocateCompleted();

                log.info("从磁盘中恢复槽位信息：[trackerIndex={}]", trackerIndex);
            }

        } catch (Exception e) {
            log.info("read slots file fail!!");
        }
    }

    protected Map<Integer, Set<Integer>> map(Map<Integer, Integer> slotNodeMap) {
        Map<Integer, Set<Integer>> nodeSlotsMap = new HashMap<>(2);
        for (Map.Entry<Integer, Integer> entry : slotNodeMap.entrySet()) {
            Set<Integer> slots = nodeSlotsMap.computeIfAbsent(entry.getValue(), k -> new HashSet<>());
            slots.add(entry.getKey());
        }
        return nodeSlotsMap;
    }

    /**
     * 触发监听器
     */
    private void invokeSlotAllocateCompleted() {
        Map<Integer, Integer> slotsMap = Collections.unmodifiableMap(slotsOfTracker);
        for (OnSlotAllocateCompletedListener listener : slotAllocateCompletedListeners) {
            listener.onCompleted(slotsMap);
        }
    }

    /**
     *  为该节点应用新快照，并删除老的节点信息中有，新的节点信息中没有的文件
     */
    public void removeMetadata(int rebalancedTrackerIndex) throws Exception {
        log.info("开始执行内存元数据删除. [rebalancedTrackerIndex={}]", rebalancedTrackerIndex);
        Map<Integer, Set<Integer>> oldSlotNodeMap = trackerOfSlots;
        writeSlots(rebalancedSlotInfo.getSlotsOfTrackerSnapshot(), rebalancedSlotInfo.getTrackerOfSlotsSnapshot());
        Map<Integer, Set<Integer>> newSlotNodeMap = trackerOfSlots;

        Set<Integer> oldCurrentSlots = oldSlotNodeMap.get(this.trackerIndex);
        Set<Integer> newCurrentSlots = newSlotNodeMap.get(this.trackerIndex);
        Set<Integer> toRemoveMetadataSlots = Sets.difference(oldCurrentSlots, newCurrentSlots);

        for (Integer toRemoveSlot : toRemoveMetadataSlots) {
            Set<Metadata> metadataSet = fileService.getFilesBySlot(toRemoveSlot);
            if (metadataSet == null) {
                continue;
            }
            for (Metadata metadata : metadataSet) {
                clientManager.removeFileStorage(metadata.getFileName(), false);
            }
        }
    }

    @Override
    public Map<Integer, Integer> getSlot() {
        return Collections.unmodifiableMap(slotsOfTracker);
    }

    @Override
    public void addOnSlotAllocateCompletedListener(OnSlotAllocateCompletedListener listener) {
        slotAllocateCompletedListeners.add(listener);
    }

    @Override
    public int getTrackerIndexBySlot(int slot) {
        return slotsOfTracker.get(slot);
    }
}
