package com.susu.dfs.storage;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.config.NodeConfig;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.storage.client.TrackerClient;
import com.susu.dfs.storage.locator.FileLocatorFactory;
import com.susu.dfs.storage.server.StorageManager;
import com.susu.dfs.storage.server.StorageServer;
import com.susu.dfs.storage.server.StorageTransportCallback;
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

    private StorageServer storageServer;

    private final TaskScheduler taskScheduler;

    private StorageManager storageManager;

    private AtomicBoolean started = new AtomicBoolean(false);

    public StorageApplication(NodeConfig nodeConfig) {
        Node node = nodeConfig.getNode();
        this.taskScheduler = new TaskScheduler("SUSU-DFS-STORAGE",8,false);
        this.storageManager = new StorageManager(Constants.DEFAULT_BASE_DIR, FileLocatorFactory.SHA1);
        this.trackerClient = new TrackerClient(node, taskScheduler, storageManager);
        StorageTransportCallback transportCallback = new StorageTransportCallback(storageManager,trackerClient);
        this.storageServer = new StorageServer(node,taskScheduler,storageManager,transportCallback);

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
     *
     * <h3> 文件上传流程 </h3>
     * <ul>
     *     <li>Client先向Tracker发起创建文件请求</li>
     *     <li>Tracker修改目录树，并为Client分配用于存储文件的Storage节点</li>
     *     <li>Client将文件上传到Storage节点</li>
     *     <li>Storage接收文件完成后上报信息给Tracker</li>
     *     <li>Tracker保存文件与节点的对应关系</li>
     *     <li>Client向Tracker确实文件是否已经上传完毕</li>
     * </ul>
     */
    public static void main(String[] args) {

        NodeConfig nodeConfig = new NodeConfig("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\storage_config.json");
        StorageApplication application = new StorageApplication(nodeConfig);
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(application::shutdown));
            application.start();
        } catch (Exception e) {
            log.info("Tracker Application Start Error!!");
            System.exit(1);
        }
    }

    /**
     * 启动
     *
     * @throws InterruptedException 中断异常
     */
    public void start() throws InterruptedException {
        if (started.compareAndSet(false, true)) {
            this.trackerClient.start();
            this.storageServer.start();
        }
    }

    /**
     * 停机
     */
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            this.taskScheduler.shutdown();
            this.trackerClient.shutdown();
            this.storageServer.shutdown();
        }
    }
}
