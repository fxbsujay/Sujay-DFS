package com.susu.dfs.common.netty.msg;


import com.susu.dfs.common.Constants;
import com.susu.dfs.common.utils.HexConvertUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * <p>Description: 消息解码器</p>
 *
 *  请求头为 16 byte
 *  +------------------------+---------------------+-------------------------+----------------------+-----------------------+---------------------------+--------------------+
 *  | Author Magic (4byte)   |  Version (1byte)    |   Message Type (1byte)  |   Sequence (8byte)   |  Packet Type (1byte)  |  Content Length (1byte)   |   Actual Content   |
 *  |       授权码            |       版本号         |       M类型              |       请求号          |       数据包类型        |          内容体长度         |      真实的数据      |
 *  +------------------------+---------------------+-------------------------+----------------------*-----------------------+---------------------------+--------------------+
 *
 *
 * @author sujay
 * @version 16:11 2022/7/6
 */
public class NetPacketEncoder extends MessageToByteEncoder<NetPacket> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NetPacket packet, ByteBuf out) throws Exception {

        out.writeBytes(Constants.SYS_AUTHOR.getBytes(StandardCharsets.UTF_8));
        out.writeByte(Constants.SYS_VERSION);
        packet.write(out);
    }
}
