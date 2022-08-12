package com.susu.dfs.tracker.tomcat;

import com.susu.dfs.common.Node;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.tomcat.servlet.CorsFilter;
import com.susu.dfs.tracker.tomcat.servlet.FileDownloadServlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.nio.charset.StandardCharsets;

/**
 * <p>Description: Tomcat  Http服务端</p>
 *
 * @author sujay
 * @version 14:48 2022/8/12
 */
@Slf4j
public class TomcatServer {

    private int port;

    private Tomcat tomcat;

    private FileDownloadServlet fileDownloadServlet;

    public TomcatServer(Node node, ServerManager serverManager, ClientManager clientManager, TrackerClusterService trackerClusterService) {
        this.port = node.getHttpPort();
        this.tomcat = new Tomcat();
        this.fileDownloadServlet = new FileDownloadServlet(node,serverManager,clientManager,trackerClusterService);
    }

    public void start() {
        tomcat.setHostname("localhost");
        tomcat.setPort(port);
        Context context = tomcat.addContext("", null);
        Tomcat.addServlet(context, FileDownloadServlet.class.getSimpleName(), fileDownloadServlet);
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

        try {
            tomcat.init();
            tomcat.start();
            log.info("Tomcat启动成功：[port={}]", port);
        } catch (Exception e) {
            log.error("Tomcat启动失败：", e);
            System.exit(0);
        }
    }

    public void shutdown() {
        try {
            tomcat.stop();
        } catch (Exception e) {
            log.error("Tomcat停止失败：", e);
        }
    }
}
