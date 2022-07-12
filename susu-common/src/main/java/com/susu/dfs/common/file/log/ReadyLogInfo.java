package com.susu.dfs.common.file.log;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

/**
 * <p>Description: ReadyLog 文件信息</p>
 *
 * @author sujay
 * @version 10:02 2022/7/12
 */
@Slf4j
@Data
public class ReadyLogInfo implements Comparable<ReadyLogInfo>{

    /**
     *  文件名
     */
    private String name;

    /**
     *  开始时间戳
     */
    private long start;

    /**
     *  结束时间戳
     */
    private long end;

    public ReadyLogInfo() {
    }

    public ReadyLogInfo(String name, long start, long end) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

    @Override
    public int compareTo(ReadyLogInfo o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReadyLogInfo that = (ReadyLogInfo) o;
        return start == that.start &&
                end == that.end &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, name);
    }
}
