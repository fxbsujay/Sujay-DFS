package com.susu.dfs.common.file;

import lombok.extern.slf4j.Slf4j;

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
    protected FileDirectory fileDirectory;

    public AbstractFileService() {
        this.fileDirectory = new FileDirectory();
    }

}
