package com.susu.dfs.tracker.client;

import com.susu.common.model.RegisterRequest;
import com.susu.dfs.common.utils.DateUtils;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sujay
 * <p>Description: 客户端管理器</p>
 * @version 0:04 2022/7/8
 */
@Slf4j
public class ClientManager {

    /**
     * 客户端实例
     *      Example:    {
     *                      key:    hostname
     *                      value:  clientInfo
     *                  }
     */
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();


    /**
     * <p>Description: 客户端注册</p>
     * <p>Description: Client Register</p>
     * @param request 注册请求
     * @return 是否注册成功 【 true / false 】
     */
    public boolean register(RegisterRequest request) {
        if (StringUtils.isBlank(request.getHostname())) {
            return false;
        }
        ClientInfo client = new ClientInfo(request.getHostname(),request.getPort());
        client.setClientId(request.getClientId());
        log.info("Client register request : [hostname:{}]",request.getHostname());
        clients.put(client.getHostname(),client);
        return true;
    }


    /**
     * <p>Description: 客户端心跳</p>
     * <p>Description: Client Heartbeat</p>
     * @param hostname 客户端主机地址
     * @return 是否更新成功 【 true / false 】
     */
    public Boolean heartbeat(String hostname) {
        ClientInfo dataNode = clients.get(hostname);
        if (dataNode == null) {
            return false;
        }
        long latestHeartbeatTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Heartbeat received from client：[hostname={}, latestHeartbeatTime={}]", hostname, DateUtils.getTime(new Date(latestHeartbeatTime)));
        }
        dataNode.setLatestHeartbeatTime(latestHeartbeatTime);
        return true;
    }

}
