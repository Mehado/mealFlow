package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lua 脚本配置类：统一把 src/main/resources/scripts/ 下的 .lua 文件加载成 Bean。
 * 好处：
 * 1. 脚本独立成文件，有语法高亮、可读性好，改动脚本不用改 Java 代码；
 * 2. 启动时 fail-fast，脚本缺失/加载失败直接启动报错，而不是运行时才发现；
 * 3. 各业务类通过构造器注入脚本 Bean，依赖清晰、可复用（如 unlock.lua 被两个切面共用）。
 */
@Configuration
@Slf4j
public class RedisScriptConfig {

    /** 库存预扣：检查 + 扣减原子执行，返回 1=成功 -1=不足 -2=未预热 */
    @Bean
    public DefaultRedisScript<Long> deductStockScript() {
        return loadScript("scripts/deduct_stock.lua");
    }

    /** 库存回补：key 存在才加回 */
    @Bean
    public DefaultRedisScript<Long> releaseStockScript() {
        return loadScript("scripts/release_stock.lua");
    }

    /** 下单幂等令牌消费：检查 + 删除原子执行 */
    @Bean
    public DefaultRedisScript<Long> idempotentTokenScript() {
        return loadScript("scripts/consume_idempotent_token.lua");
    }

    /** 分布式锁释放：值匹配才删除，防止误删别人的锁 */
    @Bean
    public DefaultRedisScript<Long> unlockScript() {
        return loadScript("scripts/unlock.lua");
    }

    /** 接口限流：INCR + 首次设过期 + 超限判断 */
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        return loadScript("scripts/rate_limit.lua");
    }

/**
 * 加载 Redis Lua 脚本并封装为 DefaultRedisScript 对象
 * @param path 脚本文件在类路径中的路径
 * @return 配置好的 DefaultRedisScript 对象，可执行脚本并返回 Long 类型结果
 */
    private DefaultRedisScript<Long> loadScript(String path) {
    // 创建 DefaultRedisScript 实例，用于封装 Redis Lua 脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // 设置脚本执行结果的类型为 Long
        script.setResultType(Long.class);
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
        // 从类路径读取脚本文件内容并设置到 script 对象中
        // 使用 UTF-8 编码读取字节数据并转换为字符串
            script.setScriptText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        // 记录脚本加载成功的日志
            log.info("加载 Lua 脚本：{}", path);
        } catch (IOException e) {
        // 如果加载脚本时发生 IO 异常，包装为 IllegalStateException 抛出
            throw new IllegalStateException("加载 Lua 脚本失败: " + path, e);
        }
    // 返回配置完成的 Redis 脚本对象
        return script;
    }
}
