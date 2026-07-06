-- 允许 alert_records.rule_id 为 NULL
-- 原因：saveAlert() 方法用于 Agent 风险预警等不需要规则匹配的场景
--       这些告警没有关联的规则，rule_id 应允许为 NULL

-- MySQL: 修改 rule_id 列允许 NULL
ALTER TABLE alert_records MODIFY COLUMN rule_id BIGINT NULL;

-- 移除外键约束（因为现在 rule_id 可以为 NULL，不能有外键引用）
-- 注意：有些告警记录可能没有关联规则，所以不适合有外键约束
-- 如果之前有外键约束的话（取决于初始版本）
