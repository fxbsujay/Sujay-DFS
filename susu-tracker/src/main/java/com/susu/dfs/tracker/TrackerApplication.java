package com.susu.dfs.tracker;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.eum.ServerEnum;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.server.TrackerChannelHandle;
import com.susu.dfs.tracker.server.TrackerServer;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import com.susu.dfs.tracker.tomcat.TomcatServer;
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

    private final TrackerChannelHandle trackerChannelHandle;

    private final ClientManager clientManager;

    private final ServerManager serverManager;

    private final TrackerServer trackerServer;

    private final TrackerFileService fileService;

    private final TrackerClusterService clusterService;

    private final TomcatServer tomcatServer;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * <h3>服务端的启动流程</h3>
     * nohup java -jar susu-storage-1.0-SNAPSHOT.jar /mydata/dfs/application.yaml &
     * <ul>
     *     <li>加载配置文件</li>
     *     <li>初始化任务执行器，一个线程池</li>
     *     <li>初始化一个 Tracker 服务端用来处理 Tracker 客户端发来的消息</li>
     * </ul>
     */
     public static void main(String[] args) {

         SysConfig config = SysConfig.loadConfig(args, ServerEnum.TRACKER);
         TrackerApplication application = new TrackerApplication(config);
         try {
             Runtime.getRuntime().addShutdownHook(new Thread(application::shutdown));
             application.start();
         } catch (Exception e) {
             e.printStackTrace();
             log.info("Tracker Application Start Error!!");
             System.exit(1);
         }
    }

    public TrackerApplication(SysConfig config) {
         Node node = config.getNode();
         this.taskScheduler = new TaskScheduler("TRACKER-TRACKER");
         this.clientManager = new ClientManager(config,taskScheduler);
         this.fileService = new TrackerFileService(config,taskScheduler,clientManager);
         this.clusterService = new TrackerClusterService(node,config.getTrackers(),taskScheduler);
         this.serverManager = new ServerManager(node,config.getTrackers(),clientManager,clusterService, fileService);
         this.trackerChannelHandle = new TrackerChannelHandle(config,taskScheduler, clientManager, serverManager, fileService, clusterService);
         this.trackerServer = new TrackerServer(node,taskScheduler,trackerChannelHandle);
         this.tomcatServer = new TomcatServer(config,trackerChannelHandle,serverManager,clientManager,clusterService,fileService);
    }

    /**
     * 启动
     *
     * @throws Exception 中断异常
     */
    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            this.fileService.start();
            this.clusterService.start();
            this.trackerServer.start();
            this.tomcatServer.start();
        }
    }

    /**
     * 停机
     */
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            this.taskScheduler.shutdown();
            this.fileService.shutdown();
            this.clusterService.shutdown();
            this.trackerServer.shutdown();
            this.tomcatServer.shutdown();
        }
    }
}
