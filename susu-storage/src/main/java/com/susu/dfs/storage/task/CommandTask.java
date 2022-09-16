package com.susu.dfs.storage.task;

import com.susu.dfs.common.model.NetPacketCommand;
import com.susu.dfs.common.eum.CommandType;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.storage.client.TrackerClient;
import com.susu.dfs.storage.server.StorageManager;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: Storage 的 接收Tracker 发来的命令任务</p>
 *
 * @author sujay
 * @version 13:21 2022/7/28
 */
@Slf4j
public class CommandTask {

    /**
     * 命令列表
     */
    private ConcurrentLinkedQueue<NetPacketCommand> commandsQueue = new ConcurrentLinkedQueue<>();

    private TrackerClient trackerClient;

    private StorageManager storageManager;

    public CommandTask(TaskScheduler taskScheduler, TrackerClient trackerClient, StorageManager storageManager) {
        this.trackerClient = trackerClient;
        this.storageManager = storageManager;
        taskScheduler.schedule("Command Task", new CommandWorker(),1000,1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 添加任务
     */
    public void addCommand(List<NetPacketCommand> commands) {
        commandsQueue.addAll(commands);
    }

    private class CommandWorker implements Runnable {

        @Override
        public void run() {
            try {
                NetPacketCommand command = commandsQueue.poll();
                if (command == null) {
                    return;
                }
                CommandType commandType = CommandType.getEnum(command.getCommand());
                switch (commandType) {
                    case FILE_REMOVE:
                        fileRemoveTaskHandel(command.getFilename());
                        break;
                    case FILE_COPY:
                        fileCopyTaskHandel(command.getFilename());
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                log.info("Command Task fail !!!");
            }
        }
    }

    /**
     * <p>Description: 删除本地文件并上报 Tracker </p>
     *
     * @param filename  文件名
     */
    private void fileRemoveTaskHandel(String filename) throws InterruptedException {
        log.info("Received a command to delete a file: filename={}",filename);
        String absolutePathByFileName = storageManager.getAbsolutePathByFileName(filename);
        File file = new File(absolutePathByFileName);
        long length = 0;
        if (file.exists()) {
            length = file.length();
        }
        FileUtils.del(file);
        if (length > 0) {
            trackerClient.clientRemoveCompletionRequest(filename,length);
        }
    }

    /**
     * <p>Description: 从别处复制副本 </p>
     *
     * @param filename  文件名
     */
    private void fileCopyTaskHandel(String filename) {
        log.info("收到一个复制文件的命令");
    }
}
