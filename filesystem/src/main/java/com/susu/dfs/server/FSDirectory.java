package com.susu.dfs.server;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责管理内存中的文件目录树的核心组件
 * @author syjay
 *
 */
public class FSDirectory {

	/**
	 * 内存中的文件目录树
	 */
	private INodeDirectory dirTree;

	public FSDirectory() {
		this.dirTree = new INodeDirectory("/");
	}

	/**
	 * 创建目录
	 * @param path 目录路径
	 */
	public void mkdir(String path) {
		// path = /home/dfs/hive
		// 先判断下，“/”目录下有没有 home 这个目录存在
		// 有 再判断 dfs 目录是否存在 以此类推
		// 没有则创建目录
		// 最后对 hive 这目录创建以恶搞节点挂载上去
		synchronized (dirTree) {
			String[] paths = path.split("/");

			INodeDirectory parent = null;

			for (String splitPath : paths) {
				if (splitPath.trim().equals("")) {
					continue;
				}
				INodeDirectory dir = findDirectory(dirTree, splitPath);

				if (dir != null) {
					parent = dir;
					continue;
				}
				INodeDirectory child = new INodeDirectory(splitPath);
				parent.addChild(child);
			}
		}

	}

	/**
	 * 递归寻找目录树
	 * @param dir 目录
	 * @param path 路径
	 */
	private INodeDirectory findDirectory(INodeDirectory dir, String path) {

		if (dir.getChildren().size() == 0) {
			return null;
		}

		INodeDirectory resultDir = null;
		for (INode child : dir.getChildren()) {
			if (child instanceof INodeDirectory) {
				INodeDirectory childDir = (INodeDirectory) child;
				if (childDir.getPath().equals(path)) {
					return childDir;
				}
				resultDir =  findDirectory( childDir,path);

				if (resultDir != null) {
					return resultDir;
				}
			}
		}
		return null;
	}

	/**
	 *  代表的是文件目录树的一个节点
	 */
	private interface INode {

	}

	/**
	 * 代表文件目录树的一个目录
	 */
	public static class INodeDirectory implements INode {

		private String path;

		private List<INode> children;

		public INodeDirectory(String path) {
			this.path = path;
			this.children = new ArrayList<INode>();
		}

		public void addChild(INode inode) {
			this.children.add(inode);
		}


		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public List<INode> getChildren() {
			return children;
		}

		public void setChildren(List<INode> children) {
			this.children = children;
		}
	}

	/**
	 * 文件目录树中的一个文件
	 */
	public static class INodeFile implements INode {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
}
