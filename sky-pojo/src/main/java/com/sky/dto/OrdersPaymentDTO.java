package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class OrdersPaymentDTO implements Serializable {
    //订单号
    @NotBlank(message = "订单号不能为空")
    private String orderNumber;

    //付款方式
    @NotNull(message = "付款方式不能为空")
    private Integer payMethod;

}
