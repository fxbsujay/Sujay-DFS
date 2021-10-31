package com.susu.dfs.server;

/**
 * @Author sujay
 * @Description 描述 自我保护机制
 * @Date 23:49 2021/10/31
 */
public class SelfProtectionPolicy {


    private static SelfProtectionPolicy instance = new SelfProtectionPolicy();
    /**
     * 期望的一个心跳的次数，如果你有10个服务实例，这个数值就是10 * 2 = 20
     */
    private long expectedHeartbeatRate = 0L;

    /**
     * 期望的心跳次数的阈值，10 * 2 * 0.85 = 17, 每分钟至少有17次心跳，才不用进入自我保护机制
     */
    private long expectedHeartbeatThreshold = 0L;



    public static SelfProtectionPolicy getInstance() {
        return instance;
    }


    public long getExpectedHeartbeatRate() {
        return expectedHeartbeatRate;
    }

    public void setExpectedHeartbeatRate(long expectedHeartbeatRate) {
        this.expectedHeartbeatRate = expectedHeartbeatRate;
    }

    public long getExpectedHeartbeatThreshold() {
        return expectedHeartbeatThreshold;
    }

    public void setExpectedHeartbeatThreshold(long expectedHeartbeatThreshold) {
        this.expectedHeartbeatThreshold = expectedHeartbeatThreshold;
    }
}
