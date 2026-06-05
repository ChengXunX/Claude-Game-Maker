-- ============================================================
-- ChengXun Game Maker - 数据库初始化
-- 版本: 2.0.0
-- 说明: 统一初始化脚本，包含所有表结构和初始数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户和权限相关表
-- ----------------------------

-- 角色表
CREATE TABLE IF NOT EXISTS `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_system` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `nickname` varchar(100) DEFAULT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `last_login_at` timestamp NULL DEFAULT NULL,
  `last_login_ip` varchar(50) DEFAULT NULL,
  `password_changed` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_users_role` (`role_id`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 角色权限表
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission` varchar(100) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rp_role` (`role_id`),
  KEY `idx_rp_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 权限定义表
CREATE TABLE IF NOT EXISTS `permission_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_key` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `category` varchar(50) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `system` tinyint(1) NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_key` (`permission_key`),
  KEY `idx_pd_category` (`category`),
  KEY `idx_pd_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户权限表
CREATE TABLE IF NOT EXISTS `user_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `permission` varchar(100) NOT NULL,
  `source` varchar(20) NOT NULL,
  `granted_by` varchar(100) DEFAULT NULL,
  `reason` text,
  `expires_at` timestamp NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission` (`user_id`, `permission`),
  KEY `idx_up_user` (`user_id`),
  KEY `idx_up_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 权限申请表
CREATE TABLE IF NOT EXISTS `permission_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `username` varchar(100) NOT NULL,
  `permission` varchar(100) NOT NULL,
  `reason` text,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(100) DEFAULT NULL,
  `approval_comment` text,
  `approved_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pr_user` (`user_id`),
  KEY `idx_pr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 系统配置相关表
-- ----------------------------

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `config_key` varchar(100) NOT NULL,
  `config_value` text,
  `description` varchar(500) DEFAULT NULL,
  `config_group` varchar(50) DEFAULT NULL,
  `value_type` varchar(20) DEFAULT 'string',
  `is_system_builtin` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key_project` (`config_key`, `project_id`),
  KEY `idx_sc_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统常量表
CREATE TABLE IF NOT EXISTS `system_constants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `constant_key` varchar(100) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `description` text,
  `value` varchar(500) NOT NULL,
  `default_value` varchar(500) NOT NULL,
  `value_type` varchar(20) NOT NULL DEFAULT 'string',
  `group_name` varchar(50) NOT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `min_value` bigint DEFAULT NULL,
  `max_value` bigint DEFAULT NULL,
  `require_restart` tinyint(1) NOT NULL DEFAULT '0',
  `system_builtin` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_constant_key` (`constant_key`),
  KEY `idx_sc_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Agent 相关表
-- ----------------------------

