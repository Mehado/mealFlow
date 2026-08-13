package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 生产者确认配置：
 * confirm 回调 = Broker 是否收到消息
 * returns 回调 = 消息是否成功路由到队列
 */
@Configuration
@Slf4j
public class RabbitMqProducerConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);

        // ack=true：Broker 已接收；ack=false：发送失败（交换机不存在、写入失败等）
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("MQ 发送失败：correlationId={}, cause={}",
                        correlationData == null ? "null":correlationData.getId(), cause);
                // 生产环境在这里落库/告警；本项目关单有 OrderTask 定时兜底，记录日志即可
            } else {
                log.debug("MQ 发送成功：correlationId={}",
                        correlationData == null ? "null":correlationData.getId());
            }
        });

        // 消息发到了交换机，但没有路由到任何队列（比如 routing key 写错）
        template.setReturnsCallback(returned ->
                log.error("MQ 路由失败：exchange={}, routingKey={}, replyText={}, body={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyText(), new String(returned.getMessage().getBody())));
        return template;
    }
}
