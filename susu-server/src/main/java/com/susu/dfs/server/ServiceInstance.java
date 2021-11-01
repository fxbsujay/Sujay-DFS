package com.susu.dfs.server;

/**
 * 代表了一个服务实例
 * 里面包含了一个服务实例的所有信息
 * 比如说服务名称、ip地址、hostname、端口号、服务实例id
 * 还有就是契约信息（Lease）
 * 
 * @author Sujay
 *
 */
public class ServiceInstance {
	
	/**
	 * 判断一个服务实例不再存活的周期
	 */
	private static final Long NOT_ALIVE_PERIOD = 90 * 1000L;

	/**
	 * 服务名称
	 */
	private String serviceName;
	/**
	 * ip地址
	 */
	private String ip;
	/**
	 * 主机名
	 */
	private String hostname;
	/**
	 * 端口号
	 */
	private int port;
	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;
	/**
	 * 契约
	 */
	private Lease lease;
	
	public ServiceInstance() {
		this.lease = new Lease();
	}
	
	/**
	 * 服务续约
	 */
	public void renew() {
		this.lease.renew();
	}
	
	/**
	 * 服务实例是否存活
	 * @return
	 */
	public Boolean isAlive() {
		return this.lease.isAlive();
	}
	
	/**
	 * 契约
	 * 维护了一个服务实例跟当前的这个注册中心之间的联系
	 * 包括了心跳的时间，创建的时间，等等
	 * 
	 * @author zhonghuashishan
	 *
	 */
	private class Lease {

		/**
		 * 最近一次心跳的时间
		 */
		private volatile Long latestHeartbeatTime = System.currentTimeMillis();
		
		/**
		 * 续约，你只要发送一次心跳，就相当于把register-client和register-server之间维护的一个契约
		 * 进行了续约，我还存活着，我们俩的契约可以维持着
		 * @param
		 */
		public void renew() {
			this.latestHeartbeatTime = System.currentTimeMillis(); 
			System.out.println("服务实例【" + serviceInstanceId + "】，进行续约：" + latestHeartbeatTime);  
		}
		
		/**
		 * 判断当前服务实例的契约是否还存活
		 * @return
		 */
		public Boolean isAlive() {
			Long currentTime = System.currentTimeMillis();
			if(currentTime - latestHeartbeatTime > NOT_ALIVE_PERIOD) {
				System.out.println("服务实例【" + serviceInstanceId + "】，不再存活");
				return false;
			}
			System.out.println("服务实例【" + serviceInstanceId + "】，保持存活");
			return true;
		}
		
	}
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}
	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}
	public Lease getLease() {
		return lease;
	}
	public void setLease(Lease lease) {
		this.lease = lease;
	}
	
	@Override
	public String toString() {
		return "ServiceInstance [serviceName=" + serviceName + ", ip=" + ip + ", hostname=" + hostname + ", port="
				+ port + ", serviceInstanceId=" + serviceInstanceId + ", lease=" + lease + "]";
	}
	
}
