package com.susu.dfs.common.file;

import com.susu.dfs.common.eum.FileNodeType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>Description: FILE NODE</p>
 * <p>Description: 文件节点</p>
 *
 * @author sujay
 * @version 15:28 2022/7/11
 */
@Slf4j
@Data
public class FileNode {

    /**
     *节点路径
     */
    private String path;

    /**
     * 类型
     */
    private int type;

    /**
     * 目录树
     * Example : {
     *              key: NodePath,
     *              value: FileNode
     *           }
     */
    private final TreeMap<String, FileNode> children;

    /**
     * 参数
     */
    private Map<String ,String> attr;

    /**
     * 上一节点
     */
    private FileNode parent;

    public FileNode() {
        this.children = new TreeMap<>();
        this.attr = new HashMap<>();
        this.parent = null;
    }

    public FileNode(String path, int type) {
        this();
        this.path = path;
        this.type = type;
    }

    /**
     * <p>Description: Is it a file/p>
     *
     * @return 是否是一个文件
     */
    public boolean isFile() {
        return type == FileNodeType.FILE.getValue();
    }

    /**
     * <p>Description: 添加参数/p>
     * <p>Description: Add parameters/p>
     */
    public void putAllAttr(Map<String, String> attr) {
        this.attr.putAll(attr);
    }

    /**
     * <p>Description: 添加子节点/p>
     *
     * @param child 子节点
     */
    public void addChildren(FileNode child) {
        synchronized (children) {
            child.setParent(this);
            this.children.put(child.getPath(), child);
        }
    }

    /**
     * <p>Description: 获取子节点/p>
     *
     * @param childPath 子节点路径
     */
    public FileNode getChildren(String childPath) {
        synchronized (children) {
            return children.get(childPath);
        }
    }

    /**
     * <p>Description: 拷贝节点/p>
     *
     * @param node  节点
     * @param level 拷贝多少个孩子层级
     * @return 拷贝节点
     */
    public static FileNode deepCopy(FileNode node, int level) {
        if (node == null) {
            return null;
        }
        FileNode ret = new FileNode();
        String path = node.getPath();
        int type = node.getType();
        ret.setPath(path);
        ret.setType(type);
        ret.putAllAttr(node.getAttr());
        if (level > 0) {
            TreeMap<String, FileNode> children = node.children;
            if (!children.isEmpty()) {
                for (String key : children.keySet()) {
                    ret.addChildren(deepCopy(children.get(key), level - 1));
                }
            }
        }
        return ret;
    }

    /**
     * 获取当前节点的全名路径
     *
     * @return 当前节点的全路径
     */
    public String getFullPath() {
        return getFullPathInternal(this);
    }

    private String getFullPathInternal(FileNode parent) {
        if (parent == null) {
            return null;
        }
        String parentPath = getFullPathInternal(parent.getParent());
        if (parentPath == null) {
            return "";
        }
        return parentPath + "/" + parent.path;
    }

}
