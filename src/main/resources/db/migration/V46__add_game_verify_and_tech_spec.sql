-- ============================================
-- V46: 游戏质量验证系统 - 新增表
-- 任务组 G7: 游戏质量提升系统数据库变更
-- 说明: 新增 game_verify_records、runtime_errors 表
-- 注意: game_projects 存储在文件系统（JSON），不在数据库中
-- ============================================

-- 游戏验证记录表
CREATE TABLE IF NOT EXISTS game_verify_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_id VARCHAR(36) NOT NULL COMMENT '项目ID',
    milestone_id VARCHAR(36) COMMENT '里程碑ID',
    verify_type VARCHAR(20) NOT NULL COMMENT '验证类型: FULL, STRUCTURAL, BUILD, RUNTIME, QUALITY',
    overall_score INT DEFAULT 0 COMMENT '综合评分(0-100)',
    structural_passed BOOLEAN DEFAULT FALSE COMMENT '结构验证是否通过',
    build_passed BOOLEAN DEFAULT FALSE COMMENT '构建验证是否通过',
    build_type VARCHAR(20) COMMENT '构建类型: npm, static, maven等',
    build_duration_ms BIGINT DEFAULT 0 COMMENT '构建耗时(毫秒)',
    build_output TEXT COMMENT '构建输出',
    runtime_passed BOOLEAN DEFAULT FALSE COMMENT '运行验证是否通过',
    runtime_port INT COMMENT '运行端口',
    runtime_duration_ms BIGINT DEFAULT 0 COMMENT '运行验证耗时(毫秒)',
    runtime_output TEXT COMMENT '运行输出',
    console_errors TEXT COMMENT '控制台错误(JSON数组)',
    resource_errors TEXT COMMENT '资源加载错误(JSON数组)',
    quality_score INT DEFAULT 0 COMMENT 'AI质量评分(0-100)',
    runnable_score INT DEFAULT 0 COMMENT '可运行性评分(0-100)',
    playable_score INT DEFAULT 0 COMMENT '可玩性评分(0-100)',
    completeness_score INT DEFAULT 0 COMMENT '完整性评分(0-100)',
    uiux_score INT DEFAULT 0 COMMENT 'UI/UX评分(0-100)',
    code_quality_score INT DEFAULT 0 COMMENT '代码质量评分(0-100)',
    quality_summary TEXT COMMENT '质量分析摘要',
    quality_issues TEXT COMMENT '质量问题(JSON数组)',
    quality_suggestions TEXT COMMENT '质量建议(JSON数组)',
    raw_ai_response TEXT COMMENT 'AI原始响应',
    overall_passed BOOLEAN DEFAULT FALSE COMMENT '是否通过',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_gvr_project (project_id),
    INDEX idx_gvr_milestone (milestone_id),
    INDEX idx_gvr_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏验证记录表';

-- 运行时错误记录表
CREATE TABLE IF NOT EXISTS runtime_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_id VARCHAR(36) NOT NULL COMMENT '项目ID',
    verify_record_id BIGINT COMMENT '关联的验证记录ID',
    error_type VARCHAR(20) NOT NULL COMMENT '错误类型: JS_RUNTIME, RESOURCE_LOAD, PORT_CONFLICT, PERMISSION, MEMORY, SYNTAX, NETWORK',
    error_message TEXT NOT NULL COMMENT '错误信息',
    line_number INT COMMENT '错误行号',
    file_path VARCHAR(500) COMMENT '错误文件路径',
    auto_fixed BOOLEAN DEFAULT FALSE COMMENT '是否已自动修复',
    fix_task_id VARCHAR(36) COMMENT '修复任务ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_re_project (project_id),
    INDEX idx_re_type (error_type),
    INDEX idx_re_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行时错误记录表';
