package com.susu.dfs.tracker.tomcat.annotation;

import java.lang.annotation.*;

/**
 * <p>Description: 请求参数JSON格式</p>
 *
 * @author sujay
 * @version 16:13 2022/8/15
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
}
