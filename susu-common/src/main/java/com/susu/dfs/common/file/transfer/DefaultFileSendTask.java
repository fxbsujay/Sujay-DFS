package com.susu.dfs.common.file.transfer;


import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.common.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * <p>Description: 默认写文件的方式</p>
 *
 * @author sujay
 * @version 17:45 2022/7/1
 */
@Slf4j
public class DefaultFileSendTask {

    private OnProgressListener listener;
    private SocketChannel socketChannel;
    private String filename;
    private File file;
    private FileAttribute fileAttribute;

    public DefaultFileSendTask(File file, String filename, SocketChannel socketChannel,
                               OnProgressListener listener) throws IOException {
       this(file,filename,socketChannel,listener,true);
    }

    public DefaultFileSendTask(File file, String filename, SocketChannel socketChannel,
                               OnProgressListener listener, boolean isHex) throws IOException {
        this.file = file;
        this.filename = filename;
        this.socketChannel = socketChannel;
        this.fileAttribute = new FileAttribute();
        this.fileAttribute.setFileName(filename);
        this.fileAttribute.setSize(file.length());
        this.fileAttribute.setId(StringUtils.getRandomString(12));
        if (isHex) {
            this.fileAttribute.setMd5(FileUtils.fileMd5(file.getAbsolutePath()));
        }
        this.listener = listener;
    }



    /**
     * 执行逻辑
     */
    public void execute(boolean force) {
        try {
            if (!file.exists()) {
                log.error("file does not exist !!：[filename={}, localFile={}]", filename, file.getAbsolutePath());
                return;
            }
            RandomAccessFile raf = new RandomAccessFile(file.getAbsoluteFile(), "r");
            FileInputStream fis = new FileInputStream(raf.getFD());
            FileChannel fileChannel = fis.getChannel();
            if (log.isDebugEnabled()) {
                log.debug("Send file header：{}", filename);
            }
            FilePacket headPackage = FilePacket.builder()
                    .type(FilePacket.HEAD)
                    .fileMetaData(fileAttribute.getAttr())
                    .build();
            NetPacket nettyPacket = NetPacket.buildPacket(headPackage.toBytes(), PacketType.UPLOAD_FILE);
            sendPackage(nettyPacket, force);
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            int len;
            int readLength = 0;
            while ((len = fileChannel.read(buffer)) > 0) {
                buffer.flip();
                byte[] data = new byte[len];
                buffer.get(data);
                byte[] content = FilePacket.builder()
                        .type(FilePacket.BODY)
                        .fileMetaData(fileAttribute.getAttr())
                        .body(data)
                        .build().toBytes();
                nettyPacket = NetPacket.buildPacket(content, PacketType.UPLOAD_FILE);
                sendPackage(nettyPacket, force);
                buffer.clear();
                readLength += len;
                float progress = new BigDecimal(String.valueOf(readLength)).multiply(new BigDecimal(100))
                        .divide(new BigDecimal(String.valueOf(fileAttribute.getSize())), 2, RoundingMode.HALF_UP).floatValue();
                if (log.isDebugEnabled()) {
                    log.debug("Send file package，filename = {}, size={}, progress={}", filename, data.length, progress);
                }
                if (listener != null) {
                    listener.onProgress(fileAttribute.getSize(), readLength, progress, len);
                }
            }
            FilePacket tailPackage = FilePacket.builder()
                    .type(FilePacket.TAIL)
                    .fileMetaData(fileAttribute.getAttr())
                    .build();
            nettyPacket = NetPacket.buildPacket(tailPackage.toBytes(), PacketType.UPLOAD_FILE);
            sendPackage(nettyPacket, force);
            if (log.isDebugEnabled()) {
                log.debug("Send file completed，filename = {}", filename);
            }
            if (listener != null) {
                listener.onCompleted();
            }
        } catch (Exception e) {
            log.error("Send file failed !!：", e);
        }
    }

    private void sendPackage(NetPacket nettyPacket, boolean force) throws InterruptedException {
        if (force) {
            socketChannel.writeAndFlush(nettyPacket).sync();
        } else {
            socketChannel.writeAndFlush(nettyPacket);
        }
    }
}
