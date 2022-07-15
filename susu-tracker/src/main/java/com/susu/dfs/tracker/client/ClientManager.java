package com.susu.dfs.tracker.client;

import com.susu.common.model.RegisterRequest;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.file.FileInfo;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.DateUtils;
import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.tracker.rebalance.RemoveReplicaTask;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

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

    /**
     * 每个文件对应存储的Datanode信息
     *      Example:    {
     *                      key:    filename
     *                      value:  clientInfo
     *                  }
     */
    private final Map<String, List<ClientInfo>> fileOfClients = new ConcurrentHashMap<>();

    /**
     * 每个DataNode 存储的文件列表
     *      Example:    {
     *                      key:    clientId
     *                      value:  FileInfo
     *                  }
     */
    private final Map<Long, Map<String, FileInfo>> clientOfFiles = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock replicaLock = new ReentrantReadWriteLock();


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
     *
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


    /**
     * <p>Description: 获取所有已经上报节点信息的节点</p>
     */
    private List<ClientInfo> selectAllClients() {
        return clients.values().stream()
                .filter(clientInfo -> clientInfo.getStatus() == ClientInfo.STATUS_INIT)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * <p>Description: 判断存储节点是否包含该文件</p>
     */
    private boolean isClientContainsFile(Long clientId, String filename) {
        replicaLock.readLock().lock();
        try {
            Map<String, FileInfo> files = clientOfFiles.getOrDefault(clientId, new HashMap<>(Constants.MAP_SIZE));
            return files.containsKey(filename);
        } finally {
            replicaLock.readLock().unlock();
        }
    }

    /**
     * <p>Description: 随机获取用于存储该文件的节点，排除已经包含该文件的节点</p>
     */
    public List<ClientInfo> selectAllClientsByFile(int size, String filename) {
        return selectAllClientsByFile(selectAllClients(),size,filename);
    }


    /**
     * <p>Description: 随机获取用于存储该文件的节点，排除已经包含该文件的节点</p>
     *
     * <h3>选择策略</h2>
     * <pre>
     *
     *  上传小文件的时候，文件大小较小，为了避免流量都打到同一台Storage机器
     *  所以如果多台Storage之间的存储空间大小误差在1G范围内，直接随机取一台
     *  如果误差范围小于1G,则按存储空间大小从低到高进行获取
     *
     *  <ul>
     *      <li>
     *          当误差在1G以内的Storage节点数量刚好和需要的节点数量一样则直接返回
     *      </li>
     *      <li>
     *          误差在1G以内的Storage节点数量小于需要的节点数量，则需要从Storage列表中继续取到足够的节点
     *      </li>
     *      <li>
     *          误差在1G以内的Storage数量很多，超过所需的节点数量，则随机取几个
     *      </li>
     *  </ul>
     *
     * </pre>
     *
     * @param clientInfos   节点数组
     * @param size          节点数量
     * @param filename      文件名
     * @return              存储节点
     */
    private  List<ClientInfo> selectAllClientsByFile(List<ClientInfo> clientInfos, int size, String filename) {

        long minStoredSize = -1;

        long maxStorageSize = 1024 * 1024 * 1024;

        List<ClientInfo> clientList = new ArrayList<>(10);

        for (ClientInfo client: clientInfos) {
            if (isClientContainsFile(client.getClientId(),filename)) {
                continue;
            }

            if (client.getStoredSize() < 0) {
                minStoredSize = client.getStoredSize();
            }
            if (client.getStoredSize() - minStoredSize <= maxStorageSize) {
                clientList.add(client);
            }
        }

        if (clientList.size() == size) {
            return clientList;
        } else if (clientList.size() < size){
            int raminStoreSize = size - clientList.size();
            for (ClientInfo client: clientInfos) {
                if (clientList.contains(client) || isClientContainsFile(client.getClientId(),filename)) {
                    continue;
                }
                clientList.add(client);
                raminStoreSize--;
                if (raminStoreSize < 0) {
                    return clientList;
                }
            }
        } else {
            Random random = new Random();
            List<ClientInfo> selectedClients = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int index = random.nextInt(clientList.size());
                ClientInfo client = clientList.get(index);
                if (selectedClients.contains(client) || isClientContainsFile(client.getClientId(),filename)) {
                    continue;
                }
                selectedClients.add(client);
            }
            return selectedClients;
        }

        log.error("Storage数量不足：【StorageList={}】",clientInfos);
        throw new RuntimeException("Storage数量不足：filename=" + filename);
    }

    /**
     * <p>Description: 删除对应文件存储信息</p>
     *
     * @param filename 文件名称
     * @param delReplica 是否删除副本
     * @return 被删除的文件信息
     */
    public FileInfo removeFileStorage(String filename, boolean delReplica) {
        replicaLock.writeLock().lock();
        try {
            List<ClientInfo> clientInfoList = fileOfClients.remove(filename);
            if (clientInfoList == null) {
                return null;
            }
            FileInfo ret = null;
            for (Map<String, FileInfo> dataNodeFileInfo : clientOfFiles.values()) {
                FileInfo fileInfo = dataNodeFileInfo.remove(filename);
                if (fileInfo != null) {
                    ret = fileInfo;
                }
            }
            if (delReplica) {
                for (ClientInfo client : clientInfoList) {
                    ClientInfo dataNode = clients.get(client.getClientId());
                    RemoveReplicaTask task = new RemoveReplicaTask(dataNode.getClientId(), filename);
                    log.info("下发副本删除任务：[clientId={}, filename={}]", dataNode.getClientId(), filename);
                    dataNode.addRemoveReplicaTask(task);
                }
            }
            return ret;
        }finally {
            replicaLock.writeLock().unlock();
        }
    }



}