-- Agent 健康状态表
CREATE TABLE IF NOT EXISTS `agent_health` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `agent_id` varchar(50) NOT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `agent_role` varchar(50) DEFAULT NULL,
  `health_status` varchar(20) NOT NULL DEFAULT 'HEALTHY',
  `avg_response_time_ms` bigint DEFAULT NULL,
  `max_response_time_ms` bigint DEFAULT NULL,
  `total_requests` bigint DEFAULT '0',
  `successful_requests` bigint DEFAULT '0',
  `failed_requests` bigint DEFAULT '0',
  `error_rate` double DEFAULT '0',
  `memory_usage_mb` bigint DEFAULT NULL,
  `cpu_usage_percent` double DEFAULT NULL,
  `active_tasks` int DEFAULT '0',
  `completed_tasks` bigint DEFAULT '0',
  `task_completion_rate` double DEFAULT '0',
  `last_activity_time` timestamp NULL DEFAULT NULL,
  `last_error_time` timestamp NULL DEFAULT NULL,
  `last_error_message` text,
  `consecutive_errors` int DEFAULT '0',
  `uptime_seconds` bigint DEFAULT '0',
  `check_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_health_agent` (`agent_id`),
  KEY `idx_health_status` (`health_status`),
  KEY `idx_health_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 绩效表
CREATE TABLE IF NOT EXISTS `agent_performance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `agent_id` varchar(50) NOT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `agent_role` varchar(50) DEFAULT NULL,
  `total_tasks` int DEFAULT '0',
  `completed_tasks` int DEFAULT '0',
  `failed_tasks` int DEFAULT '0',
  `in_progress_tasks` int DEFAULT '0',
  `avg_completion_time_ms` bigint DEFAULT '0',
  `min_completion_time_ms` bigint DEFAULT '0',
  `max_completion_time_ms` bigint DEFAULT '0',
  `avg_quality_score` double DEFAULT '0',
  `max_quality_score` double DEFAULT '0',
  `min_quality_score` double DEFAULT '0',
  `review_pass_rate` double DEFAULT '0',
  `current_load` int DEFAULT '0',
  `avg_load` double DEFAULT '0',
  `overall_score` double DEFAULT '0',
  `reliability_score` double DEFAULT '0',
  `efficiency_score` double DEFAULT '0',
  `first_task_at` timestamp NULL DEFAULT NULL,
  `last_task_at` timestamp NULL DEFAULT NULL,
  `last_evaluated_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`),
  KEY `idx_perf_role` (`agent_role`),
  KEY `idx_perf_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 干预表
CREATE TABLE IF NOT EXISTS `agent_interventions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `intervention_no` varchar(50) NOT NULL,
  `agent_id` varchar(50) NOT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `agent_role` varchar(50) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `intervention_type` varchar(50) NOT NULL,
  `instruction` text,
  `reason` text,
  `original_decision` text,
  `new_decision` text,
  `status` varchar(20) DEFAULT 'PENDING',
  `user_id` bigint DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `user_role` varchar(20) DEFAULT NULL,
  `version` int DEFAULT '0',
  `task_id` varchar(100) DEFAULT NULL,
  `message_id` varchar(100) DEFAULT NULL,
  `acknowledgement` text,
  `acknowledged_at` timestamp NULL DEFAULT NULL,
  `execution_result` text,
  `executed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_intervention_no` (`intervention_no`),
  KEY `idx_ai_agent` (`agent_id`),
  KEY `idx_ai_status` (`status`),
  KEY `idx_ai_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 日志表
CREATE TABLE IF NOT EXISTS `agent_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) NOT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `log_level` varchar(20) DEFAULT 'INFO',
  `message` text,
  `details` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_al_agent` (`agent_id`),
  KEY `idx_al_level` (`log_level`),
  KEY `idx_al_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 文件表
CREATE TABLE IF NOT EXISTS `agent_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(200) NOT NULL,
  `project_id` varchar(100) NOT NULL,
  `file_name` varchar(500) NOT NULL,
  `file_path` varchar(1000) NOT NULL,
  `file_size` bigint NOT NULL DEFAULT '0',
  `mime_type` varchar(100) DEFAULT NULL,
  `source` varchar(20) NOT NULL,
  `direction` varchar(20) NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `parent_version_id` bigint DEFAULT NULL,
  `file_hash` varchar(64) DEFAULT NULL,
  `created_by` varchar(200) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_af_agent` (`agent_id`),
  KEY `idx_af_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 能力表
CREATE TABLE IF NOT EXISTS `agent_capabilities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_role` varchar(50) NOT NULL,
  `capability_name` varchar(100) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `description` text,
  `param_schema` text,
  `method_name` varchar(100) DEFAULT NULL,
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0',
  `approval_type` varchar(50) DEFAULT NULL,
  `execution_type` varchar(20) NOT NULL DEFAULT 'java',
  `prompt_template` text,
  `target_agent_role` varchar(50) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '5',
  `category` varchar(50) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `max_retry` int NOT NULL DEFAULT '0',
  `cooldown_seconds` int NOT NULL DEFAULT '0',
  `timeout_seconds` int NOT NULL DEFAULT '0',
  `related_skill_ids` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cap_role` (`agent_role`),
  KEY `idx_cap_role_project` (`agent_role`, `project_id`),
  KEY `idx_cap_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 能力调用日志表
CREATE TABLE IF NOT EXISTS `capability_invocation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(200) NOT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `capability_name` varchar(100) NOT NULL,
  `params` text,
  `result` text,
  `status` varchar(30) NOT NULL,
  `approval_request_id` bigint DEFAULT NULL,
  `error_message` text,
  `duration_ms` bigint DEFAULT NULL,
  `reason` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cil_agent` (`agent_id`),
  KEY `idx_cil_project` (`project_id`),
  KEY `idx_cil_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent MCP 绑定表
