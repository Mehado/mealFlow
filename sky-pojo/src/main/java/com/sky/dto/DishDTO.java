package com.sky.dto;

import com.sky.entity.DishFlavor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {

    private Long id;
    //菜品名称
    @NotBlank(message = "菜品名称不能为空")
    private String name;

    //菜品分类id
    @NotNull(message = "菜品分类不能为空")
    private Long categoryId;

    //菜品价格
    @NotNull(message = "菜品价格不能为空")
    @Positive(message= "菜品价格必须大于0")
    private BigDecimal price;

    //图片
    private String image;

    //描述信息
    private String description;

    //0 停售 1 起售
    @NotNull(message = "菜品状态不能为空")
    private Integer status;

    //口味
    private List<DishFlavor> flavors = new ArrayList<>();

}
