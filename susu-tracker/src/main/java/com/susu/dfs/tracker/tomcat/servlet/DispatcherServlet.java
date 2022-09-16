package com.susu.dfs.tracker.tomcat.servlet;

import com.alibaba.fastjson.JSONObject;
import com.susu.dfs.tracker.tomcat.VariablePathParser;
import com.susu.dfs.tracker.tomcat.annotation.*;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * <p>Description: 一个低配版的 spring-mvc</p>
 *
 * @author sujay
 * @version 21:44 2022/8/14
 */
@Slf4j
public class DispatcherServlet extends HttpServlet {

    /**
     * Tomcat Servlet 扫描路径
     */
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

    /**
     *  key:    uri#method
     *  value:  mapping
     */
    private Map<String, Mapping> mappings = new ConcurrentHashMap<>();

    /**
     * 路径变量解析器
     */
    private VariablePathParser variablePathParser = new VariablePathParser();


    public DispatcherServlet() {
        try {
            scanPackage(BASE_PACKAGE);
            initController();
            injectControllerComponent();
            createRequestMapping();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String uri = trimUrl(req.getRequestURI());
            String method = req.getMethod();
            Mapping mapping = findMapping(uri, method);

            if (mapping == null) {
                sendError(resp, 404, "Unsupported request mode and path：" + req.getMethod() + " " + uri);
                return;
            }

            Method invokeMethod = mapping.getInvokeMethod();
            String className = invokeMethod.getDeclaringClass().getCanonicalName();

            String beanName = classNameToBeanNameMap.get(className);
            Object bean = beanNameToInstanceMap.get(beanName);

            Object[] args = generateParameter(mapping, req);
            Object result = invokeMethod.invoke(bean, args);
            String response = JSONObject.toJSONString(result);

            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Content-Type", "application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json;charset=UTF-8");

            resp.getWriter().write(response);
            resp.getWriter().flush();

        } catch (Exception e) {

            String msg = e.getMessage();
            Throwable cause = e.getCause();

            while (cause != null) {
                msg = cause.getMessage();
                cause = cause.getCause();
            }

            sendError(resp, 500, "Request exception：" + msg);
            log.error("Request exception：", e);
        }
    }

    /**
     * 生成反射调用参数
     *
     * @param mapping 请求映射关系
     * @param request 请求
     * @return 调用参数
     * @throws Exception 异常
     */
    private Object[] generateParameter(Mapping mapping, HttpServletRequest request) throws Exception {

        if (mapping.getParameterList().isEmpty()) {
            return new Object[0];
        }

        Map<String, String> pathVariableMap = null;
        List<Mapping.ParamMetadata> parameterList = mapping.getParameterList();
        Object[] params = new Object[parameterList.size()];

        for (int i = 0; i < parameterList.size(); i++) {
            Mapping.ParamMetadata metadata = parameterList.get(i);

            if (metadata.getType().equals(Mapping.Type.PATH_VARIABLE)) {

                if (pathVariableMap == null) {
                    pathVariableMap = variablePathParser.extractVariable(trimUrl(request.getRequestURI()));
                }

                String pathVariableValue = pathVariableMap.get(metadata.getParamKey());
                Class<?> classType = metadata.getParamClassType();
                params[i] = mapValue(classType, pathVariableValue);

            } else if (metadata.getType().equals(Mapping.Type.REQUEST_BODY)) {

                if (request.getMethod().equals(HttpMethod.GET.toString())) {
                    throw new IllegalArgumentException("@RequestBody注解不支持GET请求方式");
                }

                if (!request.getContentType().contains("application/json")) {
                    throw new IllegalArgumentException("@RequestBody注解只支持json格式数据");
                }

                String json = readInput(request.getInputStream());
                try {
                    params[i] = JSONObject.parseObject(json, metadata.getParamClassType());
                } catch (Exception e) {
                    throw new IllegalArgumentException("JSON格式有误： " + json);
                }

            } else if (metadata.getType().equals(Mapping.Type.QUERY_ENTITY)) {

                Map<String, String> queriesMap = extractQueries(request);
                Class<?> paramClassType = metadata.getParamClassType();
                Field[] declaredFields = paramClassType.getDeclaredFields();
                Object param = paramClassType.newInstance();

                for (Field field : declaredFields) {
                    String name = field.getName();
                    String value = queriesMap.get(name);
                    if (value == null) {
                        continue;
                    }
                    field.setAccessible(true);
                    field.set(param, mapValue(field.getType(), value));
                }
                params[i] = param;
            }
        }
        return params;
    }

