package com.susu.dfs.common.eum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>Description: 文件节点类型</p>
 * <p>Description: file node type</p>
 * @author sujay
 * @version 15:35 2022/7/11
 */
@Getter
@AllArgsConstructor
public enum FileNodeType {

    FILE(1, "文件"),
    DIRECTORY(2, "文件目录");

    public int value;

    private String doc;

    public static FileNodeType getEnum(int value) {
        for (FileNodeType packetType : values()) {
            if (packetType.getValue() == value) {
                return packetType;
            }
        }
        return FILE;
    }
}
