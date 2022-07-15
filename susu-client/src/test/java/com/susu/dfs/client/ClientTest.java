package com.susu.dfs.client;

import com.susu.dfs.client.service.ClientFileService;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.config.NodeConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ClientTest {

    private static final String UPLOAD_LOCAL_PATH = System.getProperty("user.dir") + "/../img/susu.jpg";
    public static void main(String[] args) {
        NodeConfig nodeConfig = new NodeConfig("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\client_config.json");
        ClientApplication application = new ClientApplication(nodeConfig);
        try {
            application.start();
            ClientFileService fileService = application.getFileService();
            Map<String, String> attr = new HashMap<>(Constants.MAP_SIZE);
            attr.put("aaa", "1222");
            fileService.put("/aaa/bbb/susu.jpg",new File(UPLOAD_LOCAL_PATH),-1,attr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
