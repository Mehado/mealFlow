package com.sky.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 放重复提交注解：配合NoRepeatSubmitAspect使用
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {
    /**
     * 锁的过期时间（秒），超过自动释放，防止死锁
     */
    int expire() default 5;
}
