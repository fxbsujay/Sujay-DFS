package com.susu.dfs.tracker.tomcat.servlet;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description: 组件提供者</p>
 *
 * @author sujay
 * @version 16:18 2022/8/15
 */
public class DispatchComponentProvider {

    public static volatile DispatchComponentProvider INSTANCE = null;

    /**
     * 组件集合
     */
    private final Map<String, Object> components = new HashMap<>();

    public static DispatchComponentProvider getInstance() {
        if (INSTANCE == null) {
            synchronized (DispatchComponentProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DispatchComponentProvider();
                }
            }
        }
        return INSTANCE;
    }

    public void addComponent(Object... objs) {
        for (Object obj : objs) {
            components.put(obj.getClass().getSimpleName(), obj);
        }
    }

    public Object getComponent(String key) {
        return components.get(key);
    }
}