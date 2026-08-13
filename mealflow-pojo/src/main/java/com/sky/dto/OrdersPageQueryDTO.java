package com.sky.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrdersPageQueryDTO implements Serializable {

    @Min(value = 1, message = "页码不能小于1")
    private int page;

    @Min(value = 1, message = "每页条数不能小于1")
    private int pageSize;

    private String number;

    private  String phone;

    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long userId;

}