CREATE TABLE IF NOT EXISTS `agent_mcp_bindings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_role` varchar(50) NOT NULL,
  `project_id` varchar(100) NOT NULL,
  `server_id` bigint NOT NULL,
  `tool_id` bigint DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '5',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_amb_agent` (`agent_role`, `project_id`),
  KEY `idx_amb_server` (`server_id`),
  KEY `idx_amb_tool` (`tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MCP 服务器表
CREATE TABLE IF NOT EXISTS `mcp_servers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `server_type` varchar(50) NOT NULL,
  `endpoint` varchar(500) NOT NULL,
  `description` text,
  `config` text,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MCP 工具表
CREATE TABLE IF NOT EXISTS `mcp_tools` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `server_id` bigint NOT NULL,
  `tool_name` varchar(100) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `description` text,
  `input_schema` text,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mt_server` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 告警相关表
-- ----------------------------

-- 告警规则表
CREATE TABLE IF NOT EXISTS `alert_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `rule_type` varchar(50) NOT NULL DEFAULT 'SYSTEM',
  `metric_name` varchar(100) NOT NULL,
  `condition_type` varchar(20) NOT NULL,
  `operator` varchar(20) NOT NULL DEFAULT '>',
  `threshold` decimal(20,4) NOT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'MEDIUM',
  `duration_seconds` int DEFAULT '0',
  `enabled` tinyint(1) DEFAULT '1',
  `rule_category` varchar(50) DEFAULT 'SYSTEM',
  `cooldown_seconds` int DEFAULT '300',
  `notification_channels` varchar(200) DEFAULT 'SYSTEM',
  `auto_resolve` tinyint(1) DEFAULT '0',
  `severity_level` int DEFAULT '1',
  `tags` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ar_metric` (`metric_name`),
  KEY `idx_ar_enabled` (`enabled`),
  KEY `idx_ar_category` (`rule_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 告警记录表
CREATE TABLE IF NOT EXISTS `alert_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL,
  `agent_id` varchar(50) DEFAULT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `title` varchar(200) NOT NULL DEFAULT 'Alert',
  `detail` text,
  `metric` varchar(100) DEFAULT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'MEDIUM',
  `rule_name` varchar(100) DEFAULT NULL,
  `metric_value` decimal(20,4) DEFAULT NULL,
  `trigger_value` decimal(20,4) DEFAULT NULL,
  `threshold_value` decimal(20,4) DEFAULT NULL,
  `threshold` decimal(20,4) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `acknowledged_by` varchar(50) DEFAULT NULL,
  `acknowledged_at` timestamp NULL DEFAULT NULL,
  `resolved_at` timestamp NULL DEFAULT NULL,
  `resolved_by` varchar(50) DEFAULT NULL,
  `resolution_notes` text,
  `resolution` text,
  `notified` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_alert_rule` (`rule_id`),
  KEY `idx_alert_status` (`status`),
  KEY `idx_alert_created` (`created_at`),
  CONSTRAINT `fk_alert_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 通知相关表
-- ----------------------------

-- 通知表
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text,
  `type` varchar(50) DEFAULT 'SYSTEM',
  `channel` varchar(20) DEFAULT 'SYSTEM',
  `priority` varchar(20) DEFAULT 'NORMAL',
  `link` varchar(500) DEFAULT NULL,
  `reference_id` varchar(100) DEFAULT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `read` tinyint(1) DEFAULT '0',
  `read_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user` (`user_id`),
  KEY `idx_notif_read` (`read`),
  KEY `idx_notif_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知偏好表
CREATE TABLE IF NOT EXISTS `notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `notification_type` varchar(50) NOT NULL,
  `channel` varchar(20) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_np_user_type_channel` (`user_id`, `notification_type`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知模板表
CREATE TABLE IF NOT EXISTS `notification_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(100) NOT NULL,
  `template_name` varchar(100) NOT NULL,
  `title_template` varchar(500) DEFAULT NULL,
  `content_template` text NOT NULL,
  `channel` varchar(20) NOT NULL DEFAULT 'SYSTEM',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_nt_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 项目相关表
-- ----------------------------

-- 项目成员表
CREATE TABLE IF NOT EXISTS `project_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'VIEWER',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_project_user` (`project_id`, `user_id`),
  KEY `idx_pm_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 项目 Token 绑定表
CREATE TABLE IF NOT EXISTS `project_token_bindings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) NOT NULL,
  `token_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ptb_project_token` (`project_id`, `token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 项目告警配置表
CREATE TABLE IF NOT EXISTS `project_alert_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) NOT NULL,
  `alert_type` varchar(50) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `threshold` double DEFAULT NULL,
  `notify_channels` varchar(200) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pac_project_type` (`project_id`, `alert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 项目通知配置表
