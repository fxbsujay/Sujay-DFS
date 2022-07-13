package com.susu.dfs.common.file;

import com.susu.common.model.Metadata;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.file.image.ImageLogWrapper;
import com.susu.dfs.common.utils.StopWatch;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * <p>Description: FILE SERVER</p>
 * <p>Description: 文件服务</p>
 *
 * @author sujay
 * @version 15:17 2022/7/11
 */
@Slf4j
public abstract class AbstractFileService implements FileService{

    /**
     * 负责管理内存文件目录树的组件
     */
    protected FileDirectory directory;

    public AbstractFileService() {
        this.directory = new FileDirectory();
    }

    /**
     * <p>Description: 基于本地文件恢复元数据空间/p>
     *
     * @throws Exception IO异常
     */
    protected abstract void recoveryNamespace() throws Exception;

    @Override
    public void mkdir(String path, Map<String, String> attr) {
        this.directory.mkdir(path, attr);
    }

    @Override
    public boolean createFile(String filename, Map<String, String> attr) {
        return this.directory.createFile(filename, attr);
    }

    @Override
    public boolean deleteFile(String filename) {
        FileNode node = this.directory.delete(filename);
        return node != null;
    }

    /**
     * 加载镜像
     */
    protected void readImage(ImageLogWrapper image) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("Staring apply FsImage file ...");
        directory.readImage(image);
        stopWatch.stop();
        log.info("Apply FsImage File cost {} ms", stopWatch.getTime());
    }

    /**
     * 保存镜像
     */
    protected void writImage() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("Staring apply FsImage file ...");
        directory.writImage();
        stopWatch.stop();
        log.info("Apply FsImage File cost {} ms", stopWatch.getTime());
    }

    @Override
    public Set<Metadata> getFilesBySlot(int slot) {
        return directory.findAllFileBySlot(slot);
    }

    /**
     * 获取文件列表
     *
     * @param filename 文件路径
     * @return 文件列表
     */
    public FileNode listFiles(String filename) {
        return this.directory.listFiles(filename);
    }

    public List<String> findAllFiles(String path) {
        return this.directory.findAllFiles(path);
    }

    public FileNode unsafeListFiles(String filename) {
        return this.directory.unsafeListFiles(filename);
    }



    /**
     * <p>Description: 扫描最新的ImageLog文件/p>
     *
     * @return 最新并合法的ImageLog
     */
    protected ImageLogWrapper scanLatestValidImageLog(String baseDir) throws IOException {
        Map<Long, String> timeFsImageMap = scanImageLogMap(baseDir);
        List<Long> sortedList = new ArrayList<>(timeFsImageMap.keySet());
        sortedList.sort((o1, o2) -> o1.equals(o2) ? 0 : (int) (o2 - o1));
        for (Long time : sortedList) {
            String path = timeFsImageMap.get(time);
            try (RandomAccessFile raf = new RandomAccessFile(path, "r"); FileInputStream fis = new FileInputStream(raf.getFD()); FileChannel channel = fis.getChannel()) {
                ImageLogWrapper fsImage = ImageLogWrapper.parse(channel, path, (int) raf.length());
                if (fsImage != null) {
                    return fsImage;
                }
            }
        }
        return null;
    }

    /**
     * <p>Description: 扫描本地文件，把所有ImageLog文件扫描出来/p>
     *
     * @param path 文件路径
     * @return ImageLog Map
     */
    public Map<Long, String> scanImageLogMap(String path) {
        Map<Long, String> timeFsImageMap = new HashMap<>(8);
        File dir = new File(path);
        if (!dir.exists()) {
            return timeFsImageMap;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return timeFsImageMap;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            if (!file.getName().contains(Constants.IMAGE_LOG_NAME)) {
                continue;
            }
            String str = file.getName().split("_")[2];
            long time = Long.parseLong(str);
            timeFsImageMap.put(time, file.getAbsolutePath());
        }
        return timeFsImageMap;
    }


    /**
     * <p>Description: 结算当前路径下的文件数量/p>
     *
     * @param path 文件路径
     * @return 文件数量
     */
    public FileNumberInfo countFileNumber(String path) {
        FileNumberInfo info = new FileNumberInfo();
        info.setFileCount(0);
        info.setTotalSize(0);
        FileNode node = unsafeListFiles(path);
        if (node == null) {
            return info;
        } else {
            internalCalculate(node, info);
        }
        return info;
    }


    /**
     * <p>Description: 结算当前节点下的文件数量/p>
     *
     * @param node 目录节点
     * @param info 当前计数
     */
    private void internalCalculate(FileNode node, FileNumberInfo info) {
        if (node.isFile()) {
            info.addFileCount();
            String fileSizeStr = node.getAttr().getOrDefault(Constants.ATTR_FILE_SIZE, "0");
            long fileSize = Long.parseLong(fileSizeStr);
            info.addTotalSize(fileSize);
        } else {
            for (String key : node.getChildren().keySet()) {
                FileNode children = node.getChildren().get(key);
                internalCalculate(children, info);
            }
        }
    }



}
