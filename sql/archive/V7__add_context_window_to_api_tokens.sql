-- 添加上下文窗口字段到 api_tokens 表
-- 用于配置每个 Token 对应的上下文窗口大小，默认 200k，支持 1M 长上下文

ALTER TABLE `api_tokens` ADD COLUMN `context_window` int DEFAULT 200000 AFTER `max_tokens`;
