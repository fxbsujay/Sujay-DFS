package com.susu.dfs.common.eum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 命令类型
 */
@Getter
@AllArgsConstructor
public enum CommandType {

    EMPTY(0),
    /**
     * 复制文件副本
     */
    FILE_COPY(1),

    /**
     * 删除文件副本
     */
    FILE_REMOVE(2),
    ;

    private int value;

    public static CommandType getEnum(int value) {
        for (CommandType packetType : values()) {
            if (packetType.getValue() == value) {
                return packetType;
            }
        }
        return EMPTY;
    }

}
