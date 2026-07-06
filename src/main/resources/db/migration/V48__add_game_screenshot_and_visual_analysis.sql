-- ============================================
-- V48: 游戏截图与视觉分析支持
-- 任务组 G8: 真实运行+截图验证
-- 说明:
--   1. 新增 game_screenshots 表：存储每次验证产生的截图
--   2. 在 game_verify_records 中增加截图和视觉分析相关字段
--   3. 新增 game_verify_results 兼容表（QUICK 验证结果，遗留表）
--   4. 新增 game:visual:view 权限
--   5. 通知模板：截图相关通知
-- ============================================

-- ===== 1. 截图存储表 =====
CREATE TABLE IF NOT EXISTS game_screenshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_id VARCHAR(36) NOT NULL COMMENT '项目ID',
    verify_record_id BIGINT COMMENT '关联的验证记录ID',
    file_path VARCHAR(500) NOT NULL COMMENT '截图文件绝对路径',
    file_name VARCHAR(255) COMMENT '截图文件名',
    file_size_kb INT DEFAULT 0 COMMENT '文件大小KB',
    frame_index INT DEFAULT 0 COMMENT '帧序号（多帧截图时）',
    captured_at DATETIME NOT NULL COMMENT '截图时间',
    description VARCHAR(500) COMMENT '截图描述（如"主菜单"）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_gs_project (project_id),
    INDEX idx_gs_verify_record (verify_record_id),
    INDEX idx_gs_captured (captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏截图记录表';

-- ===== 2. game_verify_records 新增字段 =====
ALTER TABLE game_verify_records
    ADD COLUMN screenshots_json TEXT COMMENT '截图文件路径列表(JSON数组)',
    ADD COLUMN visual_score INT DEFAULT 0 COMMENT 'AI视觉综合评分(0-100)',
    ADD COLUMN render_health_score INT DEFAULT 0 COMMENT '渲染健康度(0-100, 白屏/崩溃会很低)',
    ADD COLUMN visual_playable_score INT DEFAULT 0 COMMENT '视觉可玩性(0-100)',
    ADD COLUMN visual_uiux_score INT DEFAULT 0 COMMENT '视觉UI/UX(0-100)',
    ADD COLUMN visual_summary TEXT COMMENT '视觉分析摘要',
    ADD COLUMN visual_issues_json TEXT COMMENT '视觉问题列表(JSON数组)';

-- ===== 3. 新增 game_verify_results 兼容表（QUICK 验证结果） =====
-- 此前 entity 引用了此表但 SQL 未定义，补全
CREATE TABLE IF NOT EXISTS game_verify_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_id VARCHAR(36) NOT NULL COMMENT '项目ID',
    project_name VARCHAR(200) COMMENT '项目名称',
    success BOOLEAN DEFAULT FALSE COMMENT '验证是否通过',
    message TEXT COMMENT '验证消息',
    error TEXT COMMENT '错误信息',
    warnings_json TEXT COMMENT '警告列表(JSON)',
    verified_at DATETIME NOT NULL COMMENT '验证时间',
    verify_type VARCHAR(20) DEFAULT 'QUICK' COMMENT '验证类型: QUICK(快速), DEEP(深度), FULL(完整)',
    overall_score INT DEFAULT 0 COMMENT '综合评分(0-100)',
    runnable_score INT DEFAULT 0 COMMENT '可运行性评分',
    playable_score INT DEFAULT 0 COMMENT '可玩性评分',
    completeness_score INT DEFAULT 0 COMMENT '完整性评分',
    uiux_score INT DEFAULT 0 COMMENT 'UI/UX 评分',
    code_quality_score INT DEFAULT 0 COMMENT '代码质量评分',
    summary TEXT COMMENT '质量摘要',
    strengths_json TEXT COMMENT '优点(JSON)',
    issues_json TEXT COMMENT '问题(JSON)',
    suggestions_json TEXT COMMENT '建议(JSON)',
    -- G8 新增：截图和视觉分析
    screenshots_json TEXT COMMENT '截图文件路径列表(JSON)',
    render_health_score INT DEFAULT 0 COMMENT '视觉渲染健康度',
    visual_playable_score INT DEFAULT 0 COMMENT '视觉可玩性',
    visual_uiux_score INT DEFAULT 0 COMMENT '视觉UI/UX',
    visual_score INT DEFAULT 0 COMMENT '视觉综合评分',
    visual_summary TEXT COMMENT '视觉分析摘要',
    visual_issues_json TEXT COMMENT '视觉问题列表(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_gvr2_project (project_id),
    INDEX idx_gvr2_type (verify_type),
    INDEX idx_gvr2_verified (verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏验证结果表';

-- ===== 4. 注册 G8 新增权限 =====
-- game:visual:view - 查看游戏视觉验证（截图、视觉分析）
INSERT IGNORE INTO role_permissions (role_id, permission) VALUES
    (1, 'game:visual:view'),
    (2, 'game:visual:view'),
    (3, 'game:visual:view');
