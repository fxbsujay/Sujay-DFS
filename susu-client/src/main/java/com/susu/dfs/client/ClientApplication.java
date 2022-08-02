package com.susu.dfs.client;

import com.susu.dfs.common.config.NodeConfig;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.client.service.ClientFileService;
import com.susu.dfs.client.service.impl.ClientFileServiceImpl;
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

    public ClientApplication(NodeConfig nodeConfig) {
        this.taskScheduler = new TaskScheduler("SUSU-DFS-CLIENT",8,false);
        this.trackerClient = new TrackerClient(nodeConfig.getNode(),taskScheduler);
        this.clientFileService = new ClientFileServiceImpl(trackerClient,taskScheduler);
    }

    public ClientFileService getFileService() {
        return clientFileService;
    }

    /**
     * 启动
     *
     * @throws Exception 中断异常
     */
    public void start() throws Exception {
        this.trackerClient.start();
    }

    /**
     * 关机
     */
    public void shutdown() {
        this.taskScheduler.shutdown();
        this.trackerClient.shutdown();
    }
}
