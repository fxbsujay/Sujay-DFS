package com.susu.common.netty.msg;

import com.susu.common.eum.MsgType;
import com.susu.common.utils.HexConvertUtils;
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
            int length = 0;
            int type = 0;
            byte[] body = new byte[length];
            if (MsgType.getEnum(msgType) == MsgType.PACKET) {
              try {
                  NetPacket packet = NetPacket.read(in);
                  length = packet.getLength();
                  type = packet.getType();
                  body = packet.getBody();

                  ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body));
                  String message = (String) ois.readObject();

                  log.info("授权：{}", HexConvertUtils.decToASCII(author));
                  log.info("版本号：{}",version);
                  log.info("消息类型：{}",msgType);
                  log.info("指令类型：{}",type);
                  log.info("正文长度：{}",length);
                  log.info("正文：{}",message);
                  return packet;
              } finally {
                  ReferenceCountUtil.release(byteBuf);
              }
            }
        }
        return null;

    }
}
