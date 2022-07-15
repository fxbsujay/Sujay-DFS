package com.susu.dfs.storage.locator;

import com.susu.dfs.common.utils.NetUtils;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * <p>Description: Storage 的 文件定位器抽象类</p>
 *
 * @author sujay
 * @version 13:35 2022/7/15
 */
@Slf4j
public abstract class AbstractFileLocator implements FileLocator{

    private int hashSize;

    /**
     * 文件存储的根路径
     */
    private String basePath;

    public AbstractFileLocator(String basePath, int hashSize) {
        this.basePath = basePath;
        this.hashSize = hashSize;
        this.encodeFileName(NetUtils.getHostName(NetUtils.LINUX));
    }


    @Override
    public String locate(String filename) {
        String afterTransferPath = encodeFileName(filename);
        int hash = StringUtils.hash(afterTransferPath, hashSize * hashSize);
        int parent = hash / hashSize;
        int child = hash % hashSize;
        String parentPath = StringUtils.format(parent);
        String childPath = StringUtils.format(child);
        return basePath + File.separator + parentPath + File.separator + childPath + File.separator + afterTransferPath;
    }


    /**
     * <p>Description: 对文件名进行转码</p>
     * <p>Description: Transcode file names</p>
     *
     * @param filename  文件名
     * @return          返回文件名
     */
    protected abstract String encodeFileName(String filename);
}
