package com.sky.mq;

import com.rabbitmq.client.Channel;
import com.sky.config.RabbitMQConfig;
import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    @RabbitListener(queues = {RabbitMQConfig.ORDER_DEAD_QUEUE, RabbitMQConfig.ORDER_RETRY_QUEUE})
    public void handleTimeoutOrder(Long orderId, Channel channel, Message message) throws Exception {
        Object retryCountObj = message.getMessageProperties().getHeader("retryCount");
        int retryCount = retryCountObj instanceof Number ? ((Number) retryCountObj).intValue() : 0;
        try {
            cancelTimeoutOrder(orderId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("延迟关单处理失败：orderId={}, retryCount={}", orderId, retryCount, e);
            if (retryCount < RabbitMQConfig.MAX_RETRY) {
                // 1. 发到重试队列：5 秒后重试，次数 +1
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_RETRY_EXCHANGE,
                        RabbitMQConfig.ORDER_RETRY_ROUTING_KEY,
                        orderId,
                        msg -> {
                            msg.getMessageProperties().setHeader("retryCount", retryCount + 1);
                            msg.getMessageProperties().setExpiration("5000");
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
        Orders orderDB = orderMapper.getById(orderId);
        // 幂等：只有还是"待付款"才关单；已支付/已取消则跳过
        if (orderDB != null && Orders.PENDING_PAYMENT.equals(orderDB.getStatus())) {
            orderDB.setStatus(Orders.CANCELLED);
            orderDB.setCancelReason(MessageConstant.ORDER_TIMEOUT_CANCEL);
            orderDB.setCancelTime(LocalDateTime.now());
            orderMapper.update(orderDB);
            log.info("MQ延迟关单成功：orderId={}", orderId);
        } else {
            log.info("MQ延迟关单跳过：orderId={} 状态已变更", orderId);
        }
    }
}
