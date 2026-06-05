-- 修复 system_configs 表结构
-- 添加缺失的字段，与 SystemConfig 实体类保持一致

ALTER TABLE system_configs ADD COLUMN value_type VARCHAR(20) DEFAULT 'string' AFTER config_group;
ALTER TABLE system_configs ADD COLUMN is_system_builtin BOOLEAN DEFAULT FALSE AFTER value_type;

CREATE INDEX idx_system_configs_builtin ON system_configs(is_system_builtin);
