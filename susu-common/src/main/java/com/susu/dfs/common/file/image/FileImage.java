package com.susu.dfs.common.file.image;

/**
 * <p>Description: FILE IMAGE</p>
 * <p>Description: 目录树的内存影像</p>
 *
 * @author sujay
 * @version 16:29 2022/7/12
 */
public class FileImage {

    private static final int LENGTH_OF_FILE_LENGTH_FIELD = 4;

    private static final int LENGTH_OF_MAX_TX_ID_FIELD = 8;

    /**
     * 当前最大的txId
     */
    private long maxTxId;


}
