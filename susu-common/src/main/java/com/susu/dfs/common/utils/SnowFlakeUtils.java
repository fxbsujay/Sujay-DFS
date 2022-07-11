package com.susu.dfs.common.utils;

/**
 * <p>Description: Snow flakeId Worker</p>
 * <p>雪花算法ID</p>
 * @author sujay
 * @version 13:08 2022/4/28
 * @since JDK1.8 <br/>
 */
public class SnowFlakeUtils {

    /**
     * 数据中心 ( 0 - 31 )
     */
    private final long DATA_CENTER_ID;
    /**
     * 机器标识 ( 0 - 31 )
     */
    private final long MACHINE_ID;

    /**
     * 毫秒内序列号 ( 0 - 4095 )
     */
    private long sequence = 0L;

    /**
     * 上一次生成ID的时间戳
     */
    private long lastStamp = -1L;

    /**
     * 起始的时间戳 （ 2019-01-01 ）
     */
    private final static long START_STAMP = 1480166465631L;

    /**
     * 序列号占用的位数
     */
    private final static long SEQUENCE_BIT = 12L;

    /**
     * 机器标识占用的位数
     */
    private final static long MACHINE_BIT = 5L;

    /**
     * 数据中心占用的位数
     */
    private final static long DATACENTER_BIT = 5L;

    /**
     * ~ 运算符的含义
     * int a = 10;
     * System.out.println(~a);
     * 正数的原码 = 反码 = 补码
     * 负数的反码 = 原码符号位不变，其它位全取反，负数的补码 = 反码 + 1
     * ~表示非运算符，就是将该数的所有二进制位全取反，
     * 但又由于计算机中是以补码的形式存储的，所以0 1010全取反是1 0101（只是补码形式，还需要转成原码）
     * 此时得到的1 0101只是补码，我们需要将它先转为反码，反码 = 补码-1，得到反码为1 010
     * 我们得到反码后，将它转为原码，原码 = 反码符号位不变，其它位全取反，得到最终的原码为1 1011，转化为十进制就是-11
     *
     * 每一部分的最大值
     */
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);

    private final static long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);

    /**
     * 机器id向左移12位
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;

    /**
     * 数据标识id向左移17位 （ 12 + 5 ）
     */
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;

    /**
     * 时间戳向左移22位 （ 5 + 5 + 12 ）
     */
    private final static long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    /**
     * 构造函数
     * @param datacenterId 数据中心ID （ 0 - 31 ）
     * @param machineId  机器Id （ 0 - 31 ）
     */
    public SnowFlakeUtils(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.DATA_CENTER_ID = datacenterId;
        this.MACHINE_ID = machineId;
    }

    /**
     * <p>Description: generate next ID</p>
     * <p>产生下一个ID</p>
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timeStamp = timeGen();
        if (timeStamp < lastStamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        if (timeStamp == lastStamp) {
            //相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                timeStamp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStamp = timeStamp;

        // 移位并通过或运算拼到一起组成64位ID
        return (timeStamp - START_STAMP) << TIMESTAMP_LEFT
                | DATA_CENTER_ID << DATACENTER_LEFT
                | MACHINE_ID << MACHINE_LEFT
                | sequence;
    }

    private long getNextMill() {
        long mill = timeGen();
        while (mill <= lastStamp) {
            mill = timeGen();
        }
        return mill;
    }

    /**
     * @return 当前时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SnowFlakeUtils utilA = new SnowFlakeUtils(1,1);
        for (int i = 0; i < 10; i++) {
            long id = utilA.nextId();
            byte[] bytes = HexConvertUtils.longToBytes(id);
            System.out.println(bytes.length);
            System.out.println(String.valueOf(HexConvertUtils.bytesToLong(bytes,0)));
        }


    }

}
