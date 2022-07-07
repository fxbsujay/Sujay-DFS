package com.susu.dfs.common.task;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: Base Thread</p>
 * <p>Description: 一个最基础的任务线程，只负责打印一些日志</p>
 * @author sujay
 * @version 14:52 2022/7/5
 */
@Slf4j
public class BaseThread extends Thread{

    public BaseThread(final String name, boolean isDaemon) {
        super(name);
        configureThread(name, isDaemon);
    }

    /**
     * @param name 任务名称
     * @param runnable 任务
     * @param isDaemon 是否为守护线程
     */
    public BaseThread(final String name,Runnable runnable, boolean isDaemon) {
        super(runnable, name);
        configureThread(name, isDaemon);
    }

    /**
     * <p>Description: 配置需要启动的任务</p>
     * @param name 任务名称
     * @param isDaemon 是否为守护线程
     */
    private void configureThread(String name, boolean isDaemon) {
        setDaemon(isDaemon);
        setUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception in thread '{}':", name, e));
    }

}
