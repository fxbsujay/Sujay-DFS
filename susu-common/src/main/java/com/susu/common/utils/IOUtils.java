package com.susu.common.utils;

import java.io.*;
import java.nio.charset.Charset;

/**
 * <p>Description: IO Toolset</p>
 * <p>IO工具类</p>
 * @author sujay
 * @version 17:06 2022/2/23
 * @since JDK1.8 <br/>
 */
public class IOUtils {

    /**
     * <p>Description: code</p>
     * <p>默认编码</p>
     */
    private static final String ENCODING = "UTF-8";

    private static final int BUFFER_SIZE = 2 * 1024;

    /**
     * <p>Description: Close one or more flow objects</p>
     * <p>关闭一个或多个流对象</p>
     * @param closeables 流
     */
    public static void close(Closeable... closeables) throws IOException {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        }
    }

    /**
     * <p>Description: Close one or more flow objects</p>
     * <p>关闭一个或多个流对象，并处理异常</p>
     * @param closeables 流
     */
    public static void closeQuietly(Closeable... closeables) {
        try {
            close(closeables);
        } catch (IOException e) {
            // TODO 随便写点
        }
    }

    /**
     * <p>Description: Write stream</p>
     * <p>写入流</p>
     * @param data  数据
     * @param out   流
     * @param encoding 编码
     */
    public static void write(String data, OutputStream out, String encoding) throws IOException {
        write(data, out, Charset.forName(encoding));
    }

    /**
     * <p>Description: Write stream</p>
     * <p>写入流</p>
     * @param data  数据
     * @param out   流
     * @param encoding 编码
     */
    public static void write(String data, OutputStream out, Charset encoding) throws IOException {
        if (data != null) {
            out.write(data.getBytes(encoding));
        }
    }

    /**
     * <p>Description: To string</p>
     * <p>转字符串</p>
     * @param in   流
     */
    public static String toString(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while ((len = in.read(buf,0,buf.length)) != -1){
            out.write(buf, 0, len);
        }
        return out.toString(ENCODING);
    }

    /**
     * <p>Description: To string</p>
     * <p>转字符串</p>
     * @param input byte数组
     */
    public static String toString(byte[] input, String encoding) {
        return new String(input, Charset.forName(encoding));
    }

}
