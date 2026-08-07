package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersRejectionDTO implements Serializable {

    @NotNull(message = "订单id不能为空")
    private Long id;

    //订单拒绝原因
    @NotBlank(message = "拒绝原因不能为空")
    private String rejectionReason;

}
