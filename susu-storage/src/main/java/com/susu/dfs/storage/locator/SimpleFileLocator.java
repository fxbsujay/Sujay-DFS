package com.susu.dfs.storage.locator;

import java.io.File;

/**
 * 简单路径定位器：将文件名的 "/" 改为 "-"
 */
public class SimpleFileLocator extends AbstractFileLocator {

    public SimpleFileLocator(String basePath, int hashSize) {
        super(basePath, hashSize);
    }

    @Override
    protected String encodeFileName(String filename) {
        if (filename.startsWith(File.separator)) {
            filename = filename.substring(1);
        }
        return filename.replaceAll("/", "-");
    }
}
