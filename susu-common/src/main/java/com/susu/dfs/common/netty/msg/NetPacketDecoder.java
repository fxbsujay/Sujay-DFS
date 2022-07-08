package com.susu.dfs.common.netty.msg;

import com.susu.dfs.common.eum.MsgType;
import com.susu.dfs.common.utils.HexConvertUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * <p>Description: 消息编码器</p>
 *
 *  请求头为 8 byte
 *  +------------------------+-------------------+--------------------------+-----------------------+---------------------------+-------------------+
 *  | Author Magic (4byte)   |  Version (1byte)  |   Message Type (1byte)   | ·Packet Type (1byte)  |  Content Length (1byte)   |   Actual Content  |
 *  |       授权码            |      版本号        |       M类型               |       数据包类型        |          内容体长度         |      真实的数据     |
 *  +------------------------+-------------------+--------------------------*-----------------------+---------------------------+-------------------+
 *
 *
 * @author sujay
 * @version 16:11 2022/7/6
 */
@Slf4j
public class NetPacketDecoder extends LengthFieldBasedFrameDecoder {


    public NetPacketDecoder(int maxFrameLength) {
        super(maxFrameLength, 7, 4,0,0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        ByteBuf in = (ByteBuf) super.decode(ctx, byteBuf);
        if (byteBuf != null) {
            int author = in.readInt();
            byte version = in.readByte();
            byte msgType = in.readByte();
            if (MsgType.getEnum(msgType) == MsgType.PACKET) {
              try {
                  return NetPacket.read(in);
              } finally {
                  ReferenceCountUtil.release(byteBuf);
              }
            }
        }
        return null;

    }
}
