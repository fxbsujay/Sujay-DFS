package com.susu.dfs.tracker.tomcat.annotation;

import java.lang.annotation.*;

/**
 * <p>Description: Rest风格的Controller</p>
 *
 * @author sujay
 * @version 16:13 2022/8/15
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestController {

    String value() default "";

}
