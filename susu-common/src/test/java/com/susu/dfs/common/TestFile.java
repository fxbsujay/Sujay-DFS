package com.susu.dfs.common;

import com.susu.dfs.common.eum.ReadyLogType;
import com.susu.dfs.common.file.log.ReadyLogBuffer;
import com.susu.dfs.common.file.log.ReadyLogWrapper;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
public class TestFile {

    public static void main(String[] args) {
        testReadyLogBuffer();
    }

    /**
     * 创建一个操作日志
     */
    public static void testReadyLogBuffer() {
        ReadyLogBuffer buffer = new ReadyLogBuffer();
        ReadyLogWrapper wrapper = new ReadyLogWrapper(ReadyLogType.MKDIR,"E:\\srv\\file\\test");
        try {
            buffer.write(wrapper);
            buffer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
