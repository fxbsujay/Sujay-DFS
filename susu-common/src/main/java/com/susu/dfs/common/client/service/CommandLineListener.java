package com.susu.dfs.common.client.service;


/**
 * <p>Description: 消息响应监听器</p>
 *
 * @author sujay
 * @version 14:56 2022/10/28
 */
public interface CommandLineListener {

    /**
     * 连接失败监听器
     */
    void onConnectFailed();

    /**
     * 认证结果
     *
     * @param result 结果
     */
    void onAuthResult(boolean result);

}
