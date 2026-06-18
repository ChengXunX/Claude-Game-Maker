-- V28: 创建飞书用户绑定表，用于飞书 AI 助手集成
CREATE TABLE IF NOT EXISTS feishu_user_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(100) COMMENT '飞书用户 open_id（待绑定时为空）',
    user_id BIGINT NOT NULL COMMENT '系统用户 ID',
    binding_code VARCHAR(10) COMMENT '绑定验证码',
    status ENUM('PENDING','BOUND') DEFAULT 'PENDING' COMMENT '绑定状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_open_id (open_id),
    KEY idx_user_id (user_id),
    KEY idx_binding_code (binding_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书用户绑定表';
