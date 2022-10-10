package com.susu.dfs.common.utils;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * <p>Description: File Toolset</p>
 * <p>文件工具类</p>
 * @author sujay
 * @version 22:11 2022/1/24
 * @since JDK1.8 <br/>
 */
public class FileUtils {

    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;
    /**
     * The number of bytes in a kilobyte.
     *
     * @since 2.4
     */
    public static final BigDecimal ONE_KB_BI = BigDecimal.valueOf(ONE_KB);

    /**
     * The number of bytes in a megabyte.
     *
     * @since 2.4
     */
    public static final BigDecimal ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);

    /**
     * The number of bytes in a gigabyte.
     *
     * @since 2.4
     */
    public static final BigDecimal ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);

    /**
     * The number of bytes in a terabyte.
     *
     * @since 2.4
     */
    public static final BigDecimal ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);

    /**
     * The number of bytes in a petabyte.
     *
     * @since 2.4
     */
    public static final BigDecimal ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);

    /**
     * The number of bytes in an exabyte.
     *
     * @since 2.4
     */
    public static final BigDecimal ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);


    private static final int BUFFER_SIZE = 2 * 1024;

    /**
     * <p>Description: read file</p>
     * <p>读取文件</p>
     * @param path 文件路径  File path
     * <tr><th align="left">Value</th><th align="left">Meaning</th></tr>
     * <tr><td valign="top"><tt>"r"</tt></td>
     *     <td>	以只读的方式打开文本，也就意味着不能用write来操作文件 {@link
     *     IOException}</td></tr>
     * <tr><td valign="top"><tt>"rw"</tt></td>
     *     <td> 读操作和写操作都是允许的</td></tr>
     * <tr><td valign="top"><tt>"rws"</tt></td>
     *     <td> 每当进行写操作，同步的刷新到磁盘，刷新内容和元数据</td></tr>
     * <tr><td valign="top"><tt>"rwd"&nbsp;&nbsp;</tt></td>
     *     <td> 每当进行写操作，同步的刷新到磁盘，刷新内容 </td></tr>
     */
    public static ByteBuffer readBuffer(String path) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r"); FileInputStream fis =
                new FileInputStream(raf.getFD()); FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate((int) raf.length());
            channel.read(buffer);
            buffer.flip();
            return buffer;
        }
    }

    /**
     * <p>Description: read file</p>
     * <p>读取文件</p>
     * @param path 文件路径  File path
     */
    public static String readString(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static File getFile(String path) {
        return new File(path);
    }

    /**
     * <p>Description: Writing files in NiO mode</p>
     * <p>NIO方式写文件</p>
     * @param path       文件路径
     * @param delOldFile 是否删除旧文件
     * @param buffer     内存缓冲区
     */
    public static void writeFile(String path, boolean delOldFile, ByteBuffer buffer) throws IOException {
        mkdirParent(path);
        File file = new File(path);
        if (delOldFile) {
            if (file.exists()) {
                del(file);
            }
        }
        try (RandomAccessFile raf = new RandomAccessFile(path, "rw"); FileOutputStream fos =
                new FileOutputStream(raf.getFD()); FileChannel channel = fos.getChannel()) {
            channel.write(buffer);
            channel.force(true);
        }
    }

    /**
     * <p>Description: The parent directory where the file was created already exists</p>
     * <p>创建文件的父目录存在</p>
     * @param fileName 文件名
     */
    public static void mkdirParent(String fileName) {
        File file = new File(fileName);
        mkdirs(file.getParent());
    }

    /**
     * <p>Description: Create directory recursively</p>
     * <p>递归创建目录</p>
     * @param pathName 目录名
     */
    public static void mkdirs(String pathName) {
        File file = new File(pathName);
        file.mkdirs();
    }

    /**
     * <p>Description: is Directory</p>
     * <p>是否是文件夹</p>
     * @param path 资源路径
     * @param isFollowLinks 是否需要处理符号链接的选项
     * @return boolean  true / false
     */
    public static boolean isDirectory(Path path, boolean isFollowLinks) {
        if (null == path) {
            return false;
        } else {
            LinkOption[] options = isFollowLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
            return Files.isDirectory(path, options);
        }
    }

    public static boolean isDirectory(Path path) {
        return isDirectory(path, false);
    }

    /**
     * <p>Description: delete file</p>
     * <p>删除文件</p>
     * @param path 资源路径
     */
    protected static void delFile(Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (AccessDeniedException e) {
            if (!path.toFile().delete()) {
                throw e;
            }
        }

    }

    /**
     * <p>Description: delete file</p>
     * <p>删除文件</p>
     * @param file 文件
     */
    public static boolean del(File file) {
        if(file != null && file.exists()) {
            if (file.isDirectory()) {
                boolean isOk = clean(file);
                if (!isOk) {
                    return false;
                }
            }
            Path path = file.toPath();
            try {
                delFile(path);
            } catch (DirectoryNotEmptyException e) {
                del(path);
            } catch (IOException e) {
                e.printStackTrace();
               return false;
            }
        }
        return true;
    }

    public static boolean del(String path) {
        return del(new File(path));
    }

    /**
     * <p>Description: delete file</p>
     * <p>删除文件</p>
     * @param path 资源路径
     */
    public static boolean del(Path path) {
        if (Files.notExists(path)) {
            return true;
        } else {
            try {
                if (isDirectory(path)) {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            if (exc == null) {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            } else {
                                throw exc;
                            }
                        }
                    });
                } else {
                    delFile(path);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * <p>Description: Empty file</p>
     * <p>清空文件</p>
     */
    public static boolean clean(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (null != files) {
                int length = files.length;
                for (File childFile : files) {
                    if (!del(childFile)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * <p>Description: Compress to zip</p>
     * <p>压缩成ZIP</p>
     * @param inPath     压缩文件夹路径
     * @param outPath    压缩文件输出流
     * @param keepDirStructure  是否保留原来的目录结构,true:保留目录结构;
     * 							false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     * @exception RuntimeException 压缩失败会抛出运行时异常
     */
    public static void toZip(String inPath, String outPath, boolean keepDirStructure) throws RuntimeException{
        File source = new File(inPath);
        ZipOutputStream zos = null ;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(outPath));
            zos = new ZipOutputStream(fos);

            compress(null,source,zos,keepDirStructure);
        } catch (Exception e) {
            throw new RuntimeException("zip error from FileUtils",e);
        }finally{
            if(zos != null){
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    /**
     * @param source 源文件
     * @param zos		 zip输出流
     * @param name		 压缩后的名称
     * @param keepDirStructure  是否保留原来的目录结构,true:保留目录结构;
     * 							false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     * @exception Exception 流异常
     */
    private static void compress( String name,File source, ZipOutputStream zos, boolean keepDirStructure) throws Exception{
        String path = (name != null) ? name : source.getName();
        if(source.isFile()){
            zos.putNextEntry(new ZipEntry(path));
            int len;
            FileInputStream in = new FileInputStream(source);
            byte[] buf = new byte[BUFFER_SIZE];
            while ((len = in.read(buf,0,buf.length)) != -1){
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = source.listFiles();
            if(listFiles == null || listFiles.length == 0){
                if(keepDirStructure){
                    zos.putNextEntry(new ZipEntry(path + "/"));
                    zos.closeEntry();
                }
            }else {
                for (File file : listFiles) {
                    if (keepDirStructure) {
                        compress(path + "/" + file.getName(),file, zos, true);
                    } else {
                        compress(file.getName(),file, zos, false);
                    }

                }
            }
        }
    }

    /**
     * <p>Description: Compress to zip</p>
     * <p>压缩成ZIP</p>
     * @param inPaths     压缩文件夹路径
     * @param outPath    压缩文件输出流
     * @exception RuntimeException 压缩失败会抛出运行时异常
     */
    public static void toZip(String outPath, String... inPaths)throws RuntimeException {
        ZipOutputStream zos = null ;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(outPath));
            zos = new ZipOutputStream(fos);
            byte[] buf = new byte[BUFFER_SIZE];
            for (String path : inPaths) {
                File file = new File(path);
                zos.putNextEntry(new ZipEntry(file.getName()));
                int len;
                FileInputStream in = new FileInputStream(file);
                while ((len = in.read(buf,0,buf.length)) != -1){
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("zip error from FileUtils",e);
        }finally{
            if(zos != null){
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * <p>Description: Unzip the package</p>
     * <p>解压压缩包</p>
     * @param path 文件路径
     */
    public static void unZip(String path) {
        File source = new File(path);
        if (source.exists()) {
            ZipInputStream zis = null;
            BufferedOutputStream bos = null;
            try {
                zis = new ZipInputStream(new FileInputStream(source));
                ZipEntry entry;
                byte[] buf = new byte[BUFFER_SIZE];
                while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                    File target = new File(source.getParent(), entry.getName());
                    if (!target.getParentFile().exists()) {
                        target.getParentFile().mkdirs();
                    }
                    bos = new BufferedOutputStream(new FileOutputStream(target));
                    int read;
                    while ((read = zis.read(buf, 0, buf.length)) != -1) {
                        bos.write(buf, 0, read);
                    }
                    bos.flush();
                }
                zis.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("unzip error from FileUtils",e);
            } finally {
               if (zis != null) {
                   try {
                       zis.close();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
               if (bos != null) {
                   try {
                       bos.close();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
            }
        }
    }

    /**
     * <p>Description: get file size</p>
     * <p>获取文件大小带单位</p>
     * @param length 文件路径
     */
    public static String formatSize(long length) {
        BigDecimal size = new BigDecimal(String.valueOf(length));
        String displaySize;

        if (size.divide(ONE_EB_BI, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) > 0) {
            displaySize = size.divide(ONE_EB_BI, 2, RoundingMode.HALF_UP) + " EB";
        } else if (size.divide(ONE_PB_BI, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) > 0) {
            displaySize = size.divide(ONE_PB_BI, 2, RoundingMode.HALF_UP) + " PB";
        } else if (size.divide(ONE_TB_BI, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) > 0) {
            displaySize = size.divide(ONE_TB_BI, 2, RoundingMode.HALF_UP) + " TB";
        } else if (size.divide(ONE_GB_BI, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) > 0) {
            displaySize = size.divide(ONE_GB_BI, 2, RoundingMode.HALF_UP) + " GB";
        } else if (size.divide(ONE_MB_BI, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) > 0) {
            displaySize = size.divide(ONE_MB_BI, 2, RoundingMode.HALF_UP) + " MB";
        } else if (size.divide(ONE_KB_BI, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) > 0) {
            displaySize = size.divide(ONE_KB_BI, 2, RoundingMode.HALF_UP) + " KB";
        } else {
            displaySize = size + " bytes";
        }
        return displaySize;
    }

    public static boolean validateFileName(String filename) {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean win = osName.startsWith("win");
        if (!win && !filename.startsWith(File.separator)) {
            return false;
        }
        String name = new File(filename).getName();
        if (filename.contains("//")) {
            return false;
        }
        return !name.startsWith(".");
    }

    /**
     * 复制jar包中的资源文件到指定目录
     * @param path          jar包所在路径
     * @param tempPath      保存目录
     * @param filePrefix    需要进行复制的资源文件目录：以BOOT-INF/classes/开头
     */
    public static void copyJarResourcesFileToTemp(URI path, String tempPath, String filePrefix) {
        try {
            List<Map.Entry<ZipEntry, InputStream>> collect =
                    readJarFile(new JarFile(path.getPath()), filePrefix).collect(Collectors.toList());
            for (Map.Entry<ZipEntry, InputStream> entry : collect) {
                // 文件相对路径
                String key = entry.getKey().getName();
                // 文件流
                InputStream stream = entry.getValue();
                File newFile = new File(tempPath + key.replaceAll("BOOT-INF/classes", ""));
                if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }
                org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, newFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to copy jar source template",e);
        }
    }

    @SneakyThrows
    public static Stream<Map.Entry<ZipEntry, InputStream>> readJarFile(JarFile jarFile, String prefix) {
        Stream<Map.Entry<ZipEntry, InputStream>> readingStream =
                jarFile.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith(prefix))
                        .map(entry -> {
                            try {
                                return new AbstractMap.SimpleEntry<>(entry, jarFile.getInputStream(entry));
                            } catch (IOException e) {
                                return new AbstractMap.SimpleEntry<>(entry, null);
                            }
                        });
        return readingStream.onClose(() -> {
            try {
                jarFile.close();
            } catch (IOException e) {
                throw new RuntimeException("failed to close jarFile",e);
            }
        });
    }

    public static String fileMd5(String fileName) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            return DigestUtils.md5Hex(fis);
        }
    }

    public static void main(String[] args) throws Exception {
        toZip("D:/project/卷/test/aa","D:/project/卷/test/test.zip",true);
        toZip("D:/project/卷/test/test.zip","D:/project/卷/test/aa/新建文本文档.txt");
        unZip("D:/project/卷/test/test.zip");
    }








}
