package com.sky.service;

public interface ShopService {
    /**
     * 设置店铺营业状态
     */
    void setStatus(Integer status);

    /**
     * 获取店铺营业状态
     */
    Integer getStatus();
}