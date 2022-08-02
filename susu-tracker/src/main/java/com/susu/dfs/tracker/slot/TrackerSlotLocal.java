package com.susu.dfs.tracker.slot;


import com.susu.dfs.common.Constants;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author sujay
 * <p>Description: 本地 Slot组件</p>
 * @version 15:37 2022/7/27
 */
@Slf4j
public class TrackerSlotLocal extends AbstractTrackerSlot {

    private TrackerClusterService trackerClusterService;

    public TrackerSlotLocal(int trackerIndex, TrackerClusterService trackerClusterService, TrackerFileService trackerFileService) {
        super(trackerIndex,trackerFileService);
        this.trackerClusterService = trackerClusterService;
    }

    @Override
    public Map<Integer, Integer> initSlots() throws Exception {
        if (!initCompleted.get()) {
            List<Integer> trackerIndexList = trackerClusterService.getAllTrackerIndex();
            trackerIndexList.add(trackerIndex);
            HashMap<Integer, Integer> slotTrackerMap = new HashMap<>(Constants.SLOTS_COUNT);
            HashMap<Integer, Set<Integer>> trackerSlotMap = new HashMap<>(trackerClusterService.getAllTracker().size());
            for (int i = 0; i < Constants.SLOTS_COUNT; i++) {
                int index = i % trackerIndexList.size();
                Integer trackerIndex = trackerIndexList.get(index);
                slotTrackerMap.put(i,trackerIndex);
                Set<Integer> slots = trackerSlotMap.computeIfAbsent(trackerIndex, k -> new HashSet<>());
                slots.add(i);
            }
            replaceSlots(slotTrackerMap,trackerSlotMap);
        }
        Set<Integer> slots = trackerOfSlots.get(trackerIndex);
        log.info("初始化slot信息: [trackerIndex={},slots size={}]",trackerIndex,slots.size());
        return Collections.unmodifiableMap(slotsOfTracker);

    }

    @Override
    public void rebalancedSlots(NetPacket packet) throws Exception {

    }
}
