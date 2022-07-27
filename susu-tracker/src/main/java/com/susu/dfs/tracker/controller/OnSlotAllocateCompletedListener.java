package com.susu.dfs.tracker.controller;

import java.util.Map;

/**
 * @author sujay
 * <p>Description: 数据分配成功监听器</p>
 * @version 17:23 2022/7/27
 */
public interface OnSlotAllocateCompletedListener {

    /**
     *  分配成功
     */
    void onCompleted(Map<Integer, Integer> slots);
}
