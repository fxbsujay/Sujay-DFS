package com.susu.dfs.common.file;

import com.susu.common.model.Metadata;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

   /* *//**
     * 扫描最新的FSImage文件
     *
     * @return 最新并合法的FSImage
     *//*
    protected FsImage scanLatestValidFsImage(String baseDir) throws IOException {
        Map<Long, String> timeFsImageMap = scanFsImageMap(baseDir);
        List<Long> sortedList = new ArrayList<>(timeFsImageMap.keySet());
        sortedList.sort((o1, o2) -> o1.equals(o2) ? 0 : (int) (o2 - o1));
        for (Long time : sortedList) {
            String path = timeFsImageMap.get(time);
            try (RandomAccessFile raf = new RandomAccessFile(path, "r");
                 FileInputStream fis = new FileInputStream(raf.getFD()); FileChannel channel = fis.getChannel()) {
                FsImage fsImage = FsImage.parse(channel, path, (int) raf.length());
                if (fsImage != null) {
                    return fsImage;
                }
            }
        }
        return null;
    }*/



}
