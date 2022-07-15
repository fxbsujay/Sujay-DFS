package com.susu.dfs.storage.server;

import com.susu.dfs.common.utils.StringUtils;
import com.susu.dfs.storage.locator.FileLocator;
import com.susu.dfs.storage.locator.FileLocatorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

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

}
