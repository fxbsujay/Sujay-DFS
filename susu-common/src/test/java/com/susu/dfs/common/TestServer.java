package com.susu.dfs.common;

import com.susu.dfs.common.config.NodeConfig;
import com.susu.dfs.common.netty.NetServer;
import com.susu.dfs.common.task.TaskScheduler;

/**
 * 测试类
 */
public class TestServer {

    public static void main(String[] args) {
        TestServer.startBaseServer();
    }

    /**
     * 启动一个最基础的服务端服务器
     */
    public static void startBaseServer() {
        Node node = NodeConfig.getNode("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\config.json");
        TaskScheduler taskScheduler = new TaskScheduler("Server-Scheduler",1,false);
        NetServer netServer = new NetServer("server",taskScheduler);
        netServer.startAsync(node.getPort());
    }
}
