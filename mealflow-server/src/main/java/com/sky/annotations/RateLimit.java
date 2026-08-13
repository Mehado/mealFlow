package com.sky.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，配合RateLimitAspect使用，按照IP+方法维度限流
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流次数
     */
    int limit() default 10;

    /**
     * 限流时间，单位：秒
     */
    int window() default 60;
}
