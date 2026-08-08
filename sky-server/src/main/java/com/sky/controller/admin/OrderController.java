package com.sky.controller.admin;

import com.sky.annotations.NoRepeatSubmit;
import com.sky.annotations.RequireRole;
import com.sky.constant.RoleConstant;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Tag(name = "订单管理接口")
@RequiredArgsConstructor
public class OrderController {


    private final OrderService orderService;


    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF,RoleConstant.RIDER})
    @GetMapping("/conditionSearch")
    @Operation(summary = "订单搜索")
    public Result<PageResult> conditionSearch(@Valid OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF,RoleConstant.RIDER})
    @GetMapping("/statistics")
    @Operation(summary = "各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF,RoleConstant.RIDER})
    @GetMapping("/details/{id}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    @NoRepeatSubmit
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER})
    @PutMapping("/confirm")
    @Operation(summary = "接单")
    public Result confirm(@Valid @RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    @NoRepeatSubmit
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER})
    @PutMapping("/rejection")
    @Operation(summary = "拒单")
    public Result rejection(@Valid @RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    @NoRepeatSubmit
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER})
    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancel(@Valid @RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    @RequireRole({RoleConstant.OWNER,RoleConstant.RIDER})
    @PutMapping("/delivery/{id}")
    @Operation(summary = "派送订单")
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    @RequireRole({RoleConstant.OWNER,RoleConstant.RIDER})
    @PutMapping("/complete/{id}")
    @Operation(summary = "完成订单")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }

    @RequireRole
    @PutMapping("/simulatePayNotify/{orderNumber}")
    @Operation(summary = "模拟支付成功回调")
    public Result simulatePayNotify(@PathVariable String orderNumber) {
        log.info("模拟支付成功回调，订单号：{}", orderNumber);
        orderService.paySuccess(orderNumber);
        return Result.success();
    }
}
