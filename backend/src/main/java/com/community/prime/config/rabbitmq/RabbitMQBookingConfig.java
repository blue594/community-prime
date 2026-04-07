package com.community.prime.config.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// todo
/**
 * RabbitMQ 家政预约模块配置类
 *
 * 【面试重点】RabbitMQ 核心概念：
 * 1. Exchange（交换机）：接收生产者消息，根据路由规则分发到队列
 *    - Direct Exchange：精确匹配路由键（routing key）
 *    - Topic Exchange：模式匹配路由键（支持 * 和 # 通配符）
 *    - Fanout Exchange：广播到所有绑定队列
 *    - Headers Exchange：根据消息头属性匹配
 *
 * 2. Queue（队列）：存储消息的缓冲区，消费者从这里取消息
 *
 * 3. Binding（绑定）：交换机和队列之间的关联关系，包含路由键
 *
 * 4. Routing Key（路由键）：生产者发送消息时指定，交换机根据它决定消息去向
 *
 * 【本配置说明】
 * - 使用 Direct Exchange（直连交换机），精确匹配路由键
 * - 队列持久化（durable=true），RabbitMQ重启后队列不丢失
 * - 交换机持久化，RabbitMQ重启后交换机不丢失
 */
@Slf4j
@Configuration
public class RabbitMQBookingConfig {

    /**
     * 从 application.yml 读取配置值
     * 使用 @Value 注解读取 rabbitmq.booking 前缀的配置
     */
    @Value("${rabbitmq.booking.exchange:booking.direct.exchange}")
    private String bookingExchange;

    @Value("${rabbitmq.booking.queue:booking.create.notice.queue}")
    private String bookingQueue;

    @Value("${rabbitmq.booking.routing-key:booking.create.key}")
    private String bookingRoutingKey;

    /**
     * 创建直连交换机（Direct Exchange）
     *
     * 【面试点】Direct Exchange 特点：
     * - 消息的路由键（routing key）必须与绑定的路由键完全匹配
     * - 适用于点对点精确路由场景
     * - 本场景：预约创建消息只发送到通知队列
     *
     * @return DirectExchange 实例
     */
    @Bean
    public DirectExchange bookingDirectExchange() {
        // durable=true: 交换机持久化，RabbitMQ重启后不丢失
        // autoDelete=false: 没有队列绑定时不自动删除
        return new DirectExchange(bookingExchange, true, false);
    }

    /**
     * 创建队列 - 存储预约创建通知消息
     *
     * 【面试点】队列参数说明：
     * - durable=true: 队列持久化，RabbitMQ重启后队列和消息不丢失
     * - exclusive=false: 不排他，多个消费者可以连接
     * - autoDelete=false: 没有消费者时不自动删除
     *
     * 【扩展】死信队列配置（可选）：
     * 可以添加 x-dead-letter-exchange 和 x-dead-letter-routing-key 参数
     * 当消息被拒绝或过期时，进入死信队列做后续处理
     *
     * @return Queue 实例
     */
    @Bean
    public Queue bookingCreateNoticeQueue() {
        return QueueBuilder.durable(bookingQueue)
                // 设置队列消息过期时间（可选，单位毫秒）
                // .withArgument("x-message-ttl", 86400000) // 24小时
                // 设置队列最大长度（可选）
                // .withArgument("x-max-length", 10000)
                .build();
    }

    /**
     * 创建绑定关系 - 将队列绑定到交换机
     *
     * 【面试点】Binding 作用：
     * - 建立 Exchange 和 Queue 之间的关联
     * - 指定 routing key，Exchange 根据它路由消息
     * - 一个 Exchange 可以绑定多个 Queue，实现消息分发
     *
     * @param bookingCreateNoticeQueue 上面定义的队列
     * @param bookingDirectExchange    上面定义的交换机
     * @return Binding 实例
     */
    @Bean
    public Binding bookingCreateBinding(Queue bookingCreateNoticeQueue, DirectExchange bookingDirectExchange) {
        return BindingBuilder
                // 绑定队列
                .bind(bookingCreateNoticeQueue)
                // 到交换机
                .to(bookingDirectExchange)
                // 使用指定的路由键
                .with(bookingRoutingKey);
    }

    /**
     * 配置 RabbitTemplate - 生产者发送消息的工具类
     *
     * 【面试重点】消息可靠投递机制：
     * 1. Confirm 机制（确认消息到达交换机）：
     *    - 消息成功到达 Exchange，触发 confirm 回调（ack=true）
     *    - 消息未到达 Exchange，触发 confirm 回调（ack=false）
     *
     * 2. Return 机制（确认消息从交换机到达队列）：
     *    - 消息无法路由到任何队列时，触发 return 回调
     *    - 可以记录日志或做补偿处理
     *
     * 【事务 vs Confirm】
     * - 事务：同步阻塞，性能差，不推荐
     * - Confirm：异步非阻塞，性能好，推荐
     *
     * 【Spring Boot 2.7.x 说明】
     * 本版本使用基础发送模式，如需 Confirm 机制，需：
     * 1. 添加配置：spring.rabbitmq.publisher-confirm-type=correlated
     * 2. 实现 RabbitTemplate.ConfirmCallback 接口
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 配置好的 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // ==================== 基础配置 ====================
        // 使用默认的 SimpleMessageConverter，支持 Serializable
        // 生产环境建议使用 Jackson2JsonMessageConverter，支持 JSON 序列化
        // rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        // ==================== 扩展：Confirm 回调配置示例 ====================
        // 如需开启 Confirm 机制，取消下面注释并配置 application.yml：
        // spring.rabbitmq.publisher-confirm-type=correlated
        /*
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("[RabbitMQ Confirm] 消息成功到达交换机");
            } else {
                log.error("[RabbitMQ Confirm] 消息未到达交换机, cause: {}", cause);
            }
        });
        */

        // ==================== 扩展：Return 回调配置示例 ====================
        // 消息无法路由到队列时触发
        /*
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error("[RabbitMQ Return] 消息路由失败, exchange: {}, routingKey: {}",
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        });
        */

        return rabbitTemplate;
    }

    /**
     * 打印配置信息（调试用）
     * 应用启动时会输出 RabbitMQ 配置信息
     */
    @Bean
    public String logRabbitMQConfig() {
        log.info("========================================");
        log.info("[RabbitMQ] 家政预约模块配置加载成功");
        log.info("[RabbitMQ] 交换机: {}", bookingExchange);
        log.info("[RabbitMQ] 队列: {}", bookingQueue);
        log.info("[RabbitMQ] 路由键: {}", bookingRoutingKey);
        log.info("========================================");
        return "RabbitMQ Config Loaded";
    }
}
