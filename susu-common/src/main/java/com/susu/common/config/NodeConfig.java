package com.susu.common.config;


import com.alibaba.fastjson.JSON;
import com.susu.common.Node;
import com.susu.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * <p>Description: 负责读取启动的配置文件 config.json</p>
 * @author sujay
 * @version 13:24 2022/7/1
 */
@Slf4j
public class NodeConfig {


    private final Node node;

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
        log.info(node.getType());
    }


    public Node getNode() {
        return node;
    }
}
