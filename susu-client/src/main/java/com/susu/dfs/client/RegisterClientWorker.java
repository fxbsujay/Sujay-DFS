package com.susu.dfs.client;

/**
 * 负责向register-server发起注册申请的线程
 * @author Sujay
 *
 */
public class RegisterClientWorker extends Thread {

	public static final String SERVICE_NAME = "inventory-service";
	public static final String IP = "192.168.31.207";
	public static final String HOSTNAME = "inventory01";
	public static final int PORT = 9000;
	
	/**
	 * http通信组件
	 */
	private HttpSender httpSender;
	/**
	 * 是否完成服务注册
	 */
	private Boolean finishedRegister;
	/**
	 * 服务实例id
	 */
	private String serviceInstanceId;
	
	public RegisterClientWorker(String serviceInstanceId) {
		this.httpSender = new HttpSender();
		this.finishedRegister = false;
		this.serviceInstanceId = serviceInstanceId;
	}
	
	@Override
	public void run() {
		if(!finishedRegister) {
			// 应该是获取当前机器的信息
			// 包括当前机器的ip地址、hostname，以及你配置这个服务监听的端口号
			// 从配置文件里可以拿到
			RegisterRequest registerRequest = new RegisterRequest();
			registerRequest.setServiceName(SERVICE_NAME);
			registerRequest.setIp(IP); 
			registerRequest.setHostname(HOSTNAME);
			registerRequest.setPort(PORT);  
			registerRequest.setServiceInstanceId(serviceInstanceId);    
			
			RegisterResponse registerResponse = httpSender.register(registerRequest);
			
			System.out.println("服务注册的结果是：" + registerResponse.getStatus() + "......");   
			
			// 如果说注册成功的话
			if(RegisterResponse.SUCCESS.equals(registerResponse.getStatus())) {
				this.finishedRegister = true;
			}
		}
		
		// 如果说注册成功了，就进入while true死循环
		if(finishedRegister) {
			HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
			heartbeatRequest.setServiceInstanceId(serviceInstanceId);
			HeartbeatResponse heartbeatResponse = null;

			while(true) {
				try {
					heartbeatResponse = httpSender.heartbeat(heartbeatRequest);
					System.out.println("心跳的结果为：" + heartbeatResponse.getStatus() + "......");
					Thread.sleep(30 * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
 	}
	
}
