package com.sky.service;

import com.sky.entity.OrderDetail;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * 库存服务：Redis 预扣 + 回补
 */
public interface StockService {

    /** 下单前预扣库存；不足/未预热抛异常 */
    void deductStock(List<ShoppingCart> items);

    /** 订单取消/超时关单后回补库存（按订单明细） */
    void releaseStock(List<OrderDetail> items);

    /** 下单失败时按购物车回补（购物车和订单明细字段同构，转一下即可） */
    void releaseStockByCart(List<ShoppingCart> items);

    /** 启动时把 DB 库存预热到 Redis */
    void warmUpAll();

    /** 管理端新增/修改菜品后同步 Redis 库存（避免与预热值不一致） */
    void syncStock(Long dishId, Integer stock);
}

