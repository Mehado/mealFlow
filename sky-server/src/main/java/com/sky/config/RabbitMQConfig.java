package com.sky.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置：下单延迟关单
 * 延迟队列TTL=15分钟，消息过期->死信交换机->关单队列->消费者关单
 */
@Configuration
public class RabbitMQConfig {
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";
    public static final String ORDER_DEAD_QUEUE = "order.dead.queue";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.dead";
    public static final String ORDER_RETRY_EXCHANGE = "order.retry.exchange";
    public static final String ORDER_RETRY_QUEUE = "order.retry.queue";
    public static final String ORDER_RETRY_ROUTING_KEY = "order.retry";

    /**消费失败最大重试次数*/
    public static final int MAX_RETRY = 3;

    /**
     * 下单直连交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    /**
     * 延迟队列：消息存活15分钟，过期转头死信交换机
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 15 * 1000);//15分钟（毫秒）
        args.put("x-dead-letter-exchange", ORDER_DEAD_EXCHANGE);//过期后发到哪个交换机
        args.put("x-dead-letter-routing-key", ORDER_DEAD_ROUTING_KEY);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange orderDeadExchange() {
        return new DirectExchange(ORDER_DEAD_EXCHANGE);
    }

    /**
     * 关单队列：死信消息最终落到这里
     */
    @Bean
    public Queue orderDeadQueue() {
        return new Queue(ORDER_DEAD_QUEUE, true);
    }

    /**
     * 绑定：下单交换机->延迟队列
     */
    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 绑定：死信交换机->关单队列
     */
    @Bean
    public Binding orderDeadBinding() {
        return BindingBuilder.bind(orderDeadQueue())
                .to(orderDeadExchange())
                .with(ORDER_DEAD_ROUTING_KEY);
    }
    /**重试交换机：消费失败的消息发达这里，5秒后重新投递*/
    @Bean
    public DirectExchange orderRetryExchange() {
        return new DirectExchange(ORDER_RETRY_EXCHANGE);
    }
    /**
     * 重试队列：只做“暂存”。真正的处理逻辑还是回到消费者
     */
    @Bean
    public Queue orderRetryQueue() {
        return new Queue(ORDER_RETRY_QUEUE, true);
    }
/**
 * 创建一个绑定，将重试队列与重试交换器关联起来，并指定路由键
 *
 * @return Binding 返回一个Binding对象，表示队列、交换器和路由键之间的绑定关系
 */
    @Bean
    public Binding orderRetryBinding() {
        return BindingBuilder.bind(orderRetryQueue())
                .to(orderRetryExchange())
                .with(ORDER_RETRY_ROUTING_KEY);
    }

}
