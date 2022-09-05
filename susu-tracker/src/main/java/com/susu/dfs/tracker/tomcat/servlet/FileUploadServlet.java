package com.susu.dfs.tracker.tomcat.servlet;

import com.susu.dfs.common.Node;
import com.susu.dfs.tracker.client.ClientManager;
import com.susu.dfs.tracker.server.ServerManager;
import com.susu.dfs.tracker.service.TrackerClusterService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <p>Description: Tomcat 文件上传Servlet</p>
 *
 * @author sujay
 * @version 14:48 2022/8/12
 */
@Slf4j
public class FileUploadServlet extends HttpServlet {

    private final Node node;

    private ClientManager clientManager;

    private ServerManager serverManager;

    private TrackerClusterService trackerClusterService;

    public FileUploadServlet(Node node, ServerManager serverManager, ClientManager clientManager, TrackerClusterService trackerClusterService) {
        this.node = node;
        this.clientManager = clientManager;
        this.serverManager = serverManager;
        this.trackerClusterService = trackerClusterService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> fileItems;
        String queryString = request.getQueryString();

        try {
            fileItems = upload.parseRequest(new ServletRequestContext(request));

            if (fileItems != null && fileItems.size() > 0) {

                for (FileItem item : fileItems) {

                    if (!item.isFormField()) {
                        String fileName = new File(item.getName()).getName();
                        System.out.println(fileName);
                    }

                }
            }

        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        }

    }
}
