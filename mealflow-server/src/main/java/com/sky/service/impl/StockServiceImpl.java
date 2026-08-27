package com.sky.service.impl;

import com.sky.entity.Dish;
import com.sky.entity.OrderDetail;
import com.sky.entity.SetmealDish;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存服务实现：Lua 脚本保证"检查+扣减"原子性
 * 购物车/订单明细统一"展开"成菜品维度聚合，套餐按 setmeal_dish 拆解
 * 返回值约定：1=成功，-1=库存不足，-2=未预热
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StringRedisTemplate stringRedisTemplate; // Redis字符串操作模板
    private final DishMapper dishMapper; // 菜品数据访问层
    private final SetmealDishMapper setmealDishMapper; // 套餐菜品数据访问层
    /** 预扣脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> deductStockScript;
    /** 回补脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> releaseStockScript;

    /**
     * 生成菜品库存的Redis键
     * @param dishId 菜品ID
     * @return Redis键字符串
     */
    private String stockKey(Long dishId) {
        return "stock:dish:" + dishId;
    }

    /**
     * 从购物车列表中扣减库存
     * @param items 购物车项列表
     */
    public void deductStock(List<ShoppingCart> items) {
        // 先展开成"菜品 -> 数量"，聚合后统一扣，避免同一菜品多次扣减
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(collectDishQtyFromCart(items).entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Long, Integer> entry = entries.get(i);
            Long result = stringRedisTemplate.execute(
                    deductStockScript,
                    Collections.singletonList(stockKey(entry.getKey())),
                    String.valueOf(entry.getValue()));

            if (result == null) {
                throw new OrderBusinessException("库存扣减异常，请稍后重试");
            }
            if (result == -2L) {
                throw new OrderBusinessException("菜品库存未预热，请稍后重试");
            }
            if (result == -1L) {
                // 回补前面已经扣掉的部分，保证整体不欠库存
                for (int j = 0; j < i; j++) {
                    Map.Entry<Long, Integer> prev = entries.get(j);
                    release(prev.getKey(), prev.getValue());
                }
                throw new OrderBusinessException("菜品库存不足，请减少数量或更换套餐");
            }
        }
    }

    /**
     * 从订单明细中回补库存
     * @param items 订单明细列表
     */
    public void releaseStock(List<OrderDetail> items) {
        for (Map.Entry<Long, Integer> e : collectDishQtyFromDetail(items).entrySet()) {
            release(e.getKey(), e.getValue());
        }
    }

    /**
     * 从购物车中回补库存
     * @param items 购物车项列表
     */
    public void releaseStockByCart(List<ShoppingCart> items) {
        for (Map.Entry<Long, Integer> e : collectDishQtyFromCart(items).entrySet()) {
            release(e.getKey(), e.getValue());
        }
    }

    /**
     * 预热所有菜品库存到Redis
     */
    public void warmUpAll() {
        List<Dish> dishes = dishMapper.list(new Dish());
        for (Dish dish : dishes) {
            if (dish.getStock() != null) {
                stringRedisTemplate.opsForValue().set(stockKey(dish.getId()), String.valueOf(dish.getStock()));
            }
        }
        log.info("菜品库存预热完成，共 {} 个菜品", dishes.size());
    }

    /**
     * 管理端新增/修改菜品后同步 Redis 库存
     * @param dishId 菜品ID
     * @param stock 最新库存值
     */
    public void syncStock(Long dishId, Integer stock) {
        if (dishId == null || stock == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(stockKey(dishId), String.valueOf(stock));
        log.info("同步菜品库存 Redis 缓存：dishId={}, stock={}", dishId, stock);
    }

    /** 购物车 -> 菜品维度数量聚合（单品直接记，套餐拆解） */
    private Map<Long, Integer> collectDishQtyFromCart(List<ShoppingCart> items) {
        Map<Long, Integer> qty = new HashMap<>();
        for (ShoppingCart item : items) {
            if (item.getDishId() != null) {
                qty.merge(item.getDishId(), item.getNumber(), Integer::sum);
            } else if (item.getSetmealId() != null) {
                mergeSetmeal(qty, item.getSetmealId(), item.getNumber());
            }
        }
        return qty;
    }

    /** 订单明细 -> 菜品维度数量聚合（回补用，与扣减对称） */
    private Map<Long, Integer> collectDishQtyFromDetail(List<OrderDetail> items) {
        Map<Long, Integer> qty = new HashMap<>();
        for (OrderDetail item : items) {
            if (item.getDishId() != null) {
                qty.merge(item.getDishId(), item.getNumber(), Integer::sum);
            } else if (item.getSetmealId() != null) {
                mergeSetmeal(qty, item.getSetmealId(), item.getNumber());
            }
        }
        return qty;
    }

    /** 套餐拆解：一份套餐里每个菜品占 copies 份 */
    private void mergeSetmeal(Map<Long, Integer> qty, Long setmealId, Integer setmealCount) {
        List<SetmealDish> dishes = setmealDishMapper.getBySetmealId(setmealId);
        if (dishes == null || dishes.isEmpty()) {
            throw new OrderBusinessException("套餐内没有可售菜品：" + setmealId);
        }
        for (SetmealDish sd : dishes) {
            qty.merge(sd.getDishId(), sd.getCopies() * setmealCount, Integer::sum);
        }
    }

/**
 * 释放指定菜品库存的方法
 * @param dishId 菜品ID
 * @param number 需要释放的库存数量
 */
    private void release(Long dishId, Integer number) {
    // 使用Redis脚本执行器执行释放库存的Lua脚本
    // 参数说明:
    // 1. releaseStockScript: 从 .lua 文件加载的脚本 Bean
    // 2. Collections.singletonList(stockKey(dishId)): Redis键列表，包含菜品库存键
    // 3. String.valueOf(number): 需要释放的库存数量，转换为字符串格式
        stringRedisTemplate.execute(
                releaseStockScript,
                Collections.singletonList(stockKey(dishId)),
                String.valueOf(number));
    }
}
