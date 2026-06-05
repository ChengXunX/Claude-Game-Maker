-- 创建Agent性能评估表
-- 用于记录和分析Agent的各项性能指标
-- 注意：如果表已存在（V1中创建），则跳过创建

CREATE TABLE IF NOT EXISTS agent_performance_new (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL UNIQUE,
    agent_name VARCHAR(100),
    agent_role VARCHAR(50),

    -- 任务统计
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,
    failed_tasks INT DEFAULT 0,
    in_progress_tasks INT DEFAULT 0,

    -- 效率指标
    avg_completion_time_ms BIGINT DEFAULT 0,
    min_completion_time_ms BIGINT DEFAULT 0,
    max_completion_time_ms BIGINT DEFAULT 0,

    -- 质量指标
    avg_quality_score DOUBLE DEFAULT 0.0,
    max_quality_score DOUBLE DEFAULT 0.0,
    min_quality_score DOUBLE DEFAULT 0.0,
    review_pass_rate DOUBLE DEFAULT 0.0,

    -- 负载指标
    current_load INT DEFAULT 0,
    avg_load DOUBLE DEFAULT 0.0,

    -- 综合评分
    overall_score DOUBLE DEFAULT 0.0,
    reliability_score DOUBLE DEFAULT 0.0,
    efficiency_score DOUBLE DEFAULT 0.0,

    -- 时间戳
    first_task_at TIMESTAMP,
    last_task_at TIMESTAMP,
    last_evaluated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_perf_agent_id (agent_id),
    INDEX idx_perf_role (agent_role),
    INDEX idx_perf_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 将数据从旧表迁移到新表（如果旧表存在）
INSERT IGNORE INTO agent_performance_new (agent_id, agent_name, agent_role, total_tasks, created_at, updated_at)
SELECT agent_id, agent_name, agent_role, tasks_completed, created_at, updated_at
FROM agent_performance
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'agent_performance');

-- 删除旧表并重命名新表
DROP TABLE IF EXISTS agent_performance;
ALTER TABLE agent_performance_new RENAME TO agent_performance;
