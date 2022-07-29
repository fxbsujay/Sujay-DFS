package com.susu.dfs.common.file.image;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.common.model.ImageLog;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.common.utils.HexConvertUtils;
import com.susu.dfs.common.utils.StopWatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * <p>Description: FILE IMAGE</p>
 * <p>Description: 目录树的内存影像</p>
 *
 * @author sujay
 * @version 16:29 2022/7/12
 */
@Slf4j
@Data
public class ImageLogWrapper {

    private static final int LENGTH_OF_FILE_LENGTH_FIELD = 4;

    private static final int LENGTH_OF_MAX_TX_ID_FIELD = 8;

    /**
     * 当前最大的txId
     */
    private long maxTxId;

    /**
     * 内容
     */
    private ImageLog imageLog;

    public ImageLogWrapper(long maxTxId, ImageLog imageLog) {
        this.maxTxId = maxTxId;
        this.imageLog = imageLog;
    }

    public void writeFile(String path) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(toByteArray());
        FileUtils.writeFile(path, true, buffer);
        log.info("Save image file：[file={}]", path);
    }

    /**
     * 4位整个文件的长度 + 8位maxId + 文件内容
     *
     * @return 二进制数组
     */
    public byte[] toByteArray() {
        byte[] body = imageLog.toByteArray();
        // 文件长度包括 4位文件长度 + 8位txId + 内容长度
        int fileLength = LENGTH_OF_FILE_LENGTH_FIELD + LENGTH_OF_MAX_TX_ID_FIELD + body.length;
        byte[] ret = HexConvertUtils.intToBytes(fileLength, 0, fileLength);
        HexConvertUtils.setLong(ret, LENGTH_OF_FILE_LENGTH_FIELD, maxTxId);
        System.arraycopy(body, 0, ret, LENGTH_OF_FILE_LENGTH_FIELD + LENGTH_OF_MAX_TX_ID_FIELD, body.length);
        return ret;
    }

    /**
     * 解析FsImage文件
     *
     * @param channel 文件channel
     * @param path    文件绝对路径
     * @param length  文件长度
     * @return 如果合法返回 FsImage，不合法返回null
     * @throws IOException IO异常，文件不存在
     */
    public static ImageLogWrapper parse(FileChannel channel, String path, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH_OF_FILE_LENGTH_FIELD + LENGTH_OF_MAX_TX_ID_FIELD);
        channel.read(buffer);
        buffer.flip();
        if (buffer.remaining() < 4) {
            log.warn("ImageLog file is incomplete !!: [file={}]", path);
            return null;
        }
        int fileLength = buffer.getInt();
        if (fileLength != length) {
            log.warn("ImageLog file is incomplete !!: [file={}]", path);
            return null;
        } else {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            long maxTxId = buffer.getLong();
            int bodyLength = fileLength - LENGTH_OF_FILE_LENGTH_FIELD - LENGTH_OF_MAX_TX_ID_FIELD;
            buffer = ByteBuffer.allocate(bodyLength);
            channel.read(buffer);
            buffer.flip();
            byte[] body = new byte[bodyLength];
            buffer.get(body);
            ImageLog imageLog;

            try {
                imageLog = ImageLog.parseFrom(body);
            } catch (InvalidProtocolBufferException e) {
                log.error("Parse EditLog failed.", e);
                return null;
            }

            ImageLogWrapper wrapper = new ImageLogWrapper(maxTxId, imageLog);
            stopWatch.stop();
            log.info("Load FSImage...: [file={}, size={}, maxTxId={}, cost={} s]",
                    path, FileUtils.formatSize(length),
                    wrapper.getMaxTxId(), stopWatch.getTime() / 1000.0D);
            stopWatch.reset();
            return wrapper;
        }
    }

    /**
     * 验证FsImage文件
     *
     * @param channel File Channel
     * @param path    文件路径
     * @param length  文件长度
     * @return 如果合法返回MaxTxId, 如果不合法返回-1
     * @throws IOException 文件不存在
     */
    public static long validate(FileChannel channel, String path, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        channel.read(buffer);
        buffer.flip();
        if (buffer.remaining() < LENGTH_OF_FILE_LENGTH_FIELD) {
            log.warn("ImageLog file is incomplete !!: [file={}]", path);
            return -1;
        }
        int fileLength = buffer.getInt();
        if (fileLength != length) {
            log.warn("ImageLog file is incomplete !!: [file={}]", path);
            return -1;
        } else {
            return buffer.getLong();
        }

    }



}
