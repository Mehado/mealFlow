package com.sky.test;

import com.sky.entity.ShoppingCart;
import com.sky.service.impl.StockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 并发抢库存集成测试：需要本地 Redis 已启动
 * 验证 100 个线程抢 10 个库存，最多只有 10 个成功
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class StockConcurrencyTest {

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @Autowired
    private StockServiceImpl stockService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.opsForValue().set("stock:dish:1", "10");
    }

    @Test
    void concurrentDeduct_shouldNotOversell() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    stockService.deductStock(Collections.singletonList(
                            ShoppingCart.builder().dishId(1L).number(1).build()));
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 库存不足，预期内
                }
            }).start();
        }

        ready.await();
        start.countDown();
        Thread.sleep(2000);

        assertEquals(10, success.get(), "100 线程抢 10 库存，最多只能成功 10 次");
    }
}