CREATE TABLE IF NOT EXISTS `project_notification_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) NOT NULL,
  `notification_type` varchar(50) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `config` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pnc_project_type` (`project_id`, `notification_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Git 和代码审查相关表
-- ----------------------------

-- Git 仓库表
CREATE TABLE IF NOT EXISTS `git_repositories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `url` varchar(500) NOT NULL,
  `branch` varchar(100) DEFAULT 'main',
  `description` varchar(500) DEFAULT NULL,
  `local_path` varchar(500) DEFAULT NULL,
  `last_sync_at` timestamp NULL DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_gr_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码审查表
CREATE TABLE IF NOT EXISTS `code_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` text,
  `project_id` varchar(100) DEFAULT NULL,
  `git_repository_id` bigint DEFAULT NULL,
  `git_repository_name` varchar(100) DEFAULT NULL,
  `repository_type` varchar(50) DEFAULT NULL,
  `branch` varchar(100) DEFAULT NULL,
  `commit_hash` varchar(100) DEFAULT NULL,
  `diff_content` text,
  `changed_files` int DEFAULT NULL,
  `review_type` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `reviewer` varchar(50) DEFAULT NULL,
  `review_comment` text,
  `score` int DEFAULT NULL,
  `issue_count` int DEFAULT '0',
  `warning_count` int DEFAULT '0',
  `auto_review_result` text,
  `submitted_at` timestamp NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `agent_id` varchar(50) DEFAULT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cr_project` (`project_id`),
  KEY `idx_cr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- CICD 流水线相关表
-- ----------------------------

-- 流水线表
CREATE TABLE IF NOT EXISTS `pipelines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_no` varchar(50) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `description` text,
  `project_id` varchar(100) NOT NULL,
  `project_name` varchar(200) DEFAULT NULL,
  `pipeline_type` varchar(50) DEFAULT NULL,
  `config` text,
  `git_branch` varchar(100) DEFAULT NULL,
  `git_commit` varchar(100) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'IDLE',
  `progress` int DEFAULT '0',
  `current_stage` varchar(100) DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pipeline_no` (`pipeline_no`),
  KEY `idx_pipe_project` (`project_id`),
  KEY `idx_pipe_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 流水线阶段表
CREATE TABLE IF NOT EXISTS `pipeline_stages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_id` bigint NOT NULL,
  `stage_name` varchar(100) NOT NULL,
  `stage_type` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `order_index` int NOT NULL DEFAULT '0',
  `config` text,
  `started_at` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `error_message` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ps_pipeline` (`pipeline_id`),
  CONSTRAINT `fk_ps_pipeline` FOREIGN KEY (`pipeline_id`) REFERENCES `pipelines` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Token 相关表
-- ----------------------------

-- API Token 表
CREATE TABLE IF NOT EXISTS `api_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(255) NOT NULL,
  `token_name` varchar(100) NOT NULL DEFAULT '',
  `api_key` varchar(255) NOT NULL DEFAULT '',
  `api_url` varchar(500) DEFAULT NULL,
  `model` varchar(100) DEFAULT NULL,
  `max_tokens` int DEFAULT NULL,
  `priority` varchar(20) DEFAULT NULL,
  `agent_tags` varchar(500) DEFAULT NULL,
  `assigned_agent_id` varchar(50) DEFAULT NULL,
  `assigned_agent_name` varchar(100) DEFAULT NULL,
  `usage_count` int DEFAULT '0',
  `total_tokens_used` bigint DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `created_by` varchar(50) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `permissions` text,
  `expires_at` timestamp NULL DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_at_user` (`user_id`),
  KEY `idx_at_expires` (`expires_at`),
  CONSTRAINT `fk_at_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Token 使用记录表
CREATE TABLE IF NOT EXISTS `token_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_id` bigint NOT NULL,
  `agent_id` varchar(50) DEFAULT NULL,
  `tokens_used` int NOT NULL DEFAULT '0',
  `usage_type` varchar(50) DEFAULT NULL,
  `usage_date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tu_token` (`token_id`),
  KEY `idx_tu_date` (`usage_date`),
  CONSTRAINT `fk_tu_token` FOREIGN KEY (`token_id`) REFERENCES `api_tokens` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 其他功能表
-- ----------------------------

-- 设备信任表
CREATE TABLE IF NOT EXISTS `device_trusts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_fingerprint` varchar(255) NOT NULL,
  `device_name` varchar(100) DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `trusted_at` timestamp NULL DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `expires_at` timestamp NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dt_user` (`user_id`),
  KEY `idx_dt_fingerprint` (`device_fingerprint`),
  KEY `idx_dt_expires` (`expires_at`),
  CONSTRAINT `fk_dt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `agent_id` varchar(50) DEFAULT NULL,
  `operation` varchar(100) NOT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `target_id` varchar(100) DEFAULT NULL,
  `detail` text,
  `ip_address` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'SUCCESS',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ol_user` (`user_id`),
  KEY `idx_ol_operation` (`operation`),
  KEY `idx_ol_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 审批请求表
CREATE TABLE IF NOT EXISTS `approval_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `requester_id` varchar(200) NOT NULL,
  `request_type` varchar(50) NOT NULL,
  `request_data` text,
  `description` text,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `approver_id` varchar(200) DEFAULT NULL,
  `approver_name` varchar(100) DEFAULT NULL,
  `approval_comment` text,
  `priority` int NOT NULL DEFAULT '5',
  `approved_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ar_project` (`project_id`),
  KEY `idx_ar_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) NOT NULL,
  `knowledge_key` varchar(200) NOT NULL,
  `title` varchar(500) NOT NULL,
  `content` text NOT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `access_level` varchar(20) NOT NULL DEFAULT 'project',
  `required_permissions` varchar(500) DEFAULT NULL,
  `tags` varchar(500) DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT '1',
  `priority` int DEFAULT '10',
  `created_by` varchar(100) DEFAULT NULL,
  `usage_count` bigint DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kb_category` (`category`),
  KEY `idx_kb_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文档索引表
