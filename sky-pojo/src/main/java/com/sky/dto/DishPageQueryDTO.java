package com.sky.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class DishPageQueryDTO implements Serializable {

    @Min(value = 1, message = "页码不能小于1")
    private int page;

    @Min(value = 1, message = "每页条数不能小于1")
    private int pageSize;

    private String name;

    //分类id
    private Integer categoryId;

    //状态 0表示禁用 1表示启用
    private Integer status;

}
