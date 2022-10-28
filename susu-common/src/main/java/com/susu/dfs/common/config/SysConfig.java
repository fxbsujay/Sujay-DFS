package com.susu.dfs.common.config;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.eum.ServerEnum;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * 文件存储位置 源文件
     */
    public String DEFAULT_BASE_DIR = Constants.DEFAULT_BASE_DIR;

    /**
     * 默认系统持久化文件存储位置 Image_log, user.info, Ready_Log
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
     * 配置文件名称
     */
    private final static String CONFIG_FILE_NAME = "/application.yaml";

    /**
     * 认证信息
     */
    private final AuthConfig authConfig = new AuthConfig();

    /**
     * 当前节点信息
     */
    private final Node node = new Node();

    public Node getNode() {
        return node;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public List<TrackerInfo> getTrackers() {
        return trackers;
    }

    /**
     *  加载 resources 路径下的配置文件
     */
    private static <T>  Map<String, Object> loadFile() {
        InputStream in = null;
        try {
            in = SysConfig.class.getResourceAsStream(CONFIG_FILE_NAME);
            if (in == null) {
                log.error("The {} file under the resource directory cannot be found !!", CONFIG_FILE_NAME);
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("exception for read file");
            System.exit(1);
        }

        return new Yaml().load(in);
    }

    /**
     *  加载 path 路径下的配置文件
     */
    private static Map<String, Object> loadFile(String path) {
        InputStream in = null;
        try {
            if (StringUtils.isBlank(path)) {
                path = System.getProperty("user.dir") + CONFIG_FILE_NAME;
            }
            log.info("read config file in ：{}",path);
            Path filepath = Paths.get(path);
            in = Files.newInputStream(filepath);

        } catch (IOException e) {
            log.error("exception for read file");
            System.exit(1);
        }

        return new Yaml().load(in);
    }

    public static SysConfig loadConfig(ServerEnum type) {
        return loadConfig(null,type);
    }

    public static SysConfig loadConfig(String[] args, ServerEnum type) {

        Map<String, Object> stringObjectMap;

        if (args == null || args.length == 0) {
            stringObjectMap = loadFile();
        } else {
            stringObjectMap = loadFile(args[0]);
        }

        SysConfig config = new SysConfig();
        switch (type) {
            case TRACKER:
                config =  loadTrackerConfig(stringObjectMap);
                break;
            case STORAGE:
                config = loadStorageConfig(stringObjectMap);
                break;
            case CLIENT:
                config = loadClientConfig(stringObjectMap);
                break;
            default:
                break;
        }

        return  config;


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

    private static SysConfig loadTrackerConfig(Map<String, Object> stringObjectMap) {

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

    private static SysConfig loadStorageConfig(Map<String, Object> stringObjectMap) {

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

    public static SysConfig loadClientConfig(Map<String, Object> stringObjectMap) {

        Map<String, Object> clientConfig = (Map<String, Object>) stringObjectMap.get("client");
        SysConfig config = new SysConfig();

        String name = (String) clientConfig.get("name");
        if (StringUtils.isNotBlank(name)) {
            config.node.setName(name);
        } else {
            config.node.setName("net-client");
        }

        String trackerHost = (String) clientConfig.get("trackerHost");
        if (StringUtils.isNotBlank(trackerHost)) {
            config.node.setTrackerHost(trackerHost);
        } else {
            config.node.setHost("localhost");
        }

        Integer trackerPort = (Integer) clientConfig.get("trackerPort");
        if (trackerPort != null) {
            config.node.setTrackerPort(trackerPort);
        } else {
            config.node.setTrackerPort(9081);
        }
        AuthConfig authConfig = config.getAuthConfig();

        String username = (String) clientConfig.get("username");
        if (StringUtils.isNotBlank(username)) {
            authConfig.setUsername(username);
        } else {
            authConfig.setUsername("susu");
        }

        String password = (String) clientConfig.get("password");
        if (StringUtils.isNotBlank(password)) {
            authConfig.setPassword(password);
        } else {
            authConfig.setPassword("susu");
        }

        return config;
    }
}
