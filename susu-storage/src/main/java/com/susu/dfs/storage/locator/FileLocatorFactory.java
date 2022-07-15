package com.susu.dfs.storage.locator;

/**
 * <p>Description: 文件定位器工厂</p>
 *
 * @author sujay
 * @version 13:35 2022/7/15
 */
public class FileLocatorFactory {

    private static final String SIMPLE = "simple";

    private static final String MD5 = "md5";

    private static final String SHA1 = "sha1";

    private static final String AES = "aes";

    /**
     * <p>Description: 获取寻址器</p>
     *
     * @param type          寻址器类型
     * @param basePath      存储路径
     * @return              寻址器
     */
    public static FileLocator getFileLocator(String type, String basePath, int hashSize) {
        if (SIMPLE.equals(type)) {
            return new SimpleFileLocator(basePath, hashSize);
        } else if (MD5.equals(type)) {
            return new Md5FileLocator(basePath, hashSize);
        } else if (SHA1.equals(type)) {
            return new Sha1FileLocator(basePath, hashSize);
        } else if (AES.equals(type)) {
            return new AesFileLocator(basePath, hashSize);
        }
        return new SimpleFileLocator(basePath, hashSize);
    }
}
