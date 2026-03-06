# 邻里优享 (Community Prime)

一个整合"超市网购（次日达）"、"餐饮到店优惠券"、"家政维修预约"的社区生活服务平台，专注于高并发场景（如秒杀）和技术深度。

## 项目架构

```
community-prime/
├── backend/                 # SpringBoot 后端项目
│   ├── src/main/java/com/community/prime/
│   │   ├── config/         # 配置类（Redis、MyBatis Plus、Redisson等）
│   │   ├── controller/     # 控制器层
│   │   ├── service/        # 业务逻辑层
│   │   ├── mapper/         # 数据访问层
│   │   ├── entity/         # 实体类
│   │   ├── dto/            # 数据传输对象
│   │   ├── vo/             # 视图对象
│   │   ├── utils/          # 工具类
│   │   ├── handler/        # 全局异常处理
│   │   └── interceptor/    # 拦截器
│   └── src/main/resources/
│       ├── mapper/         # MyBatis XML映射文件
│       ├── db/             # 数据库脚本
│       ├── application.yml # 配置文件
│       └── seckill.lua     # 秒杀Lua脚本
│
└── frontend/               # Vue 前端项目
    ├── src/
    │   ├── api/           # API接口
    │   ├── views/         # 页面组件
    │   ├── components/    # 公共组件
    │   ├── router/        # 路由配置
    │   ├── store/         # Vuex状态管理
    │   ├── utils/         # 工具函数
    │   └── styles/        # 样式文件
    └── public/
```

## 核心业务模块

### 1. 用户与认证模块
- 短信验证码登录（基于Redis存储）
- Token认证机制
- 用户信息管理

### 2. 商品与秒杀模块（技术亮点）
- 超市商品浏览、搜索
- 优惠券/秒杀活动管理
- **优惠券秒杀功能**：完整的秒杀流程，是技术面试的核心演示点

### 3. 订单与交易模块
- 普通商品下单（超市用品）
- 秒杀订单下单
- 订单查询、取消

### 4. 预约服务模块
- 家政/维修服务展示、时段选择
- 服务预约、状态管理

### 5. 管理后台模块
- 商品、优惠券、预约服务的上架下架
- 订单管理

## 技术特性与实现

### Redis深度应用

#### 1. 缓存
- 用户信息、商品信息缓存
- 使用JSON序列化存储对象
- 随机过期时间防止缓存雪崩

#### 2. 分布式锁
- 秒杀扣库存时，使用 **Redisson** 实现分布式锁
- 支持可重入、自动续期（Watch Dog机制）
- 保证一人一单的原子性

#### 3. 缓存问题解决
在 `CacheClient` 类中实现了三种缓存问题的解决方案：

**缓存穿透**：缓存空值
```java
// 将空值写入Redis，设置较短过期时间
stringRedisTemplate.opsForValue().set(key, "", PRODUCT_NULL_TTL, TimeUnit.MINUTES);
```

**缓存击穿**：互斥锁
```java
// 使用SETNX实现互斥锁
private boolean tryLock(String key) {
    Boolean flag = stringRedisTemplate.opsForValue()
            .setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
    return BooleanUtil.isTrue(flag);
}
```

**缓存雪崩**：随机过期时间
```java
// 随机过期时间，防止缓存雪崩：基础时间 + 0-10分钟的随机值
long randomTime = time + (long) (Math.random() * 600);
```

#### 4. 消息队列
- 使用 **Redis Stream** 实现秒杀订单的异步创建
- 消费者组模式，支持消息确认和重试

#### 5. Lua脚本
- 秒杀库存扣减使用Lua脚本保证原子性
- 避免超卖问题

### 数据库与优化

#### 索引设计
```sql
-- 订单表索引（覆盖索引优化）
KEY idx_user_status (user_id, status) COMMENT '用户+状态联合索引（覆盖索引优化）'

-- 优惠券订单表索引（一人一单校验）
UNIQUE KEY uk_user_voucher (user_id, voucher_id) COMMENT '用户+优惠券唯一索引（一人一单）'
```

#### SQL优化对比
在 `ProductOrderMapper` 中提供了两个版本的查询：

**性能问题版本（不推荐）**：
```java
@Select("SELECT * FROM tb_product_order WHERE user_id = #{userId}")
// 问题：使用SELECT * 查询所有字段，无法使用覆盖索引
```

**优化版本（推荐）**：
```java
@Select("SELECT id, user_id, product_id... FROM tb_product_order WHERE user_id = #{userId}")
// 优化：使用覆盖索引，只查询需要的字段
```

### 事务管理
- 在下单/秒杀业务中，使用 `@Transactional` 注解管理事务
- 确保扣库存、创建订单的一致性
- 支持回滚机制

### 项目结构
- 清晰的分层：controller, service, mapper, entity, dto, vo
- 统一的响应封装（Result）
- 全局异常处理（GlobalExceptionHandler）

## 快速开始

### 环境要求
- JDK 1.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 14+

### 后端启动

1. 创建数据库
```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

2. 修改配置
编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/community_prime
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

3. 启动项目
```bash
cd backend
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run serve
```

访问 http://localhost:8081

## 接口文档

### 用户模块
- `POST /api/user/code` - 发送验证码
- `POST /api/user/login` - 登录
- `GET /api/user/me` - 获取用户信息

### 商品模块
- `GET /api/product/list` - 商品列表
- `GET /api/product/detail/{id}` - 商品详情
- `GET /api/product/search` - 搜索商品
- `POST /api/product/order` - 创建订单
- `GET /api/product/order/list` - 订单列表
- `POST /api/product/order/cancel/{id}` - 取消订单

### 优惠券模块
- `POST /api/voucher/seckill/add` - 添加秒杀券
- `GET /api/voucher/list/{shopId}` - 优惠券列表
- `GET /api/voucher/seckill/detail/{id}` - 秒杀券详情
- `POST /api/voucher/seckill/order/{id}` - 秒杀下单
- `GET /api/voucher/order/list` - 我的优惠券订单

### 服务预约模块
- `GET /api/service/type/list` - 服务类型列表
- `GET /api/service/type/detail/{id}` - 服务详情
- `POST /api/service/booking` - 创建预约
- `GET /api/service/booking/list` - 我的预约列表
- `POST /api/service/booking/cancel/{id}` - 取消预约

## 技术栈

### 后端
- Spring Boot 2.7.14
- MyBatis Plus 3.5.3.1
- Redis + Redisson
- MySQL 8.0
- Druid 连接池
- Hutool 工具包

### 前端
- Vue 2.6
- Vue Router
- Vuex
- Element UI
- Axios

## 面试要点

### 1. 秒杀系统设计
- **库存预热**：活动开始前将库存加载到Redis
- **Redis预减库存**：使用Lua脚本保证原子性
- **异步下单**：使用Redis Stream消息队列削峰填谷
- **分布式锁**：Redisson保证一人一单

### 2. 缓存问题解决方案
- 缓存穿透：缓存空值
- 缓存击穿：互斥锁
- 缓存雪崩：随机过期时间

### 3. 数据库优化
- 索引设计原则
- 覆盖索引的使用
- 慢查询优化

### 4. 高并发处理
- 消息队列异步处理
- 分布式锁
- 限流（可扩展）

## 许可证

MIT License
