package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 密码加密配置类
 * 使用BCrypt加密算法，提供加盐慢哈希保护
 */
@Configuration
@Slf4j
public class PasswordEncoderConfiguration {

    /**
     * 创建并配置密码编码器Bean
     * 该方法用于返回一个BCryptPasswordEncoder实例，用于密码的加密与验证
     * @return 返回BCryptPasswordEncoder实例，用于Spring Security的密码加密处理
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("初始化BCrypt密码编辑器");  // 记录日志信息，表明正在初始化BCrypt密码编码器
        return new BCryptPasswordEncoder();  // 创建并返回BCryptPasswordEncoder实例
    }
}