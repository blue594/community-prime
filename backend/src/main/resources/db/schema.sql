-- ========================================================
-- 邻里优享 (Community Prime) 数据库表结构
-- 版本: 1.0.0
-- 说明: 包含完整的表结构和索引设计
-- ========================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS community_prime 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE community_prime;

-- ========================================================
-- 1. 用户表 (tb_user)
-- ========================================================
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    phone VARCHAR(11) NOT NULL COMMENT '手机号',
    nick_name VARCHAR(50) DEFAULT '' COMMENT '昵称',
    icon VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    
    UNIQUE KEY uk_phone (phone) COMMENT '手机号唯一索引',
    KEY idx_create_time (create_time) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================================
-- 2. 商品表 (tb_product)
-- ========================================================
DROP TABLE IF EXISTS tb_product;
CREATE TABLE tb_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    image VARCHAR(255) DEFAULT '' COMMENT '商品图片',
    category_id BIGINT DEFAULT 0 COMMENT '分类ID',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存',
    sold INT DEFAULT 0 COMMENT '销量',
    description TEXT COMMENT '商品描述',
    status TINYINT DEFAULT 1 COMMENT '状态 0-下架 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    
    KEY idx_category (category_id) COMMENT '分类索引',
    KEY idx_status (status) COMMENT '状态索引',
    KEY idx_price (price) COMMENT '价格索引',
    KEY idx_sold (sold) COMMENT '销量索引',
    FULLTEXT KEY ft_name (name) COMMENT '商品名称全文索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ========================================================
