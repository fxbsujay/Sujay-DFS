package com.susu.dfs.tracker.tomcat.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>Description: 一个低配版的 spring-mvc</p>
 *
 * @author sujay
 * @version 21:44 2022/8/14
 */
public class DispatcherServlet extends HttpServlet {

    private static final String BASE_PACKAGE = "com.susu.dfs.tracker.tomcat";

    /**
     * BASE_PACKAGE 路径下扫描到的所有类名
     */
    private List<String> classNames = new CopyOnWriteArrayList<>();

    /**
     *  key:    类名
     *  value:  反射生成的bean对象名称，和spring的bean是一个概念
     */
    private Map<String, String> classNameToBeanNameMap = new ConcurrentHashMap<>();

    /**
     *  key:    bean名称
     *  value:  发射生成的类对象
     */
    private Map<String, Object> beanNameToInstanceMap = new ConcurrentHashMap<>();

    public DispatcherServlet() {

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }
}
