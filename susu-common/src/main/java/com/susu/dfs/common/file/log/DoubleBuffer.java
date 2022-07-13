package com.susu.dfs.common.file.log;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.file.PlaybackReadyLogCallback;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.common.utils.StopWatch;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Description: ReadyLog 双缓冲机制 </p>
 *
 * <p>
 *    提升磁盘IO的另外一个技巧，
 *    一次尽可能多写入或多读取。也就是说，将程序的读写buffer设置得尽可能大一些。
 *    例如日志或者redo log的写入，不是每次调用都直接写磁盘，
 *    而是先缓存到内存中，等buffer满了再写入磁盘，也可以定时写入磁盘
 * </p>
 *
 * @author sujay
 * @version 11:04 2022/7/12
 */
@Slf4j
public class DoubleBuffer {

    /**
     * 验证文件名中的index
     */
    private static Pattern indexPattern = Pattern.compile("(\\d+)_(\\d+)");

    /**
     * 主要的操作缓存区
     */
    private ReadyLogBuffer regularBuffer;

    /**
     * 同步的操作缓存区
     */
    private ReadyLogBuffer syncBuffer;

    /**
     * 每条readyLog的id，自增
     */
    private volatile long txIdSeq = 0;

    /**
     * 每个线程保存的txDd
     */
    private ThreadLocal<Long> localTxId = new ThreadLocal<>();

    /**
     * 当前刷新磁盘最大的txId
     */
    private volatile long syncTxId = 0;

    /**
     * 当前是否在刷磁盘
     */
    private volatile boolean isSyncRunning = false;

    /**
     * 是否正在调度一次刷盘的操作
     */
    private volatile Boolean isSchedulingSync = false;

    /**
     * 磁盘中的readyLog文件, 升序
     */
    private List<ReadyLogInfo> readyLogs = null;

    public DoubleBuffer() {
        this.regularBuffer = new ReadyLogBuffer();
        this.syncBuffer = new ReadyLogBuffer();
        loadReadyLogs();
    }

    /**
     * write readyLog
     *
     * @param readyLog 内容
     */
    public void writeLog(ReadyLogWrapper readyLog) {
        synchronized (this) {

            waitSchedulingSync();

            txIdSeq++;
            long txId = txIdSeq;
            localTxId.set(txId);
            readyLog.setTxId(txId);

            try {
               write(readyLog);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!isForceSync()) {
                return;
            }
            isSchedulingSync = true;
        }
        logSync();
    }

    /**
     * 等待正在调度的刷磁盘的操作
     */
    private void waitSchedulingSync() {
        try {
            while (isSchedulingSync) {
                wait(1000);
                // 此时就释放锁，等待一秒再次尝试获取锁，去判断
                // isSchedulingSync是否为false，就可以脱离出while循环
            }
        } catch (Exception e) {
            log.info("waitSchedulingSync has interrupted !!");
        }
    }

    /**
     * <p>Description: 写入缓冲区 </p>
     *
     * @param wrapper  操作磁盘的一条文件记录
     * @throws IOException IO异常
     */
    private void write(ReadyLogWrapper wrapper) throws IOException {
        regularBuffer.write(wrapper);
    }

    /**
     * 异步刷磁盘
     */
    private void logSync() {
        synchronized (this) {
            long txId = localTxId.get();
            localTxId.remove();
            /*
             * 在这种情况下需要等待：
             * 1. 有其他线程正在刷磁盘，但是其他线程刷的磁盘的最大txid比当前需要刷磁盘的线程id少。
             * 这通常表示：正在刷磁盘的线程不会把当前线程需要刷的数据刷到磁盘中
             */
            while (txId > syncTxId && isSyncRunning) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            /*
             * 多个线程在上面等待，当前一个线程刷磁盘操作完成后，唤醒了一堆线程，此时只有一个线程获取到锁。
             * 这个线程会进行刷磁盘操作，当这个线程释放锁之后，其他被唤醒的线程会依次获取到锁。
             * 此时每个被唤醒的线程需要重新判断一次，自己要做的事情是不是被其他线程干完了
             */
            if (txId <= syncTxId) {
                return;
            }

            // 交换两块缓冲区
            regularToSync();

            // 记录最大的txId
            syncTxId = txId;

            // 设置当前正在同步到磁盘的标志位
            isSchedulingSync = false;

            // 唤醒哪些正在wait的线程
            notifyAll();

            // 正在刷磁盘
            isSyncRunning = true;
        }

        try {
            ReadyLogInfo readyLog = flush();
            if (readyLog != null) {
                readyLogs.add(readyLog);
            }
        } catch (IOException e) {
            log.info("ReadyLog 刷磁盘失败：", e);
        }

        synchronized (this) {
            // 同步完了磁盘之后，就会将标志位复位，再释放锁
            isSyncRunning = false;
            notifyAll();
        }
    }

