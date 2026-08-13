package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    public void saveWithFlavor(DishDTO dishDTO);

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据id批量删除菜品
     * @param ids
     * @return
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询菜品和对应口味信息
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 更新菜品和对应口味信息
     * @param dishDTO
     * @return
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> list(Long categoryId);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);


    /**
     * 根据菜品id起售停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}
