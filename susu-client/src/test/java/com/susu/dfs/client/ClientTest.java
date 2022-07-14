package com.susu.dfs.client;

import com.susu.dfs.client.service.ClientFileService;
import com.susu.dfs.common.config.NodeConfig;

public class ClientTest {


    public static void main(String[] args) {
        NodeConfig nodeConfig = new NodeConfig("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\client_config.json");
        ClientApplication application = new ClientApplication(nodeConfig);
        try {
            application.start();
            ClientFileService fileService = application.getFileService();
            fileService.mkdir("/aaa/bbb");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
