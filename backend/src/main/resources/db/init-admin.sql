-- 创建管理员表
CREATE TABLE IF NOT EXISTS tb_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员账号',
    password VARCHAR(64) NOT NULL COMMENT '密码（MD5加密）',
    name VARCHAR(50) COMMENT '管理员姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    role TINYINT DEFAULT 1 COMMENT '角色：1-超级管理员，2-普通管理员',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    icon VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员账号（密码：admin123）
INSERT INTO tb_admin (username, password, name, role, status) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '超级管理员', 1, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- 插入测试管理员账号（密码：123456）
INSERT INTO tb_admin (username, password, name, role, status) VALUES
('test', 'e10adc3949ba59abbe56e057f20f883e', '测试管理员', 2, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
