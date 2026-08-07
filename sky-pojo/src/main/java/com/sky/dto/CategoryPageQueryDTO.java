package com.sky.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryPageQueryDTO implements Serializable {

    //页码
    @Min(value = 1,message = "页码不能小于1")
    private int page;

    //每页记录数
    @Min(value = 1,message = "每页记录数不能小于1")
    private int pageSize;

    //分类名称
    private String name;

    //分类类型 1菜品分类  2套餐分类
    private Integer type;

}
