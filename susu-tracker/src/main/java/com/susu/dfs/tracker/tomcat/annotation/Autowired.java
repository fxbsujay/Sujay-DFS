package com.susu.dfs.tracker.tomcat.annotation;

import java.lang.annotation.*;


/**
 * <p>Description: 自动注入</p>
 *
 * @author sujay
 * @version 16:13 2022/8/15
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

    String value() default "";

}
