package com.susu.dfs.storage;

import com.susu.dfs.common.config.NodeConfig;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.storage.client.TrackerClient;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * <p>Description: DFS 的 执行器，用于存储和管理文件</p>
 *
 * @author sujay
 * @version 14:01 2022/7/8
 */
@Slf4j
public class StorageApplication {

    private TrackerClient trackerClient;
    private final TaskScheduler taskScheduler;

    private AtomicBoolean started = new AtomicBoolean(false);

    public StorageApplication(NodeConfig nodeConfig) {
        this.taskScheduler = new TaskScheduler("SUSU-DFS-CLIENT",1,false);
        this.trackerClient = new TrackerClient(nodeConfig.getNode(),taskScheduler);
    }

    /**
     * <h3> 服务端的启动流程 </h3>
     * <ul>
     *     <li>加载配置文件</li>
     *     <li>初始化任务执行器，一个线程池</li>
     *     <li>初始化一个 Tracker 客户端</li>
     *     <li>向 Tracker 注册</li>
     *     <li>注册成功，发送心跳</li>
     * </ul>
     */
    public static void main(String[] args) {

        NodeConfig nodeConfig = new NodeConfig("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\client_config.json");
        StorageApplication application = new StorageApplication(nodeConfig);
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(application::shutdown));
            application.start();
        } catch (Exception e) {
            log.info("Tracker Application Start Error!!");
        }
    }

    /**
     * 启动
     *
     * @throws Exception 中断异常
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            this.trackerClient.start();
        }
    }

    /**
     * 停机
     */
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            this.taskScheduler.shutdown();
            this.trackerClient.shutdown();
        }
    }
}
