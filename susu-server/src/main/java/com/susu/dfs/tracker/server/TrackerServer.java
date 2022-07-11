package com.susu.dfs.tracker.server;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.netty.NetServer;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.SnowFlakeUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: Tracker 的 通讯服务端</p>
 *
 * @author sujay
 * @version 14:48 2022/7/7
 */
@Slf4j
public class TrackerServer {

    private NetServer netServer;

    private TrackerChannelHandle trackerChannelHandle;

    private final Node node;

    public TrackerServer(Node node, TaskScheduler taskScheduler) {
        this.node = node;
        this.netServer = new NetServer(node.getName(),taskScheduler);
        this.trackerChannelHandle = new TrackerChannelHandle();
    }

    public void start() {
        log.info("Start Tracker Server :{}",node.getName());
        this.netServer.addHandler(trackerChannelHandle);
        netServer.startAsync(node.getPort());
    }

    public void shutdown() {
        log.info("Shutdown Tracker Server :{}",node.getName());
        netServer.shutdown();
    }
}
