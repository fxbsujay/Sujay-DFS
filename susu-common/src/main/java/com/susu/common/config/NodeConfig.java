package com.susu.common.config;


import com.alibaba.fastjson.JSON;
import com.susu.common.Node;
import com.susu.common.utils.FileUtils;
import com.susu.common.utils.SnowFlakeUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

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
     * 全局id生成策略
     */
    private final SnowFlakeUtils snowFlake;

    /**
     * @param path 配置文件路径
     */
    public NodeConfig(String path) {
        Node node = null;
        try {
            log.info("read config file in ：{}",path);
            String json = FileUtils.readString(path);
            node = JSON.parseObject(json, Node.class);
        } catch (IOException e) {
            log.error("exception for read file");
            System.exit(1);
        }
        this.node = node;
        snowFlake = new SnowFlakeUtils(1,2);
    }


    public Node getNode() {
        return node;
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
