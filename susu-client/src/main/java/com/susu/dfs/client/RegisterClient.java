package com.susu.dfs.client;

import java.util.UUID;

/**
 * 在服务上被创建和启动，负责跟register-server进行通信
 * @author Amazons
 *
 */
public class RegisterClient {

	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;
	
	public RegisterClient() {
		this.serviceInstanceId = UUID.randomUUID().toString().replace("-", "");
	}
	
	public void start() {
		// 一旦启动了这个组件之后，他就负责在服务上干两个事情
		// 第一个事情，就是开启一个线程向register-server去发送请求，注册这个服务
		// 第二个事情，就是在注册成功之后，就会开启另外一个线程去发送心跳
		
		// 我们来简化一下这个模型
		// 我们在register-client这块就开启一个线程
		// 这个线程刚启动的时候，第一个事情就是完成注册
		// 如果注册完成了之后，他就会进入一个while true死循环
		// 每隔30秒就发送一个请求去进行心跳
		
		new RegisterClientWorker(serviceInstanceId).start();
	}
	
}
