package com.susu.dfs.common.file.transfer;

/**
 * <p>Description: File download progress listener</p>
 * <p>Description: 文件上传下载进度监听器</p>
 *
 * @author sujay
 * @version 10:57 2022/7/14
 */
public interface OnProgressListener {

    /**
     * 上传 / 下载 进度
     */
    void onProgress(long total, long current, float progress,int currentReadBytes);

    /**
     * 完成 上传 / 下载
     */
    default void onCompleted() {
        // TODO 完成下载
    }
}
