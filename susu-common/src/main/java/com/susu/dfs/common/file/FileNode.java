package com.susu.dfs.common.file;

import com.susu.common.model.ImageLog;
import com.susu.dfs.common.eum.FileNodeType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

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

    public TreeMap<String, FileNode> getChildren() {
        return children;
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
     * <p>Description: 获取当前节点的全名路径/p>
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

    /**
     * <p>Description: 将文件目录树转换成目录树内存镜像记录/p>
     *
     * @param node  文件节点
     * @return 目录树内存镜像
     */
    public static ImageLog toImage(FileNode node) {
        ImageLog.Builder builder = ImageLog.newBuilder();
        String path = node.getPath();
        int type = node.getType();
        builder.setPath(path);
        builder.setType(type);
        builder.putAllAttr(node.getAttr());
        Collection<FileNode> children = node.getChildren().values();
        if (children.isEmpty()) {
            return builder.build();
        }
        List<ImageLog> tmpNode = new ArrayList<>(children.size());
        for (FileNode child : children) {
            ImageLog iNode = toImage(child);
            tmpNode.add(iNode);
        }
        builder.addAllChildren(tmpNode);
        return builder.build();
    }

    /**
     * <p>Description: 目录树内存镜像记录转换成文件目录树/p>
     *
     * @param imageLog  目录树镜像
     * @return 文件目录树
     */
    public static FileNode parseImage(ImageLog imageLog) {
        return parseImage(imageLog, null);
    }

    /**
     * <p>Description: 目录树内存镜像记录转换成文件目录树/p>
     *
     * @param imageLog  目录树镜像
     * @return 文件目录树
     */
    public static FileNode parseImage(ImageLog imageLog, String parent) {
        FileNode node = new FileNode();
        if (parent != null && log.isDebugEnabled()) {
            log.debug("parseINode executing :[path={},  type={}]", parent, node.getType());
        }
        String path = imageLog.getPath();
        int type = imageLog.getType();
        node.setPath(path);
        node.setType(type);
        node.putAllAttr(imageLog.getAttrMap());
        List<ImageLog> children = imageLog.getChildrenList();
        if (children.isEmpty()) {
            return node;
        }
        for (ImageLog child : children) {
            node.addChildren(parseImage(child, parent == null ? null : parent + "/" + child.getPath()));
        }
        return node;
    }

    @Override
    public String toString() {
        return "FileNode{" +
                "path='" + path + '\'' +
                ", type=" + type +
                ", children=" + children +
                ", attr=" + attr +
                '}';
    }

}