    /**
     * <p>Description: 扫描包路径下所有的类</p>
     *
     * @param packageName   包路径
     */
    private void scanPackage(String packageName) throws IOException {
        String basePackage = packageName.replaceAll("\\.", "/");
        URL url = this.getClass().getClassLoader().getResource(basePackage);

        if (log.isDebugEnabled()) {
            log.debug("scanClass Url={}", url);
        }

        if (url == null) return;

        String rootPath = getRootPath(url);
        if (rootPath.endsWith("jar")) {
            readFromJarFile(rootPath, basePackage);
            return;
        }

        File basePackageFile = new File(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name()));
        File[] children = basePackageFile.listFiles();

        if (children == null) {
            return;
        }

        for (File file : children) {
            if (file.isDirectory()) {
                scanPackage(packageName + "." + file.getName());
            } else {
                String clazz = packageName + "." + file.getName().split("\\.")[0];
                if (log.isDebugEnabled()) {
                    log.debug("Scan to class：[class={}]", clazz);
                }
                classNames.add(clazz);
            }
        }
    }

    /**
     * <p>Description: 扫描jar包</p>
     * <p>Description: Scan jar package</p>
     *
     * @param jar           jar包路径
     * @param basePackage   要扫描的包路径
     * @throws IOException  IO异常
     */
    private void readFromJarFile(String jar, String basePackage) throws IOException {
        JarInputStream jarIn = new JarInputStream(Files.newInputStream(Paths.get(jar)));
        ZipEntry nextEntry = jarIn.getNextEntry();

        while (nextEntry != null) {

            String name = nextEntry.getName();

            if (name.startsWith(basePackage) && name.endsWith(".class")) {
                name = name.substring(0, name.indexOf(".")).replaceAll("/", ".");
                if (log.isDebugEnabled()) {
                    log.debug("Scan to class：[class={}]", name);
                }
                classNames.add(name);
            }

            nextEntry = jarIn.getNextJarEntry();
        }
    }

    /**
     * 初始化 controller
     */
    private void initController() throws Exception {
        for (String className : classNames) {
            Class<?> clazz = Class.forName(className);

            if (!clazz.isAnnotationPresent(RestController.class)) {
                continue;
            }

            RestController annotation = clazz.getAnnotation(RestController.class);
            String beanName = annotation.value();

            if ("".equals(beanName)) {
                beanName = clazz.getSimpleName();
            }

            beanNameToInstanceMap.put(beanName, clazz.newInstance());
            classNameToBeanNameMap.put(className, beanName);

            if (log.isDebugEnabled()) {
                log.debug("init controller: [name={}]", beanName);
            }
        }
    }

    /**
     * 向controller中注入组件
     */
    private void injectControllerComponent() throws Exception {

        for (Map.Entry<String, Object> entry : beanNameToInstanceMap.entrySet()) {

            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {

                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }

                Autowired annotation = field.getAnnotation(Autowired.class);
                String needInjectBeanName = annotation.value();

                if (needInjectBeanName.length() == 0) {
                    needInjectBeanName = field.getType().getSimpleName();
                }

                field.setAccessible(true);
                Object component = DispatchComponentProvider.getInstance().getComponent(needInjectBeanName);

                if (component == null) {
                    throw new IllegalArgumentException("Can not find component " + needInjectBeanName);
                }

                field.set(entry.getValue(), component);

                if (log.isDebugEnabled()) {
                    log.debug("Inject controller components：[{}] ", entry.getValue().getClass().getSimpleName() + "#" + field.getName());
                }
            }
        }
    }

    /**
     * 创建请求URL与方法的映射
     */
    private void createRequestMapping() throws ClassNotFoundException {
        for (String className : classNames) {
            Class<?> clazz = Class.forName(className);

            if (!clazz.isAnnotationPresent(RestController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                baseUrl = annotation.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (!m.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }

                RequestMapping annotation = m.getAnnotation(RequestMapping.class);
                String path = annotation.value();
                String fullPath = trimUrl(baseUrl + path);

                if (variablePathParser.containVariable(path)) {
                    variablePathParser.add(fullPath);
                    if (log.isDebugEnabled()) {
                        log.debug("Save path variable mapping：[path={}]", fullPath);
                    }
                }

                Mapping mapping = new Mapping();
                String method = annotation.method();
                mapping.setUrl(fullPath);
                mapping.setMethod(method);
                mapping.setInvokeMethod(m);

                parseParameterMetadata(mapping, m.getParameters());

                if (log.isDebugEnabled()) {
                    log.debug("Save URL mapping：[header={}, method={}]", method + " " + fullPath, className + "#" + m.getName());
                }

                String key = fullPath + "#" + method;
                if (mappings.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate interface declaration：" + key);
                }

                mappings.put(key, mapping);
            }
        }
    }

    private void parseParameterMetadata(Mapping mapping, Parameter[] parameters) {
        boolean hasRequestBody = false;
        boolean hasQueries = false;

        String name = mapping.getInvokeMethod().getDeclaringClass().getSimpleName() + "#" + mapping.getInvokeMethod().getName();

        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable annotation = parameter.getAnnotation(PathVariable.class);
                String variableKey = annotation.value();
                mapping.addParameterList(Mapping.Type.PATH_VARIABLE, variableKey, parameter.getType());
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {

                if (hasRequestBody) {
                    throw new IllegalArgumentException("Only one @RequestBody annotation is supported on the same interface: " + name);
                }

                hasRequestBody = true;
                Class<?> type = parameter.getType();
                mapping.addParameterList(Mapping.Type.REQUEST_BODY, null, type);
            } else {

                if (hasQueries) {
                    throw new IllegalArgumentException("Only one get request entity is supported：" + name);
                }

                hasQueries = true;
                Class<?> type = parameter.getType();
                mapping.addParameterList(Mapping.Type.QUERY_ENTITY, null, type);
            }
        }
    }

    /**
     * <p>Description: Failure result returned</p>
     *
     * @param code  状态码
     * @param msg   失败消息提醒
     * @throws IOException  IO异常
     */
    private void sendError(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Type", "application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");

        JSONObject object = new JSONObject();
        object.put("code", code);
        object.put("msg", msg);

        resp.getWriter().write(object.toJSONString());
        resp.getWriter().flush();
    }

    /**
     * <p>Description: 解析url地址</p>
     *
     * <pre>
     *     For example:
     *         //github.com/../
     *         /github.com/..
     * </pre>
     *
     * @param uri   请求路径
     */
    private String trimUrl(String uri) {
        uri = uri.replaceAll("//", "/");
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /**
     * 获取根路径
     */
    private String getRootPath(URL url) {
        String file = url.getFile();
        int pos = file.indexOf("!");
        if (pos == -1) {
            return file;
        } else {
            return file.substring(5, pos);
        }
    }


    /**
     * <p>Description: 将参数转化为具体的类型</p>
     * <p>Description: Convert parameters to specific types</p>
     *
     * @param classType 参数类型
     * @param value     参数值
     * @return 转换后的结果
     */
    private Object mapValue(Class<?> classType, String value) {
        if (classType.equals(String.class)) {
            return value;
        } else if (classType.equals(Integer.class)) {
            return Integer.parseInt(value);
        } else if (classType.equals(Long.class)) {
            return Long.parseLong(value);
        }
        return value;
    }

    /**
     * <p>Description: 根据url和servlet方法名获取映射</p>
     * <p>Description: Get the mapping according to the URL and servlet method name</p>
     *
     * @param uri       路径
     * @param method    方法名
     * @return          接口映射
     */
    private Mapping findMapping(String uri, String method) {
        Mapping mapping = mappings.get(uri + "#" + method);
        if (mapping != null) {
            return mapping;
        }
        String urlWithVariable = variablePathParser.match(uri);
        if (urlWithVariable != null) {
            mapping = mappings.get(urlWithVariable + "#" + method);
        }
        return mapping;
    }

    /**
     * 读取请求中的输入流
     */
    private String readInput(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String temp;
        while ((temp = br.readLine()) != null) {
            sb.append(temp);
        }
        br.close();
        return sb.toString();
    }

    /**
     * 提取请求中的请求参数
     */
    private Map<String, String> extractQueries(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return new HashMap<>(2);
        }

        StringTokenizer st = new StringTokenizer(request.getQueryString(), "&");
        int i;
        Map<String, String> queries = new HashMap<>(2);

        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            i = s.indexOf("=");

            if (i > 0 && s.length() >= i + 1) {
                String name = s.substring(0, i);
                String value = s.substring(i + 1);

                try {
                    name = URLDecoder.decode(name, "UTF-8");
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                queries.put(name, value);

            } else if (i == -1) {
                String name = s;
                String value = "";

                try {
                    name = URLDecoder.decode(name, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                queries.put(name, value);
            }
        }
        return queries;
    }
}
