package com.susu.dfs.storage.server;

import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.StorageInfo;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.storage.locator.FileLocator;
import com.susu.dfs.storage.locator.FileLocatorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Description: Storage 的 存储管理组件</p>
 *
 * @author sujay
 * @version 13:32 2022/7/15
 */
@Slf4j
public class StorageManager {

    private static final String STORAGE_INFO = "storage.info";

    private static final String STORAGE_TEMP = "storage.temp";

    private static final int HASH_SIZE = 256;

    /**
     * 存储根路径
     */
    private final String baseDir;

    /**
     * 文件寻址器
     */
    private final FileLocator fileLocator;

    /**
     * @param baseDir       存储根路径
     * @param locatorType   寻址器类型 {@link FileLocator}
     */
    public StorageManager(String baseDir,String locatorType) {
        this.baseDir = baseDir;
        this.fileLocator = FileLocatorFactory.getFileLocator(locatorType, baseDir, HASH_SIZE);
        init();
    }

    /**
     *  初始化
     */
    private void init() {
        File file = new File(baseDir);
        if (file.exists()) {
            return;
        }
        log.info("Storage Manager init start: [baseDir={}]", baseDir);
        for (int i = 0; i < HASH_SIZE; i++) {
            for (int j = 0; j < HASH_SIZE; j++) {
                String parent = StringUtils.format(i);
                String child = StringUtils.format(j);
                File tar = new File(file, parent + "/" + child);
                if (!tar.mkdirs()) {
                    throw new IllegalStateException("Storage Manager init error: " + tar.getAbsolutePath());
                }
            }
        }
        log.info("Storage Manager init success...");
    }

    /**
     * 获取存储信息
     */
    public StorageInfo getStorageInfo() {
        StorageInfo storageInfo = new StorageInfo();
        File fileDir = new File(baseDir);
        storageInfo.setFreeSpace(fileDir.getFreeSpace());
        log.info("Storage File storage path：[file={}]", fileDir.getAbsolutePath());
        List<FileInfo> fileInfos = scanFile(fileDir);
        long storageSize = 0L;
        for (FileInfo fileInfo : fileInfos) {
            storageSize += fileInfo.getFileSize();
        }
        storageInfo.setStorageSize(storageSize);
        storageInfo.setFiles(fileInfos);
        return storageInfo;
    }

    /**
     * <p>Description: Scan file</p>
     *
     * @param dir   目录
     */
    private List<FileInfo> scanFile(File dir) {
        List<FileInfo> files = new LinkedList<>();
        try {
            for (int i = 0; i < HASH_SIZE; i++) {
                for (int j = 0; j < HASH_SIZE; j++) {
                    String parent = String.format("%03d", i);
                    String child = String.format("%03d", j);
                    File storageInfoFile = new File(dir, parent + File.separator + child + File.separator + STORAGE_INFO);
                    if (!storageInfoFile.exists()) {
                        continue;
                    }
                    List<FileInfo> currentFolderFile = new ArrayList<>();
                    int fileCount = 0;
                    try (FileInputStream fis = new FileInputStream(storageInfoFile); FileChannel channel = fis.getChannel()) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate((int) storageInfoFile.length());
                        channel.read(byteBuffer);
                        byteBuffer.flip();
                        while (byteBuffer.hasRemaining()) {
                            try {
                                int filenameBytesLength = byteBuffer.getInt();
                                long fileSize = byteBuffer.getLong();
                                byte[] fileNameBytes = new byte[filenameBytesLength];
                                byteBuffer.get(fileNameBytes);
                                String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);
                                fileCount++;
                                if (isExists(fileName)) {
                                    FileInfo fileInfo = new FileInfo();
                                    fileInfo.setFileName(fileName);
                                    fileInfo.setFileSize(fileSize);
                                    files.add(fileInfo);
                                    currentFolderFile.add(fileInfo);
                                }
                            } catch (Exception e) {
                                log.error("Parse storageInfo failed.", e);
                                System.exit(0);
                            }
                        }
                    }
                    /*
                     * 这里是处理一种这样的情况：
                     *
                     * 假设在DataNode保存了2个文件：
                     *     - file1
                     *     - file2
                     *
                     * 由于副本过多，NameNode下发命令删除了file1, 这个时候storageInfo文件保存的信息还是2个文件.
                     *
                     * 没有比较好的方式删除掉storageInfo的file1记录，所以在DataNode每次重启的时候，
                     * 校验一下，如果file1在磁盘中是不存在的，则从storageInfo中删除
                     * (这里的删除方式是新建一个storageInfo文件只保存了file2,然后替换掉原)
                     *
                     */
                    if (fileCount == currentFolderFile.size()) {
                        continue;
                    }
                    for (FileInfo fileInfo : currentFolderFile) {
                        recordReplicaReceive(fileInfo.getFileName(), getAbsolutePathByFileName(fileInfo.getFileName()), fileInfo.getFileSize(), STORAGE_TEMP);
                    }
                    File storageTempFile = new File(dir, parent + File.separator + child + File.separator + STORAGE_TEMP);
                    storageTempFile.createNewFile();
                    FileUtils.del(storageInfoFile);
                    boolean b = storageTempFile.renameTo(storageInfoFile);
                    if (b) {
                        log.info("删除旧的storage文件，用新的storage文件替换：[oldFileSize={}, newFileSize={}, file={}]",
                                fileCount, currentFolderFile.size(), storageInfoFile.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    /**
     * <p>Description: 文件是否存在</p>
     * <p>Description: Whether the file exists</p>
     *
     * @param fileName  文件名
     */
    private boolean isExists(String fileName) {
        String absolutePathByFileName = getAbsolutePathByFileName(fileName);
        File file = new File(absolutePathByFileName);
        return file.exists();
    }

    /**
     * <p>Description: Get the local absolute path according to the file name</p>
     * <p>Description: 根据文件名获取本地绝对路径</p>
     *
     * @param fileName 文件名
     * @return 本地绝对路径
     */
    public String getAbsolutePathByFileName(String fileName) {
        return fileLocator.locate(fileName);
    }

    /**
     * <p>Description: 记录收到一个文件</p>
     *
     * @param filename      文件名
     * @param absolutePath  绝对路径
     * @param fileSize      文件大小
     */
    public void recordReplicaReceive(String filename, String absolutePath, long fileSize) throws IOException {
        recordReplicaReceive(filename, absolutePath, fileSize, STORAGE_INFO);
    }

    /**
     * <p>Description: 记录收到一个文件</p>
     *
     * @param filename      文件名
     * @param absolutePath  绝对路径
     * @param fileSize      文件大小
     * @param file          需要记录的文件名称
     */
    public void recordReplicaReceive(String filename, String absolutePath, long fileSize, String file) throws IOException {
        synchronized (this) {
            File f = new File(absolutePath);
            String parent = f.getParent();
            File recordFile = new File(parent, file);
            try (FileOutputStream fos = new FileOutputStream(recordFile, true);
                 FileChannel channel = fos.getChannel()) {
                byte[] bytes = filename.getBytes();
                ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 12);
                byteBuffer.putInt(bytes.length);
                byteBuffer.putLong(fileSize);
                byteBuffer.put(bytes);
                byteBuffer.flip();
                channel.write(byteBuffer);
                channel.force(true);
            }
        }
    }

}
