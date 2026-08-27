package com.sky.test;

import com.alibaba.fastjson.TypeReference;
import com.sky.utils.RedisCacheClient;
import com.sky.vo.DishVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存工具类单侧：重点覆盖“控制缓存”和“互斥锁”两个分支
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheClientTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String,String> valueOperations;

    @InjectMocks
    private RedisCacheClient cacheClient;

    /** 空值标记应返回null：调用方会走DB，而不是反序列化出脏数据*/
    @Test
    void getJson_whenNullMark_shouldReturnNull() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("dish:category:1")).thenReturn(RedisCacheClient.NULL_MARK);
        List<DishVO> result=cacheClient.getJson("dish:category:1",new TypeReference<List<DishVO>>(){});
        assertNull(result);
    }
    /** 缓存不存在则返回null*/
    @Test
    void getJson_whenCacheMiss_shouldReturnNull(){
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("dish:category:1")).thenReturn(null);
        List<DishVO> result=cacheClient.getJson("dish:category:1",new TypeReference<List<DishVO>>(){});
        assertNull(result);
    }
    /** setnx 成功=拿到锁 */
    @Test
    void tryLock_whenSetNxSuccess_shouldReturnTrue() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(
                "cache:lock:dish:1"),anyString(),any(Duration.class))).thenReturn(true);
        assertTrue(cacheClient.tryLock("dish:1",10));
    }
    /** setnx 失败 = 别人持有锁 */
    @Test
    void tryLock_whenSetnxFail_shouldReturnFalse() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("cache:lock:dish:1"), anyString(), any(Duration.class)))
                .thenReturn(false);

        assertFalse(cacheClient.tryLock("dish:1", 10));
    }
}
