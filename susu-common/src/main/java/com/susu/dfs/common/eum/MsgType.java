package com.susu.dfs.common.eum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>Description: 消息类型</p>
 * <p>Description: message type</p>
 * @author sujay
 * @version 11:36 2022/7/6
 */
@Getter
@AllArgsConstructor
public enum MsgType {

    UNKNOWN(404, "未知的消息类型"),
    STRING(0, "String类型消息"),
    PACKET(1, "byte数据包"),
    HTML(2, "html请求");

    public int value;

    private String doc;

    public static MsgType getEnum(int value) {
        for (MsgType packetType : values()) {
            if (packetType.getValue() == value) {
                return packetType;
            }
        }
        return UNKNOWN;
    }
}
