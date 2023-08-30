package com.susu.dfs.storage.locator;

/**
 * <p>Description: Storage 的 文件定位器</p>
 *
 * @author sujay
 * @version 13:35 2022/7/15
 */
public interface FileLocator {

    /**
     * <p>Description: 根据文件名定位绝对路径</p>
     * <p>Description: Locate the absolute path according to the file name</p>
     *
     * @param filename  文件名
     * @return          绝对路径
     */
    String locate(String filename);
}
