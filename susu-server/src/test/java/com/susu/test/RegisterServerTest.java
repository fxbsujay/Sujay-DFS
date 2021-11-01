package com.susu.test;


import com.susu.dfs.server.HeartbeatRequest;
import com.susu.dfs.server.RegisterRequest;
import com.susu.dfs.server.RegisterServerController;

import java.util.UUID;

/**
 * register-server组件的测试类
 * @author zhonghuashishan
 *
 */
public class RegisterServerTest {

	public static void main(String[] args) throws InterruptedException {

		RegisterServerController controller = new RegisterServerController();
		String serviceInstanceId = UUID.randomUUID().toString().replace("-","");

		// 模拟一次请求
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setHostname("inventory-service-01");
		registerRequest.setIp("127.0.0.1");
		registerRequest.setPort(9000);
		registerRequest.setServiceInstanceId(serviceInstanceId);
		registerRequest.setServiceName("inventory-service");

		controller.register(registerRequest);

		// 模拟一次心跳 完成续约
		HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
		heartbeatRequest.setServiceName("inventory-service");
		heartbeatRequest.setServiceInstanceId(serviceInstanceId);

		controller.heartbeat(heartbeatRequest);

		// 开启一个线程， 检查服务状态



	}

	
}
