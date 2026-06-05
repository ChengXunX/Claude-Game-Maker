-- ============================================
-- 修复 agent_interventions 表结构
-- 版本: 15
-- 说明: V1 创建的表结构与 JPA 实体不匹配，
--       V10 的 CREATE TABLE IF NOT EXISTS 是空操作。
--       此迁移将表结构对齐到 AgentIntervention 实体。
-- ============================================

-- 1. 删除旧外键约束（V1 创建，引用 operator_id -> users.id）
ALTER TABLE agent_interventions DROP FOREIGN KEY agent_interventions_ibfk_1;

-- 2. 重命名列以匹配实体字段
ALTER TABLE agent_interventions RENAME COLUMN operator_id TO user_id;
ALTER TABLE agent_interventions RENAME COLUMN operator_name TO username;
ALTER TABLE agent_interventions RENAME COLUMN intervention_id TO intervention_no;

-- 3. 删除 V1 中多余的列（实体不需要）
ALTER TABLE agent_interventions DROP COLUMN new_direction;
ALTER TABLE agent_interventions DROP COLUMN completed_at;

-- 4. 添加实体需要但表中缺少的列
ALTER TABLE agent_interventions ADD COLUMN agent_name VARCHAR(100);
ALTER TABLE agent_interventions ADD COLUMN agent_role VARCHAR(50);
ALTER TABLE agent_interventions ADD COLUMN project_id VARCHAR(100);
ALTER TABLE agent_interventions ADD COLUMN user_role VARCHAR(20);
ALTER TABLE agent_interventions ADD COLUMN task_id VARCHAR(100);
ALTER TABLE agent_interventions ADD COLUMN message_id VARCHAR(100);
ALTER TABLE agent_interventions ADD COLUMN acknowledgement TEXT;
ALTER TABLE agent_interventions ADD COLUMN execution_result TEXT;

-- 5. 重建索引（对齐实体 @Index 定义）
-- 删除 V1/V3 创建的旧索引
DROP INDEX idx_agent_interventions_agent_id ON agent_interventions;
DROP INDEX idx_agent_interventions_status ON agent_interventions;
DROP INDEX idx_agent_interventions_type ON agent_interventions;
DROP INDEX idx_agent_interventions_created_at ON agent_interventions;
DROP INDEX idx_agent_interventions_agent_status ON agent_interventions;
DROP INDEX idx_agent_interventions_created_type ON agent_interventions;
DROP INDEX idx_agent_interventions_operator ON agent_interventions;

-- 创建与实体 @Index 一致的新索引
CREATE INDEX idx_intervention_agent ON agent_interventions(agent_id);
CREATE INDEX idx_intervention_user ON agent_interventions(user_id);
CREATE INDEX idx_intervention_type ON agent_interventions(intervention_type);
CREATE INDEX idx_intervention_created ON agent_interventions(created_at);
CREATE INDEX idx_intervention_status ON agent_interventions(status);
CREATE INDEX idx_agent_interventions_agent_status ON agent_interventions(agent_id, status);
CREATE INDEX idx_agent_interventions_user_created ON agent_interventions(user_id, created_at);
