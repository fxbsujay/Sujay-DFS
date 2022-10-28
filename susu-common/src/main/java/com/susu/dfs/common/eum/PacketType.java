package com.susu.dfs.common.eum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 请求类型
 *
 * @author sujay
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
    STORAGE_REPORT_INFO(1004,"Storage上报自身信息"),

    MKDIR(2001, "Client往Tracker发送Mkdir请求"),
    CREATE_FILE(2002, "Client往Tracker发送创建文件请求"),
    UPLOAD_FILE(2003, "Client向Storage进行文件传输的二进制包"),
    UPLOAD_FILE_CONFIRM(2004, "Client往Storage上传完文件之后，再发请求往Tracker确认"),
    UPLOAD_FILE_COMPLETE(2005, "Storage保存完文件之后，再发请求往Tracker上报信息"),
    READ_ATTR(2006,"Client从Tracker读取文件属性"),
    REMOVE_FILE(2007,"Client从Tracker删除文件"),
    REMOVE_FILE_COMPLETE(2008,"Storage删除文件成功上报Tracker"),
    GET_STORAGE_FOR_FILE(2009,"Client从Storage下载文件前先获取文件所在Storage节点"),
    GET_FILE(2010, "Client从Storage下载文件，或Storage之间相互同步副本请求"),
    AUTH_INFO(2011,"Client向Tracker发起认证"),

    TRACKER_SERVER_AWARE(3001, "Tracker 服务端 相互之间发起连接时的感知请求"),
    TRACKER_SLOT_BROADCAST(3002, "往所有Tracker广播Slots信息"),
    TRACKER_RE_BALANCE_SLOTS(3003, "新节点加入集群, 申请重新分配Slots"),
    TRACKER_FETCH_SLOT_META_DATA(3004, "新节点从旧节点中拉取文件目录树元数据"),
    TRACKER_FETCH_SLOT_META_DATA_RESPONSE(3005, "旧节点往新节点发送文件目录树元数据"),
    TRACKER_FETCH_SLOT_META_DATA_COMPLETED(3006, "新节点完成了文件目录树的拉取，通知主节点"),
    TRACKER_FETCH_SLOT_META_DATA_COMPLETED_BROADCAST(3007, "主节点广播给所有Tracker，新节点完成了元数据拉取回放"),
    TRACKER_REMOVE_META_DATA_COMPLETED(3008, "旧节点删除不属于自身Slot的内存目录树之后，往主上报"),
    NEW_TRACKER_INFO(3009, "Tracker的主节点在新节点上线后发送的信息"),
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