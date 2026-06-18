-- V26: agent_logs.summary 字段从 VARCHAR(500) 扩容为 TEXT
-- AI 生成的摘要经常超过 500 字符，导致日志写入失败
ALTER TABLE agent_logs MODIFY COLUMN summary TEXT;
