package com.susu.dfs.storage.locator;

import com.susu.dfs.common.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 基于AES加密算法的 文件路径定位器
 */
@Slf4j
public class AesFileLocator extends AbstractFileLocator {

    private String key;

    public AesFileLocator(String basePath, int hashSize) {
        super(basePath, hashSize);
    }

    @Override
    protected String encodeFileName(String filename) {

        try {
            return aesEncrypt(getKey(), filename);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getKey() {
        if (key == null) {
            key = NetUtils.getHostName(NetUtils.LINUX);
        }
        return key;
    }

    public static String aesEncrypt(String key, String content) throws Exception {
        Cipher cipher = getCipher(key);
        byte[] byteEncode = content.getBytes(StandardCharsets.UTF_8);
        byte[] byteAes = cipher.doFinal(byteEncode);
        return Base64.getUrlEncoder().encodeToString(byteAes);
    }

    private static Cipher getCipher(String seed) throws Exception {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(seed.getBytes());
        keygen.init(128, secureRandom);
        SecretKey originalKey = keygen.generateKey();
        byte[] raw = originalKey.getEncoded();
        SecretKey key = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

}
