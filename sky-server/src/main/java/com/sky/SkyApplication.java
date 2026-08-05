package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot应用程序主类
 * 该类是整个应用程序的入口点，包含多个功能注解来启用不同的特性
 */
@SpringBootApplication // 标记为Spring Boot应用程序的主类，自动配置组件扫描
@EnableTransactionManagement //开启注解方式的事务管理，允许使用@Transactional注解
@Slf4j // Lombok注解，自动生成日志器变量，可通过log直接使用
@EnableCaching // 启用Spring的缓存功能，允许使用@Cacheable等缓存注解
@EnableScheduling//开启定时任务
public class SkyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyApplication.class, args);
        log.info("SkyApplication start success");
    }
}
