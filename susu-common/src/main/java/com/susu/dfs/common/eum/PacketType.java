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
    STORAGE_REGISTER(1,"Storage注册"),
    STORAGE_HEART_BEAT(2,"Storage心跳"),
    MKDIR(3, "Client往Tracker发送Mkdir请求"),
    CREATE_FILE(4, "Client往Tracker发送创建文件请求"),
    UPLOAD_FILE(5, "Client向Storage进行文件传输的二进制包"),
    UPLOAD_FILE_CONFIRM(6, "Client往Storage上传完文件之后，再发请求往Tracker确认"),
    UPLOAD_FILE_COMPLETE(7, "Storage保存完文件之后，再发请求往Tracker上报信息"),
    GET_FILE(8, "Client从Storage下载文件，或Storage之间相互同步副本请求"),

    TEST(9, "测试请求"),
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