    /**
     * <p>Description: Load all ReadyLog file information from disk </p>
     * <p>Description: 从磁盘中加载所有的log文件信息 </p>
     */
    private void loadReadyLogs() {
        this.readyLogs = new CopyOnWriteArrayList<>();
        File dir = new File(Constants.DEFAULT_BASE_DIR);
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            if (!file.getName().contains("edits")) {
                continue;
            }
            long[] index = getIndexFromFileName(file.getName());
            this.readyLogs.add(new ReadyLogInfo(Constants.DEFAULT_BASE_DIR + File.separator + file.getName(),index[0], index[1]));
        }
        this.readyLogs.sort(null);
    }

    /**
     * <p>Description: Extract index from file name </p>
     * <p>Description: 从文件名中提取index </p>
     *
     * Example:
     *      name    :   ready_log_1_99.log
     *      return  :   [1,99]
     *
     * @param name 文件名
     * @return index数组
     */
    private long[] getIndexFromFileName(String name) {
        Matcher matcher = indexPattern.matcher(name);
        long[] result = new long[2];
        if (matcher.find()) {
            result[0] = Long.parseLong(matcher.group(1));
            result[1] = Long.parseLong(matcher.group(2));
        }
        return result;
    }


    /**
     * 交换缓冲区
     */
    private void regularToSync() {
        ReadyLogBuffer temp = regularBuffer;
        regularBuffer = syncBuffer;
        syncBuffer = temp;
    }

    /**
     * 强制把内存缓冲里的数据刷入磁盘中
     */
    public void flushBuffer() {
        synchronized (this) {
            try {
                regularToSync();
                ReadyLogInfo readyLog = flush();
                if (readyLog != null) {
                    readyLogs.add(readyLog);
                }
            } catch (IOException e) {
                log.error("Forced flush of buffer to disk failed!!.", e);
            }
        }
    }

    /**
     * 将缓冲区的内容刷入磁盘，保存记录日志
     */
    private ReadyLogInfo flush() throws IOException {
        ReadyLogInfo readyLogInfo = syncBuffer.flush();
        if (readyLogInfo != null) {
            syncBuffer.clear();
        }
        return readyLogInfo;
    }

    /**
     * <p>Description: 当前是否能刷新缓冲区 </p>
     * <p>Description: Whether the buffer can be flushed currently</p>
     *
     * @return 当前是否能刷新缓冲区
     */
    private boolean isForceSync() {
        return regularBuffer.size() >= Constants.READY_LOG_FLUSH_THRESHOLD;
    }

    /**
     * <p>Description: 获取当前正则写操作的缓冲区 </p>
     *
     * @return  缓冲区内的记录
     */
    public List<ReadyLogWrapper> getRegularReadyLog() {
        synchronized (this) {
            return regularBuffer.getReadyLog();
        }
    }

    /**
     * <p>Description: 服务重启时需要回放readyLog到内存中</p>
     * <p>Description: The readyLog needs to be played back to memory when the service is restarted</p>
     *
     * @param txiId    回放比txId大的日志
     * @param callback 回放日志回调
     * @throws IOException IO异常
     */
    public void playbackReadyLog(long txiId, PlaybackReadyLogCallback callback) throws IOException {
        long regularTxSeq = txiId;
        this.txIdSeq = regularTxSeq;
        List<ReadyLogInfo> readyLogInfos = getSortedReadyLogFiles(txiId);
        StopWatch stopWatch = new StopWatch();
        for (ReadyLogInfo info : readyLogInfos) {
            if (info.getEnd() <= regularTxSeq) {
                continue;
            }
            List<ReadyLogWrapper> readyLogWrappers = readReadyLogFromFile(info.getName());
            stopWatch.start();
            for (ReadyLogWrapper readyLogWrapper : readyLogWrappers) {
                long tmpTxId = readyLogWrapper.getTxId();
                if (tmpTxId <= regularTxSeq) {
                    continue;
                }
                regularTxSeq = tmpTxId;
                this.txIdSeq = regularTxSeq;
                if (callback != null) {
                    callback.playback(readyLogWrapper);
                }
            }
            stopWatch.stop();
            log.info("playback readyLog file: [file={}, cost={} s]", info.getName(), stopWatch.getTime() / 1000.0D);
            stopWatch.reset();
        }
    }

    /**
     * <p>Description: 获取比minTxId更大的readyLog文件，经过排序后的文件</p>
     * <p>Description: Get the readyLog file larger than minTxId, and the sorted file</p>
     *
     * Example:
     * <pre>
     *      1_1000.log
     *      1001_2000.log
     *      2001_3000.log
     * </pre>
     *
     * <p>
     *     minTxId = 1500
     *     return    [1001_2000.log, 2001_3000.log]
     * </p>
     *
     * @param minTxId 最小的txId
     * @return 排序后的文件信息
     */
    public List<ReadyLogInfo> getSortedReadyLogFiles(long minTxId) {
        List<ReadyLogInfo> result = new ArrayList<>();
        for (ReadyLogInfo readyLog : readyLogs) {
            if (readyLog.getEnd() <= minTxId) {
                continue;
            }
            result.add(readyLog);
        }
        return result;
    }

    /**
     * <p>Description: 从文件中读取ReadyLog</p>
     * <p>Description: Read readyLog from file</p>
     *
     * @param absolutePath 绝对路径
     * @return EditLog
     * @throws IOException IO异常
     */
    public List<ReadyLogWrapper> readReadyLogFromFile(String absolutePath) throws IOException {
        return ReadyLogWrapper.parseFrom(FileUtils.readBuffer(absolutePath));
    }

    /**
     * <p>Description: 清除比txId小的日志文件</p>
     * <p>Description: Clear log files smaller than txId</p>
     *
     * Example:
     * <pre>
     *      1_1000.log
     *      1001_2000.log
     *      2001_3000.log
     * </pre>
     *
     * <p>
     *      minTxId = 1500
     *      return    [1_1000.log]
     * </p>
     *
     * @param txId txId
     */
    public void cleanReadyLogByTxId(long txId) {
        List<ReadyLogInfo> toRemoveEditLog = new ArrayList<>();
        for (ReadyLogInfo readyLogInfo : readyLogs) {
            if (readyLogInfo.getEnd() > txId) {
                continue;
            }
            File file = new File(readyLogInfo.getName());
            if (file.delete()) {
                log.info("delete ReadyLog file: [file={}]", readyLogInfo.getName());
            }
            toRemoveEditLog.add(readyLogInfo);
        }
        readyLogs.removeAll(toRemoveEditLog);
    }

}
