package com.sky.mq;

import com.rabbitmq.client.Channel;
import com.sky.config.RabbitMQConfig;
import com.sky.constant.MessageConstant;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 延迟关单消费者：监听关单队列 + 重试队列
 * 幂等：只有"待付款"才关单；失败按 retryCount 重试，超过上限告警丢弃
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderDeadLetterConsumer {

    private final OrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OrderDetailMapper orderDetailMapper;
    private final StockService stockService;

/**
 * 处理延迟关单的监听方法
 * 通过RabbitMQ监听死信队列，处理超时的订单
 * @param orderId 订单ID
 * @param channel RabbitMQ通道
 * @param message 消息对象
 * @throws Exception 可能抛出的异常
 */
    @RabbitListener(queues = {RabbitMQConfig.ORDER_DEAD_QUEUE}) // 监听死信队列
    public void handleTimeoutOrder(Long orderId, Channel channel, Message message) throws Exception {
    // 从消息头中获取重试次数，如果没有则默认为0
        Object retryCountObj = message.getMessageProperties().getHeader("retryCount");
        int retryCount = retryCountObj instanceof Number ? ((Number) retryCountObj).intValue() : 0;
        try {
        // 尝试取消超时订单
            cancelTimeoutOrder(orderId);
        // 确认消息处理成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
        // 记录错误日志，包含订单ID和当前重试次数
            log.error("延迟关单处理失败：orderId={}, retryCount={}", orderId, retryCount, e);
        // 如果重试次数未达到最大值
            if (retryCount < RabbitMQConfig.MAX_RETRY) {
                // 1. 发到重试队列：5 秒后重试，次数 +1
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_RETRY_EXCHANGE,
                        RabbitMQConfig.ORDER_RETRY_ROUTING_KEY,
                        orderId,
                        msg -> {
                            msg.getMessageProperties().setHeader("retryCount", retryCount + 1);
                            msg.getMessageProperties().setExpiration("5000");
                            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return msg;
                        });
                // 2. 当前这条消息确认掉，后续由重试队列负责
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 3. 超过最大重试：丢弃并告警（生产可扩展为进"最终失败队列"或落表人工处理）
                log.error("延迟关单超过最大重试次数，需人工介入：orderId={}", orderId);
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            }
        }
    }

    /** 关单逻辑抽出来，主队列和重试队列共用，保证只写一份 */
    private void cancelTimeoutOrder(Long orderId) {
        int rows = orderMapper.cancelByIdIfPendingPayment(orderId,LocalDateTime.now());
        if (rows>0) {
            List<OrderDetail> details = orderDetailMapper.getByOrderId(orderId);
            stockService.releaseStock(details);
            log.info("MQ延迟关单成功并回补库存：orderId={}", orderId);
        } else {
            log.info("MQ延迟关单跳过：orderId={} 状态已变更", orderId);
        }
    }
}
