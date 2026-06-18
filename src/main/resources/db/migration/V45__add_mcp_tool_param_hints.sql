-- MCP 工具参数提示
-- 为 AI 提供每个参数的填写指导和示例

ALTER TABLE mcp_tools ADD COLUMN param_hints TEXT COMMENT '参数提示（JSON 格式，为 AI 提供填写指导）';
