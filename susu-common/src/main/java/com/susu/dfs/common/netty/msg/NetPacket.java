package com.susu.dfs.common.netty.msg;

import com.google.protobuf.InvalidProtocolBufferException;
import com.susu.dfs.common.model.NetPacketHeader;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * <p>Description: 统一的网络数据包</p>
 * <p>Description: Unified network packets</p>
 *
 * @author sujay
 * @version 11:36 2022/7/6
 */
@Data
@Builder
@Slf4j
@NoArgsConstructor
public class NetPacket {

    /**
     * 请求头
     */
    private Map<String, String> header;

    /**
     * 数据包
     */
    protected byte[] body;

    public NetPacket(Map<String, String> header, byte[] body) {
        this.header = header;
        this.body = body;
    }

    /**
     * 一个静态的构造器
     *
     * @param body          请求体
     * @param packetType    请求体类型
     * @return              数据包
     */
    public static NetPacket buildPacket(byte[] body, PacketType packetType){
        NetPacketBuilder builder = NetPacket.builder();
        builder.body = body;
        builder.header = new HashMap<>(Constants.MAP_SIZE);
        NetPacket nettyPacket = builder.build();
        nettyPacket.setType(packetType.value);
        return nettyPacket;
    }

    /**
     * 复制
     */
    public static NetPacket copy(NetPacket nettyPacket) {
        return new NetPacket(new HashMap<>(nettyPacket.getHeader()),nettyPacket.getBody());
    }


    /**
     * 设置请求序列号
     *
     * @param sequence 请求序列号
     */
    public void setSequence(long sequence) {
        header.put("sequence", String.valueOf(sequence));
    }

    /**
     * 设置请求序列号
     */
    public long getSequence() {
        return StringUtils.isNotBlank(header.get("sequence")) ? Long.parseLong(header.get("sequence")) : 0;
    }

    public void setTrackerIndex(int trackerIndex) {
        header.put("trackerIndex", String.valueOf(trackerIndex));
    }

    public int getTrackerIndex() {
        String trackerIndex = header.getOrDefault("trackerIndex", "-1");
        return Integer.parseInt(trackerIndex);
    }


    /**
     * 请求包类型
     *
     * @return 请求包类型
     */
    public int getType() {
        return Integer.parseInt(header.getOrDefault("type","0"));
    }

    /**
     * 设置请求包类型
     *
     * @param packetType 请求包类型
     */
    public void setType(int packetType) {
        header.put("type", String.valueOf(packetType));
    }

    /**
     * 是否为消息体的最后一个包
     */
    public boolean isSupportChunked() {
        return Boolean.parseBoolean(header.getOrDefault("supportChunked", "false"));
    }

    public void setSupportChunked(boolean chunkedFinish) {
        header.put("supportChunked", String.valueOf(chunkedFinish));
    }

    /**
     * 请求是否需要继续广播给其他节点
     *
     * @param broadcast 是否需要广播
     */
    public void setBroadcast(boolean broadcast) {
        header.put("broadcast", String.valueOf(broadcast));
    }

    /**
     * 请求是否需要继续广播给其他节点
     */
    public boolean getBroadcast() {
        return Boolean.parseBoolean(header.getOrDefault("broadcast", "false"));
    }


    /**
     * <p>Description: 写入数据</p>
     *
     * @param out 输出
     */
    public void write(ByteBuf out) {
        NetPacketHeader netPacketHeader = NetPacketHeader.newBuilder()
                .putAllHeaders(header)
                .build();
        byte[] headerBytes = netPacketHeader.toByteArray();
        out.writeInt(headerBytes.length);
        out.writeBytes(headerBytes);
        out.writeInt(body.length);
        out.writeBytes(body);
    }

    /**
     * <p>Description: 读取ByteBuf，转成NetPacket</p>
     *
     * @param in 输入
     * @return 数据包
     */
    public static NetPacket read(ByteBuf in) throws InvalidProtocolBufferException {
        int headerLength = in.readInt();
        byte[] headerBytes = new byte[headerLength];
        in.readBytes(headerBytes);
        NetPacketHeader nettyPackageHeader = NetPacketHeader.parseFrom(headerBytes);
        int bodyLength = in.readInt();
        byte[] bodyBytes = new byte[bodyLength];
        in.readBytes(bodyBytes);
        return NetPacket.builder()
                .header(new HashMap<>(nettyPackageHeader.getHeadersMap()))
                .body(bodyBytes)
                .build();
    }

    /**
     * <p>Description: 拆分消息体</p>
     * <p>Description: Split message body</p>
     *
     * @param supportChunked 是否支持chunked特性
     * @param maxPackageSize 拆分包后每个包的消息体最大的数量
     * @return 拆分后的消息集合
     */
    public List<NetPacket> partitionChunk(boolean supportChunked, int maxPackageSize) {
        if (!supportChunked) {
            return Collections.singletonList(this);
        }
        int bodyLength = body.length;
        if (bodyLength <= maxPackageSize) {
            return Collections.singletonList(this);
        }
        int packageCount = bodyLength / maxPackageSize;
        if (bodyLength % maxPackageSize > 0) {
            packageCount++;
        }
        List<NetPacket> results = new LinkedList<>();
        int remainLength = bodyLength;
        for (int i = 0; i < packageCount; i++) {
            int partitionBodyLength = Math.min(maxPackageSize, remainLength);
            byte[] partitionBody = new byte[partitionBodyLength];
            System.arraycopy(body, bodyLength - remainLength, partitionBody, 0, partitionBodyLength);
            remainLength -= partitionBodyLength;
            NetPacket partitionPackage = new NetPacket();
            partitionPackage.body = partitionBody;
            partitionPackage.header = this.header;
            partitionPackage.setSupportChunked(true);
            results.add(partitionPackage);
        }
        NetPacket tailPackage = new NetPacket();
        tailPackage.body = new byte[0];
        tailPackage.header = this.header;
        tailPackage.setSupportChunked(true);
        results.add(tailPackage);
        return results;
    }

    /**
     * <p>Description: Merge message body</p>
     * <p>Description: 合并消息体</p>
     *
     * @param otherPackage 网络包
     */
    public void mergeChunkedBody(NetPacket otherPackage) {
        int newBodyLength = body.length + otherPackage.getBody().length;
        byte[] newBody = new byte[newBodyLength];
        System.arraycopy(body, 0, newBody, 0, body.length);
        System.arraycopy(otherPackage.getBody(), 0, newBody, body.length, otherPackage.getBody().length);
        this.body = newBody;
    }

}
