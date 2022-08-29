package com.susu.dfs.tracker.tomcat.controller;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.Result;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.tomcat.annotation.Autowired;
import com.susu.dfs.tracker.tomcat.annotation.RequestMapping;
import com.susu.dfs.tracker.tomcat.annotation.RestController;
import com.susu.dfs.tracker.tomcat.dto.StorageDTO;
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

    public Result<List<StorageDTO>> selectList() {
        List<ClientInfo> clientList = clientManager.getClientList();
        List<StorageDTO> result = new ArrayList<>();

        if (clientList.isEmpty()) {
            return Result.ok(result);
        }

        for (ClientInfo clientInfo : clientList) {
            StorageDTO dto = new StorageDTO();
            dto.setStatus(ClientInfo.STATUS_READY == clientInfo.getStatus());
            dto.setHost(clientInfo.getHostname());
            dto.setStoredSize(clientInfo.getStoredSize());
            dto.setFilePath(Constants.DEFAULT_BASE_DIR);
            result.add(dto);
        }

        return Result.ok(result);
    }

}
