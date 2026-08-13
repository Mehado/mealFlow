package com.sky.annotations;

import com.sky.constant.RoleConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解
 * 标注在Controller方法上，表示该方法需要指定角色权限才能访问
 * 默认仅店主OWNER角色可以访问
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
        String[] value() default {RoleConstant.OWNER};


}
