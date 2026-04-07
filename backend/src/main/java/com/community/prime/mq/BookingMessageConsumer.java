package com.community.prime.mq;

import com.community.prime.dto.BookingMessageDTO;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 家政预约消息消费者
 *
 * 【面试重点】消费者核心职责：
 * 1. 监听队列，接收消息
 * 2. 处理业务逻辑（发送短信、站内信等）
 * 3. 手动 ACK 确认消息处理结果
 * 4. 异常处理和重试机制
 *
 * 【@RabbitListener 注解说明】
 * - queues: 监听的队列名称
 * - concurrency: 并发消费者数量，"3-10"表示最少3个，最多10个
 * - ackMode: 确认模式，MANUAL=手动确认，AUTO=自动确认，NONE=不确认
 *
 * 【手动 ACK 模式】
 * - basicAck: 成功处理，消息从队列删除
 * - basicNack: 处理失败，可选择是否重新入队
 * - basicReject: 拒绝单条消息，可选择是否重新入队
 *
 * @author Community Prime
 * @since 1.0.0
 */
@Slf4j
@Component
@RabbitListener(
        queues = "${rabbitmq.booking.queue:booking.create.notice.queue}",
        concurrency = "1-5",  // 最小1个，最大5个并发消费者
        ackMode = "MANUAL"    // 手动确认模式
)
public class BookingMessageConsumer {

    /**
     * 幂等性控制 - 已处理消息ID集合
     * 【面试点】防止消息重复消费：
     * 1. 使用 Set 记录已处理的消息ID（实际生产用 Redis）
     * 2. 处理前检查是否已存在
     * 3. 处理成功后加入集合
     *
     * 生产环境建议使用 Redis：
     * - SETNX booking:consume:{messageId} 1 EX 86400
     * - 原子操作，设置过期时间
     */
    private final ConcurrentHashMap<String, Boolean> processedMessages = new ConcurrentHashMap<>();

    /**
     * 处理预约创建消息
     *
     * 【面试重点】消费流程：
     * 1. 接收消息（自动反序列化为 DTO）
     * 2. 幂等性校验（防止重复消费）
     * 3. 业务处理（发送通知）
     * 4. 手动 ACK 确认
     * 5. 异常处理（NACK 或重试）
     *
     * @param messageDTO 预约消息 DTO
     * @param message    原始消息对象（包含消息属性）
     * @param channel    RabbitMQ 通道（用于手动 ACK）
     */
    @RabbitHandler
    public void handleBookingCreateMessage(BookingMessageDTO messageDTO,
                                            Message message,
                                            Channel channel) {
        // 获取消息唯一标识（deliveryTag 用于 ACK）
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        log.info("[BookingConsumer] 收到消息, deliveryTag: {}, messageId: {}, bookingId: {}, userId: {}",
                deliveryTag, messageId, messageDTO.getBookingId(), messageDTO.getUserId());

        try {
            // ==================== 1. 幂等性校验 ====================
            // 【面试点】为什么要幂等性？
            // - 网络抖动可能导致消息重复投递
            // - 消费者处理成功但 ACK 失败，消息会重新入队
            // - 必须保证同一消息只处理一次

            if (isDuplicate(messageId)) {
                log.warn("[BookingConsumer] 消息重复消费，直接确认, messageId: {}", messageId);
                basicAck(channel, deliveryTag);
                return;
            }

            // ==================== 2. 业务处理 ====================
            // 模拟发送通知（短信 + 站内信）
            processNotification(messageDTO);

            // ==================== 3. 记录已处理 ====================
            markAsProcessed(messageId);

            // ==================== 4. 手动 ACK ====================
            // 【面试点】basicAck 参数说明：
            // - deliveryTag: 消息的唯一标识
            // - multiple: false=只确认当前消息，true=确认该 deliveryTag 之前的所有消息
            basicAck(channel, deliveryTag);

            log.info("[BookingConsumer] 消息处理成功, bookingId: {}", messageDTO.getBookingId());

        } catch (Exception e) {
            // ==================== 异常处理 ====================
            // 【面试点】消费异常处理策略：
            // 1. 记录详细错误日志
            // 2. 根据重试次数决定：重试 / 丢弃 / 转入死信队列
            // 3. 不重新入队（避免无限循环），可转入死信队列人工处理

            log.error("[BookingConsumer] 消息处理失败, bookingId: {}, error: {}",
                    messageDTO.getBookingId(), e.getMessage(), e);

            handleConsumeError(channel, deliveryTag, message, e);
        }
    }