CREATE TABLE IF NOT EXISTS `document_index` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_path` varchar(500) NOT NULL,
  `file_name` varchar(500) NOT NULL,
  `doc_type` varchar(50) NOT NULL,
  `agent_id` varchar(100) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `title` varchar(500) DEFAULT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `keywords` varchar(500) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `content_hash` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_di_agent` (`agent_id`),
  KEY `idx_di_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 绩效指标表
CREATE TABLE IF NOT EXISTS `performance_metrics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `agent_id` varchar(50) DEFAULT NULL,
  `metric_name` varchar(100) NOT NULL,
  `metric_value` double NOT NULL,
  `metric_type` varchar(50) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pm_agent` (`agent_id`),
  KEY `idx_pm_metric` (`metric_name`),
  KEY `idx_pm_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 绩效评审表
CREATE TABLE IF NOT EXISTS `performance_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_no` varchar(50) DEFAULT NULL,
  `agent_id` varchar(50) NOT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `agent_role` varchar(50) DEFAULT NULL,
  `producer_id` varchar(50) NOT NULL,
  `producer_name` varchar(100) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `project_name` varchar(200) DEFAULT NULL,
  `review_period` varchar(20) NOT NULL,
  `quality_score` int DEFAULT NULL,
  `efficiency_score` int DEFAULT NULL,
  `collaboration_score` int DEFAULT NULL,
  `innovation_score` int DEFAULT NULL,
  `overall_score` int DEFAULT NULL,
  `strengths` text,
  `improvements` text,
  `comments` text,
  `highlights` text,
  `status` varchar(20) DEFAULT 'COMPLETED',
  `is_warning` tinyint(1) DEFAULT '0',
  `warning_reason` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_no` (`review_no`),
  KEY `idx_pr_agent` (`agent_id`),
  KEY `idx_pr_producer` (`producer_id`),
  KEY `idx_pr_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 解雇申请表
CREATE TABLE IF NOT EXISTS `dismissal_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_no` varchar(50) DEFAULT NULL,
  `agent_id` varchar(50) NOT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `agent_role` varchar(50) DEFAULT NULL,
  `is_system_role` tinyint(1) DEFAULT '0',
  `producer_id` varchar(50) NOT NULL,
  `producer_name` varchar(100) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `project_name` varchar(200) DEFAULT NULL,
  `reason_type` varchar(50) DEFAULT NULL,
  `reason` text,
  `warning_count` int DEFAULT '0',
  `last_warning_at` datetime DEFAULT NULL,
  `last_warning_reason` text,
  `performance_history` text,
  `consecutive_low_score_periods` int DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `approval_comment` text,
  `rejection_reason` text,
  `executed_at` datetime DEFAULT NULL,
  `executed_by` varchar(50) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dr_request_no` (`request_no`),
  KEY `idx_dr_agent` (`agent_id`),
  KEY `idx_dr_producer` (`producer_id`),
  KEY `idx_dr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 制作人更换记录表
