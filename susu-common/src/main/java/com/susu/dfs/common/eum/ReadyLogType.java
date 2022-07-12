package com.susu.dfs.common.eum;

import lombok.Getter;

/**
 * <p>Description: 文件操作类型</p>
 *
 * @author sujay
 * @version 10:08 2022/7/12
 */
@Getter
public enum ReadyLogType {

    /**
     * 创建文件夹
     */
    MKDIR(1),

    /**
     * 创建文件
     */
    CREATE(2),

    /**
     * 删除文件或者文件夹
     */
    DELETE(3);

    private int value;

    ReadyLogType(int value) {
        this.value = value;
    }
}
