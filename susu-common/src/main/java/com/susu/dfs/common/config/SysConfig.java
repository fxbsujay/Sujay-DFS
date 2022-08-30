package com.susu.dfs.common.config;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SysConfig {

    /**
     * 默认客户端心跳间隔
     */
    public int HEARTBEAT_INTERVAL = Constants.HEARTBEAT_INTERVAL;

    /**
     * 默认客户端存活时间
     */
    public int HEARTBEAT_OUT_TIME = Constants.HEARTBEAT_OUT_TIME;

    /**
     * 默认呵客户端存活检测时间间隔
     */
    public int HEARTBEAT_CHECK_INTERVAL = Constants.HEARTBEAT_CHECK_INTERVAL;

    /**
     * 客户端文件存储位置
     */
    public String DEFAULT_BASE_DIR = Constants.DEFAULT_BASE_DIR;

    /**
     * 默认系统持久化文件存储位置
     */
    public String SYS_LOG_BASE_DIR = Constants.DEFAULT_BASE_DIR;

    /**
     * 文件存储默认路径前缀
     */
    public String DEFAULT_BASE_FILE_PATH = "/susu";

    /**
     * 集群信息
     */
    private final List<TrackerInfo> trackers = new ArrayList<>();

    /**
     * 当前节点信息
     */
    private final Node node = new Node();

    public Node getNode() {
        return node;
    }

    public List<TrackerInfo> getTrackers() {
        return trackers;
    }

    private static <T>  Map<String, Object> loadFile(Class<T> tClass) {
        BufferedReader br = null;
        try {
            URL resource = tClass.getClassLoader().getResource("application.yaml");

            if (resource == null) {
                log.error("The application.yaml file under the resource directory cannot be found !!");
                System.exit(1);
            }

            String path = resource.getPath().substring(1);
            log.info("read config file in ：{}",path);
            br = new BufferedReader(new FileReader(path));
        } catch (Exception e) {
            log.error("exception for read file");
            System.exit(1);
        }

        return new Yaml().load(br);
    }

    private static SysConfig loadConfig(Map<String, Object> configParams) {
        SysConfig config = new SysConfig();

        String name = (String) configParams.get("name");
        if (StringUtils.isNotBlank(name)) {
            config.node.setName(name);
        } else {
            config.node.setName("net-storage");
        }

        String host = (String) configParams.get("host");
        if (StringUtils.isNotBlank(host)) {
            config.node.setHost(host);
        } else {
            config.node.setHost("localhost");
        }

        Map<String, Object> configs = (Map<String, Object>) configParams.get("config");

        if (configs == null) {
            return config;
        }

        Integer heartbeatInterval = (Integer) configs.get("heartbeatInterval");
        if (heartbeatInterval != null) {
            config.HEARTBEAT_INTERVAL = heartbeatInterval;
        }

        Integer heartbeatOutTime = (Integer) configs.get("heartbeatOutTime");
        if (heartbeatOutTime != null) {
            config.HEARTBEAT_OUT_TIME = heartbeatOutTime;
        }

        Integer heartbeatCheckInterval = (Integer) configs.get("heartbeatCheckInterval");
        if (heartbeatCheckInterval != null) {
            config.HEARTBEAT_CHECK_INTERVAL = heartbeatCheckInterval;
        }

        String defaultBaseDir = (String) configs.get("defaultBaseDir");
        if (StringUtils.isNotBlank(defaultBaseDir)) {
            config.DEFAULT_BASE_DIR = defaultBaseDir;
        }

        String defaultBaseFilePath = (String) configs.get("defaultBaseFilePath");
        if (StringUtils.isNotBlank(defaultBaseFilePath)) {
            config.DEFAULT_BASE_FILE_PATH = defaultBaseFilePath;
        }

        String sysLogBaseDir = (String) configs.get("sysLogBaseDir");
        if (StringUtils.isNotBlank(sysLogBaseDir)) {
            config.SYS_LOG_BASE_DIR = sysLogBaseDir;
        }

        return config;
    }


    public static <T> SysConfig loadTrackerConfig(Class<T> tClass) {

        Map<String, Object> stringObjectMap = loadFile(tClass);

        Map<String, Object> trackerConfig = (Map<String, Object>) stringObjectMap.get("tracker");

        SysConfig config = loadConfig(trackerConfig);

        Integer port = (Integer) trackerConfig.get("port");
        if (port != null) {
            config.node.setPort(port);
        } else {
            config.node.setPort(9081);
        }

        Integer httpPort = (Integer) trackerConfig.get("httpPort");
        if (httpPort != null) {
            config.node.setHttpPort(httpPort);
        } else {
            config.node.setHttpPort(9080);
        }

        List<String> cluster = (List<String>) trackerConfig.get("cluster");

        if (cluster != null && cluster.size() > 1) {
            config.node.setIsCluster(true);
            for (int i = 0; i < cluster.size(); i++) {
                String[] info = cluster.get(i).split(":");
                TrackerInfo trackerInfo = new TrackerInfo(i,info[0],info[1],Integer.parseInt(info[2]));
                if (config.node.getHost().equals(trackerInfo.getHostname()) && config.node.getPort() == trackerInfo.getPort()) {
                    config.node.setIndex(i);
                }
                config.trackers.add(trackerInfo);
            }
        }

        return config;
    }
    public static <T> SysConfig loadStorageConfig(Class<T> tClass) {

        Map<String, Object> stringObjectMap = loadFile(tClass);

        Map<String, Object> storageConfig = (Map<String, Object>) stringObjectMap.get("storage");
        SysConfig config = loadConfig(storageConfig);

        Integer port = (Integer) storageConfig.get("port");
        if (port != null) {
            config.node.setPort(port);
        } else {
            config.node.setPort(9082);
        }


        Integer httpPort = (Integer) storageConfig.get("httpPort");
        if (httpPort != null) {
            config.node.setHttpPort(httpPort);
        } else {
            config.node.setHttpPort(9083);
        }

        String trackerHost = (String) storageConfig.get("trackerHost");
        if (StringUtils.isNotBlank(trackerHost)) {
            config.node.setTrackerHost(trackerHost);
        } else {
            config.node.setHost("localhost");
        }

        Integer trackerPort = (Integer) storageConfig.get("trackerPort");
        if (trackerPort != null) {
            config.node.setTrackerPort(trackerPort);
        } else {
            config.node.setTrackerPort(9081);
        }

        return config;
    }

    public static <T> SysConfig loadClientConfig(Class<T> tClass) {

        Map<String, Object> stringObjectMap = loadFile(tClass);

        Map<String, Object> storageConfig = (Map<String, Object>) stringObjectMap.get("client");
        SysConfig config = new SysConfig();

        String name = (String) storageConfig.get("name");
        if (StringUtils.isNotBlank(name)) {
            config.node.setName(name);
        } else {
            config.node.setName("net-storage");
        }

        String trackerHost = (String) storageConfig.get("trackerHost");
        if (StringUtils.isNotBlank(trackerHost)) {
            config.node.setTrackerHost(trackerHost);
        } else {
            config.node.setHost("localhost");
        }

        Integer trackerPort = (Integer) storageConfig.get("trackerPort");
        if (trackerPort != null) {
            config.node.setTrackerPort(trackerPort);
        } else {
            config.node.setTrackerPort(9081);
        }

        return config;
    }
}
