package com.sky.config;

import com.sky.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动完成后把 DB 库存预热到 Redis
 * 这是一个 Spring Boot 应用启动任务组件，用于在应用启动后执行库存预热操作
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockWarmUpRunner implements ApplicationRunner {

    // 注入 StockService 服务，用于执行库存预热操作
    private final StockService stockService;

    /**
     * ApplicationRunner 接口的实现方法
     * 在应用启动完成后自动执行，用于执行库存预热到 Redis 的操作
     * @param args 应用启动参数，此处未使用
     */
    public void run(ApplicationArguments args) {
        try {
            // 调用 stockService 的 warmUpAll 方法，将所有库存数据预热到 Redis
            stockService.warmUpAll();
        } catch (Exception e) {
            log.warn("库存预热失败（Redis未启动？），下单时将提示'库存未预热'"+ e);
        }
    }
}
