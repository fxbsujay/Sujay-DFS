package com.susu.dfs.common.task;

import com.susu.dfs.common.file.AbstractFileService;
import com.susu.dfs.common.file.image.ImageLogWrapper;
import com.susu.dfs.common.file.log.DoubleBuffer;
import com.susu.dfs.common.utils.FileUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: garbage collection </p>
 * <p>Description: 系统垃圾回收 </p>
 *
 * @author sujay
 * @version 14:21 2022/7/28
 */
@Slf4j
public class ClearImageTask implements Runnable{

    private String baseDir;

    private AbstractFileService fileService;

    private DoubleBuffer doubleBuffer;

    public ClearImageTask(String baseDir, AbstractFileService fileService) {
        this(baseDir, fileService,null);
    }

    public ClearImageTask(String baseDir, AbstractFileService fileService, DoubleBuffer doubleBuffer) {
        this.fileService = fileService;
        this.baseDir = baseDir;
        this.doubleBuffer = doubleBuffer;
    }

    @Override
    @SneakyThrows
    public void run() {

        boolean verify = false;
        long maxTxId = -1;

        Map<Long, String> images = fileService.scanImageLogMap(baseDir);
        List<Long> times = new ArrayList<>(images.keySet());
        times.sort((o1, o2) -> o1.equals(o2) ? 0 : (int) (o2 - o1));

        for (Long time : times) {
            String path = images.get(time);
            if (verify) {
                FileUtils.del(path);
                log.info("删除 Image: [file={}]", path);
                continue;
            }

            try (RandomAccessFile raf = new RandomAccessFile(path, "r");
                 FileInputStream fis = new FileInputStream(raf.getFD());
                 FileChannel channel = fis.getChannel()) {

                maxTxId = ImageLogWrapper.validate(channel, path, (int) raf.length());

                if (maxTxId > 0) {
                    verify = true;
                    log.info("清除FSImage任务，找到最新的合法的FsImage: [file={}]", path);
                } else {
                    raf.close();
                    FileUtils.del(path);
                    log.info("删除FSImage: [file={}]", path);
                }
            }
        }

        if (verify && doubleBuffer != null) {
            doubleBuffer.cleanReadyLogByTxId(maxTxId);
        }
    }
}
