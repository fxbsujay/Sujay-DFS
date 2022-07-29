package com.susu.dfs.tracker.slot;

import java.util.Map;

/**
 * @author sujay
 * <p>Description: 数据分配成功监听器</p>
 * @version 17:23 2022/7/27
 */
public interface OnSlotCompletedListener {

    /**
     *  分配成功
     */
    void onCompleted(Map<Integer, Integer> slots);
}
