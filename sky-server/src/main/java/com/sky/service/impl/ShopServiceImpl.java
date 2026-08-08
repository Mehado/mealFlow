package com.sky.service.impl;

import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    private static final String KEY = "SHOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;
/**
 * 设置状态的方法
 * @param status 要设置的状态值，使用Integer类型以支持null值
 */
    public void setStatus(Integer status) {
        redisTemplate.opsForValue().set(KEY, status);
    }


/**
 * 获取状态的方法
 * @return 返回一个Integer类型的状态值，当前固定返回0
 */
    public Integer getStatus() {
        return (Integer) redisTemplate.opsForValue().get(KEY);
    }
}
