package com.susu.common.task;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Description: Task Scheduler</p>
 * <p>Description: 任务调度器</p>
 * @author sujay
 * @version 14:45 2022/7/5
 */
@Slf4j
public class TaskScheduler {

    /**
     * 默认启动线程池
     */
    private ScheduledThreadPoolExecutor executor;

    /**
     * 计数器
     */
    private AtomicInteger taskId = new AtomicInteger(0);

    private AtomicBoolean shutdown = new AtomicBoolean(true);

    public TaskScheduler(String threadNamePrefix) {
        this(threadNamePrefix, Runtime.getRuntime().availableProcessors() * 2, true);
    }

    public TaskScheduler(String threadNamePrefix, int threads) {
        this(threadNamePrefix, threads, true);
    }

    /**
     * <p>Description: 初始化调度器，并设置该任务随着调度器结束而结束</p>
     *
     * @param schedulerName 任务调度器的名称
     * @param corePoolSize  任务数量
     * @param isDaemon      该任务是否为守护线程
     */
    public TaskScheduler(String schedulerName, int corePoolSize,boolean isDaemon) {
        if (shutdown.compareAndSet(true, false)) {
            executor = new ScheduledThreadPoolExecutor(corePoolSize,
                    r -> new BaseThread(schedulerName + "-" + taskId.getAndIncrement(), r, isDaemon));
            executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }
    }

    /**
     * <p>Description: 执行一个任务</p>
     *
     * @param name 任务名称
     * @param r    任务
     */
    public void scheduleOnce(String name, Runnable r) {
        scheduleOnce(name, r, 0);
    }

    /**
     * <p>Description: 执行一个任务</p>
     *
     * @param name  任务名称
     * @param r     任务
     * @param delay 延迟
     */
    public void scheduleOnce(String name, Runnable r, long delay) {
        schedule(name, r, delay, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * <p>Description: 执行任务</p>
     *
     * @param name     任务名称
     * @param r        任务
     * @param delay    延迟执行时间
     * @param period   调度周期
     * @param timeUnit 时间单位
     */
    public void schedule(String name, Runnable r, long delay, long period, TimeUnit timeUnit) {
        if (log.isTraceEnabled()) {
            log.debug("Scheduling task {} with initial delay {} ms and period {} ms.", name, delay, period);
        }
        Runnable delegate = () -> {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Beginning execution of scheduled task {}.", name);
                }
                String loggerId = String.valueOf(System.nanoTime() + new Random().nextInt());
                MDC.put("logger_id", loggerId);
                r.run();
            } catch (Throwable e) {
                log.error("Uncaught exception in scheduled task {} :", name, e);
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace("Completed execution of scheduled task {}.", name);
                }
                MDC.remove("logger_id");
            }
        };
        if (shutdown.get()) {
            return;
        }
        if (period > 0) {
            executor.scheduleWithFixedDelay(delegate, delay, period, timeUnit);
        } else {
            executor.schedule(delegate, delay, timeUnit);
        }
    }

    /**
     * 关闭调度器，终止所有任务
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("Shutdown Task Scheduler.");
            executor.shutdown();
        }
    }

    public static void main(String[] args) {
        TaskScheduler taskScheduler = new TaskScheduler("DataNode-Scheduler");
        taskScheduler.scheduleOnce("Test Task",() -> {
            log.info("==============test==============");
        });

    }

}
