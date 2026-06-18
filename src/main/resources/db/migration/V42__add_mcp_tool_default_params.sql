-- V42: 为 mcp_tools 表新增 default_params 字段
-- 存储工具级默认参数（JSON 格式），调用时自动注入

ALTER TABLE mcp_tools ADD COLUMN IF NOT EXISTS default_params TEXT;