CREATE TABLE IF NOT EXISTS `producer_replacements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `replacement_no` varchar(50) DEFAULT NULL,
  `project_id` varchar(100) NOT NULL,
  `project_name` varchar(200) DEFAULT NULL,
  `old_producer_id` varchar(50) NOT NULL,
  `old_producer_name` varchar(100) DEFAULT NULL,
  `new_producer_id` varchar(50) NOT NULL,
  `new_producer_name` varchar(100) DEFAULT NULL,
  `reason_type` varchar(50) DEFAULT NULL,
  `reason` text,
  `new_guidelines` text,
  `admin_id` bigint DEFAULT NULL,
  `admin_name` varchar(50) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pr_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 招聘申请表
CREATE TABLE IF NOT EXISTS `recruitment_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_no` varchar(50) DEFAULT NULL,
  `producer_id` varchar(50) NOT NULL,
  `producer_name` varchar(100) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `role` varchar(50) NOT NULL,
  `role_name` varchar(100) DEFAULT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `description` text,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(100) DEFAULT NULL,
  `approval_comment` text,
  `approved_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rr_producer` (`producer_id`),
  KEY `idx_rr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 干预表
CREATE TABLE IF NOT EXISTS `interventions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `content` text,
  `status` varchar(20) DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_iv_agent` (`agent_id`),
  KEY `idx_iv_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat 会话表
CREATE TABLE IF NOT EXISTS `chat_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL DEFAULT '新对话',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cs_user` (`user_id`),
  KEY `idx_cs_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat 消息表
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `content` text,
  `thinking` text,
  `tool_uses` text,
  `tasks` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cm_session` (`session_id`),
  CONSTRAINT `fk_cm_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agent 预设表
