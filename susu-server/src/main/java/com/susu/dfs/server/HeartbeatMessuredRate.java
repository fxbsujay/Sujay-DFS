package com.susu.dfs.server;

/**
 * 心跳测量计数器
 * @author Sujay
 *
 */
public class HeartbeatMessuredRate {

	/**
	 * 单例实例
	 */
	private static HeartbeatMessuredRate instance = 
			new HeartbeatMessuredRate();
	
	/**
	 * 最近一分钟的心跳次数
	 */
	private long latestMinuteHeartbeatRate = 0L;
	/**
	 * 最近一分钟的时间戳
	 */
	private long latestMinuteTimestamp = System.currentTimeMillis();
	
	/**
	 * 获取单例实例
	 * @return
	 */
	public static HeartbeatMessuredRate getInstance() {
		return instance;
	}
	
	/**
	 * 增加一次最近一分钟的心跳次数
	 */
	public synchronized void increment() {
		long currentTime = System.currentTimeMillis();
		
		if(currentTime - latestMinuteTimestamp > 60 * 1000) {
			latestMinuteHeartbeatRate = 0L;
			this.latestMinuteTimestamp = System.currentTimeMillis();
		}
		
		latestMinuteHeartbeatRate++;
	}
	
	/**
	 * 获取最近一分钟的心跳次数
	 */
	public synchronized long get() {
		return latestMinuteHeartbeatRate;
	}
	
}
