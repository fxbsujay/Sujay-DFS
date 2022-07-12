package com.susu.dfs.tracker.service;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.file.AbstractFileService;
import com.susu.dfs.common.file.log.DoubleBuffer;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.task.TrashPolicyTask;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * <p>Description: Tracker 文件目录树的服务</p>
 *
 * @author sujay
 * @version 9:34 2022/7/12
 */
@Slf4j
public class TrackerFileService extends AbstractFileService {

    /**
     * 操作缓存
     */
    private DoubleBuffer doubleBuffer;

    public TrackerFileService(TaskScheduler taskScheduler, ClientManager clientManager) {
        super();
        this.doubleBuffer = new DoubleBuffer();
        TrashPolicyTask trashPolicyTask = new TrashPolicyTask(this,clientManager);
        taskScheduler.schedule("定时扫描物理删除文件",trashPolicyTask, Constants.TRASH_CLEAR_INTERVAL, Constants.TRASH_CLEAR_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void recoveryNamespace() throws Exception {

    }

    /**
     * 优雅停机
     * 强制把内存里的edits log刷入磁盘中
     */
    public void shutdown() {
        log.info("Shutdown DiskNameSystem.");
        this.doubleBuffer.flushBuffer();
    }

    /**
     * 获取DoubleBuffer
     *
     * @return editLog
     */
    public DoubleBuffer getEditLog() {
        return doubleBuffer;
    }
}
