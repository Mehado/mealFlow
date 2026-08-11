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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 延迟关单消费者：监听关单队列，订单超时未支付则取消
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderDeadLetterConsumer {

    private final OrderMapper orderMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_DEAD_QUEUE)
    public void handleTimeoutOrder(Long orderId, Channel channel, Message message) throws Exception {
        try {
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
            // 手动 ack：处理成功才确认，避免消息丢失被重投
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("MQ延迟关单失败：orderId={}", orderId, e);
            // 处理失败：不确认（requeue=false，丢弃并记录；生产往往进重试/死信）
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}