    /**
     * 处理通知逻辑
     *
     * 【模拟实现】实际项目接入短信平台和站内信系统
     *
     * @param messageDTO 预约消息
     * @throws InterruptedException 模拟处理耗时
     */
    private void processNotification(BookingMessageDTO messageDTO) throws InterruptedException {
        log.info("[BookingConsumer] 开始处理通知, bookingId: {}, serviceName: {}",
                messageDTO.getBookingId(), messageDTO.getServiceName());

        // ==================== 1. 发送短信通知 ====================
        // 【实际接入】调用阿里云/腾讯云短信服务
        // SmsUtils.sendSms(messageDTO.getUserPhone(), messageDTO.generateSmsContent());

        String smsContent = messageDTO.generateSmsContent();
        log.info("[BookingConsumer] 【模拟短信通知】手机号: {}, 内容: {}",
                messageDTO.getUserPhone() != null ? messageDTO.getUserPhone() : "未设置", smsContent);

        // 模拟短信发送耗时
        TimeUnit.MILLISECONDS.sleep(100);

        // ==================== 2. 发送站内信 ====================
        // 【实际接入】保存到数据库，用户登录后查看
        // SiteMessageService.save(messageDTO.getUserId(), messageDTO.generateSiteMessageContent());

        String siteContent = messageDTO.generateSiteMessageContent();
        log.info("[BookingConsumer] 【模拟站内信通知】用户ID: {}, 内容: {}",
                messageDTO.getUserId(), siteContent);

        // 模拟站内信保存耗时
        TimeUnit.MILLISECONDS.sleep(50);

        // ==================== 3. 其他通知渠道 ====================
        // 可扩展：微信推送、APP推送、邮件等

        log.info("[BookingConsumer] 通知处理完成, bookingId: {}", messageDTO.getBookingId());
    }

    /**
     * 幂等性检查
     *
     * @param messageId 消息ID
     * @return true-已处理, false-未处理
     */
    private boolean isDuplicate(String messageId) {
        if (messageId == null) {
            return false;
        }
        return processedMessages.containsKey(messageId);
    }

    /**
     * 标记消息已处理
     *
     * @param messageId 消息ID
     */
    private void markAsProcessed(String messageId) {
        if (messageId != null) {
            processedMessages.put(messageId, true);
        }
    }

    /**
     * 确认消息
     *
     * @param channel     RabbitMQ 通道
     * @param deliveryTag 消息标识
     */
    private void basicAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("[BookingConsumer] ACK 失败, deliveryTag: {}, error: {}", deliveryTag, e.getMessage());
        }
    }

    /**
     * 处理消费异常
     *
     * 【面试点】异常处理策略：
     * 1. 不重试：直接 NACK，消息丢弃或转入死信队列
     * 2. 有限重试：记录重试次数，超过阈值后丢弃
     * 3. 无限重试：NACK 并重新入队（可能阻塞队列，不推荐）
     *
     * @param channel     RabbitMQ 通道
     * @param deliveryTag 消息标识
     * @param message     原始消息
     * @param e           异常
     */
    private void handleConsumeError(Channel channel, long deliveryTag, Message message, Exception e) {
        try {
            // 获取重试次数（RabbitMQ 的 x-death 头信息）
            Integer retryCount = getRetryCount(message);

            if (retryCount < 3) {
                // 重试次数 < 3，重新入队（延迟重试）
                log.warn("[BookingConsumer] 消费失败，重新入队重试, deliveryTag: {}, retryCount: {}",
                        deliveryTag, retryCount);

                // NACK 并重新入队
                // multiple=false, requeue=true
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 重试次数 >= 3，丢弃或转入死信队列
                log.error("[BookingConsumer] 消费失败超过最大重试次数，丢弃消息, deliveryTag: {}, retryCount: {}",
                        deliveryTag, retryCount);

                // NACK 不重新入队（如果配置了死信队列，会进入死信队列）
                channel.basicNack(deliveryTag, false, false);

                // TODO: 记录到数据库，人工介入处理
                saveFailedMessage(message, e);
            }
        } catch (IOException ioException) {
            log.error("[BookingConsumer] NACK 失败, deliveryTag: {}, error: {}",
                    deliveryTag, ioException.getMessage());
        }
    }

    /**
     * 获取消息重试次数
     *
     * @param message 消息对象
     * @return 重试次数
     */
    private Integer getRetryCount(Message message) {
        // 从消息头中获取 x-death 信息（RabbitMQ 自动维护）
        // 实际实现需要解析 message.getMessageProperties().getXDeathHeader()
        // 这里简化返回 0
        return 0;
    }

    /**
     * 保存失败消息（用于人工处理）
     *
     * @param message 消息对象
     * @param e       异常信息
     */
    private void saveFailedMessage(Message message, Exception e) {
        // TODO: 将失败消息保存到数据库
        log.info("[BookingConsumer] 保存失败消息到数据库，待人工处理");
    }
}
