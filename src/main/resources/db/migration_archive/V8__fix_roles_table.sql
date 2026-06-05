-- 修复 roles 表结构
-- 添加缺失的字段，与 Role 实体类保持一致

ALTER TABLE roles ADD COLUMN display_name VARCHAR(100) AFTER name;
ALTER TABLE roles ADD COLUMN is_system BOOLEAN DEFAULT FALSE AFTER description;

CREATE INDEX idx_roles_is_system ON roles(is_system);
