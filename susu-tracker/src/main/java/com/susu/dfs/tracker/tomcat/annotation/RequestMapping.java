package com.susu.dfs.tracker.tomcat.annotation;

import java.lang.annotation.*;

/**
 * <p>Description: 请求映射关系</p>
 *
 * @author sujay
 * @version 16:13 2022/8/15
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    /**
     * 请求路径
     *
     * @return 请求路径
     */
    String value() default "";

    /**
     * 请求方式
     *
     * @return 请求方式
     */
    String method() default "GET";
}
