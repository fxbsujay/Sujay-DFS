package com.susu.dfs.client;

/**
 * 心跳请求
 * @author Amazons
 *
 */
public class HeartbeatRequest {

	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}
	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}
	
}
