package com.susu.dfs.tracker.controller;

import com.susu.common.model.TrackerSlots;
import com.susu.dfs.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author sujay
 * <p>Description: 数据分片的控制器服务组件抽象类</p>
 * @version 17:12 2022/7/27
 */
@Slf4j
public abstract class AbstractController implements Controller{


    protected int trackerIndex;

    /**
     * Slots持久化文件的存储路径
     */
    private final String baseDir;

    protected Map<Integer, Integer> slotsOfTracker;

    protected Map<Integer, Set<Integer>> trackerOfSlots;

    protected AtomicBoolean initCompleted;


    /**
     * 数据槽位分配成功监听调用
     */
    private final List<OnSlotAllocateCompletedListener> slotAllocateCompletedListeners = new ArrayList<>();


    public AbstractController(String baseDir, int trackerIndex) {
        this.baseDir = baseDir;
        this.trackerIndex = trackerIndex;
        this.initCompleted = new AtomicBoolean(false);
        loadReadySlots();
    }

    /**
     * <p>Description: Persist to local disk </p>
     * <p>Description: 持久化到本地磁盘 </p>
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
        FileUtils.writeFile(baseDir, true, buffer);
        invokeSlotAllocateCompleted();

        log.info("保存槽位信息到磁盘中：[TrackerIndex={}]", trackerIndex);
    }


    /**
     * <p>Description: Load all slots file information from disk </p>
     * <p>Description: 从磁盘中加载所有的slots文件信息 </p>
     */
    protected void loadReadySlots() {
        try {

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
