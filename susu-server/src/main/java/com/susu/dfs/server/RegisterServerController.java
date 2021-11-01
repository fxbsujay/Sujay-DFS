package com.susu.dfs.server;

import java.util.Map;

/**
 * 这个controller是负责接收register-client发送过来的请求的
 * 在Spring Cloud Eureka中用的组件是jersey，百度一下jersey是什么东西
 * 在国外很常用jersey，restful框架，可以接受http请求
 * 
 * @author Sujay
 *
 */
public class RegisterServerController {

	private ServiceRegistry registry = ServiceRegistry.getInstance();
	
	/**
	 * 服务注册
	 * @param registerRequest 注册请求
	 * @return 注册响应
	 */
	public RegisterResponse register(RegisterRequest registerRequest) {
		RegisterResponse registerResponse = new RegisterResponse();
		
		try {
			ServiceInstance serviceInstance = new ServiceInstance();
			serviceInstance.setHostname(registerRequest.getHostname()); 
			serviceInstance.setIp(registerRequest.getIp()); 
			serviceInstance.setPort(registerRequest.getPort()); 
			serviceInstance.setServiceInstanceId(registerRequest.getServiceInstanceId()); 
			serviceInstance.setServiceName(registerRequest.getServiceName());  
			
			registry.register(serviceInstance);

			// 更新自我保护机制
			synchronized (SelfProtectionPolicy.class) {
				SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
				selfProtectionPolicy.setExpectedHeartbeatRate(
						selfProtectionPolicy.getExpectedHeartbeatRate() + 2
				);
				selfProtectionPolicy.setExpectedHeartbeatThreshold(
						(long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85)
				);
			}

			registerResponse.setStatus(RegisterResponse.SUCCESS);

		} catch (Exception e) {
			e.printStackTrace(); 
			registerResponse.setStatus(RegisterResponse.FAILURE);  
		}
		
		return registerResponse;
	}
	
	/**
	 * 发送心跳
	 * @param heartbeatRequest 心跳请求
	 * @return 心跳响应
	 */
	public HeartbeatResponse heartbeat(HeartbeatRequest heartbeatRequest) { 
		HeartbeatResponse heartbeatResponse = new HeartbeatResponse();
		
		try {
			// 对服务实例进行续约
			ServiceInstance serviceInstance = registry.getServiceInstance(
					heartbeatRequest.getServiceName(), 
					heartbeatRequest.getServiceInstanceId());
			serviceInstance.renew();
			
			// 记录一下每分钟的心跳的次数
			HeartbeatMeasuredRate heartbeatMessuredRate = HeartbeatMeasuredRate.getInstance();
			heartbeatMessuredRate.increment();
			
			heartbeatResponse.setStatus(HeartbeatResponse.SUCCESS); 
		} catch (Exception e) {
			e.printStackTrace(); 
			heartbeatResponse.setStatus(HeartbeatResponse.FAILURE); 
		}
		
		return heartbeatResponse;
	}
	
	/**
	 * 拉取服务注册表
	 * @return
	 */
	public Map<String, Map<String, ServiceInstance>> fetchServiceRegistry() {
		return registry.getRegistry();
	}


	/**
	 * 服务下线
	 * @param serviceName
	 * @param serviceInstanceId
	 */
	public void cancel(String serviceName,String serviceInstanceId) {
		registry.remove(serviceName,serviceInstanceId);

		// 更新自我保护机制
		synchronized (SelfProtectionPolicy.class) {
			SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
			selfProtectionPolicy.setExpectedHeartbeatRate(
					selfProtectionPolicy.getExpectedHeartbeatRate() - 2
			);
			selfProtectionPolicy.setExpectedHeartbeatThreshold(
					(long) (selfProtectionPolicy.getExpectedHeartbeatThreshold() * 0.85)
			);
		}
	}
}
