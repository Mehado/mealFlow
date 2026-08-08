package com.sky.aspect;

import com.sky.annotations.RateLimit;
import com.sky.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 接口限流切面：Redis 固定窗口计数
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 固定窗口计数脚本：INCR + EXPIRE + 超限判断，Redis 原子执行
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
    static {
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
        RATE_LIMIT_SCRIPT.setScriptText(
                "local count = redis.call('incr', KEYS[1]) " +
                        "if count == 1 then redis.call('expire', KEYS[1], tonumber(ARGV[2])) end " +
                        "if count > tonumber(ARGV[1]) then return 0 end " +
                        "return 1"
        );
    }

    @Pointcut("@annotation(rateLimit)")
    public void rateLimitPointcut(RateLimit rateLimit) {
    }

    @Around("rateLimitPointcut(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 取客户端 IP
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = attrs != null ? attrs.getRequest().getRemoteAddr() : "unknown";

        // 2. 拼 key：限流维度 = IP + 方法
        String methodKey = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String key = "rate:limit:" + ip + ":" + methodKey;

        // 3. 执行 Lua 计数，返回 0 = 超限
        Long allow = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(rateLimit.limit()),
                String.valueOf(rateLimit.window()));

        if (allow == null || allow != 1L) {
            log.warn("接口限流生效：{} 超过 {} 次/{}秒", key, rateLimit.limit(), rateLimit.window());
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }
}