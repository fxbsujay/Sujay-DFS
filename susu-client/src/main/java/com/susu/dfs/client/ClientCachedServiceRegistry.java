package com.susu.dfs.client;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author sujay
 * @Description 描述 服务注册中心的客户端缓存注册表
 * @Date 20:57 2021/10/31
 */
public class ClientCachedServiceRegistry {

    /**
     * 服务注册表间隔
     *
     */
    private static final Long SERVICE_REGISTRY_FETCH_INTERVAL = 30 *1000L;
    /**
     *  客户端缓存注册表
     */
    private Map<String, Map<String, ServiceInstance>> registry = new HashMap<>();

    /**
     * 负责定时拉取注册表到本地进行缓存
     */
    private Daemon daemon;

    /**
     *  RegisterClient
     */
    private RegisterClient registerClient;

    /**
     *  http通信组件
     */
    private HttpSender httpSender;

    public ClientCachedServiceRegistry(RegisterClient registerClient,HttpSender httpSender) {
        this.daemon = new Daemon();
        this.registerClient = registerClient;
        this.httpSender = httpSender;
    }

    /**
     *  启动线程
     */
    public void initialize(){
        this.daemon.start();
    }

    /**
     *  销毁组件
     */
    public void destroy() {
        this.daemon.interrupt();
    }

    /**
     *  负责定时拉取注册表到本地缓存
     */
    private class Daemon extends Thread {

        @Override
        public void run() {
            while (registerClient.isRunning()) {

                try {
                    registry = httpSender.fetchServiceRegistry();
                    Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取服务注册表
     * @return
     */
    public Map<String, Map<String, ServiceInstance>> getRegistry() {
        return registry;
    }
}
