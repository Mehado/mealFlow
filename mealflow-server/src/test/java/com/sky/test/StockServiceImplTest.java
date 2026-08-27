package com.sky.test;

import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.service.impl.StockServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 库存服务单测：Redis execute 的返回值和业务分支的对应关系
 */
@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<Long> deductStockScript;

    @Mock
    private DefaultRedisScript<Long> releaseStockScript;

    @InjectMocks
    private StockServiceImpl stockService;

    private static final String KEY="stock:dish:1";
    /** Lua 返回 -1 = 库存不足，应抛业务异常 */
    @Test
    void deductStock_whenStockNotEnough_shouldThrow() {
        when(stringRedisTemplate.execute(any(RedisScript.class),anyList(),any(String.class)))
                .thenReturn(-1L);

        ShoppingCart item = ShoppingCart.builder().dishId(1L).number(5).build();

        assertThrows(OrderBusinessException.class,
                () -> stockService.deductStock(Collections.singletonList(item)));
    }

    /** Lua 返回 1 = 扣减成功，不抛异常 */
    @Test
    void deductStock_whenSuccess_shouldNotThrow() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(String.class))).thenReturn(1L);

        ShoppingCart item = ShoppingCart.builder().dishId(1L).number(1).build();

        stockService.deductStock(Collections.singletonList(item));
    }
}

