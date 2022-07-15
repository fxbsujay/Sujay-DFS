package com.susu.dfs.common.file;

import com.susu.common.model.ImageLog;
import com.susu.common.model.Metadata;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.eum.FileNodeType;
import com.susu.dfs.common.file.image.ImageLogWrapper;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Description: The core component responsible for managing the directory tree of files in memory</p>
 * <p>Description: 负责管理内存中文件目录树</p>
 *
 * @author sujay
 * @version 15:28 2022/7/11
 */
@Slf4j
public class FileDirectory {

    private FileNode root;

    /**
     * ImageLog文件的存储路径
     */
    private final String baseDir;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public FileDirectory(String baseDir) {
        this.baseDir = baseDir;
        this.root = new FileNode("/", FileNodeType.DIRECTORY.getValue());
    }

    /**
     * <p>Description: 创建文件目录/p>
     *
     * @param path 文件目录路径
     */
    public void mkdir(String path, Map<String, String> attr) {
        try {
            lock.writeLock().lock();
            String[] paths = path.split("/");
            FileNode current = root;
            for (String p : paths) {
                if ("".equals(p)) {
                    continue;
                }
                current = findDirectory(current, p);
            }
            current.putAllAttr(attr);
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * <p>Description: 创建文件/p>
     *
     * @param filename 文件名
     * @return 是否创建成功
     */
    public boolean createFile(String filename, Map<String, String> attr) {
        try {
            lock.writeLock().lock();
            String[] paths =  filename.split("/");
            String fileNode = paths[paths.length - 1];
            FileNode fileParentNode = getFileParent(paths);
            FileNode childrenNode = fileParentNode.getChildren(fileNode);
            if (childrenNode != null) {
                log.warn("File already exists, creation failed !! : {}", filename);
                return false;
            }
            FileNode child = new FileNode(fileNode, FileNodeType.FILE.getValue());
            child.putAllAttr(attr);
            fileParentNode.addChildren(child);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * <p>Description: 获取父节点/p>
     *
     * @param paths 路径
     * @return 父节点
     */
    private FileNode getFileParent(String[] paths) {
        FileNode current = root;
        for (int i = 0; i < paths.length - 1; i++) {
            String p = paths[i];
            if ("".equals(p)) {
                continue;
            }
            current = findDirectory(current, p);
        }
        return current;
    }

    /**
     * <p>Description: 删除文件/p>
     * <p>Description: delete file/p>
     *
     * @param filename 文件名
     */
    public FileNode delete(String filename) {
        lock.writeLock().lock();
        try {
            String[] paths = filename.split("/");
            String name = paths[paths.length - 1];
            FileNode current = getFileParent(paths);
            FileNode childrenNode;
            if ("".equals(name)) {
                childrenNode = current;
            } else {
                childrenNode = current.getChildren(name);
            }
            if (childrenNode == null) {
                log.warn("File does not exist, deletion failed!! ：[filename={}]", filename);
                return null;
            }
            if (childrenNode.getType() == FileNodeType.DIRECTORY.getValue()) {
                if (!childrenNode.getChildren().isEmpty()) {
                    log.warn("There are sub files in the folder, deletion failed!! ：[filename={}]", filename);
                    return null;
                }
            }
            FileNode remove = current.getChildren().remove(name);

            FileNode parent = remove.getParent();
            FileNode child = remove;
            while (parent != null) {
                if (child.getChildren().isEmpty()) {
                    child.setParent(null);
                    parent.getChildren().remove(child.getPath());
                }
                child = parent;
                parent = parent.getParent();
            }
            return FileNode.deepCopy(remove, Integer.MAX_VALUE);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * <p>Description: 查查询当前节点下查询是否有这个目录Path/p>
     * <p>Description: query directory/p>
     *
     * @param current 当前节点
     * @param path 节点路径
     */
    private FileNode findDirectory(FileNode current, String path) {
        FileNode childrenNode = current.getChildren(path);
        if (childrenNode == null) {
            childrenNode = new FileNode(path, FileNodeType.DIRECTORY.getValue());
            current.addChildren(childrenNode);
        }
        current = childrenNode;
        return current;
    }

    /**
     * 根据内存目录树生成Image
     *
     * @return FsImage
     */
    public ImageLogWrapper getImage() {
        try {
            lock.readLock().lock();
            ImageLog image = FileNode.toImage(root);
            return new ImageLogWrapper(0L, image);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * <p>Description: 保存ImageLog文件/p>
     */
    public void writImage() throws Exception {
        String fileName = baseDir + File.separator + Constants.IMAGE_LOG_NAME + System.currentTimeMillis();
        ImageLogWrapper image = getImage();
        image.writeFile(fileName);
    }


    /**
     * 根据Image初始化内存目录树
     *
     * @param image 镜像文件
     */
    public void readImage(ImageLogWrapper image) {
        try {
            lock.writeLock().lock();
            this.root = FileNode.parseImage(image.getImageLog(), "");
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * <p>Description: 查看某个目录文件/p>
     *
     * @param parent 目录路径
     * @param level 层级
     * @return 文件路径
     */
    public FileNode listFiles(String parent, int level) {
        return FileNode.deepCopy(unsafeListFiles(parent), level);
    }


    /**
     * <p>Description: 查看某个目录文件/p>
     *
     * @param parent 目录路径
     * @return 文件路径
     */
    public FileNode listFiles(String parent) {
        return listFiles(parent, Integer.MAX_VALUE);
    }

    /**
     * <p>Description: 查看某个目录文件/p>
     *
     * @param parent 目录路径
     * @return 文件路径
     */
    public FileNode unsafeListFiles(String parent) {
        if (root.getPath().equals(parent)) {
            return root;
        }
        lock.readLock().lock();
        try {
            String[] paths = parent.split("/");
            String name = paths[paths.length - 1];
            FileNode current = getFileParent(paths);
            return current.getChildren(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * <pre>
     *  Example:
     *
     *      /aaa/bbb/c1.png
     *      /aaa/bbb/c2.png
     *      /bbb/ccc/c3.png
     *
     *  param:  /aaa
     *  return: [/bbb/c1.png, /bbb/c2.png]
     * </pre>
     *
     * @return 返回文件名
     */
    public List<String> findAllFiles(String path) {
        FileNode node = listFiles(path);
        if (node == null) {
            return new ArrayList<>();
        }
        return findAllFiles(node);
    }

    /**
     * <p>Description: 该节点下所有的文件名称/p>
     * <p>Description: All file names under this node/p>
     *
     * @param node   文件节点
     * @return       [/bbb/c1.png, /bbb/c2.png]
     */
    private List<String> findAllFiles(FileNode node) {
        List<String> ret = new ArrayList<>();
        if (node.isFile()) {
            ret.add(node.getFullPath());
        } else {
            for (String key : node.getChildren().keySet()) {
                FileNode child = node.getChildren().get(key);
                ret.addAll(findAllFiles(child));
            }
        }
        return ret;
    }

    public Set<Metadata> findAllFileBySlot(int slot) {
        return findAllFilesFilterBySlot(root, slot);
    }

    public Set<Metadata> findAllFilesFilterBySlot(FileNode node, int slot) {
        Set<Metadata> ret = new HashSet<>();
        if (node.isFile()) {
            String fullPath = node.getFullPath();
            int slotIndex = StringUtils.hash(fullPath, Constants.SLOTS_COUNT);
            if (slotIndex == slot) {
                ret.add(Metadata.newBuilder()
                        .setFileName(fullPath)
                        .setType(FileNodeType.FILE.getValue())
                        .putAllAttr(node.getAttr())
                        .build());
            }
        } else {
            for (String key : node.getChildren().keySet()) {
                FileNode child = node.getChildren().get(key);
                ret.addAll(findAllFilesFilterBySlot(child, slot));
            }
        }
        return ret;
    }
}
