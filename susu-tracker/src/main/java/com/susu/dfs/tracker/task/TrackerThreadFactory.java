package com.susu.dfs.tracker.task;

import com.susu.dfs.common.task.BaseThread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class TrackerThreadFactory implements ThreadFactory {

    private boolean daemon;
    private String prefix;
    private AtomicInteger threadId = new AtomicInteger();

    public TrackerThreadFactory(String prefix) {
        this(prefix, true);
    }

    public TrackerThreadFactory(String prefix, boolean daemon) {
        this.prefix = prefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new BaseThread(prefix + threadId.getAndIncrement(), r, daemon);
    }
}
