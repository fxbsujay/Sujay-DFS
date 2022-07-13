package com.susu.dfs.common.file.log;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: ReadyLog 缓冲区 </p>
 * <p>
 *    提升磁盘IO的另外一个技巧，
 *    一次尽可能多写入或多读取。也就是说，将程序的读写buffer设置得尽可能大一些。
 *    例如日志或者redo log的写入，不是每次调用都直接写磁盘，
 *    而是先缓存到内存中，等buffer满了再写入磁盘，也可以定时写入磁盘
 * </p>
 *
 * @author sujay
 * @version 10:02 2022/7/12
 */
@Slf4j
public class ReadyLogBuffer {

    /**
     * 缓冲区
     */
    private ByteArrayOutputStream buffer;

    /**
     * readyLog 记录开始Id号，-1 表示为空
     */
    private volatile long startTxId = -1L;

    /**
     * readyLog 记录结束Id号，-1 表示为空
     */
    private volatile long endTxId = 0L;

    public ReadyLogBuffer() {
        this.buffer = new ByteArrayOutputStream((Constants.READY_LOG_FLUSH_THRESHOLD * 2));
    }

    /**
     * <p>Description: 向缓冲区写入一条记录</p>
     *
     * @param readyLog 操作磁盘文件记录
     * @throws IOException IO异常
     */
    public void write(ReadyLogWrapper readyLog) throws IOException {
        if (startTxId == -1) {
            startTxId = readyLog.getTxId();
        }
        endTxId = readyLog.getTxId();
        buffer.write(readyLog.toByteArray());
    }

    public ReadyLogInfo flush() throws IOException {
        if (buffer.size() <= 0) {
            return null;
        }
        byte[] data = buffer.toByteArray();
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        String path = Constants.DEFAULT_BASE_DIR + File.separator + Constants.READY_LOG_NAME + startTxId + "_" + endTxId + ".log";
        log.info("保存 ReadyLog 文件：[file={}]", path);
        FileUtils.writeFile(path,false,dataBuffer);
        return new ReadyLogInfo(path, startTxId, endTxId);
    }

    public List<ReadyLogWrapper> getReadyLog() {
        byte[] bytes = buffer.toByteArray();
        if (bytes.length == 0) {
            return new ArrayList<>();
        }
        return ReadyLogWrapper.parseFrom(bytes);
    }

    /**
     * <p>Description: 清空缓冲区</p>
     */
    public void clear() {
        buffer.reset();
        startTxId = -1;
        endTxId = -1;
    }

    public Integer size() {
        return buffer.size();
    }



}
