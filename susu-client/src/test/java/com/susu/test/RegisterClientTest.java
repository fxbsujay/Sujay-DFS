package com.susu.test;

import com.susu.dfs.client.RegisterClient;

/**
 * register-client组件的测试类
 * @author zhonghuashishan
 *
 */
public class RegisterClientTest {

	public static void main(String[] args) throws InterruptedException {
		RegisterClient registerClient = new RegisterClient();
		registerClient.start();

		Thread.sleep(3500);

		registerClient.shutdown();
	}
	
}
