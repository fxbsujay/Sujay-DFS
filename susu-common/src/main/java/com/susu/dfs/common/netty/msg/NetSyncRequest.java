package com.susu.dfs.common.netty.msg;

import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: 网络同步请求</p>
 * <p>Description: network Request</p>
 *
 * @author sujay
 * @version 23:07 2022/7/9
 */
@Slf4j
public class NetSyncRequest {


    private SocketChannel socketChannel;

    private String name;
}
