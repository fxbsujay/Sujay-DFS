package com.susu.dfs.common.eum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 请求类型
 *
 * @author Sun Dasheng
 */
@Getter
@AllArgsConstructor
public enum PacketType {

    /**
     * 请求类型
     */
    UNKNOWN(0, "未知的包类型"),
    CLIENT_REGISTER(1,"Storage注册"),
    CLIENT_HEART_BEAT(2,"Storage心跳"),
    MKDIR(3, "Client往Tracker发送Mkdir请求"),
    CREATE_FILE(4, "Client往Tracker发送创建文件请求"),
    UPLOAD_FILE(5, "文件传输的二进制包类型"),
    GET_FILE(6, "Client从Storage下载文件，或Storage之间相互同步副本请求"),
    CREATE_FILE_CONFIRM(7, "Client往Storage上传完文件之后，再发请求往Tracker确认"),
    TEST(2, "测试请求"),
    ;

    public int value;
    private String description;

    public static PacketType getEnum(int value) {
        for (PacketType packetType : values()) {
            if (packetType.getValue() == value) {
                return packetType;
            }
        }
        return UNKNOWN;
    }
}