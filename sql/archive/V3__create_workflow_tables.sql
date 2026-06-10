-- V3: 创建工作流运行时表
-- 工作流实例、步骤执行、审批记录、审计日志

-- 工作流实例表（工作流运行时状态持久化）
CREATE TABLE IF NOT EXISTS workflow_instances (
    id VARCHAR(64) PRIMARY KEY COMMENT '实例ID（UUID）',
    template_id VARCHAR(64) NOT NULL COMMENT '关联的模板ID',
    project_id VARCHAR(100) COMMENT '所属项目ID',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/RUNNING/PAUSED/COMPLETED/FAILED/CANCELLED',
    parameters_json TEXT COMMENT '启动参数JSON',
    context_json TEXT COMMENT '全局上下文JSON（步骤间共享数据）',
    error_message TEXT COMMENT '错误信息',
    started_at TIMESTAMP NULL COMMENT '启动时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_wfi_template (template_id),
    INDEX idx_wfi_project (project_id),
    INDEX idx_wfi_status (status),
    INDEX idx_wfi_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流实例表';

-- 工作流步骤执行表（每个步骤的执行记录）
CREATE TABLE IF NOT EXISTS workflow_step_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '执行ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) NOT NULL COMMENT '步骤ID（模板中定义的）',
    agent_id VARCHAR(100) COMMENT '分配的Agent运行时ID',
    agent_role VARCHAR(50) COMMENT 'Agent角色',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/WAITING_DEPENDENCIES/READY/RUNNING/COMPLETED/FAILED/SKIPPED',
    input_data_json TEXT COMMENT '输入数据JSON（从依赖步骤收集）',
    output_data_json TEXT COMMENT '输出数据JSON（本步骤执行结果）',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '当前重试次数',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    timeout_minutes INT DEFAULT 30 COMMENT '超时时间（分钟）',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_wfse_instance (instance_id),
    INDEX idx_wfse_status (status),
    INDEX idx_wfse_agent (agent_id),
    UNIQUE KEY uk_wfse_instance_step (instance_id, step_id),
    FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流步骤执行表';

-- 工作流审批表（审批流程记录）
CREATE TABLE IF NOT EXISTS workflow_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审批ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) NOT NULL COMMENT '关联的步骤ID',
    approver_id BIGINT COMMENT '审批人用户ID',
    approver_name VARCHAR(50) COMMENT '审批人用户名',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    comment TEXT COMMENT '审批意见',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    decided_at TIMESTAMP NULL COMMENT '决定时间',
    INDEX idx_wfa_instance (instance_id),
    INDEX idx_wfa_status (status),
    INDEX idx_wfa_approver (approver_id),
    FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流审批表';

-- 工作流审计日志表（所有关键操作的审计记录）
CREATE TABLE IF NOT EXISTS workflow_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    instance_id VARCHAR(64) NOT NULL COMMENT '关联的实例ID',
    step_id VARCHAR(64) COMMENT '关联的步骤ID（可为空，表示实例级操作）',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: INSTANCE_CREATED/INSTANCE_STARTED/INSTANCE_PAUSED/INSTANCE_RESUMED/INSTANCE_COMPLETED/INSTANCE_FAILED/INSTANCE_CANCELLED/STEP_STARTED/STEP_COMPLETED/STEP_FAILED/STEP_RETRIED/STEP_TIMEOUT/APPROVAL_REQUESTED/APPROVAL_APPROVED/APPROVAL_REJECTED',
    actor_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '操作者类型: SYSTEM/USER/AGENT',
    actor_id VARCHAR(100) COMMENT '操作者ID',
    actor_name VARCHAR(100) COMMENT '操作者名称',
    detail_json TEXT COMMENT '详情JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_wfal_instance (instance_id),
    INDEX idx_wfal_action (action),
    INDEX idx_wfal_actor (actor_type, actor_id),
    INDEX idx_wfal_created (created_at),
    FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流审计日志表';
