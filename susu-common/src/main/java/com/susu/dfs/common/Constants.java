package com.susu.dfs.common;

/**
 * <p>Description: Constants</p>
 * <p>Description: 常量</p>
 * @author sujay
 * @version 15:56 2022/7/6
 */
public class Constants {

    /**
     * 系统版本号
     */
    public static final int SYS_VERSION = 1;

    /**
     *  系统授权名称
     */
    public static final String SYS_AUTHOR = "SUSA";

    /**
     * Netty 最大传输字节数
     */
    public static final int MAX_BYTES = 10 * 1024 * 1024;

    /**
     * 客户端心跳间隔
     */
    public static final int HEARTBEAT_INTERVAL = 30000;

    /**
     * 客户端存活时间
     */
    public static final int HEARTBEAT_OUT_TIME = 600000;

    /**
     * 客户端存活检测时间间隔
     */
    public static final int HEARTBEAT_CHECK_INTERVAL = 30000;

    /**
     * slot槽位的总数量
     */
    public static final int SLOTS_COUNT = 16384;
}
