# 邻里优享平台 - RabbitMQ 家政预约异步通知集成指南

## 一、集成概述

### 1.1 功能说明
用户创建家政预约成功后，通过 RabbitMQ 发送异步通知消息，消费者接收消息后模拟发送短信和站内信通知。

### 1.2 核心优势
- **解耦**：预约创建与通知逻辑分离
- **异步**：接口立即返回，通知异步处理
- **削峰**：MQ 缓冲请求，保护下游服务
- **可靠**：消息持久化、手动 ACK、失败重试

### 1.3 技术选型
- **交换机类型**：Direct Exchange（直连交换机）
- **序列化**：Java 序列化（可扩展为 JSON）
- **确认模式**：手动 ACK

---

## 二、文件清单

### 2.1 修改的文件
| 文件 | 说明 |
|------|------|
| `pom.xml` | 添加 `spring-boot-starter-amqp` 依赖 |
| `application.yml` | 添加 RabbitMQ 连接配置和自定义参数 |
| `ServiceBookingServiceImpl.java` | 注入 Producer，在 createBooking 中发送消息 |

### 2.2 新增的文件
| 文件 | 路径 | 说明 |
|------|------|------|
| `RabbitMQBookingConfig.java` | `config/rabbitmq/` | 交换机、队列、绑定配置 |
| `BookingMessageDTO.java` | `dto/` | 消息传输对象 |
| `BookingMessageProducer.java` | `mq/` | 消息生产者 |
| `BookingMessageConsumer.java` | `mq/` | 消息消费者 |

---

## 三、配置参数

### 3.1 RabbitMQ 连接配置（application.yml）
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    # 生产者确认机制
    publisher-confirm-type: correlated
    publisher-returns: true
    # 消费者配置
    listener:
      simple:
        acknowledge-mode: manual  # 手动ACK
        prefetch: 1               # 每次预取1条
        retry:
          enabled: true
          max-attempts: 3
```

### 3.2 自定义配置
```yaml
rabbitmq:
  booking:
    exchange: booking.direct.exchange
    routing-key: booking.create.key
    queue: booking.create.notice.queue
```

---

## 四、核心代码说明

### 4.1 配置类（RabbitMQBookingConfig）
```java
// 创建直连交换机
@Bean
public DirectExchange bookingDirectExchange() {
    return new DirectExchange(exchange, true, false);
}

// 创建持久化队列
@Bean
public Queue bookingCreateNoticeQueue() {
    return QueueBuilder.durable(queue).build();
}

