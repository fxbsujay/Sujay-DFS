package com.susu.dfs.client;

import com.susu.dfs.client.service.ClientFileService;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.config.NodeConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class ClientTest {

    private static final String UPLOAD_LOCAL_PATH = System.getProperty("user.dir") + "/img/susu.jpg";
    public static void main(String[] args) {
        NodeConfig nodeConfig = new NodeConfig("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\client_config.json");
        ClientApplication application = new ClientApplication(nodeConfig);
        try {
            application.start();
            ClientFileService fileService = application.getFileService();
            Map<String, String> stringStringMap = fileService.readAttr("/aaa/bbb/test.jpg");
            System.out.println(stringStringMap.get("aaa"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
