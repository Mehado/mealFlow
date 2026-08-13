package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTask {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final StockService stockService;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(-15);

        List<Orders> ordersList =
                orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, deadline);
        if (ordersList == null || ordersList.isEmpty()) {
            return;
        }
        for (Orders orders : ordersList) {
            // 条件更新：只有还处于待付款才取消，防与 MQ 关单并发
            int rows = orderMapper.cancelByIdIfPendingPayment(
                    orders.getId(), LocalDateTime.now());
            if (rows > 0) {
                // 更新成功才回补库存，天然幂等
                stockService.releaseStock(orderDetailMapper.getByOrderId(orders.getId()));
                log.info("定时关单成功并回补库存：orderId={}", orders.getId());
            } else {
                log.info("定时关单跳过（状态已变更）：orderId={}", orders.getId());
            }
        }
    }
}

