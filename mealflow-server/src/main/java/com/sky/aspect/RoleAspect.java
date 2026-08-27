package com.sky.aspect;

import com.sky.annotations.RequireRole;
import com.sky.context.BaseContext;
import com.sky.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 角色权限切面类
 * 使用 AOP 实现基于注解的角色权限控制
 */
@Aspect
@Component
@Slf4j
public class RoleAspect {

    // 切点：凡是带 @RequireRole 注解的方法
    @Pointcut("@annotation(com.sky.annotations.RequireRole)")
    public void rolePointcut() {
    }

    // 通知：在这些方法执行之前检查角色
    @Before("rolePointcut()")
    public void checkRole(JoinPoint joinPoint) {
        // 1. 通过连接点拿到"正在被调用的方法"，反射读它上面的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);

        // 2. 取出允许访问的角色数组，例如 ["OWNER", "CASHIER"]
        String[] allowedRoles = requireRole.value();

        // 3. 从 BaseContext 取当前登录员工的角色
        String currentRole = BaseContext.getRole();
        log.info("角色校验: current={}, allowed={}", currentRole, Arrays.toString(allowedRoles));

        // 4. 角色为空 或 不在允许列表 → 拒绝（fail-closed 默认拒绝）
        if (currentRole == null || !Arrays.asList(allowedRoles).contains(currentRole)) {
            throw new PermissionDeniedException("当前角色无权访问该接口");
        }
    }
}