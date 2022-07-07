package com.susu.dfs.server;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.netty.NetServer;
import com.susu.dfs.common.task.TaskScheduler;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: Tracker 的 通讯服务</p>
 *
 * @author sujay
 * @version 14:48 2022/7/7
 */
@Slf4j
public class TrackerServer {

    private NetServer netServer;

    private TrackerChannelHandle trackerChannelHandle;

    private int port;

    public TrackerServer(Node node, TaskScheduler taskScheduler) {
        this.netServer = new NetServer(node.getName(),taskScheduler);
        this.port = node.getPort();
        this.trackerChannelHandle = new TrackerChannelHandle();
    }

    public void start() {
        this.netServer.addHandler(trackerChannelHandle);
        netServer.startAsync(port);
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        log.info("Shutdown NameNodeServer.");
        netServer.shutdown();
    }
}
