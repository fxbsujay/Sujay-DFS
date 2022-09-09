package com.susu.dfs.tracker.tomcat.servlet;

import com.alibaba.fastjson.JSONObject;
import com.susu.dfs.common.FileInfo;
import com.susu.dfs.common.Node;
import com.susu.dfs.common.Result;
import com.susu.dfs.common.config.SysConfig;
import com.susu.dfs.common.file.transfer.DefaultFileSendTask;
import com.susu.dfs.common.file.transfer.OnProgressListener;
import com.susu.dfs.tracker.client.ClientInfo;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import com.susu.dfs.tracker.service.TrackerFileService;
import com.susu.dfs.tracker.tomcat.dto.UploadCompletedDTO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Description: Tomcat 文件上传Servlet</p>
 *
 * @author sujay
 * @version 14:48 2022/8/12
 */
@Slf4j
public class FileUploadServlet extends HttpServlet {

    private final SysConfig config;

    private ClientManager clientManager;

    private ServerManager serverManager;

    private TrackerClusterService trackerClusterService;

    private TrackerFileService trackerFileService;

    public FileUploadServlet(SysConfig config, ServerManager serverManager, ClientManager clientManager, TrackerClusterService trackerClusterService,
                             TrackerFileService trackerFileService) {
        this.config = config;
        this.clientManager = clientManager;
        this.serverManager = serverManager;
        this.trackerClusterService = trackerClusterService;
        this.trackerFileService = trackerFileService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> fileItems;
        String queryString = request.getQueryString();

        try {
            fileItems = upload.parseRequest(new ServletRequestContext(request));

            if (fileItems != null && fileItems.size() > 0) {

                for (FileItem item : fileItems) {

                    if (!item.isFormField()) {
                        File file = new File(item.getName());
                        item.write(file);
                        String filename = config.DEFAULT_BASE_FILE_PATH + file.getName();
                        List<ClientInfo> clients = clientManager.selectAllClientsByFileAndChannel(1,filename);
                        for (ClientInfo client : clients) {
                            ChannelHandlerContext clientChannel = clientManager.getClientChannel(client.getHostname());

                            OnProgressListener onProgressListener = new OnProgressListener() {

                                @Override
                                public void onProgress(long total, long current, float progress, int currentReadBytes) {
                                    log.info("{},{},{},{}",total,current,progress,currentReadBytes);
                                }

                                @Override
                                public void onCompleted() {
                                    log.info("create a file：[ filename={},storages={}]",filename,client.getHostname());
                                    trackerFileService.createFile(filename,new HashMap<>());
                                    UploadCompletedDTO completedDTO = new UploadCompletedDTO();
                                    completedDTO.setStorageHost(client.getHostname());
                                    returnResult(response,Result.ok(completedDTO));
                                }

                            };

                            DefaultFileSendTask sendTask = new DefaultFileSendTask(file,filename,(SocketChannel) clientChannel.channel(),onProgressListener,false);
                            sendTask.execute(true);
                        }

                    }

                }
            }

        } catch (Exception e) {
            sendError(response,500,e.getMessage());
        }
    }

    private void returnResult(HttpServletResponse resp, Result<?> result) {
        String response = JSONObject.toJSONString(result);

        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Type", "application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");

        try {
            resp.getWriter().write(response);
            resp.getWriter().flush();
        } catch (IOException e) {
            log.error("Request exception：", e);
        }
    }

    /**
     * <p>Description: Failure result returned</p>
     *
     * @param code  状态码
     * @param msg   失败消息提醒
     */
    private void sendError(HttpServletResponse resp, int code, String msg) {
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Type", "application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");

        JSONObject object = new JSONObject();
        object.put("code", code);
        object.put("msg", msg);

        try {
            resp.getWriter().write(object.toJSONString());
            resp.getWriter().flush();
        } catch (IOException e) {
            log.error("Request exception：", e);
        }
    }
}
