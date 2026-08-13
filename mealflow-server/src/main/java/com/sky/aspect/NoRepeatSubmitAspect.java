package com.sky.aspect;

import com.sky.annotations.NoRepeatSubmit;
import com.sky.context.BaseContext;
import com.sky.exception.RepeatSubmitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.core.ObjectReadContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 放重复提交切面，使用Redis setIfAbsent 做分布式锁
 */
@Aspect
@Component
@Slf4j
public class NoRepeatSubmitAspect {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //释放锁脚本：值匹配才删除，防止TTL过期后误删别人的锁
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT=new DefaultRedisScript<>();
    static {
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end");
    }

    @Pointcut("@annotation(noRepeatSubmit)")
    public void noRepeatPointcut(NoRepeatSubmit noRepeatSubmit) {}

/**
 * 使用AOP环绕通知实现防止重复提交的功能
 * @param joinPoint 切点，用于获取目标方法的参数、方法名等信息
 * @param noRepeatSubmit 注解对象，包含过期时间等配置
 * @return 目标方法的执行结果
 * @throws Throwable 可能抛出的异常
 */
    @Around("noRepeatPointcut(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint,NoRepeatSubmit noRepeatSubmit) throws Throwable {
        //1.拼key：用户+方法+参数hash
        Long userId= BaseContext.getCurrentId();
        String userKey=userId==null?"anonymous":String.valueOf(userId);
        String methodKey=joinPoint.getSignature().getDeclaringType().getSimpleName()
                  +"."+joinPoint.getSignature().getName();
        int argHash= Arrays.deepHashCode(joinPoint.getArgs());
        String key="repeat:submit:"+userKey+"."+methodKey+"."+argHash;

        //2.抢锁：SETNX+TTL，value是本请求的UUID
        String value = UUID.randomUUID().toString();
        Boolean isLock = stringRedisTemplate.opsForValue().
                setIfAbsent(key, value,
                        noRepeatSubmit.expire(),
                        TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(isLock)) {
            log.warn("重复提交被拦截：{}",key);
            throw new RepeatSubmitException("操作过于频繁，请勿重复提交");
        }
        try {
            return joinPoint.proceed();
        }finally {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key),value);
        }
    }

}
