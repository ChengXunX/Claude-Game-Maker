-- system_configs 添加 project_id（全局默认 + 项目覆盖）
ALTER TABLE system_configs ADD COLUMN project_id VARCHAR(100) DEFAULT NULL COMMENT '所属项目 ID（null=全局默认）';
CREATE INDEX idx_config_project ON system_configs(project_id);
-- config_key 改为 (config_key, project_id) 联合唯一，支持全局+项目级配置
CREATE UNIQUE INDEX uk_config_key_project ON system_configs(config_key, project_id);

-- agent_health 添加 project_id
ALTER TABLE agent_health ADD COLUMN project_id VARCHAR(100) DEFAULT NULL COMMENT '所属项目 ID';
CREATE INDEX idx_health_project ON agent_health(project_id);

-- agent_performance 添加 project_id
ALTER TABLE agent_performance ADD COLUMN project_id VARCHAR(100) DEFAULT NULL COMMENT '所属项目 ID';
CREATE INDEX idx_perf_project ON agent_performance(project_id);
