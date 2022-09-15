package com.susu.dfs.tracker.slot;

import com.susu.dfs.common.model.TrackerSlots;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;

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
     * <p>Description: 需要重平衡的节点抓取到其他节点的元数据信息</p>
     *
     * @param request    请求
     * @throws Exception 异常
     */
    default void onFetchMetadata(NetRequest request) throws Exception {
        // TODO 重平衡节点抓取元数据的方法
    }

    /**
     * <p>Description: 让各个需要重平衡的节点删除自己的元数据，并让新的slot分配信息生效</p>
     *
     * <pre>
     *   对于主节点：收到的是需要重平衡的节点，需要移除内存元数据，并发送广播给所有其他的Tracker，让它们也移除元数据
     *   对于一般节点：收到的是主节点的广播信息，需要移除内存元数据，并上报给主节点
     * <pre/>
     *
     * @throws Exception 中断异常
     * @param rebalancedNodeId 重平衡请求发起的节点
     */
    void onFetchSlotMetadataCompleted(int rebalancedNodeId) throws Exception;

    /**
     * <p>Description: 主节点收到其他Tracker节点删除内存元数据的上报请求 </p>
     *
     * @param packet     请求
     * @throws Exception 序列化异常
     */
    default void onRemoveMetadataCompleted(NetPacket packet) throws Exception {}

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
