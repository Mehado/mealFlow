package com.sky.aspect;

import com.sky.annotations.SelfPermission;
import com.sky.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * 越权校验切面
 * 拦截所有标注 @SelfPermission 的 Controller 方法，
 * 通过 SpEL 解析目标用户 ID 并与当前登录用户比对
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {

    // SpEL 表达式解析器，用于解析注解中的表达式
    private final ExpressionParser parser = new SpelExpressionParser();
    // 参数名发现器，用于获取方法的参数名称
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    /**
     * 定义切点：拦截所有带有 @SelfPermission 注解的方法
     */
    @Pointcut("@annotation(com.sky.annotations.SelfPermission)")
    public void permissionPointcut() {
    }

    /**
     * 权限校验前置通知
     * @param joinPoint 连接点，可以获取方法相关信息
     */
    @Before("permissionPointcut()")
    public void checkPermission(JoinPoint joinPoint) {
        // 1. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        SelfPermission annotation = signature.getMethod().getAnnotation(SelfPermission.class);

        // 2. 获取方法参数名和参数值
        String[] paramNames = discoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        // 3. 构建 SpEL 上下文（参数名 → 参数值）
        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 4. 解析 SpEL 表达式，获取目标用户 ID
        String expression = annotation.targetId();
        Long targetId = parser.parseExpression(expression).getValue(context, Long.class);
        Long currentId = BaseContext.getCurrentId();

        log.info("权限校验: current={}, target={}, type={}, expression={}",
                currentId, targetId, annotation.type(), expression);

        if (targetId == null || currentId == null) {
            throw new RuntimeException("权限校验失败：无法获取用户 ID");
        }

        // 5. 根据校验类型执行判断
        if (annotation.type() == SelfPermission.CheckType.SELF) {
            // SELF: 目标必须等于当前用户
            if (!targetId.equals(currentId)) {
                throw new RuntimeException("无权操作其他用户的数据");
            }
        } else {
            // NOT_SELF: 目标必须不等于当前用户
            if (targetId.equals(currentId)) {
                throw new RuntimeException("不能操作自己的账号");
            }
        }
    }
}

