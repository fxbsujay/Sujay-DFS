package com.susu.dfs.storage.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * <p>Description: Storage 的 Http文件下载</p>
 *
 * @author sujay
 * @version 15:20 2022/8/12
 */
@Slf4j
public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private StorageManager storageManager;

    public HttpFileServerHandler(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        String urlPrefix = "/api/download";
        String url = URLDecoder.decode(request.uri(), "UTF-8");
        if (!url.startsWith(urlPrefix)) {
            return;
        }
        String filename = url.substring(urlPrefix.length());
        if (!request.method().equals(HttpMethod.GET)) {
            log.info("Ignore http request : [uri={}, method={}]", request.uri(), request.method());
            return;
        }

        log.debug("File download request received：[filename={}]", filename);
        String absolutePath = storageManager.getAbsolutePathByFileName(filename);
        String name = filename.substring(filename.lastIndexOf("/") + 1);
        File file = new File(absolutePath);
        if (!file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, String.format("file not found: %s", filename));
            return;
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, raf.length());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
            response.headers().add(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" +
                    new String(name.getBytes("GBK"), "ISO8859-1") + "\"");
            ctx.write(response);
            ChannelFuture sendFileFuture;
            if (ctx.pipeline().get(SslHandler.class) == null) {
                sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0,
                        raf.length()), ctx.newProgressivePromise());
            } else {
                sendFileFuture = ctx.write(new ChunkedFile(raf, 0,
                        raf.length(), 8192), ctx.newProgressivePromise());
            }
            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {

                private long lastProgress = 0;

                @Override
                public void operationComplete(ChannelProgressiveFuture future)
                        throws Exception {
                    log.debug("file transfer complete. [filename={}]", filename);
                    raf.close();
                }

                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                    if (total < 0) {
                        log.warn("file transfer progress: [filename={}, progress={}]", file.getName(), progress);
                    } else {
                        int deltaProgress = (int) (progress - lastProgress);
                        lastProgress = progress;
                        log.debug("file transfer progress: [filename={}, progress={}, diskSize={}]", file.getName(), progress / total, deltaProgress);
                    }
                }
            });
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } catch (FileNotFoundException e) {
            log.warn("file not found:  [filename={}]", file.getPath());
            sendError(ctx, HttpResponseStatus.NOT_FOUND, String.format("file not found: %s", file.getPath()));
        } catch (IOException e) {
            log.warn("file has a IOException:  [filename={}, err={}]", file.getName(), e.getMessage());
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, String.format("Exception reading file：%s", absolutePath));
        }
    }

    /**
     * 请求处理失败返回
     */
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
        ByteBuf content = Unpooled.copiedBuffer("Failure: " + status + "\r\n" + msg, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,content);
        response.headers().set("Content-Type", "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
