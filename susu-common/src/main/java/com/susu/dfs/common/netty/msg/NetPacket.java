package com.susu.dfs.common.netty.msg;

import com.susu.dfs.common.eum.MsgType;
import com.susu.dfs.common.eum.PacketType;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;

/**
 * <p>Description: 统一的网络数据包</p>
 * <p>Description: Unified network packets</p>
 *
 * @author sujay
 * @version 11:36 2022/7/6
 */
@Data
@Builder
public class NetPacket {

    /**
     * 消息类型 1位
     */
    private static final int MSG_TYPE = MsgType.PACKET.value;

    /**
     * 数据包类型 1位
     */
    private int type;

    /**
     * 数据包内容长度
     */
    private int length;

    /**
     * 数据包
     */
    protected byte[] body;

    /**
     * 一个静态的构造器
     *
     * @param body          请求体
     * @param packetType    请求体类型
     * @return              数据包
     */
    public static NetPacket buildPacket(byte[] body, PacketType packetType){
        NetPacketBuilder packet = NetPacket.builder();
        packet.length = body.length;
        packet.body = body;
        packet.type = packetType.getValue();
        return packet.build();
    }

    /**
     * 将数据写入ByteBuf
     *
     * @param out 输出
     */
    public void write(ByteBuf out) {
        out.writeByte(MSG_TYPE);
        out.writeByte(type);
        out.writeInt(length);
        out.writeBytes(body);
    }

    /**
     * 读取ByteBuf，转成NetPacket
     * @param in 输入
     * @return 数据包
     */
    public static NetPacket read(ByteBuf in) {
        int type = in.readByte();
        int length = in.readInt();
        byte[] body = new byte[length];
        in.readBytes(body, 0, length);
        return NetPacket.builder()
                .type(type)
                .length(length)
                .body(body).build();
    }
}