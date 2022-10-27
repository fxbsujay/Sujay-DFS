package com.susu.dfs.common.client;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.client.service.ClientFileService;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.client.service.impl.ClientFileServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: DFS 客户端</p>
 *
 * @author sujay
 * @version 10:11 2022/7/14
 */
@Slf4j
public class ClientApplication {

    private TaskScheduler taskScheduler;

    private TrackerClient trackerClient;

    private ClientFileService clientFileService;

    private volatile boolean inService = false;

    private ClientApplication() {
        this.taskScheduler = new TaskScheduler("CLIENT-TRACKER");
        this.trackerClient = new TrackerClient(taskScheduler);
        this.clientFileService = new ClientFileServiceImpl(trackerClient,taskScheduler);
    }

    /**
     *  初始化启动
     */
    public static ClientApplication initStart() throws Exception {
        SysConfig config = SysConfig.loadClientConfig();
        ClientApplication clientApplication = new ClientApplication();
        Node node = config.getNode();
        clientApplication.start(node.getTrackerHost(),node.getTrackerPort());
        clientApplication.inService = true;
        return clientApplication;
    }

    public ClientFileService getFileService() {
        return clientFileService;
    }

    /**
     * 启动
     *
     * @param host  tracker 主机地址
     * @param port  tracker 主机端口
     * @throws Exception    netty连接异常
     */
    public ClientFileService start(String host, int port) throws Exception {
        if (inService) {
            throw new RuntimeException("Client started !!");
        }
        this.trackerClient.start(host,port);
        return clientFileService;
    }

    /**
     * 关机
     */
    public void shutdown() {
        if (inService) {
            this.taskScheduler.shutdown();
            this.trackerClient.shutdown();
        }
    }
}
