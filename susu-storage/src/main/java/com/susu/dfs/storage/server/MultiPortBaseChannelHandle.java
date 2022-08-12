package com.susu.dfs.storage.server;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.netty.BaseChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * <p>Description: Storage 多端口处理器</p>
 *
 * @author sujay
 * @version 15:20 2022/8/12
 */
public class MultiPortBaseChannelHandle extends BaseChannelHandler {

    private Node node;

    private StorageManager storageManager;

    public MultiPortBaseChannelHandle(Node node,StorageManager storageManager) {
        this.node = node;
        this.storageManager = storageManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        int localPort = ch.localAddress().getPort();
        if (localPort == node.getPort()) {
            super.initChannel(ch);
        } else if (localPort == node.getHttpPort()) {
            ch.pipeline().addLast(new HttpServerCodec());
            ch.pipeline().addLast(new HttpObjectAggregator(65536));
            ch.pipeline().addLast(new ChunkedWriteHandler());
            ch.pipeline().addLast(new HttpFileServerHandler(storageManager));
        }
    }
}
