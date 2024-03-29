package com.susu.dfs.tracker.tomcat.servlet;

import com.susu.dfs.common.Node;
import com.susu.dfs.common.eum.PacketType;
import com.susu.dfs.common.model.GetStorageForFileRequest;
import com.susu.dfs.common.model.GetStorageForFileResponse;
import com.susu.dfs.common.model.StorageNode;
import com.susu.dfs.common.netty.msg.NetPacket;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import lombok.extern.slf4j.Slf4j;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * <p>Description: Tomcat 文件下载Servlet</p>
 *
 * @author sujay
 * @version 14:48 2022/8/12
 */
@Slf4j
public class FileDownloadServlet extends HttpServlet {

    private final Node node;

    private final String REQUEST_HEADER = "susu-dfs-http—get-file";

    private ClientManager clientManager;

    private ServerManager serverManager;

    private TrackerClusterService trackerClusterService;

    public FileDownloadServlet(Node node,ServerManager serverManager,ClientManager clientManager,TrackerClusterService trackerClusterService) {
        this.node = node;
        this.clientManager = clientManager;
        this.serverManager = serverManager;
        this.trackerClusterService = trackerClusterService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestUri = req.getRequestURI();
        String filename = URLDecoder.decode(requestUri, "UTF-8");
        if (!filename.startsWith("/api/download")) {
            return;
        }
        filename = filename.substring("/api/download".length());

        int trackerIndex = serverManager.getTrackerIndexByFilename(filename);
        if (serverManager.isCurrentTracker(trackerIndex)) {
            relayStorage(resp, requestUri, filename);
            return;
        }

        GetStorageForFileRequest request = GetStorageForFileRequest.newBuilder()
                .setFilename(filename)
                .build();
        NetPacket packet = NetPacket.buildPacket(request.toByteArray(), PacketType.GET_STORAGE_FOR_FILE);
        NetPacket fileResp;

        try {
            fileResp = trackerClusterService.sendSync(trackerIndex, packet);
        } catch (InterruptedException e) {
            log.error("download fail：", e);
            resp.sendError(500, "File Failed: " + filename);
            return;
        }

        GetStorageForFileResponse response = GetStorageForFileResponse.parseFrom(fileResp.getBody());
        StorageNode storage = response.getStorage();
        String redirectUrl = "http://" + storage.getHostname() + ":" + storage.getHttpPort() + requestUri;
        relayStorage(resp,redirectUrl,filename);
    }

    /**
     * 将请求重定向到 storage节点
     */
    private void relayStorage(HttpServletResponse resp, String requestUri, String filename) throws IOException {
        ClientInfo client = clientManager.chooseReadableClientByFileName(filename);
        if (client == null) {
            resp.sendError(404, "File Not Found: " + filename);
            return;
        }
        String redirectUrl = "http://" + client.getHostname() + ":" + client.getHttpPort() + requestUri;
        if (log.isDebugEnabled()) {
            log.debug("Redirect the storage node for file download: [url={}]", redirectUrl);
        }
        resp.sendRedirect(redirectUrl);
    }
}
