package com.susu.common;


import lombok.Data;

/**
 * <p>Description: 启动的节点信息</p>
 * @author sujay
 * @version 14:36 2022/7/1
 */
@Data
public class Node {


    /**
     * 启动的是服务端还是客户端
     */
    private String type = "server";

    /**
     * 默认启动端口
     */
    private int port = 8090;


}
