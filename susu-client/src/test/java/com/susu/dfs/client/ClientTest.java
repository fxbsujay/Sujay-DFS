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
            Map<String, String> attr = new HashMap<>(Constants.MAP_SIZE);
            int multiCount = 20;
            CountDownLatch latch = new CountDownLatch(multiCount);
            for (int i = 0; i < multiCount; i++) {
                new Thread(() -> {
                    Random random = new Random();
                    try {
                        fileService.put("/aaa/bbb/test-" + random.nextInt(10000000),new File(UPLOAD_LOCAL_PATH),-1,attr );
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            latch.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
