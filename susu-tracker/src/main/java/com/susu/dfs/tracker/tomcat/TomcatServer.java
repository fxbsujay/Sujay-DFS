package com.susu.dfs.tracker.tomcat;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.server.TrackerChannelHandle;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import com.susu.dfs.tracker.tomcat.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.scan.StandardJarScanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * <p>Description: Tomcat  Http服务端</p>
 *
 * @author sujay
 * @version 14:48 2022/8/12
 */
@Slf4j
public class TomcatServer {

    private int port;

    private final String basedir = System.getProperty("user.dir") + "/tomcat";

    private Tomcat tomcat;

    private FileDownloadServlet fileDownloadServlet;

    private FileUploadServlet fileUploadServlet;

    private DispatcherServlet dispatcherServlet;

    public TomcatServer(SysConfig config, TrackerChannelHandle trackerChannelHandle, ServerManager serverManager, ClientManager clientManager, TrackerClusterService trackerClusterService, TrackerFileService trackerFileService) {
        DispatchComponentProvider.getInstance().addComponent(config,serverManager,clientManager,trackerClusterService,trackerFileService,trackerChannelHandle);
        Node node = config.getNode();
        this.port = node.getHttpPort();
        this.tomcat = new Tomcat();
        this.dispatcherServlet = new DispatcherServlet();
        this.fileDownloadServlet = new FileDownloadServlet(node,serverManager,clientManager,trackerClusterService);
        this.fileUploadServlet = new FileUploadServlet(config,serverManager,clientManager,trackerClusterService,trackerFileService);
    }

    public void start() {


        tomcat.setBaseDir(basedir);
        tomcat.setHostname("localhost");
        tomcat.setPort(port);

        Context context = tomcat.addContext("", null);
        Tomcat.addServlet(context, FileUploadServlet.class.getSimpleName(), fileUploadServlet);
        Tomcat.addServlet(context, FileDownloadServlet.class.getSimpleName(), fileDownloadServlet);
        Tomcat.addServlet(context, DispatcherServlet.class.getSimpleName(), dispatcherServlet);

        context.addServletMappingDecoded("/api/upload", FileUploadServlet.class.getSimpleName());
        context.addServletMappingDecoded("/api/*", DispatcherServlet.class.getSimpleName());
        context.addServletMappingDecoded("/*", FileDownloadServlet.class.getSimpleName());
        context.addWatchedResource("");

        FilterDef filterDef = new FilterDef();
        filterDef.setFilter(new CorsFilter());
        filterDef.setFilterName("CorsFilter");
        FilterMap filterMap = new FilterMap();
        filterMap.addURLPatternDecoded("/*");
        filterMap.addServletName("*");
        filterMap.setFilterName("CorsFilter");
        filterMap.setCharset(StandardCharsets.UTF_8);
        context.addFilterDef(filterDef);
        context.addFilterMap(filterMap);

        URL resource = this.getClass().getResource("/webapp");
        try {
            if (resource != null) {
                String webappPath = resource.getPath();
                if (resource.getPath().contains(".jar!")) {
                    // webappPath = file:/home/dfs/tracker.jar!/webapp
                    webappPath = basedir + File.separator +  "webapp";
                    File file = new File(webappPath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    file.deleteOnExit();
                }

                Context webContext = tomcat.addWebapp("/home", webappPath);
                ((StandardJarScanner) webContext.getJarScanner()).setScanManifest(false);
            }

            tomcat.init();
            tomcat.start();
            log.info("Tomcat Server started on port：{}", port);
        } catch (Exception e) {
            log.error("Tomcat启动失败：", e);
            System.exit(0);
        }
    }

    public void shutdown() {
        try {
            tomcat.stop();
        } catch (Exception e) {
            log.error("Shutdown Tomcat Server：", e);
        }
    }

    private File createTempDir(String prefix) {
        try {
            File tempDir = Files.createTempDirectory(prefix).toFile();
            tempDir.deleteOnExit();
            return tempDir;
        } catch (IOException var3) {
            throw new RuntimeException("Unable to create tempDir. java.io.tmpdir is set to " + System.getProperty("java.io.tmpdir"), var3);
        }
    }
}
