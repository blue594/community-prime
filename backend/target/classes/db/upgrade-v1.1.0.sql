-- ========================================================
-- 邻里优享 (Community Prime) 增量SQL脚本
-- 版本: 1.1.0
-- 说明: 新增核销码、服务评价、商品秒杀功能
-- ========================================================

USE community_prime;

-- ========================================================
-- 1. 优惠券订单表 - 新增核销码字段
-- ========================================================
ALTER TABLE tb_voucher_order ADD COLUMN verify_code VARCHAR(32) DEFAULT NULL COMMENT '核销码（支付后生成）' AFTER refund_time;


-- ========================================================
-- 2. 服务评价表 (tb_service_review) - 新增
-- ========================================================
DROP TABLE IF EXISTS tb_service_review;
CREATE TABLE tb_service_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    booking_id BIGINT NOT NULL COMMENT '预约ID',
    service_type_id BIGINT NOT NULL COMMENT '服务类型ID',
    rating TINYINT NOT NULL DEFAULT 5 COMMENT '评分（1-5）',
    content VARCHAR(500) DEFAULT '' COMMENT '评价内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',

    -- 索引设计说明：
    -- 1. uk_booking_id: 一个预约只能评价一次
    -- 2. idx_service_type_id: 查询某服务的所有评价
    -- 3. idx_user_id: 查询某用户的所有评价

    UNIQUE KEY uk_booking_id (booking_id) COMMENT '预约ID唯一索引（一次预约一次评价）',
    KEY idx_service_type_id (service_type_id) COMMENT '服务类型索引',
    KEY idx_user_id (user_id) COMMENT '用户ID索引',
    KEY idx_rating (rating) COMMENT '评分索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务评价表';


-- ========================================================
-- 3. 商品秒杀表 (tb_seckill_product) - 新增
-- ========================================================
DROP TABLE IF EXISTS tb_seckill_product;
CREATE TABLE tb_seckill_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '关联商品ID',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    begin_time DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time DATETIME NOT NULL COMMENT '秒杀结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',

    -- 索引设计说明：
    -- 1. idx_product_id: 查询某商品的秒杀信息
    -- 2. idx_time: 查询正在进行的秒杀活动

    KEY idx_product_id (product_id) COMMENT '商品ID索引',
    KEY idx_time (begin_time, end_time) COMMENT '时间范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品秒杀表';


-- ========================================================
-- 4. 新增普通优惠券示例数据（餐饮到店券）
-- ========================================================
INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, stock, begin_time, end_time) VALUES
(1, '火锅8折券', '到店消费', '限指定火锅店使用，不与其他优惠叠加', 16.00, 20.00, 0, 50, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),
(1, '奶茶买一送一券', '饮品特惠', '限指定奶茶店使用，每人限购一张', 9.90, 25.00, 0, 100, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY)),
(1, '烧烤满100减30券', '夜宵优惠', '消费满100元可用，不可拆分', 15.00, 30.00, 0, 80, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY)),
(1, '甜品下午茶套餐券', '精致生活', '含两份甜品+两杯饮品，到店出示核销码', 39.90, 78.00, 0, 30, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY));


-- ========================================================
-- 5. 新增商品秒杀示例数据
-- ========================================================
INSERT INTO tb_seckill_product (product_id, seckill_price, stock, begin_time, end_time) VALUES
(1, 9.90, 50, NOW(), DATE_ADD(NOW(), INTERVAL 6 HOUR)),
(3, 29.90, 30, NOW(), DATE_ADD(NOW(), INTERVAL 4 HOUR)),
(5, 19.90, 40, DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR));
