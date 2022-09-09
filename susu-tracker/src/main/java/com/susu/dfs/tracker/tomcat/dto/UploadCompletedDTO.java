package com.susu.dfs.tracker.tomcat.dto;

import lombok.Data;

@Data
public class UploadCompletedDTO {

    private String storageHost;

    private String path;
}