-- 3. 商品订单表 (tb_product_order)
-- ========================================================
DROP TABLE IF EXISTS tb_product_order;
CREATE TABLE tb_product_order (
    id BIGINT PRIMARY KEY COMMENT '订单ID（使用Redis生成）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    address VARCHAR(255) NOT NULL COMMENT '收货地址',
    phone VARCHAR(11) NOT NULL COMMENT '收货人手机号',
    status TINYINT DEFAULT 0 COMMENT '订单状态 0-待付款 1-待发货 2-待收货 3-已完成 4-已取消',
    pay_type TINYINT DEFAULT NULL COMMENT '支付方式 1-余额 2-支付宝 3-微信',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    delivery_time DATETIME DEFAULT NULL COMMENT '发货时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引设计说明：
    -- 1. idx_user_status: 用户查询自己的订单，最常用场景
    -- 2. idx_product_id: 商家查看某商品的所有订单
    -- 3. idx_status: 按状态统计订单
    -- 4. idx_create_time: 按时间范围查询
    
    KEY idx_user_status (user_id, status) COMMENT '用户+状态联合索引（覆盖索引优化）',
    KEY idx_product_id (product_id) COMMENT '商品ID索引',
    KEY idx_status (status) COMMENT '状态索引',
    KEY idx_create_time (create_time) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单表';

-- ========================================================
-- 4. 优惠券表 (tb_voucher)
-- ========================================================
DROP TABLE IF EXISTS tb_voucher;
CREATE TABLE tb_voucher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    shop_id BIGINT NOT NULL DEFAULT 0 COMMENT '商家ID',
    title VARCHAR(100) NOT NULL COMMENT '优惠券标题',
    sub_title VARCHAR(255) DEFAULT '' COMMENT '副标题',
    rules VARCHAR(500) DEFAULT '' COMMENT '使用规则',
    pay_value DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额（秒杀价）',
    actual_value DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '抵扣金额（原价）',
    type TINYINT DEFAULT 0 COMMENT '类型 0-普通优惠券 1-秒杀优惠券',
    stock INT DEFAULT 0 COMMENT '库存（秒杀券用）',
    begin_time DATETIME DEFAULT NULL COMMENT '生效时间',
    end_time DATETIME DEFAULT NULL COMMENT '失效时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    
    -- 索引设计说明：
    -- 1. idx_shop_id: 查询商家的优惠券
    -- 2. idx_type_status: 查询特定类型的优惠券
    -- 3. idx_time: 查询有效期内的优惠券
    
    KEY idx_shop_id (shop_id) COMMENT '商家ID索引',
    KEY idx_type (type) COMMENT '类型索引',
    KEY idx_time (begin_time, end_time) COMMENT '时间范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- ========================================================
-- 5. 优惠券订单表 (tb_voucher_order)
-- ========================================================
DROP TABLE IF EXISTS tb_voucher_order;
CREATE TABLE tb_voucher_order (
    id BIGINT PRIMARY KEY COMMENT '订单ID（使用Redis生成）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    voucher_id BIGINT NOT NULL COMMENT '优惠券ID',
    pay_type TINYINT DEFAULT NULL COMMENT '支付方式',
    status TINYINT DEFAULT 0 COMMENT '状态 0-未支付 1-已支付 2-已核销 3-已取消 4-退款中 5-已退款',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    use_time DATETIME DEFAULT NULL COMMENT '核销时间',
    refund_time DATETIME DEFAULT NULL COMMENT '退款时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引设计说明：
    -- 1. idx_user_voucher: 查询用户是否购买过某优惠券（一人一单校验）
    -- 2. idx_user_id: 查询用户的优惠券订单
    -- 3. idx_voucher_id: 查询某优惠券的所有订单
    -- 4. idx_status: 按状态统计
    
    UNIQUE KEY uk_user_voucher (user_id, voucher_id) COMMENT '用户+优惠券唯一索引（一人一单）',
    KEY idx_user_id (user_id) COMMENT '用户ID索引',
    KEY idx_voucher_id (voucher_id) COMMENT '优惠券ID索引',
    KEY idx_status (status) COMMENT '状态索引',
    KEY idx_create_time (create_time) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券订单表';

-- ========================================================
-- 6. 服务类型表 (tb_service_type)
-- ========================================================
DROP TABLE IF EXISTS tb_service_type;
CREATE TABLE tb_service_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '服务名称',
    category TINYINT DEFAULT 1 COMMENT '分类 1-家政 2-维修',
    image VARCHAR(255) DEFAULT '' COMMENT '服务图片',
    description TEXT COMMENT '服务描述',
    base_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础价格',
    unit VARCHAR(20) DEFAULT '次' COMMENT '单位（小时/次/平米等）',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0-下架 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    
    KEY idx_category (category) COMMENT '分类索引',
    KEY idx_status (status) COMMENT '状态索引',
    KEY idx_sort (sort) COMMENT '排序索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务类型表';

-- ========================================================
-- 7. 服务预约表 (tb_service_booking)
-- ========================================================
DROP TABLE IF EXISTS tb_service_booking;
CREATE TABLE tb_service_booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    service_type_id BIGINT NOT NULL COMMENT '服务类型ID',
    service_name VARCHAR(50) NOT NULL COMMENT '服务名称',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '服务价格',
    booking_date DATE NOT NULL COMMENT '预约日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    address VARCHAR(255) NOT NULL COMMENT '服务地址',
    phone VARCHAR(11) NOT NULL COMMENT '联系电话',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待确认 1-已确认 2-服务中 3-已完成 4-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    
    -- 索引设计说明：
    -- 1. idx_user_id: 查询用户的预约
    -- 2. idx_service_type: 查询某服务类型的预约
    -- 3. idx_booking_date: 查询某日的预约（时间段冲突检测）
    -- 4. idx_status: 按状态查询
    
    KEY idx_user_id (user_id) COMMENT '用户ID索引',
    KEY idx_service_type (service_type_id) COMMENT '服务类型索引',
    KEY idx_booking_date (booking_date) COMMENT '预约日期索引',
    KEY idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务预约表';

-- ========================================================
-- 初始化数据
-- ========================================================

-- 插入示例商品数据
INSERT INTO tb_product (name, image, price, stock, sold, description, status) VALUES
('新鲜苹果 5斤装', 'https://example.com/apple.jpg', 29.90, 1000, 500, '新鲜红富士苹果，脆甜多汁', 1),
('优质大米 10kg', 'https://example.com/rice.jpg', 59.90, 500, 200, '东北五常大米，香糯可口', 1),
('纯牛奶 250ml*12', 'https://example.com/milk.jpg', 49.90, 800, 350, '全脂纯牛奶，营养丰富', 1),
('鸡蛋 30枚', 'https://example.com/egg.jpg', 25.90, 600, 400, '农家散养土鸡蛋', 1),
('洗衣液 3kg', 'https://example.com/laundry.jpg', 39.90, 300, 150, '深层洁净，护色增艳', 1);

-- 插入示例服务类型数据
INSERT INTO tb_service_type (name, category, image, description, base_price, unit, sort, status) VALUES
('日常保洁', 1, 'https://example.com/cleaning.jpg', '2小时日常保洁服务，包含地面清洁、家具擦拭', 120.00, '次', 1, 1),
('深度保洁', 1, 'https://example.com/deep-clean.jpg', '4小时深度保洁，包含厨房、卫生间深度清洁', 280.00, '次', 2, 1),
('家电清洗', 1, 'https://example.com/appliance.jpg', '空调、洗衣机、冰箱等家电清洗', 150.00, '台', 3, 1),
('水管维修', 2, 'https://example.com/plumbing.jpg', '水管漏水、堵塞维修服务', 80.00, '次', 1, 1),
('电路维修', 2, 'https://example.com/electric.jpg', '电路故障排查与维修', 100.00, '次', 2, 1),
('家具安装', 2, 'https://example.com/furniture.jpg', '各类家具安装服务', 120.00, '件', 3, 1);

-- 插入示例优惠券数据（秒杀券）
INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, stock, begin_time, end_time) VALUES
(1, '5折优惠券', '限时秒杀', '满100可用', 10.00, 50.00, 1, 100, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR)),
(1, '满减券', '超值优惠', '满200减50', 5.00, 50.00, 1, 200, DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR));
