package com.susu.dfs.tracker.client;

import com.susu.common.model.RegisterRequest;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.file.FileNode;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.DateUtils;
import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.tracker.rebalance.RemoveReplicaTask;
import com.susu.dfs.tracker.rebalance.ReplicaTask;
import com.susu.dfs.tracker.service.TrackerFileService;
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
     * 客户端心跳超时时间
     */
    private final int HEARTBEAT_OUT_TIME;

    /**
     * 客户端实例
     *      Example:    {
     *                      key:    hostname
     *                      value:  clientInfo
     *                  }
     */
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();

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
     *                      key:    hostname
     *                      value:  FileInfo
     *                  }
     */
    private final Map<String, Map<String, FileInfo>> clientOfFiles = new ConcurrentHashMap<>();

    private TrackerFileService trackerFileService;

    private final ReentrantReadWriteLock replicaLock = new ReentrantReadWriteLock();


    public ClientManager(SysConfig config,TaskScheduler taskScheduler) {
        taskScheduler.schedule("Client-Check", new DataNodeAliveMonitor(),
                config.HEARTBEAT_CHECK_INTERVAL, config.HEARTBEAT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        this.HEARTBEAT_OUT_TIME = config.HEARTBEAT_OUT_TIME;
    }

    public void setTrackerFileService(TrackerFileService trackerFileService) {
        this.trackerFileService = trackerFileService;
    }

    public ClientInfo getClientByHost(String hostname) {
        return clients.get(hostname);
    }

    public List<ClientInfo> getClientList() {
        return new ArrayList<>(clients.values());
    }

    public FileInfo getFileStorage(String filename) {
        replicaLock.readLock().lock();
        try {
            for (Map<String, FileInfo> map : clientOfFiles.values()) {
                FileInfo fileInfo = map.get(filename);
                if (fileInfo != null) {
                    return fileInfo;
                }
            }
            return null;
        } finally {
            replicaLock.readLock().unlock();
        }
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
        clients.put(request.getHostname(),client);
        return true;
    }

    /**
     * <p>Description: 客户端上报了自己自身信息，修改状态为可读</p>
     * <p>Description: The client reported its own information, and the modification status is readable</p>
     *
     * @param hostname storage节点IP
     */
    public void setStorageReady(String hostname) {
        ClientInfo clientInfo = clients.get(hostname);
        if (clientInfo != null) {
            clientInfo.setStatus(ClientInfo.STATUS_READY);
        }
    }
    /**
     * <p>Description: 客户端心跳</p>
     * <p>Description: Client Heartbeat</p>
     *
     * @param hostname 客户端地址
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

    /**
     * client 是否存活的监控线程, 清理掉线的客户端并将该客户端中的文件复制一份
     */
    private class DataNodeAliveMonitor implements Runnable {
        @Override
        public void run() {
            Iterator<ClientInfo> iterator = clients.values().iterator();
            List<ClientInfo> toRemoveStorage = new ArrayList<>();
            while (iterator.hasNext()) {
                ClientInfo next = iterator.next();
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis < next.getLatestHeartbeatTime() + HEARTBEAT_OUT_TIME) {
                    continue;
                }
                log.info("Client out time，remove client：[hostname={}, current={}, latestHeartbeatTime={}]",
                        next, DateUtils.getTime(new Date(currentTimeMillis)),DateUtils.getTime(new Date(next.getLatestHeartbeatTime())));
                iterator.remove();
                toRemoveStorage.add(next);
            }
            for (ClientInfo client : toRemoveStorage) {
                createLostReplicaTask(client);

            }
        }
    }

    /**
     * 创建丢失副本任务,当Storage掉线，会导致一下文件不可读，则进行删除对应文件列表，并将文件尽量复制一份
     * 等待storage上线后再进行删除多余的副本
     */
    private void createLostReplicaTask(ClientInfo clientInfo) {
        Map<String, FileInfo> fileByClient = removeFileByStorage(clientInfo.getHostname());
        if (fileByClient == null) {
            return;
        }
        for (FileInfo fileInfo : fileByClient.values()) {
            ClientInfo  sourceStorage = chooseReadableClientByFileName(fileInfo.getFileName(), clientInfo);
            if (sourceStorage == null) {
                log.warn("警告：找不到适合的Storage用来获取文件：" + fileInfo.getFileName());
                continue;
            }
            ClientInfo client = allocateReplicateStorages(fileInfo, sourceStorage);
            if (client == null) {
                log.warn("警告：找不到适合的Storage用来Rebalance");
                continue;
            }
            ReplicaTask task = new ReplicaTask(fileInfo.getFileName(), sourceStorage.getHostname(), client.getPort());
            log.info("创建副本复制任务：[filename={}, from={}, to={}]", fileInfo.getFileName(),
                    sourceStorage.getHostname(), client.getHostname());
            client.addReplicaTask(task);
        }
    }

    /**
     * <p>Description: 删除Storage的文件列表</p>
     * <p>Description: Delete the file list of storage</p>
     *
     * @param hostname  storage节点地址ip
     * @return 该节点的文件列表
     */
    public Map<String, FileInfo> removeFileByStorage(String hostname) {
        replicaLock.writeLock().lock();
        try {
            return clientOfFiles.remove(hostname);
        } finally {
            replicaLock.writeLock().unlock();
        }
    }

    /**
     * <p>Description: 为复制任务申请副本，申请的Storage需要排除目标Storage的</p>
     * <p>Description: Apply for a copy for the replication task. The requested storage needs to exclude the target storage's</p>
     *
     * @param fileInfo          文件信息
     * @param excludeStorage    排除的Storage节点
     */
    private ClientInfo allocateReplicateStorages(FileInfo fileInfo, ClientInfo excludeStorage) {
        List<ClientInfo> dataNodeInfos = clients.values().stream()
                .filter(dataNodeInfo -> !dataNodeInfo.equals(excludeStorage) &&
                        dataNodeInfo.getStatus() == ClientInfo.STATUS_READY)
                .sorted()
                .collect(Collectors.toList());
        try {
            List<ClientInfo> clientInfoList = selectAllClientsByFile(dataNodeInfos,
                    1, fileInfo.getFileName());
            return clientInfoList.get(0);
        } catch (Exception e) {
            log.warn("allocateReplicateStorages select node failed.", e);
            return null;
        }
    }

    /**
     * <p>Description: 获取所有已经上报节点信息的节点</p>
     */
    private List<ClientInfo> selectAllClients() {
        return clients.values().stream()
                .filter(clientInfo -> clientInfo.getStatus() == ClientInfo.STATUS_READY)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * <p>Description: 判断存储节点是否包含该文件</p>
     */
    private boolean isClientContainsFile(String hostname, String filename) {
        replicaLock.readLock().lock();
        try {
            Map<String, FileInfo> files = clientOfFiles.getOrDefault(hostname, new HashMap<>(Constants.MAP_SIZE));
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
            if (isClientContainsFile(client.getHostname(),filename)) {
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
                if (clientList.contains(client) || isClientContainsFile(client.getHostname(),filename)) {
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
                if (selectedClients.contains(client) || isClientContainsFile(client.getHostname(),filename)) {
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
                    ClientInfo dataNode = clients.get(client.getHostname());
                    RemoveReplicaTask task = new RemoveReplicaTask(dataNode.getHostname(), filename);
                    log.info("下发副本删除任务：[hostname={}, filename={}]", dataNode.getHostname(), filename);
                    dataNode.addRemoveReplicaTask(task);
                }
            }
            return ret;
        }finally {
            replicaLock.writeLock().unlock();
        }
    }

    /**
     * <p>Description: 添加文件</p>
     * 1. 判断是否在垃圾箱中，在则发送一条删除副本指令
     * 2. 查看副本真实存在的数量是否大于副本的本身数量，如果大于则删除这个副本，并不在添加信息
     * 3. 不然则添加副本信息
     */
    public void addFile(FileInfo file) {
        replicaLock.writeLock().lock();
        try {
            ClientInfo clientInfo = clients.get(file.getHostname());
            List<ClientInfo> clientInfoList = fileOfClients.computeIfAbsent(file.getFileName(), k -> new ArrayList<>());
            FileNode fileNode = isInTrash(file.getFileName());
            if (fileNode == null) {
                log.warn("Receive file submission by Storage, But the file was not found,must delete file: [hostname={}, filename={}]", file.getHostname(), file.getFileName());
                RemoveReplicaTask task = new RemoveReplicaTask(file.getHostname(),file.getFileName());
                clientInfo.addRemoveReplicaTask(task);
                return;
            }
            int replicaNum = Integer.parseInt(fileNode.getAttr().getOrDefault(Constants.ATTR_REPLICA_NUM, "1"));
            if (clientInfoList.size() > replicaNum) {
                RemoveReplicaTask task = new RemoveReplicaTask(clientInfo.getHostname(), file.getFileName());
                log.info("Delete replica command：[hostname={}, filename={}]", clientInfo.getHostname(), file.getFileName());
                clientInfo.addRemoveReplicaTask(task);
                return;
            }
            clientInfoList.add(clientInfo);
            Map<String, FileInfo> files = clientOfFiles.computeIfAbsent(file.getHostname(), k -> new HashMap<>(Constants.MAP_SIZE));
            files.put(file.getFileName(), file);
            if (log.isDebugEnabled()) {
                log.debug("Receive file submission by Storage：[hostname={}, filename={}]", file.getHostname(), file.getFileName());
            }
        }finally {
            replicaLock.writeLock().unlock();
        }
    }

    /**
     * <p>Description: 文件是否在目录树种或者垃圾箱中</p>
     *
     * @param fileName  文件名
     * @return          文件节点
     */
    private FileNode isInTrash(String fileName) {
        FileNode node = trackerFileService.listFiles(fileName);
        if (node != null) {
            return node;
        }
        String[] split = fileName.split("/");
        String[] newSplit = new String[split.length + 1];
        newSplit[0] = split[0];
        newSplit[1] = split[1];
        newSplit[2] = Constants.TRASH_DIR;
        System.arraycopy(split, 2, newSplit, 3, split.length - 2);
        String trashPath = String.join("/", newSplit);
        return trackerFileService.listFiles(trashPath);
    }

    /**
     * <p>Description: 等待Storage上报Client上传Storage的文件</p>
     *
     * @param filename  文件名
     * @param timeout   超时等待时间
     */
    public void waitUploadFile(String filename, long timeout) throws InterruptedException {
        long remainTimeout = timeout;
        synchronized (this) {
            while (chooseReadableClientByFileName(filename) == null) {
                if (remainTimeout < 0) {
                    throw new RuntimeException("Timeout waiting for file upload confirmation ！！" + filename);
                }
                wait(10);
                remainTimeout -= 10;
            }
        }
    }

    /**
     * <p>Description: 根据文件名查询所在的Storage客户端，删除不可用的客户端</p>
     *
     * @param filename          文件名
     * @return                  随机获取一个可以读该文件的客户端
     */
    public ClientInfo chooseReadableClientByFileName(String filename) {
        return chooseReadableClientByFileName(filename, null);
    }

    /**
     * <p>Description: 根据文件名查询所在的Storage客户端，删除不可用的客户端</p>
     *
     * @param filename          文件名
     * @param toRemoveClient    客户端
     * @return                  随机获取一个可以读该文件的客户端
     */
    public ClientInfo chooseReadableClientByFileName(String filename, ClientInfo toRemoveClient) {
        replicaLock.readLock().lock();
        try {
            List<ClientInfo> clientInfoList = fileOfClients.get(filename);
            if (clientInfoList == null) {
                return null;
            }
            if (toRemoveClient != null) {
                clientInfoList.remove(toRemoveClient);
            }
            if (clientInfoList.isEmpty()) {
                return null;
            }
            int size = clientInfoList.size();
            Random random = new Random();
            int i = random.nextInt(size);
            return clientInfoList.get(i);
        } finally {
            replicaLock.readLock().unlock();
        }
    }
}
