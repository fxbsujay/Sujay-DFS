package com.susu.dfs.common.client;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.client.service.CommandLineListener;
import com.susu.dfs.common.config.AuthConfig;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.model.AuthenticateInfoRequest;
import com.susu.dfs.common.model.AuthenticateInfoResponse;
import com.susu.dfs.common.netty.NetClient;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.common.netty.msg.NetRequest;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description: DFS 客户端</p>
 *
 * @author sujay
 * @version 10:11 2022/7/14
 */
@Slf4j
public class TrackerClient {

    private static final int AUTH_INIT = 0;

    private static final int AUTH_SUCCESS = 1;

    private static final int AUTH_FAIL = 2;

    private volatile int authStatus = AUTH_INIT;

    private final SysConfig config;

    private final NetClient netClient;

    private final TaskScheduler taskScheduler;

    private CommandLineListener commandLineListener;

    public TrackerClient(SysConfig config, TaskScheduler taskScheduler) {
        this.config = config;
        this.taskScheduler = taskScheduler;
        this.netClient = new NetClient("Tracker-Client",taskScheduler,-1);
    }

    public void start() throws InterruptedException {
        this.netClient.addPackageListener(this::onTrackerResponse);
        this.netClient.addConnectListener(isConnected -> {
            log.info("Tracker Client Connect Start : {}",isConnected);
            if (isConnected) {
                authenticate();
            }
        });
        this.netClient.addClientFailListener(() -> {
            if (commandLineListener != null) {
                commandLineListener.onConnectFailed();
            }
        });
        Node node = config.getNode();
        this.netClient.start(node.getTrackerHost(),node.getTrackerPort());
        this.netClient.ensureStart();
    }

    /**
     *  auth
     */
    private void authenticate() {
        taskScheduler.scheduleOnce("Initiate user authentication",() -> {
            AuthConfig authConfig = config.getAuthConfig();
            AuthenticateInfoRequest authInfo = AuthenticateInfoRequest.newBuilder()
                    .setUsername(authConfig.getUsername())
                    .setPassword(authConfig.getPassword())
                    .build();
            NetPacket request = NetPacket.buildPacket(authInfo.toByteArray(),PacketType.AUTH_INFO);
            try {
                NetPacket response = authSendSync(request,false);
                AuthenticateInfoResponse authenticateInfoResponse = AuthenticateInfoResponse.parseFrom(response.getBody());
                authStatus = AUTH_SUCCESS;
                authConfig.setToken(authenticateInfoResponse.getToken());
                notifyAuthenticate();
                log.info("Successfully initiated authentication：[username={}, token={}]", authConfig.getUsername(), authConfig.getToken());

                if (commandLineListener != null) {
                    commandLineListener.onAuthResult(true);
                }
            } catch (Exception e) {
                log.error("Failed to initiate authentication：", e);
                authStatus = AUTH_FAIL;
                shutdown();
                if (commandLineListener != null) {
                    commandLineListener.onAuthResult(false);
                }
            }
        });
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        log.info("Shutdown Tracker Client");
        taskScheduler.shutdown();
        netClient.shutdown();
    }

    public NetPacket authSendSync(NetPacket packet) throws InterruptedException {
        return authSendSync(packet,true);
    }

    /**
     * <p>Description: Authorize and send messages</p>
     * <p>Description: 授权并发送消息</p>
     *
     * @param packet        Request body
     * @param isValidate    Whether authorization authentication is required
     * @return              Response Body
     * @throws InterruptedException Network message exception
     */
    public NetPacket authSendSync(NetPacket packet, boolean isValidate) throws InterruptedException {
        if (isValidate) {
            validate();
        }
        String token = config.getAuthConfig().getToken();
        if (StringUtils.isNotBlank(token)) {
            packet.setToken(token);
        }
        return netClient.sendSync(packet);
    }

    /**
     * <p>Description: 处理 Tracker Server 返回的信息</p>
     * <p>Description: Processing requests returned by Tracker Server</p>
     *
     * @param request NetWork Request 网络请求
     */
    private void onTrackerResponse(NetRequest request) throws Exception {
        PacketType packetType = PacketType.getEnum(request.getRequest().getType());

        log.info("Tracker onTrackerResponse");
    }

    /**
     * 认证成功后，唤醒等待的线程
     */
    private void notifyAuthenticate() {
        synchronized (this) {
            notifyAll();
        }
    }

    private void validate()  {
        try {
            while (authStatus == AUTH_INIT) {
                synchronized (this) {
                    wait(10);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (authStatus != AUTH_SUCCESS) {
            throw new RuntimeException("Failed to initiate authentication ！！");
        }
    }

    public void setCommandLineListener(CommandLineListener commandLineListener) {
        this.commandLineListener = commandLineListener;
    }
}
