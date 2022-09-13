package com.susu.dfs.tracker.tomcat.controller;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.Result;
import com.susu.dfs.common.TrackerInfo;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.file.FileNode;
import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.service.TrackerFileService;
import com.susu.dfs.tracker.tomcat.annotation.Autowired;
import com.susu.dfs.tracker.tomcat.annotation.RequestMapping;
import com.susu.dfs.tracker.tomcat.annotation.RestController;
import com.susu.dfs.tracker.tomcat.dto.FileTreeDTO;
import com.susu.dfs.tracker.tomcat.dto.StorageDTO;
import com.susu.dfs.tracker.tomcat.dto.TrackerDTO;
import com.susu.dfs.tracker.tomcat.dto.UploadDTO;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: 调度器 API</p>
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

    @Autowired
    private ServerManager serverManager;
    @RequestMapping("/list")
    public Result<List<TrackerDTO>> queryList() {
        List<TrackerDTO> result = new ArrayList<>();
        List<TrackerInfo> trackers = sysConfig.getTrackers();
        if (trackers.isEmpty()) {
            return Result.ok(result);
        }

        return Result.ok(result);
    }

    @RequestMapping("/info")
    public Result<TrackerDTO> info() {
        long totalStorageSize = 0;
        List<ClientInfo> clientList = clientManager.getClientList();
        for (ClientInfo clientInfo : clientList) {
            totalStorageSize += clientInfo.getStoredSize();
        }
        TrackerDTO dto = new TrackerDTO();
        Node node = sysConfig.getNode();
        dto.setHost(node.getHost());
        dto.setPort(node.getPort());
        dto.setHttpPort(node.getHttpPort());
        dto.setBaseDir(sysConfig.DEFAULT_BASE_DIR);
        dto.setLogBaseDir(sysConfig.SYS_LOG_BASE_DIR);
        dto.setTotalStoredSize(totalStorageSize);
        dto.setFileCount(clientManager.countFiles());
        return Result.ok(dto);
    }

    @RequestMapping("/tree")
    public Result<FileTreeDTO> queryFileTree(UploadDTO dto) {
        String filepath = StringUtils.isNotBlank(dto.getPath()) ? dto.getPath() : "/";
        FileNode fileNode = trackerFileService.listFiles(filepath);
        FileTreeDTO tree = FileTreeDTO.tree(fileNode, filepath,0);
        if (tree == null) {
            tree = new FileTreeDTO();
            tree.setPath("/");
        }
        return Result.ok(tree);
    }

}
