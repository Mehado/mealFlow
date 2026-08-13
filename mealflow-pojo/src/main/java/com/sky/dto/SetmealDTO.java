package com.sky.dto;

import com.sky.entity.SetmealDish;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SetmealDTO implements Serializable {

    private Long id;

    //分类id
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    //套餐名称
    @NotBlank(message = "套餐名称不能为空")
    private String name;

    //套餐价格
    @NotNull(message = "套餐价格不能为空")
    @Positive(message = "套餐价格不能小于0")
    private BigDecimal price;

    //状态 0:停用 1:启用
    @NotNull(message = "状态不能为空")
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    //套餐菜品关系
    private List<SetmealDish> setmealDishes = new ArrayList<>();

}
