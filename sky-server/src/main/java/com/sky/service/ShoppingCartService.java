package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

/**
 * 向购物车添加商品的方法
 * 该方法接收一个ShoppingCartDTO对象作为参数，用于将商品信息添加到购物车中
 *
 * @param shoppingCartDTO 购物车数据传输对象，包含商品ID、数量、价格等信息
 */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询购物车列表的方法
     * 该方法返回一个List<ShoppingCart>对象，用于展示购物车中的商品列表
     * @return 购物车列表
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车的方法
     */
    void cleanShoppingCart();

    /**
     * 删除购物车中一个商品的方法
     * @param shoppingCartDTO
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
