package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersCancelDTO implements Serializable {

    @NotNull(message = "订单id不能为空")
    private Long id;
    //订单取消原因
    @NotBlank(message = "订单取消原因不能为空")
    private String cancelReason;

}
