package com.susu.dfs.tracker;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.config.NodeConfig;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.server.TrackerServer;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Description: DFS 的 调度器，也是核心管理器</p>
 *
 * @author sujay
 * @version 14:48 2022/7/7
 */
@Slf4j
public class TrackerApplication {

    private final TaskScheduler taskScheduler;

    private final TrackerServer trackerServer;

    private final AtomicBoolean started = new AtomicBoolean(false);


    /**
     * <h3> 服务端的启动流程 </h3>
     * <ul>
     *     <li>加载配置文件</li>
     *     <li>初始化任务执行器，一个线程池</li>
     *     <li>初始化一个 Tracker 服务端用来处理 Tracker 客户端发来的消息</li>
     * </ul>
     */
     public static void main(String[] args) {

        NodeConfig nodeConfig = new NodeConfig("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\server_config.json");
        TrackerApplication application = new TrackerApplication(nodeConfig);
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(application::shutdown));
            application.start();
        } catch (Exception e) {
            log.info("Tracker Application Start Error!!");
        }
    }

    public TrackerApplication(NodeConfig nodeConfig) {
        Node node = nodeConfig.getNode();
        this.taskScheduler = new TaskScheduler("SUSU-DFS-SERVER",8,false);
        this.trackerServer = new TrackerServer(node,taskScheduler);
    }

    /**
     * 启动
     *
     * @throws Exception 中断异常
     */
    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            this.trackerServer.start();
        }
    }

    /**
     * 停机
     */
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            this.taskScheduler.shutdown();
            this.trackerServer.shutdown();
        }
    }
}
