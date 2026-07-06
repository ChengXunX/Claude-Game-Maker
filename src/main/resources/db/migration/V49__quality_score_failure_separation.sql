-- ============================================
-- V49: 质量评分失败分离 + 硬上限系统常量
-- 任务组: 验证分数固定0/50 + 里程碑反复失败 + 迭代循环收敛 三合一修复
--
-- 根因:
--   1. QualityGateService 在 AI 分析失败时硬编码 score=50/0，掩盖真实问题
--      ProducerAgent 据此反复触发改进迭代死循环
--   2. triggerQualityIteration 硬上限是 6 次失败才升级，导致"武器系统失败 7 次"
--   3. 5 分钟防重复太短，工作流 COMPLETED 事件反复触发质量门禁
--
-- 修复:
--   1. QualityAnalysisResult 加 failureType 枚举字段（已通过 Java 代码）
--   2. QualityCheckResult 加 analysisFailed 布尔字段（已通过 Java 代码）
--   3. system_constants 加 quality.iteration.max-fail-count 配置（默认 3）
--   4. system_constants 加 quality.iteration.cooldown-minutes 配置（默认 30）
--   5. system_constants 加 quality.score.analysis-failed-marker 配置（默认 -1）
-- ============================================

-- ===== 1. 系统常量：硬上限 =====
-- 同一里程碑最多失败次数，超过则升级到管理员不再自动迭代
INSERT INTO system_constants (constant_key, display_name, description, default_value, value, value_type, group_name, unit, min_value, max_value, system_builtin, created_at, updated_at)
VALUES ('quality.iteration.max-fail-count', '质量改进硬上限', '同一里程碑验证失败超过此值时停止自动改进，升级到管理员', '3', '3', 'int', 'quality', '次', 1, 10, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 同一里程碑两次质量迭代触发之间的最小间隔
INSERT INTO system_constants (constant_key, display_name, description, default_value, value, value_type, group_name, unit, min_value, max_value, system_builtin, created_at, updated_at)
VALUES ('quality.iteration.cooldown-minutes', '质量改进冷却时间', '同一里程碑两次自动改进迭代之间的最小间隔', '30', '30', 'int', 'quality', '分钟', 5, 1440, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 分析失败时的标记分数（-1 让调用方明确知道"分析失败"而非"零分"）
INSERT INTO system_constants (constant_key, display_name, description, default_value, value, value_type, group_name, unit, min_value, max_value, system_builtin, created_at, updated_at)
VALUES ('quality.score.analysis-failed-marker', '分析失败标记分数', '当 AI/工具分析失败时使用的标记分数（区分"失败"与"低分"）', '-1', '-1', 'int', 'quality', '分', -1, -1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 质量门禁低于此分数视为低分（之前是 60，保留为可调）
INSERT INTO system_constants (constant_key, display_name, description, default_value, value, value_type, group_name, unit, min_value, max_value, system_builtin, created_at, updated_at)
VALUES ('quality.gate.low-score-threshold', '质量门禁低分阈值', 'AI 质量评分低于此值时触发改进迭代', '60', '60', 'int', 'quality', '分', 0, 100, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();