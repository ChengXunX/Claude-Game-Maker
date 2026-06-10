-- 给 agent_presets 表增加提示词版本追踪字段
-- 支持角色提示词进化历史

ALTER TABLE agent_presets ADD COLUMN prompt_version INT DEFAULT 0 COMMENT '提示词版本号（每次进化/编辑+1）';
ALTER TABLE agent_presets ADD COLUMN last_evolution_source VARCHAR(50) COMMENT '上次进化来源：manual/ai/evolution';
ALTER TABLE agent_presets ADD COLUMN last_evolution_at DATETIME COMMENT '上次进化时间';
