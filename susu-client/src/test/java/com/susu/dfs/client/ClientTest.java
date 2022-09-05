package com.susu.dfs.client;

import com.susu.dfs.common.client.ClientApplication;
import com.susu.dfs.common.client.service.ClientFileService;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ClientTest {

    private static final String UPLOAD_LOCAL_PATH = System.getProperty("user.dir") + "/img/susu.jpg";

    private static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/download/susu.jpg";

    public static void main(String[] args) {
        try {
            ClientApplication application = ClientApplication.initStart();
            ClientFileService fileService = application.getFileService();
            Map<String,String> attr = new HashMap<>();
            attr.put("aaa","bbbb");
            fileService.put("/aaa/bbb/test.jpg",new File(UPLOAD_LOCAL_PATH),-1,attr);
            Map<String, String> stringStringMap = fileService.readAttr("/aaa/bbb/test.jpg");
            System.out.println(stringStringMap.get("aaa"));
            fileService.get("/aaa/bbb/test.jpg",DOWNLOAD_PATH);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
