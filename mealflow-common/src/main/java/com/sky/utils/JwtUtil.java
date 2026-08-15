package com.sky.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类，用于生成和解析JWT令牌
 */
public class JwtUtil {

    /**
     * 根据密钥生成SecretKey对象
     * @param secretKey 密钥字符串
     * @return 返回SecretKey对象
     */
    private static SecretKey getSecretKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建JWT令牌
     * @param secretKey 密钥字符串
     * @param ttlMillis 令牌过期时间（毫秒）
     * @param claims 自定义声明内容
     * @return 返回生成的JWT字符串
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 计算过期时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 使用Jwts构建器创建JWT令牌
        return Jwts.builder()
                .claims(claims) // 设置自定义声明
                .signWith(getSecretKey(secretKey)) // 设置签名密钥
                .expiration(exp) // 设置过期时间
                .compact(); // 紧凑序列化
    }

    /**
     * 解析JWT令牌
     * @param secretKey 密钥字符串
     * @param token JWT令牌字符串
     * @return 返回解析后的Claims对象
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 使用Jwts解析器解析JWT令牌
        return Jwts.parser()
                .verifyWith(getSecretKey(secretKey)) // 设置验证密钥
                .build() // 构建解析器
                .parseSignedClaims(token) // 解析签名声明
                .getPayload(); // 获取载荷
    }

}
