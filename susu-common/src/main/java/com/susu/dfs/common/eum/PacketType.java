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
    EMPTY(1001, "空的的包类型"),
    STORAGE_REGISTER(1002,"Storage注册"),
    STORAGE_HEART_BEAT(1003,"Storage心跳"),

    MKDIR(2001, "Client往Tracker发送Mkdir请求"),
    CREATE_FILE(2002, "Client往Tracker发送创建文件请求"),
    UPLOAD_FILE(2003, "Client向Storage进行文件传输的二进制包"),
    UPLOAD_FILE_CONFIRM(2004, "Client往Storage上传完文件之后，再发请求往Tracker确认"),
    UPLOAD_FILE_COMPLETE(2005, "Storage保存完文件之后，再发请求往Tracker上报信息"),
    READ_ATTR(2006,"Client从Tracker读取文件属性"),
    REMOVE_FILE(2007,"Client从Tracker删除文件"),
    GET_STORAGE_FOR_FILE(2008,"Client从Storage下载文件前先获取文件所在Storage节点"),
    GET_FILE(2009, "Client从Storage下载文件，或Storage之间相互同步副本请求"),

    TRACKER_SERVER_AWARE(3001, "Tracker 服务端 相互之间发起连接时的感知请求"),
    ;

    public int value;
    private String description;

    public static PacketType getEnum(int value) {
        for (PacketType packetType : values()) {
            if (packetType.getValue() == value) {
                return packetType;
            }
        }
        return EMPTY;
    }
}