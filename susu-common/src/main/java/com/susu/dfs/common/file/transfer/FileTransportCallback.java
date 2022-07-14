package com.susu.dfs.common.file.transfer;

import java.io.IOException;

/**
 * <p>Description: 接收文件的回调/p>
 *
 * @author sujay
 * @version 17:20 2022/7/14
 */
public interface FileTransportCallback {

    /**
     * <p>Description: Get the save address of the file on this computer </p>
     * <p>Description: 获取文件在本机的保存地址/p>
     *
     * @param filename 文件路径
     * @return 文件保存在本机的绝对路径
     */
    String getPath(String filename);


    /**
     * <p>Description: File transfer progress listener </p>
     * <p>Description: 文件传输进度监听器/p>
     *
     * @param filename          文件名称
     * @param total             总大小
     * @param current           当前大小
     * @param progress          进度 0-100，保留1位小数
     * @param currentWriteBytes 当次回调写文件的字节数
     */
    default void onProgress(String filename, long total, long current, float progress, int currentWriteBytes) {
        // TODO 文件传输进度监听器
    }

    /**
     * <p>Description: File transfer complete </p>
     * <p>Description: 文件传输完成/p>
     *
     * @param attr 文件属性
     */
    default void onCompleted(FileAttribute attr) throws InterruptedException, IOException {
        // TODO 文件传输完成
    }

}
