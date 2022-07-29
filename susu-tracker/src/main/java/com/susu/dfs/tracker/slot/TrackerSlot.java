package com.susu.dfs.tracker.slot;

import com.susu.common.model.TrackerSlots;
import com.susu.dfs.common.netty.msg.NetPacket;
import java.util.Map;

/**
 * @author sujay
 * <p>Description: 数据分片的控制器服务组件</p>
 * @version 16:57 2022/7/27
 */
public interface TrackerSlot {


    /**
     * <p>Description: 组件初始化</p>
     */
    Map<Integer, Integer> initSlots() throws Exception;

    /**
     * <p>Description: 接收Slots信息</p>
     *
     * @param slots slots信息
     * @throws Exception 异常
     */
    default void onReceiveSlots(TrackerSlots slots) throws Exception {
        // TODO 默认收到Slot信息所触发的方法
    }

    /**
     * <p>Description: 重新平衡Slots</p>
     *
     * @param packet     网络包
     * @throws Exception 异常
     */
    void rebalancedSlots(NetPacket packet) throws Exception;

    /**
     * <p>Description: 获取 Slots</p>
     *
     * @return slot信息
     */
    Map<Integer, Integer> getSlot();

    /**
     * 根据Slot返回节点id
     *
     * @param slot slot槽位
     * @return 节点标记
     */
    int getTrackerIndexBySlot(int slot);

    /**
     * 添加slot分配完成的监听器
     *
     * @param listener 监听器
     */
    void addOnSlotCompletedListener(OnSlotCompletedListener listener);

}
