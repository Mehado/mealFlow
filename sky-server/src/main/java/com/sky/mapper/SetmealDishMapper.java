package com.sky.mapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询套餐菜品
     * @param dishIds
     * @return
     */

    //
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
}
