package com.susu.dfs.tracker.tomcat.dto;

import com.susu.dfs.common.Constants;
import com.susu.dfs.common.file.FileNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * <p>Description: FILE Tree</p>
 * <p>Description: 文件目录树</p>
 *
 * @author sujay
 * @version 10:19 2022/8/30
 */
@Slf4j
@Data
public class FileTreeDTO {

    private int index;

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
     */
    private List<FileTreeDTO> children;

    /**
     * 参数
     */
    private Map<String ,String> attr;

    private Long fileSize;

    public static FileTreeDTO tree(FileNode node,String path,int index) {
        if (node == null) {
            return null;
        }

        if (!path.contains(Constants.TRASH_DIR) && node.getPath().equals(Constants.TRASH_DIR)) {
            return null;
        }

        index++;

        FileTreeDTO dto = new FileTreeDTO();
        dto.setAttr(node.getAttr());
        dto.setType(node.getType());
        dto.setPath(node.getPath());
        dto.setIndex(index);
        long fileSize = Long.parseLong(node.getAttr().getOrDefault(Constants.ATTR_FILE_SIZE, "0"));
        dto.setFileSize(fileSize);

        List<FileTreeDTO> children = new LinkedList<>();

        for (String key : node.getChildren().keySet()) {
            FileNode child = node.getChildren().get(key);
            FileTreeDTO childDto = tree(child,path,index);
            if (childDto == null) {
                continue;
            }
            children.add(childDto);
        }

        dto.setChildren(children);
        return dto;
    }


}
