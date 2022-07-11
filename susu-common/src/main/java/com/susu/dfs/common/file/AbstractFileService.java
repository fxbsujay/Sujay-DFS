package com.susu.dfs.common.file;

import com.susu.common.model.Metadata;
import lombok.extern.slf4j.Slf4j;

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


}
