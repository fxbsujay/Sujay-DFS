package com.susu.dfs.storage.locator;

import org.apache.commons.codec.digest.DigestUtils;

/**
 *  Sha-1 算法查找文件存储位置
 */
public class Sha1FileLocator extends AbstractFileLocator{

    public Sha1FileLocator(String basePath, int hashSize) {
        super(basePath, hashSize);
    }

    @Override
    protected String encodeFileName(String filename) {
        return DigestUtils.sha1Hex(filename.getBytes());
    }
}
