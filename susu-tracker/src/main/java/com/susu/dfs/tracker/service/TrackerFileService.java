package com.susu.dfs.tracker.service;

import com.susu.common.model.ReadyLog;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.eum.ReadyLogType;
import com.susu.dfs.common.file.AbstractFileService;
import com.susu.dfs.common.file.image.ImageLogWrapper;
import com.susu.dfs.common.file.log.DoubleBuffer;
import com.susu.dfs.common.file.log.ReadyLogWrapper;
import com.susu.dfs.common.task.ClearImageTask;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.task.TrashPolicyTask;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
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
     * 操作缓存 写磁盘操作日志
     */
    private DoubleBuffer doubleBuffer;

    private TaskScheduler taskScheduler;

    private TrashPolicyTask trashPolicyTask;

    private ClearImageTask clearImageTask;

    /**
     * Ready_Log文件的存储路径
     */
    private final String baseDir;

    private volatile long maxTxId = 0L;

    public TrackerFileService(SysConfig config,TaskScheduler taskScheduler, ClientManager clientManager) {
        super(config.DEFAULT_BASE_DIR);
        this.taskScheduler = taskScheduler;
        this.baseDir = config.SYS_LOG_BASE_DIR;
        this.doubleBuffer = new DoubleBuffer(baseDir);
        clientManager.setTrackerFileService(this);
        this.trashPolicyTask =  new TrashPolicyTask(this,clientManager);
        this.clearImageTask = new ClearImageTask(baseDir,this,doubleBuffer);
    }

    private void txIdIncrement() {
        synchronized (this) {
            maxTxId++;
        }
    }

    /**
     * 设置当前最大的TxId
     *
     * @param maxTxId TxId
     */
    public void setMaxTxId(Long maxTxId) {
        this.maxTxId = maxTxId;
    }

    @Override
    public void recoveryNamespace() throws Exception {
        try {
            ImageLogWrapper image = scanLatestValidImageLog(baseDir);
            long txId = 0L;
            if (image != null) {
                txId = image.getMaxTxId();
                readImage(image);
            }
            this.doubleBuffer.playbackReadyLog(txId, obj -> {
                ReadyLog readyLog = obj.getReadyLog();
                int type = readyLog.getType();
                if (type == ReadyLogType.MKDIR.getValue()) {
                    super.mkdir(readyLog.getPath(), readyLog.getAttrMap());
                } else if (type == ReadyLogType.CREATE.getValue()) {
                    super.createFile(readyLog.getPath(), readyLog.getAttrMap());
                } else if (type == ReadyLogType.DELETE.getValue()) {
                    super.deleteFile(readyLog.getPath());
                }
                setMaxTxId(readyLog.getTxId());
            });
        } catch (Exception e) {
            log.info("Recover directory tree exception according to imageLog !!：", e);
            throw e;
        }
    }

    @Override
    public void mkdir(String path, Map<String, String> attr) {
        super.mkdir(path, attr);
        txIdIncrement();
        doubleBuffer.writeLog(new ReadyLogWrapper(ReadyLogType.MKDIR,path,attr));
    }

    @Override
    public boolean createFile(String filename, Map<String, String> attr) {
        boolean result = super.createFile(filename, attr);
        if (!result) {
            return false;
        }
        txIdIncrement();
        doubleBuffer.writeLog(new ReadyLogWrapper(ReadyLogType.CREATE,filename,attr));
        return true;
    }

    @Override
    public boolean deleteFile(String filename) {
        boolean result = super.deleteFile(filename);
        if (!result) {
            return false;
        }
        txIdIncrement();
        doubleBuffer.writeLog(new ReadyLogWrapper(ReadyLogType.DELETE,filename));
        return true;
    }

    public void start() throws Exception {
        log.info("Start TrackerFileService.");
        try {
            recoveryNamespace();
            taskScheduler.schedule("Trash Policy Task Start", trashPolicyTask, Constants.TRASH_CLEAR_INTERVAL, Constants.TRASH_CLEAR_INTERVAL, TimeUnit.MILLISECONDS);
            taskScheduler.schedule("Clear Image Task Start", clearImageTask, Constants.TRASH_CLEAR_INTERVAL, Constants.TRASH_CLEAR_INTERVAL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("RecoveryNamespace error of Start Tracker File Service!!.");
            throw e;
        }
    }

    /**
     * 优雅停机
     * 保存日志
     */
    public void shutdown() {
        log.info("Shutdown TrackerFileService.");
        try {
            doubleBuffer.flushBuffer();
            writImage(maxTxId);
        } catch (Exception e) {
            log.error("Failed to save operation log !!");
        }
    }

    /**
     * 获取DoubleBuffer
     *
     * @return editLog
     */
    public DoubleBuffer getEditLog() {
        return doubleBuffer;
    }

    public String getBaseDir() {
        return baseDir;
    }


}
