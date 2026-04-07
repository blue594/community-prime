package com.community.prime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家政预约消息 DTO - 用于 RabbitMQ 消息传输
 *
 * 【面试重点】为什么实现 Serializable？
 * 1. RabbitMQ 默认使用 Java 序列化（SimpleMessageConverter）
 * 2. 消息需要在网络中传输，必须序列化为字节流
 * 3. 生产环境建议使用 JSON 序列化（Jackson2JsonMessageConverter），跨语言兼容
 *
 * 【消息设计原则】
 * 1. 包含足够上下文：接收方不需要再查询数据库就能处理
 * 2. 精简字段：只传必要数据，减少消息体积
 * 3. 幂等性考虑：包含唯一标识（bookingId），防止重复处理
 *
 * @author Community Prime
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingMessageDTO implements Serializable {

    /**
     * 序列化版本号
     * 修改类结构时需要更新，保证序列化兼容性
     */
    private static final long serialVersionUID = 1L;

    /**
     * 预约ID - 唯一标识
     * 用于幂等性校验，防止重复通知
     */
    private Long bookingId;

    /**
     * 用户ID - 通知接收方
     */
    private Long userId;

    /**
     * 服务名称 - 如"日常保洁"、"家电维修"
     * 用于短信/站内信内容展示
     */
    private String serviceName;

    /**
     * 预约日期 - 格式：yyyy-MM-dd
     */
    private String bookingDate;

    /**
     * 开始时间 - 格式：HH:mm
     */
    private String startTime;

    /**
     * 用户手机号 - 用于短信通知
     * 可选字段，根据实际业务需求
     */
    private String userPhone;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 消息类型 - 扩展字段
     * CREATE: 创建预约
     * CANCEL: 取消预约
     * REMIND: 服务提醒
     */
    private String messageType;

    /**
     * 构造方法 - 快速创建预约创建消息
     *
     * @param bookingId   预约ID
     * @param userId      用户ID
     * @param serviceName 服务名称
     * @return BookingMessageDTO 实例
     */
    public static BookingMessageDTO createBookingMessage(Long bookingId, Long userId, String serviceName,
                                                          String bookingDate, String startTime) {
        return BookingMessageDTO.builder()
                .bookingId(bookingId)
                .userId(userId)
                .serviceName(serviceName)
                .bookingDate(bookingDate)
                .startTime(startTime)
                .createTime(LocalDateTime.now())
                .messageType("CREATE")
                .build();
    }

    /**
     * 生成短信通知内容
     *
     * @return 短信内容
     */
    public String generateSmsContent() {
        return String.format("【邻里优享】您已成功预约%s，预约日期：%s %s，订单号：%d。如有变动请提前联系。",
                serviceName, bookingDate, startTime, bookingId);
    }

    /**
     * 生成站内信通知内容
     *
     * @return 站内信内容
     */
    public String generateSiteMessageContent() {
        return String.format("您已成功预约【%s】，预约时间：%s %s，请准时等候服务人员上门。",
                serviceName, bookingDate, startTime);
    }

    @Override
    public String toString() {
        return "BookingMessageDTO{" +
                "bookingId=" + bookingId +
                ", userId=" + userId +
                ", serviceName='" + serviceName + '\'' +
                ", bookingDate='" + bookingDate + '\'' +
                ", startTime='" + startTime + '\'' +
                ", messageType='" + messageType + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
