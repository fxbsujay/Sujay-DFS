package com.susu.test;

import com.susu.dfs.common.client.ClientApplication;
import com.susu.dfs.common.client.service.ClientFileService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class ClientTest {

    private static final String UPLOAD_LOCAL_PATH = System.getProperty("user.dir") + "/img/susu.jpg";

    private static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/download/susuc.jpg";

    public static void main(String[] args) {
        try {
            ClientApplication client = ClientApplication.initStart();
            ClientFileService fileService = client.getFileService();
            fileService.get("/susuA.jpg",DOWNLOAD_PATH);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Client Application Start Error!!");
            System.exit(1);
        }
    }
}
