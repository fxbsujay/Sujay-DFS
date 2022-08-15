package com.susu.dfs.tracker.tomcat.annotation;

import java.lang.annotation.*;

/**
 * <p>Description: 路径参数</p>
 *
 * @author sujay
 * @version 16:13 2022/8/15
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

    /**
     * 参数值
     *
     * @return 参数值
     */
    String value() default "";

}
