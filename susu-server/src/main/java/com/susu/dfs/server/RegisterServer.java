package com.susu.dfs.server;

import java.util.UUID;

/**
 * 代表了服务注册中心的这么一个东西
 * 
 * @author Sujay
 *
 */
public class RegisterServer {
	
	public static void main(String[] args) throws Exception {
//		System.out.println(Thread.currentThread().getName() + "线程的线程组是：" 
//				+ Thread.currentThread().getThreadGroup());  
//		System.out.println(Thread.currentThread().getName() + "线程的优先级是" 
//				+ Thread.currentThread().getPriority()); 
		RegisterServerController controller = new RegisterServerController();
		
		String serviceInstanceId = UUID.randomUUID().toString().replace("-", "");
		
		// 模拟发起一个服务注册的请求
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setHostname("inventory-service-01");  
		registerRequest.setIp("192.168.31.208");  
		registerRequest.setPort(9000); 
		registerRequest.setServiceInstanceId(serviceInstanceId);    
		registerRequest.setServiceName("inventory-service");  

		controller.register(registerRequest);
		
		// 模拟进行一次心跳，完成续约
		HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
		heartbeatRequest.setServiceName("inventory-service");  
		heartbeatRequest.setServiceInstanceId(serviceInstanceId); 
		
		controller.heartbeat(heartbeatRequest);
		
		// 开启一个后台线程，检测微服务的存活状态
		ServiceAliveMonitor serviceAliveMonitor = new ServiceAliveMonitor();
		serviceAliveMonitor.start();
		
		// 一般来说像这种register-server这种东西，当然不会只有一个main线程作为工作线程
		// 他一般来说是一个web工程，部署在一个web服务器里面
		// 他最核心的工作线程，就是专门用于接收和处理register-client发送过来的请求的那些工作线程
		// 正常来说，他只要有工作线程，是不会随便退出的
		// 当然如果说工作线程都停止了，那么daemon线程就会跟着jvm进程一块儿退出
		
		// 我们暂时这个项目里是没有网络这块东西的
		// 暂时就是在main方法里加一块while true，让main线程，这个工作线程，不要退出
		// 就可以保证这个服务一直运行，daemon线程也会正常的工作
		
		// 如果说register-server中的核心工作线程，就是用于接收和处理请求的工作线程
		// 他们都结束了，停止工作了，销毁了
		// 此时register-server里就只剩下一个daemon线程了，难道还要阻止jvm进程退出吗？
		// 此时当然是应该跟着jvm进程一块儿退出了
		
		while(true) {
			Thread.sleep(30 * 1000);  
		}
	}

}
