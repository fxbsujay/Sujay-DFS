package com.susu.dfs.common.config;


import com.alibaba.fastjson.JSON;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.common.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: 负责读取启动的配置文件 config.json</p>
 * @author sujay
 * @version 13:24 2022/7/1
 */
@Slf4j
public class NodeConfig {

    /**
     * 节点的信息
     */
    private final Node node;

    /**
     * 集群
     */
    private final List<TrackerInfo> trackers = new ArrayList<>();

    /**
     * @param path 配置文件路径
     */
    public NodeConfig(String path) {
        Node node = null;
        try {
            log.info("read config file in ：{}",path);
            String json = FileUtils.readString(path);
            node = JSON.parseObject(json, Node.class);
            node.setHost("localhost");
            if (node.getIsCluster()) {
                List<String> servers = node.getServers();
                for (int i = 0; i < servers.size(); i++) {
                    String[] info = servers.get(i).split(":");
                    TrackerInfo trackerInfo = new TrackerInfo(i,info[0],info[1],Integer.parseInt(info[2]));
                    if (node.getHost().equals(trackerInfo.getHostname()) && node.getPort() == trackerInfo.getPort()) {
                        node.setIndex(i);
                    }
                    trackers.add(trackerInfo);
                }
            }else {
                node.setIndex(0);
            }
        } catch (IOException e) {
            log.error("exception for read file");
            System.exit(1);
        }
        this.node = node;
    }


    public Node getNode() {
        return node;
    }

    public List<TrackerInfo> getTrackers() {
        return trackers;
    }

    public static Node getNode(String path) {
        Node node = null;
        try {
            log.info("read config file in ：{}",path);
            String json = FileUtils.readString(path);
            node = JSON.parseObject(json, Node.class);
        } catch (IOException e) {
            log.error("exception for read file");
            System.exit(1);
        }
        return node;
    }
}
