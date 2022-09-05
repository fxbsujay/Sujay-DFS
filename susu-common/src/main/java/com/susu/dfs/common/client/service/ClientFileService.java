package com.susu.dfs.common.client.service;

import com.susu.dfs.common.file.transfer.OnProgressListener;
import java.io.File;
import java.util.Map;

/**
 * <p>Description: DFS 客户端的文件API服务</p>
 *
 * @author sujay
 * @version 10:45 2022/7/14
 */
public interface ClientFileService{

    /**
     * <p>Description: Create directory</p>
     * <p>Description: 创建目录</p>
     *
     * @param path          Path corresponding to directory
     * @throws Exception    file does not exist
     */
    void mkdir(String path) throws Exception;

    /**
     * <p>Description: Create directory</p>
     * <p>Description: 创建目录</p>
     *
     * @param path          Path corresponding to directory
     * @param attr          file attribute
     * @throws Exception    file does not exist
     */
    void mkdir(String path, Map<String, String> attr) throws Exception;

    /**
     * <p>Description: Upload file</p>
     * <p>Description: 上传文件</p>
     *
     * @param filename      Server file path
     * @param file          Local file
     * @throws Exception    file does not exist
     */
    void put(String filename, File file) throws Exception;

    /**
     * <p>Description: Upload file</p>
     * <p>Description: 上传文件</p>
     *
     * @param filename      Server file path
     * @param file          Local file
     * @param numOfReplica  Number of copies of documents
     * @throws Exception    file does not exist
     */
    void put(String filename, File file, int numOfReplica) throws Exception;


    /**
     * <p>Description: Upload file</p>
     * <p>Description: 上传文件</p>
     *
     * @param filename      Server file path
     * @param file          Local file
     * @param numOfReplica  Number of copies of documents
     * @param attr          file attribute
     * @throws Exception    file does not exist
     */
    void put(String filename, File file, int numOfReplica, Map<String, String> attr) throws Exception;

    /**
     * <p>Description: Upload file</p>
     * <p>Description: 上传文件</p>
     *
     * @param filename      Server file path
     * @param file          Local file
     * @param numOfReplica  Number of copies of documents
     * @param attr          file attribute
     * @param listener      Progress monitoring
     * @throws Exception    file does not exist
     */
    void put(String filename, File file, int numOfReplica, Map<String, String> attr, OnProgressListener listener) throws Exception;

    /**
     * <p>Description: 读取文件属性</p>
     * <p>Description: Read file properties</p>
     *
     * @param filename      文件名
     * @return              文件属性
     * @throws Exception    file does not exist
     */
    Map<String, String> readAttr(String filename) throws Exception;

    /**
     * <p>Description: Download files</p>
     * <p>Description: 下载文件</p>
     *
     * @param filename      Server file path
     * @param absolutePath  Local path
     * @throws Exception    file does not exist
     */
    void get(String filename, String absolutePath) throws Exception;

    /**
     * <p>Description: Download files</p>
     * <p>Description: 下载文件</p>
     *
     * @param filename      Server file path
     * @param absolutePath  Local path
     * @param listener      Progress monitoring
     * @throws Exception    file does not exist
     */
    void get(String filename, String absolutePath, OnProgressListener listener) throws Exception;

    /**
     * <p>Description: Delete file</p>
     * <p>Description: 删除文件</p>
     *
     * @param filename      Server file path
     * @throws Exception    file does not exist
     */
    void remove(String filename) throws Exception;
}
