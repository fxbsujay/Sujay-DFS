package com.susu.dfs.tracker.tomcat.controller;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.Result;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.server.TrackerChannelHandle;
import com.susu.dfs.tracker.tomcat.annotation.Autowired;
import com.susu.dfs.tracker.tomcat.annotation.PathVariable;
import com.susu.dfs.tracker.tomcat.annotation.RequestMapping;
import com.susu.dfs.tracker.tomcat.annotation.RestController;
import com.susu.dfs.tracker.tomcat.dto.StorageDTO;
import com.susu.dfs.tracker.tomcat.dto.UploadDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: 存储器 API</p>
 *
 * @author sujay
 * @version 17:12 2022/8/29
 */
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private TrackerChannelHandle trackerChannelHandle;

    @RequestMapping("/list")
    public Result<List<StorageDTO>> queryList() {
        List<ClientInfo> clientList = clientManager.getClientList();
        List<StorageDTO> result = new ArrayList<>();

        if (clientList.isEmpty()) {
            return Result.ok(result);
        }

        for (ClientInfo clientInfo : clientList) {
            StorageDTO dto = new StorageDTO();
            dto.setStatus(clientInfo.getStatus());
            dto.setHost(clientInfo.getHostname());
            dto.setPort(clientInfo.getPort());
            dto.setHttpPort(clientInfo.getHttpPort());
            dto.setStoredSize(clientInfo.getStoredSize());
            dto.setFilePath(Constants.DEFAULT_BASE_DIR);
            result.add(dto);
        }

        return Result.ok(result);
    }

    @RequestMapping(value = "/remove",method = "DELETE")
    public Result<Boolean> removeFile(UploadDTO dto) {
        trackerChannelHandle.removeFile(dto.getPath());
        return Result.ok(true);
    }

}
