-- V14: 创建系统常量表

CREATE TABLE IF NOT EXISTS system_constants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    constant_key VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    value VARCHAR(500) NOT NULL,
    default_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'string',
    group_name VARCHAR(50) NOT NULL,
    unit VARCHAR(20),
    min_value BIGINT,
    max_value BIGINT,
    require_restart BOOLEAN NOT NULL DEFAULT FALSE,
    system_builtin BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sc_key (constant_key),
    INDEX idx_sc_group (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
