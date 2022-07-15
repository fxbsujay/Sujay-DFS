package com.susu.dfs.storage.locator;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 基于MD5 HASH 算法定位文件
 */
public class Md5FileLocator extends AbstractFileLocator {

    public Md5FileLocator(String basePath, int hashSize) {
        super(basePath, hashSize);
    }

    @Override
    protected String encodeFileName(String filename) {
        return DigestUtils.md5Hex(filename.getBytes());
    }
}
