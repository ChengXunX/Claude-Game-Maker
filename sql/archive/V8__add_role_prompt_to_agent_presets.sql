-- 给 agent_presets 表增加角色提示词和协作配置字段
-- 支持角色提示词数据库持久化和知识自进化

ALTER TABLE agent_presets ADD COLUMN IF NOT EXISTS prompt TEXT COMMENT '角色系统提示词（完整角色定义）';
ALTER TABLE agent_presets ADD COLUMN IF NOT EXISTS notify_targets VARCHAR(500) DEFAULT 'producer' COMMENT '完成任务后的通知目标角色（逗号分隔）';
ALTER TABLE agent_presets ADD COLUMN IF NOT EXISTS reviewer VARCHAR(50) COMMENT '审查者角色';
ALTER TABLE agent_presets ADD COLUMN IF NOT EXISTS role_name VARCHAR(100) COMMENT '角色中文名称';
