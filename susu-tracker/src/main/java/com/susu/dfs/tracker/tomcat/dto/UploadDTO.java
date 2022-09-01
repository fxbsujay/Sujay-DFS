package com.susu.dfs.tracker.tomcat.dto;

import lombok.Data;

import java.io.File;

@Data
public class UploadDTO {

    private String filepath;

    private File file;
}
