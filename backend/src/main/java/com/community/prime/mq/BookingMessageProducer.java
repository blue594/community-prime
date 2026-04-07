package com.community.prime.mq;

import com.community.prime.dto.BookingMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

// todo
/**
 * 家政预约消息生产者
 *
 * 【面试重点】生产者职责：
 * 1. 构造消息内容（DTO）
 * 2. 发送消息到 RabbitMQ
 * 3. 处理发送异常（降级方案）
 *
 * 【消息发送方式】
 * 1. convertAndSend：简单发送，不关心结果
 * 2. convertAndSend + CorrelationData：带确认机制，可追踪消息
 * 3. convertSendAndReceive：RPC 模式，等待消费者响应
 *
 * 【可靠性保障】
 * - Confirm 机制：确保消息到达交换机
 * - Return 机制：确保消息从交换机到达队列
 * - 异常捕获：发送失败时记录日志，不阻塞主流程
 *
 * @author Community Prime
 * @since 1.0.0
 */
@Slf4j
@Component
public class BookingMessageProducer {

    /**
     * 注入 RabbitTemplate - Spring 提供的 RabbitMQ 操作模板
     * 已在 RabbitMQBookingConfig 中配置 Confirm 和 Return 回调
     */
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 从配置文件读取交换机名称
     */
    @Value("${rabbitmq.booking.exchange:booking.direct.exchange}")
    private String exchange;

    /**
     * 从配置文件读取路由键
     */
    @Value("${rabbitmq.booking.routing-key:booking.create.key}")
    private String routingKey;

    /**
     * 发送预约创建通知消息
     *
     * 【面试点】消息发送流程：
     * 1. 构造消息 DTO，包含完整上下文
     * 2. 生成唯一消息ID（correlationId），用于追踪
     * 3. 调用 rabbitTemplate.convertAndSend 发送
     * 4. 捕获异常，记录日志，不影响主业务
     *
     * 【事务边界】
     * - 消息发送与数据库操作不在同一事务
     * - 数据库提交成功后发送消息，避免消息已发但事务回滚
     * - 极端情况：数据库提交成功但消息发送失败，需要补偿机制
     *
     * @param messageDTO 预约消息 DTO
     * @return true-发送成功, false-发送失败（但主流程继续）
     */
    public boolean sendBookingCreateMessage(BookingMessageDTO messageDTO) {
        if (messageDTO == null) {
            log.warn("[BookingMQ] 消息内容为空，跳过发送");
            return false;
        }

        try {
            // 【面试点】消息发送流程：
            // 1. 构造消息 DTO，包含完整上下文
            // 2. 调用 rabbitTemplate.convertAndSend 发送
            // 3. 捕获异常，记录日志，不影响主业务
            //
            // 【扩展】生产环境可添加 Confirm 机制确保消息到达交换机
            // 需要配置：spring.rabbitmq.publisher-confirm-type=correlated
            // 然后在 RabbitTemplate 中设置 ConfirmCallback

            log.info("[BookingMQ] 发送预约创建消息, bookingId: {}, userId: {}, serviceName: {}",
                    messageDTO.getBookingId(), messageDTO.getUserId(), messageDTO.getServiceName());

            // 发送消息
            // 参数说明：
            // - exchange: 交换机名称
            // - routingKey: 路由键，Direct Exchange 精确匹配
            // - message: 消息内容，会被自动序列化
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    messageDTO
            );

            log.info("[BookingMQ] 消息发送成功, bookingId: {}", messageDTO.getBookingId());
            return true;

        } catch (AmqpException e) {
            // 【面试点】异常处理原则：
            // 1. 记录详细错误日志，包含消息内容（注意脱敏）
            // 2. 返回 false 表示发送失败
            // 3. 不抛出异常，不影响主业务流程
            // 4. 可扩展：将失败消息存入数据库，定时重试

            log.error("[BookingMQ] 消息发送失败, bookingId: {}, error: {}",
                    messageDTO.getBookingId(), e.getMessage(), e);

            // TODO: 降级方案 - 将消息存入数据库，定时任务补偿发送
            // saveFailedMessageToDb(messageDTO);

            return false;
        }
    }

    /**
     * 发送消息（简化版 - 不带 CorrelationData）
     *
     * 适用于不需要追踪消息发送结果的场景
     *
     * @param messageDTO 预约消息 DTO
     */
    public void sendBookingCreateMessageSimple(BookingMessageDTO messageDTO) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, messageDTO);
            log.debug("[BookingMQ] 消息发送成功, bookingId: {}", messageDTO.getBookingId());
        } catch (AmqpException e) {
            log.error("[BookingMQ] 消息发送失败, bookingId: {}, error: {}",
                    messageDTO.getBookingId(), e.getMessage());
        }
    }

    /**
     * 延迟发送消息（用于定时提醒）
     *
     * 【扩展】需要配合延迟队列插件（rabbitmq_delayed_message_exchange）
     * 或使用 TTL + 死信队列实现
     *
     * @param messageDTO  预约消息 DTO
     * @param delayMillis 延迟时间（毫秒）
     */
    public void sendDelayMessage(BookingMessageDTO messageDTO, long delayMillis) {
        // TODO: 实现延迟消息发送
        // 方案1：使用 rabbitmq_delayed_message_exchange 插件
        // 方案2：TTL + 死信队列
        log.info("[BookingMQ] 延迟消息功能待实现，delay: {}ms", delayMillis);
    }
}
