package com.sky.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 越权校验注解
 * 基于 AOP 在方法执行前校验当前用户是否有权操作目标数据
 *
 * SpEL 表达式引用方法参数名（需开启 -parameters 编译）
 * 示例：
 *   @SelfPermission(targetId = "#id", type = CheckType.NOT_SELF)   — 直接 Long 参数
 *   @SelfPermission(targetId = "#dto.empId", type = CheckType.SELF) — DTO 内嵌字段
 */
@Target(ElementType.METHOD)    // 注解的目标元素类型，表示该注解只能用于方法上
@Retention(RetentionPolicy.RUNTIME)  // 注解的保留策略，表示该注解会在运行时保留
public @interface SelfPermission {    // 定义一个名为 SelfPermission 的注解接口

    /** 校验类型 */
    CheckType type() default CheckType.SELF;

    /** SpEL 表达式，指向待校验的目标用户 ID */
    String targetId();

    /** 校验类型枚举 */
    enum CheckType {
        /** 必须等于当前登录用户 */
        SELF,
        /** 必须不等于当前登录用户 */
        NOT_SELF
    }
}
