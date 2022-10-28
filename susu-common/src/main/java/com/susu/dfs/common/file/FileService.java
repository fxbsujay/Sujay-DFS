package com.susu.dfs.common.file;


import com.susu.dfs.common.model.Metadata;

import java.util.Map;
import java.util.Set;

/**
 * <p>Description: FILE SERVER</p>
 * <p>Description: 文件服务</p>
 *
 * @author sujay
 * @version 15:17 2022/7/11
 */
public interface FileService {

    /**
     * <p>Description: 创建文件夹</p>
     * <p>Description: create folder</p>
     *
     * @param path 文件路径
     * @param attr 文件属性
     */
    void mkdir(String path, Map<String, String> attr);

    /**
     * <p>Description: 创建文件</p>
     * <p>Description: create a file</p>
     *
     * @param filename 文件名称
     * @param attr 文件属性
     * @return 是否创建成功
     */
    boolean createFile(String filename, Map<String, String> attr);


    /**
     * <p>Description: 删除文件</p>
     * <p>Description: Delete file</p>
     *
     * @param filename 文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String filename);

    /**
     * <p>Description: 根据Slot获取文件名</p>
     *
     * @param slot slot
     * @return 文件名
     */
    Set<Metadata> getFilesBySlot(int slot);
}
