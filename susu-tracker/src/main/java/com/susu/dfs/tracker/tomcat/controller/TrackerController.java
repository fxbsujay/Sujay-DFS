package com.susu.dfs.tracker.tomcat.controller;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.Result;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.file.FileNode;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.service.TrackerFileService;
import com.susu.dfs.tracker.tomcat.annotation.Autowired;
import com.susu.dfs.tracker.tomcat.annotation.RequestMapping;
import com.susu.dfs.tracker.tomcat.annotation.RestController;
import com.susu.dfs.tracker.tomcat.dto.FileTreeDTO;
import com.susu.dfs.tracker.tomcat.dto.TrackerDTO;

/**
 * <p>Description: 存储器 API</p>
 *
 * @author sujay
 * @version 17:12 2022/8/29
 */
@RestController
@RequestMapping("/api/tracker")
public class TrackerController {

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private TrackerFileService trackerFileService;

    @Autowired
    private SysConfig sysConfig;

    @RequestMapping("/info")
    public Result<TrackerDTO> info() {
        TrackerDTO dto = new TrackerDTO();
        Node node = sysConfig.getNode();
        dto.setHost(node.getHost());
        dto.setPort(node.getPort());
        dto.setHttpPort(node.getHttpPort());
        dto.setBaseDir(sysConfig.DEFAULT_BASE_DIR);
        dto.setLogBaseDir(sysConfig.SYS_LOG_BASE_DIR);
        return Result.ok(dto);
    }

    @RequestMapping("/tree")
    public Result<FileTreeDTO> queryFileTree() {
        FileNode fileNode = trackerFileService.listFiles("/");
        FileTreeDTO tree = FileTreeDTO.tree(fileNode, "/");
        return Result.ok(tree);
    }

}
