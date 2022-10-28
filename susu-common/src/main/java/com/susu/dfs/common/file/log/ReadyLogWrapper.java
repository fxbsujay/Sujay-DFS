package com.susu.dfs.common.file.log;

import com.susu.dfs.common.eum.ReadyLogType;
import com.susu.dfs.common.model.ReadyLog;
import com.susu.dfs.common.utils.HexConvertUtils;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: Method to operate ready log</p>
 * <p>Description: 方便操作 Ready Log 的工具</p>
 *
 * @author sujay
 * @version 10:02 2022/7/12
 */
@Slf4j
public class ReadyLogWrapper {

    /**
     * <p>Description: 准备好操作磁盘的一条文件记录</p>
     *
     * <blockquote><pre>
     *    txId:  记录id，顺序递增
     *    type:  磁盘操作类型 {@link com.susu.dfs.common.eum.ReadyLogType}
     *    path:  操作文件路径
     * </pre></blockquote>
     *
     */
    private ReadyLog readyLog;

    public ReadyLogWrapper(ReadyLogType type, String path) {
       this(type, path, new HashMap<>(32));
    }

    public ReadyLogWrapper(ReadyLogType type, String path, Map<String,String> attr) {
        this.readyLog = ReadyLog.newBuilder()
                .setType(type.getValue())
                .setPath(path)
                .putAllAttr(attr)
                .build();
    }

    public ReadyLogWrapper(ReadyLog readyLog) {
        this.readyLog = readyLog;
    }

    public ReadyLog getReadyLog() {
        return readyLog;
    }

    public void setTxId(long txId) {
        this.readyLog = this.readyLog.toBuilder()
                .setTxId(txId)
                .build();
    }

    public long getTxId() {
        return this.readyLog.getTxId();
    }

    public byte[] toByteArray() {
        byte[] body = readyLog.toByteArray();
        int bodyLength = body.length;
        byte[] bytes = HexConvertUtils.intToBytes(body.length + 4, 0, bodyLength);
        System.arraycopy(body, 0, bytes, 4, bodyLength);
        return bytes;
    }

    public static List<ReadyLogWrapper> parseFrom(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return parseFrom(byteBuffer);
    }

    public static List<ReadyLogWrapper> parseFrom(ByteBuffer byteBuffer) {
        List<ReadyLogWrapper> ret = new LinkedList<>();
        while (byteBuffer.hasRemaining()) {
            try {
                int bodyLength = byteBuffer.getInt();
                byte[] body = new byte[bodyLength];
                byteBuffer.get(body);
                ReadyLog editLog = ReadyLog.parseFrom(body);
                ret.add(new ReadyLogWrapper(editLog));
            } catch (Exception e) {
                log.error("Parse ReadyLog failed.", e);
            }
        }
        return ret;
    }


}
