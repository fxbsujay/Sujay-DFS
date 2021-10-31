package com.susu.dfs.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 负责发送各种http请求的组件
 * @author Sujay
 *
 */
public class HttpSender {

	/**
	 * 发送注册请求
	 * @param request
	 * @return
	 */
	public RegisterResponse register(RegisterRequest request) {
		// 实际上会基于类似HttpClient这种开源的网络包
		// 你可以去构造一个请求，里面放入这个服务实例的信息，比如服务名称，ip地址，端口号
		// 然后通过这个请求发送过去
		System.out.println("服务实例【" + request + "】，发送请求进行注册......");  
		
		// 收到register-server响应之后，封装一个Response对象
		RegisterResponse response = new RegisterResponse();
		response.setStatus(RegisterResponse.SUCCESS); 
		
		return response;
	}
	
	/**
	 * 发送心跳请求
	 * @param request
	 * @return
	 */
	public HeartbeatResponse heartbeat(HeartbeatRequest request) {
		System.out.println("服务实例【" + request + "】，发送请求进行心跳......");
		HeartbeatResponse response = new HeartbeatResponse();
		response.setStatus(RegisterResponse.SUCCESS);

		return response;
	}

	/**
	 * 拉取注册表
	 * @return
	 */
	public Map<String, Map<String, ServiceInstance>> fetchServiceRegistry() {
		Map<String, Map<String, ServiceInstance>> registry = new HashMap<>();

		ServiceInstance serviceInstance = new ServiceInstance();
		serviceInstance.setHostname("finance-service-0");
		serviceInstance.setIp("127.0.0.1");
		serviceInstance.setPort(9000);
		serviceInstance.setServiceInstanceId("FINANCE-SERVICE-127.0.0.1:9000");
		serviceInstance.setServiceName("FINANCE-SERVICE");

		Map<String, ServiceInstance> serviceInstances = new HashMap<>();
		serviceInstances.put("FINANCE-SERVICE-127.0.0.1:9000",serviceInstance);

		registry.put("FINANCE-SERVICE-127.0.0.1",serviceInstances);
		System.out.println("拉取注册表:" + registry);

		return registry;
	}
	
}
