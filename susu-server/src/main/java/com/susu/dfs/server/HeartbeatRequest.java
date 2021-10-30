package com.susu.dfs.server;

/**
 * 心跳请求
 * @author Sujay
 *
 */
public class HeartbeatRequest {

	/**
	 * 服务名称
	 */
	private String serviceName;
	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;

	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}
	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}
	
	@Override
	public String toString() {
		return "HeartbeatRequest [serviceName=" + serviceName + ", serviceInstanceId=" + serviceInstanceId + "]";
	}
	
}