CREATE TABLE IF NOT EXISTS `agent_presets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `preset_name` varchar(100) NOT NULL,
  `description` text,
  `agent_role` varchar(50) NOT NULL,
  `config` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认角色
INSERT INTO `roles` (`id`, `name`, `display_name`, `description`, `is_system`) VALUES
(1, 'ADMIN', '系统管理员', '系统管理员，拥有所有权限', 1),
(2, 'PROJECT_MANAGER', '项目经理', '项目经理，项目和Agent管理权限', 1),
(3, 'DEVELOPER', '开发者', '开发者，项目开发权限', 1),
(4, 'OPS_ENGINEER', '运维工程师', '运维工程师，系统运维和监控权限', 1),
(5, 'OBSERVER', '观察者', '观察者，只读权限', 1),
(6, 'USER', '普通用户', '普通用户，基础权限', 1),
(7, 'READONLY', '只读访客', '只读权限，可查看所有模块但不能修改，供外部人员了解系统特性', 1);

-- 默认管理员用户 (密码: REDACTED_PASSWORD)
INSERT INTO `users` (`id`, `username`, `password`, `email`, `nickname`, `role_id`, `status`, `password_changed`) VALUES
(1, 'admin', '$2a$10$REDACTED_BCRYPT_HASH_PLACEHOLDER_V1', 'admin@example.com', '管理员', 1, 'APPROVED', 1);

-- 角色权限关联
-- ADMIN 管理员：拥有所有权限
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(1, 'dashboard:view'), (1, 'agents:view'), (1, 'agents:manage'), (1, 'agents:task'),
(1, 'ai:use'), (1, 'ai:admin'), (1, 'projects:view'), (1, 'projects:manage'),
(1, 'skills:view'), (1, 'skills:manage'), (1, 'tokens:view'), (1, 'tokens:manage'),
(1, 'notification:view'), (1, 'notification:manage'), (1, 'code:review'), (1, 'knowledge:manage'),
(1, 'pipeline:view'), (1, 'pipeline:manage'), (1, 'pipeline:execute'), (1, 'pipeline:approve'), (1, 'pipeline:intervene'),
(1, 'workflow:view'), (1, 'workflow:manage'), (1, 'approval:view'), (1, 'approval:manage'),
(1, 'users:view'), (1, 'users:manage'), (1, 'roles:manage'), (1, 'logs:view'),
(1, 'system:view'), (1, 'system:monitor'), (1, 'system:monitor:manage'), (1, 'system:config'),
(1, 'system:config:manage'), (1, 'system:manage'), (1, 'admin:manage'), (1, 'terminal:use');

-- PROJECT_MANAGER 项目经理：项目和Agent管理
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(2, 'dashboard:view'), (2, 'agents:view'), (2, 'agents:manage'), (2, 'agents:task'),
(2, 'ai:use'), (2, 'projects:view'), (2, 'projects:manage'), (2, 'skills:view'), (2, 'skills:manage'),
(2, 'tokens:view'), (2, 'notification:view'), (2, 'notification:manage'), (2, 'code:review'), (2, 'knowledge:manage'),
(2, 'pipeline:view'), (2, 'pipeline:manage'), (2, 'pipeline:execute'), (2, 'pipeline:approve'), (2, 'pipeline:intervene'),
(2, 'workflow:view'), (2, 'workflow:manage'), (2, 'approval:view'), (2, 'approval:manage'), (2, 'logs:view'),
(2, 'system:view'), (2, 'system:monitor');

-- DEVELOPER 开发者：开发相关权限
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(3, 'dashboard:view'), (3, 'agents:view'), (3, 'agents:task'), (3, 'ai:use'),
(3, 'projects:view'), (3, 'skills:view'), (3, 'tokens:view'), (3, 'notification:view'), (3, 'code:review'),
(3, 'pipeline:view'), (3, 'pipeline:execute'), (3, 'workflow:view'), (3, 'approval:view');

-- OPS_ENGINEER 运维工程师：运维和监控权限
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(4, 'dashboard:view'), (4, 'agents:view'), (4, 'projects:view'),
(4, 'notification:view'), (4, 'notification:manage'),
(4, 'pipeline:view'), (4, 'pipeline:manage'), (4, 'pipeline:execute'), (4, 'pipeline:approve'), (4, 'pipeline:intervene'),
(4, 'workflow:view'), (4, 'workflow:manage'), (4, 'logs:view'),
(4, 'system:view'), (4, 'system:monitor'), (4, 'system:monitor:manage'), (4, 'system:config');

-- OBSERVER 观察者：只读权限
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(5, 'dashboard:view'), (5, 'agents:view'), (5, 'ai:use'), (5, 'projects:view'), (5, 'skills:view'),
(5, 'tokens:view'), (5, 'notification:view'), (5, 'code:review'), (5, 'pipeline:view'), (5, 'workflow:view'),
(5, 'approval:view'), (5, 'logs:view'), (5, 'system:view'), (5, 'system:monitor');

-- USER 普通用户：基础权限
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(6, 'dashboard:view'), (6, 'ai:use'), (6, 'projects:view'), (6, 'notification:view'), (6, 'system:view');

-- READONLY 只读访客：所有模块只读
INSERT INTO `role_permissions` (`role_id`, `permission`) VALUES
(7, 'dashboard:view'), (7, 'agents:view'), (7, 'ai:use'), (7, 'projects:view'), (7, 'skills:view'),
(7, 'tokens:view'), (7, 'notification:view'), (7, 'code:review'), (7, 'pipeline:view'), (7, 'workflow:view'),
(7, 'approval:view'), (7, 'users:view'), (7, 'logs:view'), (7, 'system:view'), (7, 'system:monitor');

-- 权限定义
INSERT INTO `permission_definitions` (`permission_key`, `name`, `description`, `category`, `enabled`, `system`, `sort_order`) VALUES
-- 工作台
('dashboard:view', '仪表盘查看', '查看仪表盘和系统概览', '工作台', 1, 1, 1),
-- Agent
('agents:view', 'Agent查看', '查看Agent列表、状态、详情', 'Agent', 1, 1, 1),
('agents:manage', 'Agent管理', '启动、停止、重启Agent，修改Agent配置', 'Agent', 1, 1, 2),
('agents:task', 'Agent任务', '向Agent发送任务和指令', 'Agent', 1, 1, 3),
-- AI
('ai:use', 'AI助手使用', '使用AI助手进行对话', 'AI', 1, 1, 1),
('ai:admin', 'AI助手管理', '管理AI配置、知识库、技能生成', 'AI', 1, 1, 2),
-- 项目
('projects:view', '项目查看', '查看项目列表和详情', '项目', 1, 1, 1),
('projects:manage', '项目管理', '创建、编辑、删除项目，管理项目配置', '项目', 1, 1, 2),
-- 技能
('skills:view', '技能查看', '查看技能列表和详情', '技能', 1, 1, 1),
('skills:manage', '技能管理', '创建、编辑、删除技能，AI生成技能', '技能', 1, 1, 2),
-- Token
('tokens:view', 'Token查看', '查看Token列表和用量统计', 'Token', 1, 1, 1),
('tokens:manage', 'Token管理', '创建、编辑、删除Token，分配Token给Agent', 'Token', 1, 1, 2),
-- 通知
('notification:view', '通知查看', '查看系统通知和消息', '通知', 1, 1, 1),
('notification:manage', '通知管理', '管理系统通知、模板、清理无效通知', '通知', 1, 1, 2),
-- 代码
('code:review', '代码审查', '查看和执行代码审查', '代码', 1, 1, 1),
-- 知识库
('knowledge:manage', '知识库管理', '管理知识库、知识进化、文档索引', '知识库', 1, 1, 1),
-- 流水线
('pipeline:view', '流水线查看', '查看CI/CD流水线列表和状态', '流水线', 1, 1, 1),
('pipeline:manage', '流水线管理', '创建、编辑、删除流水线', '流水线', 1, 1, 2),
('pipeline:execute', '流水线执行', '触发流水线执行', '流水线', 1, 1, 3),
('pipeline:approve', '流水线审批', '审批流水线执行请求', '流水线', 1, 1, 4),
('pipeline:intervene', '流水线干预', '干预正在执行的流水线', '流水线', 1, 1, 5),
-- 工作流
('workflow:view', '工作流查看', '查看工作流模板和实例', '工作流', 1, 1, 1),
('workflow:manage', '工作流管理', '创建、编辑、管理工作流', '工作流', 1, 1, 2),
-- 审批
('approval:view', '审批查看', '查看审批记录和流程', '审批', 1, 1, 1),
('approval:manage', '审批管理', '处理审批请求，批准或驳回', '审批', 1, 1, 2),
-- 用户
('users:view', '用户查看', '查看用户列表和详情', '用户', 1, 1, 1),
('users:manage', '用户管理', '创建、编辑、删除用户，审批注册', '用户', 1, 1, 2),
-- 角色
('roles:manage', '角色管理', '创建、编辑、删除角色，分配权限', '角色', 1, 1, 1),
-- 日志
('logs:view', '日志查看', '查看操作日志和审计记录', '日志', 1, 1, 1),
-- 系统
('system:view', '系统查看', '查看系统信息和状态', '系统', 1, 1, 1),
('system:monitor', '系统监控', '查看系统监控、资源用量、Agent健康', '系统', 1, 1, 2),
('system:monitor:manage', '监控管理', '管理告警规则、处理告警', '系统', 1, 1, 3),
('system:config', '配置查看', '查看系统配置和常量', '系统', 1, 1, 4),
('system:config:manage', '配置管理', '修改系统配置和常量', '系统', 1, 1, 5),
('system:manage', '系统管理', '系统级管理操作', '系统', 1, 1, 6),
('admin:manage', '管理后台', '访问管理后台功能', '系统', 1, 1, 7),
('terminal:use', '终端使用', '使用系统终端执行命令', '系统', 1, 1, 8);
('PERM_workflow:manage', '工作流管理', '管理工作流', '工作流', 1, 1, 1),
('PERM_workflow:view', '工作流查看', '查看工作流', '工作流', 1, 1, 2);

-- 系统配置
INSERT INTO `system_configs` (`config_key`, `config_value`, `description`, `config_group`, `value_type`, `is_system_builtin`) VALUES
('system.name', 'ChengXun Game Maker', '系统名称', 'system', 'string', 1),
('system.version', '2.0.0', '系统版本', 'system', 'string', 1),
('agent.max_concurrent', '10', 'Agent 最大并发数', 'agent', 'int', 1),
('agent.task_timeout', '3600', 'Agent 任务超时时间（秒）', 'agent', 'int', 1),
('security.session_timeout', '1800', '会话超时时间（秒）', 'security', 'int', 1),
('security.max_login_attempts', '5', '最大登录尝试次数', 'security', 'int', 1);

