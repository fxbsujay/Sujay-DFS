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

    /**
     * 默认的文件目录
     */
    public static final String DEFAULT_BASE_DIR = "E:\\srv\\file";


    /**
     * 文件垃圾箱目录
     */
    public static final String TRASH_DIR = ".Trash";

    /**
     * 缓冲区的大小
     */
    public static final int READY_LOG_FLUSH_THRESHOLD = 524288;

    /**
     * 文件删除后能被垃圾清理任务清理的时间
     */
    public static final int TRASH_CLEAR_THRESHOLD = 86400000;


    /**
     *  垃圾清理任务的时间间隔
     */
    public static final int TRASH_CLEAR_INTERVAL = 3600000;

    /**
     * 文件属性之删除时间
     */
    public static final String ATTR_FILE_DEL_TIME = "DEL_TIME";


    /**
     * 文件属性之文件大小
     */
    public static final String ATTR_FILE_SIZE = "FILE_SIZE";

    /**
     * 目录树文件名称
     */
    public static final String IMAGE_LOG_NAME = "image_log_";

    /**
     * 磁盘操作日志文件名称
     */
    public static final String READY_LOG_NAME = "ready_log_";
}