// 绑定交换机和队列
@Bean
public Binding bookingCreateBinding(Queue queue, DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(routingKey);
}
```

### 4.2 生产者（BookingMessageProducer）
```java
// 发送消息
public boolean sendBookingCreateMessage(BookingMessageDTO messageDTO) {
    log.info("发送预约创建消息, bookingId: {}", messageDTO.getBookingId());

    rabbitTemplate.convertAndSend(
        exchange,
        routingKey,
        messageDTO
    );

    log.info("消息发送成功");
    return true;
}
```

### 4.3 消费者（BookingMessageConsumer）
```java
@RabbitListener(
    queues = "${rabbitmq.booking.queue}",
    concurrency = "1-5",
    ackMode = "MANUAL"
)
public void handleBookingCreateMessage(BookingMessageDTO dto,
                                        Message message,
                                        Channel channel) {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();

    try {
        // 1. 幂等性校验
        // 2. 处理业务（发送通知）
        // 3. 手动 ACK
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        // 异常处理：NACK 或重试
        channel.basicNack(deliveryTag, false, false);
    }
}
```

### 4.4 业务集成（ServiceBookingServiceImpl）
```java
@Transactional
public Result createBooking(ServiceBookingDTO bookingDTO) {
    // 1. 保存预约到数据库
    save(booking);

    // 2. 发送 MQ 消息（事务提交后）
    try {
        BookingMessageDTO messageDTO = BookingMessageDTO.createBookingMessage(
            booking.getId(),
            userId,
            serviceType.getName(),
            bookingDate,
            startTime
        );
        bookingMessageProducer.sendBookingCreateMessage(messageDTO);
    } catch (Exception e) {
        // 记录日志，不影响主流程
        log.error("发送 MQ 消息异常", e);
    }

    return Result.ok(booking.getId());
}
```

---

## 五、面试重点

### 5.1 为什么使用 RabbitMQ？
- **解耦**：生产者与消费者独立演进
- **异步**：提升接口响应速度
- **削峰**：平滑处理流量高峰
- **可靠**：消息持久化，不丢失

### 5.2 如何保证消息不丢失？
1. **生产者**：Confirm 机制确认到达交换机
2. **交换机/队列**：设置 durable=true 持久化
3. **消费者**：手动 ACK，处理成功后确认

### 5.3 如何处理重复消费？
- 消息携带唯一 ID
- 消费者幂等性校验（Redis/数据库去重）
- 业务逻辑天然幂等（如更新状态为已通知）

### 5.4 消息发送与事务的关系？
- 消息发送在事务提交后
- 发送失败不影响主流程
- 可配合本地消息表实现最终一致性

---

## 六、启动步骤

### 6.1 安装 RabbitMQ
```bash
# Docker 方式启动
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# 访问管理界面 http://localhost:15672
# 默认账号：guest / guest
```

### 6.2 启动应用
```bash
# 1. 更新 Maven 依赖
mvn clean install

# 2. 启动 Spring Boot 应用
mvn spring-boot:run

# 3. 观察日志输出，确认 RabbitMQ 配置加载成功
```

### 6.3 测试验证
```bash
# 调用创建预约接口
curl -X POST http://localhost:8080/api/service-booking/create \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceTypeId": 1,
    "bookingDate": "2026-04-10",
    "startTime": "14:00",
    "address": "测试地址",
    "phone": "13800138000"
  }'

# 预期结果：
# 1. 接口立即返回预约ID
# 2. 日志显示消息发送成功
# 3. 消费者日志显示消息接收和通知发送
```

---

## 七、日志输出示例

### 7.1 应用启动
```
========================================
[RabbitMQ] 家政预约模块配置加载成功
[RabbitMQ] 交换机: booking.direct.exchange
[RabbitMQ] 队列: booking.create.notice.queue
[RabbitMQ] 路由键: booking.create.key
========================================
```

### 7.2 创建预约
```
用户1001创建预约成功，预约ID：123，服务：日常保洁
[createBooking] 预约通知消息发送成功, bookingId: 123
```

### 7.3 消费消息
```
[BookingConsumer] 收到消息, deliveryTag: 1, bookingId: 123, userId: 1001
[BookingConsumer] 【模拟短信通知】内容: 【邻里优享】您已成功预约日常保洁...
[BookingConsumer] 【模拟站内信通知】内容: 您已成功预约【日常保洁】...
[BookingConsumer] 消息处理成功, bookingId: 123
```

---

## 八、扩展建议

### 8.1 延迟消息（预约提醒）
使用 TTL + 死信队列实现预约前 1 小时自动提醒。

### 8.2 消息轨迹追踪
添加消息ID，记录发送时间、消费时间、处理耗时。

### 8.3 监控告警
- 队列积压监控
- 消费延迟告警
- 失败消息人工处理

---

## 九、常见问题

### Q1: 消息发送成功但消费者没收到？
- 检查队列是否正确绑定到交换机
- 检查路由键是否匹配
- 查看 RabbitMQ 管理界面队列状态

### Q2: 消费者重复消费消息？
- 检查幂等性校验逻辑
- 生产环境使用 Redis 替代内存 Map

### Q3: 消息发送失败怎么办？
- 检查 RabbitMQ 连接配置
- 查看生产者 Confirm 回调日志
- 实现本地消息表补偿机制
