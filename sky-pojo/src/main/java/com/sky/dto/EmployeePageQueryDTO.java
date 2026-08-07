package com.sky.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeePageQueryDTO implements Serializable {

    //员工姓名
    private String name;

    //页码
    @Min(value = 1, message = "页码必须大于等于1")
    private int page = 1;

    //每页显示记录数
    @Min(value = 1, message = "每页显示记录数必须大于等于1")
    private int pageSize = 10;

}
