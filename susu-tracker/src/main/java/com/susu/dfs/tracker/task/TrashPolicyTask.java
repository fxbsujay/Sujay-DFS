package com.susu.dfs.tracker.task;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.FileNodeType;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.file.FileNode;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.service.TrackerFileService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 * @author sujay
 *
 * <p>Description: 垃圾清理</p>
 * @version 15:37 2022/7/27
 */
@Slf4j
public class TrashPolicyTask implements Runnable {

    /**
     * 客户端管理组件
     */
    private ClientManager clientManager;

    /**
     * 文件管理组件
     */
    private TrackerFileService fileService;

    public TrashPolicyTask(TrackerFileService fileService, ClientManager clientManager) {
        this.fileService = fileService;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.info("定时扫描垃圾箱线程启动.");
        }
        long currentTime = System.currentTimeMillis();
        FileNode node = fileService.listFiles("/");
        TreeMap<String, FileNode> children = node.getChildren();
        for (String user : children.keySet()) {
            FileNode userTrashNode = node.getChildren().get(user).getChildren().get(Constants.TRASH_DIR);
            if (userTrashNode != null) {
                List<String> toRemoveFilename = new LinkedList<>();
                scan(File.separator + user, userTrashNode, currentTime, toRemoveFilename);
                for (String filename : toRemoveFilename) {
                    String dataNodeFilename = filename.replaceAll(File.separator + Constants.TRASH_DIR, "");
                    FileInfo fileInfo = clientManager.removeFileStorage(dataNodeFilename, true);
                    if (fileInfo == null) {
                        log.error("找不到文件信息，等待下一次定时任务扫描再删除文件：[filename={}]", dataNodeFilename);
                        continue;
                    }
                    log.debug("删除内存目录树：[filename={}]", filename);
                    fileService.deleteFile(filename);
                }
            }
        }
    }

    private void scan(String path, FileNode node, long currentTime, List<String> toRemoveFilename) {
        String basePath = path + File.separator + node.getPath();
        if (node.getChildren().isEmpty()) {
            String deleteTime = node.getAttr().get(Constants.ATTR_FILE_DEL_TIME);
            if (deleteTime == null) {
                return;
            }
            long delTime = Long.parseLong(deleteTime);
            boolean isFile = FileNodeType.FILE.getValue() == node.getType();
            if (currentTime - Constants.TRASH_CLEAR_THRESHOLD > delTime && isFile) {
                toRemoveFilename.add(basePath);
            }
        } else {
            for (String key : node.getChildren().keySet()) {
                FileNode children = node.getChildren().get(key);
                scan(basePath, children, currentTime, toRemoveFilename);
            }
        }
    }
}
