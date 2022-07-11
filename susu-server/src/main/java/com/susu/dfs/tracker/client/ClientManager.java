package com.susu.dfs.tracker.client;

import com.susu.common.model.RegisterRequest;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.DateUtils;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private final Map<Long, ClientInfo> clients = new ConcurrentHashMap<>();


    public ClientManager(TaskScheduler taskScheduler) {
        taskScheduler.schedule("Client-Check", new DataNodeAliveMonitor(),
                Constants.HEARTBEAT_CHECK_INTERVAL, Constants.HEARTBEAT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }
    /**
     * <p>Description: 客户端注册</p>
     * <p>Description: Client Register</p>
     * @param request 注册请求
     * @return 是否注册成功 【 true / false 】
     */
    public boolean register(RegisterRequest request,Long clientId) {
        if (StringUtils.isBlank(request.getHostname())) {
            return false;
        }
        ClientInfo client = new ClientInfo(request.getHostname(),request.getPort());
        client.setName(request.getName());
        client.setClientId(clientId);
        log.info("Client register request : [hostname:{}]",request.getHostname());
        clients.put(clientId,client);
        return true;
    }

    /**
     * <p>Description: 客户端心跳</p>
     * <p>Description: Client Heartbeat</p>
     * @param clientId 客户端Id
     * @return 是否更新成功 【 true / false 】
     */
    public Boolean heartbeat(Long clientId) {
        ClientInfo dataNode = clients.get(clientId);
        if (dataNode == null) {
            return false;
        }
        long latestHeartbeatTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Heartbeat received from client：[clientId={}, latestHeartbeatTime={}]", clientId, DateUtils.getTime(new Date(latestHeartbeatTime)));
        }
        dataNode.setLatestHeartbeatTime(latestHeartbeatTime);
        return true;
    }

    /**
     * client 是否存活的监控线程
     */
    private class DataNodeAliveMonitor implements Runnable {
        @Override
        public void run() {
            Iterator<ClientInfo> iterator = clients.values().iterator();
            while (iterator.hasNext()) {
                ClientInfo next = iterator.next();
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis < next.getLatestHeartbeatTime() + Constants.HEARTBEAT_OUT_TIME) {
                    continue;
                }
                log.info("Client out time，remove client：[hostname={}, current={}, latestHeartbeatTime={}]",
                        next, DateUtils.getTime(new Date(currentTimeMillis)),DateUtils.getTime(new Date(next.getLatestHeartbeatTime())));
                iterator.remove();
            }
        }
    }
}
