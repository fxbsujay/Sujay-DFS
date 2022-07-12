package com.susu.dfs.common.file.log;

/**
 * <p>Description: 回放 </p>
 *
 * @author sujay
 * @version 14:41 2022/7/12
 */
public interface PlaybackReadyLogCallback {

    /**
     * 回放
     *
     * @param wrapper 准备好操作磁盘的一条文件记录
     */
    void playback(ReadyLogWrapper wrapper);
}
