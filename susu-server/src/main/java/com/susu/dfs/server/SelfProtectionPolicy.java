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


    /**
     * 返回实例
     * @return
     */
    public static SelfProtectionPolicy getInstance() {
        return instance;
    }

    /**
     * 是否需要开启自我保护机制
     * @return
     */
    public Boolean isEnable() {
        HeartbeatMeasuredRate heartbeatMessuredRate = HeartbeatMeasuredRate.getInstance();
        // 最近一分钟心跳次数
        long latestMinuteHeartbeatRate = heartbeatMessuredRate.get();
        if (latestMinuteHeartbeatRate < this.expectedHeartbeatThreshold) {
            System.out.printf("【自我保护机制开启】最近一次心跳次数=" + latestMinuteHeartbeatRate + ", 期望心跳次数=" + this.expectedHeartbeatThreshold);
            return true;
        }
        System.out.printf("【自我保护机制未开启】最近一次心跳次数=" + latestMinuteHeartbeatRate + ", 期望心跳次数=" + this.expectedHeartbeatThreshold);
        return false;
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
