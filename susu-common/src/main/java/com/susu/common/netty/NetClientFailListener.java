package com.susu.common.netty;


/**
 * <p>Description: 客户端连接失败监听器</p>
 * @author sujay
 * @version 9:40 2022/7/7
 */
public interface NetClientFailListener {

    /**
     * 连接是失败触发该方法
     */
    void onConnectFail();
}
