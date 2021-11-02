package com.susu.dfs.server;

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
	}


	/**
	 *  代表的是文件目录树的一个节点
	 */
	private interface INode {

	}

	/**
	 * 代表文件目录树的一个目录
	 */
	private class INodeDirectory implements INode {

		private String path;

		private List<INode> children;

		public INodeDirectory(String path) {
			this.path = path;
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
	private class INodeFile implements INode {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
}
