package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存工具类：Cache-Aside 的手动实现
 * 防穿透：空值缓存 NULL_MARK
 * 防击穿：setnx 互斥锁 + 双重检查
 * 防雪崩：随机 TTL
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisCacheClient {

    /** 空值缓存标记：读到它等同于没数据，防止缓存穿透 */
    public static final String NULL_MARK = "NULL";

    private static final String LOCK_PREFIX = "cache:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 写 JSON 缓存，随机 TTL 防雪崩
     * @param key            缓存 key
     * @param value          要缓存的对象
     * @param baseSeconds    基础过期秒数
     * @param jitterSeconds  随机抖动区间，最终 TTL = base + [0, jitter)
     */
    public void setJson(String key, Object value, long baseSeconds, long jitterSeconds) {
        String json = JSON.toJSONString(value);
        long ttl = baseSeconds + ThreadLocalRandom.current().nextLong(jitterSeconds);
        stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttl));
        log.debug("缓存写入：key={}, ttl={}s", key, ttl);
    }

    /**
     * 读 JSON 缓存
     * 命中空值标记时返回 null（调用方按"无缓存"走 DB），避免把 "NULL" 反序列化成脏数据
     */
    public <T> T getJson(String key, TypeReference<T> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || NULL_MARK.equals(json)||"null".equals(json)) {
            return null;
        }
        return JSON.parseObject(json, type);
    }

    /** 删缓存（Cache-Aside 的"先写库后删缓存"） */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            stringRedisTemplate.delete(Arrays.asList(keys));
        }
    }

    /** 尝试获取互斥锁：setnx + 过期时间，原子操作 */
    public boolean tryLock(String key, long expireSeconds) {
        String lockKey = LOCK_PREFIX + key;
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, Thread.currentThread().getName(), Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(ok);
    }

    /** 释放互斥锁：Lua 比较 value，只释放自己持有的锁，防止误删别人的锁 */
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) else return 0 end",
                Long.class);
        stringRedisTemplate.execute(script, Collections.singletonList(lockKey),
                Thread.currentThread().getName());
    }
}
