package com.susu.dfs.common;

import com.susu.dfs.common.config.NodeConfig;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.task.TaskScheduler;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Scanner;

/**
 * 测试类
 */
@Slf4j
public class TestClient {

    public static void main(String[] args) {
        TestClient.startBaseClient();
    }

    /**
     * 启动一个最基础的客户端服务器
     */
    public static void startBaseClient() {
        TaskScheduler taskScheduler = new TaskScheduler("Client-Scheduler",1,true);
        Node node = NodeConfig.getNode("E:\\fxbsuajy@gmail.com\\Sujay-DFS\\doc\\config.json");
        NetClient netClient = new NetClient(node.getName(),taskScheduler);
        netClient.addClientFailListener(() -> {
            log.info("连接失败了！！");
        });
        netClient.addPackageListener(packet -> {
            log.info("客户端收到了一条消息");
        });
        netClient.addConnectListener(connected -> {
            log.info("客户端的连接状态{}",connected);
        });
        netClient.start(node.getHost(),node.getPort());

        try {
            netClient.ensureStart();
            if (netClient.isConnected()) {
                Scanner scanner = new Scanner(System.in);
                while(scanner.hasNextLine()) {
                    String msg = scanner.nextLine();
                    log.debug("user input：{}",msg);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = null;
                    try {
                        oos = new ObjectOutputStream(bos);
                        oos.writeObject(msg);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] bytes = bos.toByteArray();
                    netClient.send(NetPacket.buildPacket(bytes, PacketType.STORAGE_REGISTER));
                }
            }else {
                log.info("没有连接成功");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
