-- ============================================================
-- ChengXun Game Maker - 数据库初始化脚本
-- 版本: 2.0.0
-- 日期: 2026-06-04
-- 说明: 基于当前生产环境数据库导出，包含所有表结构和初始数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_capabilities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `capability_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `param_schema` text COLLATE utf8mb4_unicode_ci,
  `method_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0',
  `approval_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `execution_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'java',
  `prompt_template` text COLLATE utf8mb4_unicode_ci,
  `target_agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '5',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `max_retry` int NOT NULL DEFAULT '0',
  `cooldown_seconds` int NOT NULL DEFAULT '0',
  `timeout_seconds` int NOT NULL DEFAULT '0',
  `related_skill_ids` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cap_role` (`agent_role`),
  KEY `idx_cap_role_project` (`agent_role`,`project_id`),
  KEY `idx_cap_name_role` (`capability_name`,`agent_role`),
  KEY `idx_cap_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL DEFAULT '0',
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `direction` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `parent_version_id` bigint DEFAULT NULL,
  `file_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_af_agent` (`agent_id`),
  KEY `idx_af_project` (`project_id`),
  KEY `idx_af_name` (`file_name`),
  KEY `idx_af_source` (`source`),
  KEY `idx_af_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_health` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `health_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HEALTHY',
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
  `last_error_message` text COLLATE utf8mb4_unicode_ci,
  `consecutive_errors` int DEFAULT '0',
  `uptime_seconds` bigint DEFAULT '0',
  `check_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属项目 ID',
  PRIMARY KEY (`id`),
  KEY `idx_health_agent` (`agent_id`),
  KEY `idx_health_status` (`health_status`),
  KEY `idx_health_time` (`check_time`),
  KEY `idx_health_project` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_interventions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `intervention_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intervention_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `instruction` text COLLATE utf8mb4_unicode_ci,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `original_decision` text COLLATE utf8mb4_unicode_ci,
  `new_decision` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `user_id` bigint DEFAULT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `acknowledged_at` timestamp NULL DEFAULT NULL,
  `executed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int DEFAULT '0',
  `task_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `acknowledgement` text COLLATE utf8mb4_unicode_ci,
  `execution_result` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `intervention_id` (`intervention_no`),
  KEY `idx_intervention_agent` (`agent_id`),
  KEY `idx_intervention_user` (`user_id`),
  KEY `idx_intervention_type` (`intervention_type`),
  KEY `idx_intervention_created` (`created_at`),
  KEY `idx_intervention_status` (`status`),
  KEY `idx_agent_interventions_agent_status` (`agent_id`,`status`),
  KEY `idx_agent_interventions_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `log_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'INFO',
  `message` text COLLATE utf8mb4_unicode_ci,
  `details` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_logs_agent_id` (`agent_id`),
  KEY `idx_agent_logs_log_level` (`log_level`),
  KEY `idx_agent_logs_created_at` (`created_at`),
  KEY `idx_agent_logs_agent_level` (`agent_id`,`log_level`),
  KEY `idx_agent_logs_created_level` (`created_at`,`log_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_mcp_bindings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `server_id` bigint NOT NULL,
  `tool_id` bigint DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '5',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_amb_agent` (`agent_role`,`project_id`),
  KEY `idx_amb_server` (`server_id`),
  KEY `idx_amb_tool` (`tool_id`),
  CONSTRAINT `agent_mcp_bindings_ibfk_1` FOREIGN KEY (`server_id`) REFERENCES `mcp_servers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `agent_mcp_bindings_ibfk_2` FOREIGN KEY (`tool_id`) REFERENCES `mcp_tools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_performance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属项目 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `agent_id` (`agent_id`),
  KEY `idx_perf_agent_id` (`agent_id`),
  KEY `idx_perf_role` (`agent_role`),
  KEY `idx_perf_updated` (`updated_at`),
  KEY `idx_perf_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Alert',
  `detail` text COLLATE utf8mb4_unicode_ci,
  `metric` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metric_value` decimal(20,4) DEFAULT NULL,
  `trigger_value` decimal(20,4) DEFAULT NULL,
  `threshold_value` decimal(20,4) DEFAULT NULL,
  `threshold` decimal(20,4) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `acknowledged_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `acknowledged_at` timestamp NULL DEFAULT NULL,
  `resolved_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL,
  `rule_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规则类别：AGENT, DATABASE, CACHE, SYSTEM',
  `severity_level` int DEFAULT '1' COMMENT '严重程度等级：1-5，5最严重',
  `resolved_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '解决人用户ID',
  `resolution_notes` text COLLATE utf8mb4_unicode_ci COMMENT '解决说明和处理措施',
  `resolution` text COLLATE utf8mb4_unicode_ci,
  `notified` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_alert_records_rule_id` (`rule_id`),
  KEY `idx_alert_records_status` (`status`),
  KEY `idx_alert_records_created_at` (`created_at`),
  KEY `idx_alert_records_rule_status` (`rule_id`,`status`),
  KEY `idx_alert_records_created_status` (`created_at`,`status`),
  KEY `idx_alert_records_category` (`rule_category`),
  KEY `idx_alert_records_severity` (`severity_level`),
  KEY `idx_alert_record_agent_id` (`agent_id`),
  KEY `idx_alert_record_project_id` (`project_id`),
  KEY `idx_alert_record_status` (`status`),
  CONSTRAINT `alert_records_ibfk_1` FOREIGN KEY (`rule_id`) REFERENCES `alert_rules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rule_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM',
  `metric_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `condition_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '>',
  `threshold` decimal(20,4) NOT NULL,
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM',
  `duration_seconds` int DEFAULT '0',
  `enabled` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `rule_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '规则类别：AGENT, DATABASE, CACHE, SYSTEM',
  `cooldown_seconds` int DEFAULT '300' COMMENT '告警冷却时间（秒），避免重复告警',
  `notification_channels` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '通知渠道：SYSTEM, FEISHU, EMAIL, MULTI',
  `auto_resolve` tinyint(1) DEFAULT '0' COMMENT '是否自动解决告警',
  `severity_level` int DEFAULT '1' COMMENT '严重程度等级：1-5，5最严重',
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签，用于分类和筛选',
  PRIMARY KEY (`id`),
  KEY `idx_alert_rules_metric` (`metric_name`),
  KEY `idx_alert_rules_enabled` (`enabled`),
  KEY `idx_alert_rules_category` (`rule_category`),
  KEY `idx_alert_rules_severity` (`severity_level`),
  KEY `idx_alert_rule_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `api_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `api_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `api_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `max_tokens` int DEFAULT NULL,
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assigned_agent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assigned_agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usage_count` int DEFAULT '0',
  `total_tokens_used` bigint DEFAULT '0',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `permissions` text COLLATE utf8mb4_unicode_ci,
  `expires_at` timestamp NULL DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `idx_api_tokens_user_id` (`user_id`),
  KEY `idx_api_tokens_token` (`token`),
  KEY `idx_api_tokens_expires_at` (`expires_at`),
  KEY `idx_api_tokens_token_expires` (`token`,`expires_at`),
  KEY `idx_api_tokens_user_expires` (`user_id`,`expires_at`),
  CONSTRAINT `api_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requester_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_data` text COLLATE utf8mb4_unicode_ci,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `approver_id` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approval_comment` text COLLATE utf8mb4_unicode_ci,
  `priority` int NOT NULL DEFAULT '5',
  `approved_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ar_project` (`project_id`),
  KEY `idx_ar_status` (`status`),
  KEY `idx_ar_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `capability_invocation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `capability_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `params` text COLLATE utf8mb4_unicode_ci,
  `result` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approval_request_id` bigint DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `duration_ms` bigint DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cil_agent` (`agent_id`),
  KEY `idx_cil_project` (`project_id`),
  KEY `idx_cil_status` (`status`),
  KEY `idx_cil_created` (`created_at`),
  KEY `idx_cil_cap_name` (`capability_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `content` text,
  `thinking` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_session` (`session_id`),
  CONSTRAINT `chat_messages_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL DEFAULT '新对话',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_sessions_user` (`user_id`),
  KEY `idx_chat_sessions_updated` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `git_repository_id` bigint DEFAULT NULL,
  `git_repository_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `repository_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `commit_hash` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `diff_content` text COLLATE utf8mb4_unicode_ci,
  `changed_files` int DEFAULT NULL,
  `review_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `reviewer` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_comment` text COLLATE utf8mb4_unicode_ci,
  `score` int DEFAULT NULL,
  `issue_count` int DEFAULT '0',
  `warning_count` int DEFAULT '0',
  `auto_review_result` text COLLATE utf8mb4_unicode_ci,
  `submitted_at` timestamp NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_project` (`project_id`),
  KEY `idx_review_status` (`status`),
  KEY `idx_review_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_trusts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_fingerprint` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trusted_at` timestamp NULL DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expires_at` timestamp NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_trusts_user_id` (`user_id`),
  KEY `idx_device_trusts_fingerprint` (`device_fingerprint`),
  KEY `idx_device_trusts_expires_at` (`expires_at`),
  KEY `idx_device_trusts_fingerprint_user` (`device_fingerprint`,`user_id`),
  KEY `idx_device_trusts_expires` (`expires_at`),
  CONSTRAINT `device_trusts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dismissal_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_system_role` tinyint(1) DEFAULT '0',
  `producer_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `producer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `warning_count` int DEFAULT '0',
  `last_warning_at` datetime DEFAULT NULL,
  `last_warning_reason` text COLLATE utf8mb4_unicode_ci,
  `performance_history` text COLLATE utf8mb4_unicode_ci,
  `consecutive_low_score_periods` int DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `approval_comment` text COLLATE utf8mb4_unicode_ci,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `executed_at` datetime DEFAULT NULL,
  `executed_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `request_no` (`request_no`),
  KEY `idx_dismissal_agent` (`agent_id`),
  KEY `idx_dismissal_producer` (`producer_id`),
  KEY `idx_dismissal_status` (`status`),
  KEY `idx_dismissal_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `git_repositories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `repository_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MAIN',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remote_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'main',
  `local_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `relative_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `default_branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'main',
  `current_branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_sync_at` timestamp NULL DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `assigned_agents` text COLLATE utf8mb4_unicode_ci,
  `review_rules` text COLLATE utf8mb4_unicode_ci,
  `auto_review_enabled` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_git_repositories_name` (`name`),
  KEY `idx_git_repositories_status` (`status`),
  KEY `idx_git_repositories_status_updated` (`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interventions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `urgency` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL',
  `duration` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ONCE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `processed` tinyint(1) DEFAULT '0',
  `processed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_interventions_agent_id` (`agent_id`),
  KEY `idx_interventions_user_id` (`user_id`),
  KEY `idx_interventions_processed` (`processed`),
  KEY `idx_interventions_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `knowledge_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `access_level` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'public',
  `required_permissions` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` int NOT NULL DEFAULT '5',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `usage_count` int DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kb_category` (`category`),
  KEY `idx_kb_project` (`project_id`),
  KEY `idx_kb_access` (`access_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mcp_servers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `transport_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `command` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `args` text COLLATE utf8mb4_unicode_ci,
  `env` text COLLATE utf8mb4_unicode_ci,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `headers` text COLLATE utf8mb4_unicode_ci,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `template` tinyint(1) NOT NULL DEFAULT '0',
  `template_key` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tool_count` int NOT NULL DEFAULT '0',
  `last_test_at` timestamp NULL DEFAULT NULL,
  `last_test_result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `connected` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_mcp_server_template_key` (`template_key`),
  KEY `idx_mcp_server_project` (`project_id`),
  KEY `idx_mcp_server_enabled` (`enabled`),
  KEY `idx_mcp_server_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mcp_tools` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `server_id` bigint NOT NULL,
  `tool_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `input_schema` text COLLATE utf8mb4_unicode_ci,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `call_count` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mcp_tool_server` (`server_id`),
  KEY `idx_mcp_tool_name` (`tool_name`),
  KEY `idx_mcp_tool_enabled` (`enabled`),
  CONSTRAINT `mcp_tools_ibfk_1` FOREIGN KEY (`server_id`) REFERENCES `mcp_servers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `notification_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `do_not_disturb` tinyint(1) NOT NULL DEFAULT '0',
  `quiet_start` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quiet_end` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_np_user_type_channel` (`user_id`,`notification_type`,`channel`),
  KEY `idx_np_user` (`user_id`),
  KEY `idx_np_type` (`notification_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码（唯一）',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知渠道：EMAIL, FEISHU, SYSTEM',
  `category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：ALERT, TASK, AGENT, SYSTEM',
  `subject` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主题（邮件主题/飞书标题）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容模板',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `system_builtin` tinyint(1) DEFAULT '0' COMMENT '是否系统内置',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_channel` (`channel`),
  KEY `idx_category` (`category`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` timestamp NULL DEFAULT NULL,
  `link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user_id` (`user_id`),
  KEY `idx_notifications_is_read` (`is_read`),
  KEY `idx_notifications_created_at` (`created_at`),
  KEY `idx_notifications_user_read` (`user_id`,`is_read`),
  KEY `idx_notifications_created` (`created_at`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUSTOM',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` text COLLATE utf8mb4_unicode_ci,
  `level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'INFO',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS',
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `operation` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUSTOM',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_params` text COLLATE utf8mb4_unicode_ci,
  `response_data` text COLLATE utf8mb4_unicode_ci,
  `duration_ms` bigint DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `response_status` int DEFAULT NULL,
  `execution_time` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_logs_user_id` (`user_id`),
  KEY `idx_operation_logs_operation` (`operation`),
  KEY `idx_operation_logs_created_at` (`created_at`),
  KEY `idx_operation_logs_created_user` (`created_at`,`user_id`),
  KEY `idx_operation_logs_operation_created` (`operation`,`created_at`),
  CONSTRAINT `operation_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `performance_metrics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `metric_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metric_value` decimal(20,4) DEFAULT NULL,
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metric_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `api_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `recorded_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_performance_metrics_name` (`metric_name`),
  KEY `idx_performance_metrics_type` (`metric_type`),
  KEY `idx_performance_metrics_recorded_at` (`recorded_at`),
  KEY `idx_performance_metrics_name_time` (`metric_name`,`recorded_at`),
  KEY `idx_performance_metrics_type_time` (`metric_type`,`recorded_at`),
  KEY `idx_metric_agent` (`agent_id`),
  KEY `idx_metric_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `performance_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `producer_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `producer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_period` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quality_score` int DEFAULT NULL,
  `efficiency_score` int DEFAULT NULL,
  `collaboration_score` int DEFAULT NULL,
  `innovation_score` int DEFAULT NULL,
  `overall_score` int DEFAULT NULL,
  `strengths` text COLLATE utf8mb4_unicode_ci,
  `improvements` text COLLATE utf8mb4_unicode_ci,
  `comments` text COLLATE utf8mb4_unicode_ci,
  `highlights` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'COMPLETED',
  `is_warning` tinyint(1) DEFAULT '0',
  `warning_reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `review_no` (`review_no`),
  KEY `idx_review_agent` (`agent_id`),
  KEY `idx_review_producer` (`producer_id`),
  KEY `idx_review_project` (`project_id`),
  KEY `idx_review_period` (`review_period`),
  KEY `idx_review_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `system` tinyint(1) NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission_key` (`permission_key`),
  KEY `idx_pd_key` (`permission_key`),
  KEY `idx_pd_category` (`category`),
  KEY `idx_pd_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approval_comment` text COLLATE utf8mb4_unicode_ci,
  `approved_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pr_user` (`user_id`),
  KEY `idx_pr_status` (`status`),
  KEY `idx_pr_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipelines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pipeline_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `current_stage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `progress` int DEFAULT '0',
  `config` text COLLATE utf8mb4_unicode_ci,
  `git_branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `git_commit` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trigger_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `triggered_by` bigint DEFAULT NULL,
  `triggered_by_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `auto_triggered` tinyint(1) DEFAULT '0',
  `auto_trigger_config` text COLLATE utf8mb4_unicode_ci,
  `requires_approval` tinyint(1) DEFAULT '0',
  `approval_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  `approved_by_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approval_comment` text COLLATE utf8mb4_unicode_ci,
  `approved_at` timestamp NULL DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `execution_log` text COLLATE utf8mb4_unicode_ci,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `production_deploy` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pipeline_no` (`pipeline_no`),
  KEY `idx_pipeline_project` (`project_id`),
  KEY `idx_pipeline_status` (`status`),
  KEY `idx_pipeline_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `producer_replacements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `replacement_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `old_producer_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_producer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_producer_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `new_producer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `new_guidelines` text COLLATE utf8mb4_unicode_ci,
  `admin_id` bigint DEFAULT NULL,
  `admin_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `replacement_no` (`replacement_no`),
  KEY `idx_replacement_project` (`project_id`),
  KEY `idx_replacement_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission`),
  KEY `idx_role_permissions_role_id` (`role_id`),
  CONSTRAINT `role_permissions_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_system` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_roles_name` (`name`),
  KEY `idx_roles_is_system` (`is_system`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci,
  `config_group` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `value_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'string',
  `is_system_builtin` tinyint(1) DEFAULT '0',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属项目 ID（null=全局默认）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  UNIQUE KEY `uk_config_key_project` (`config_key`,`project_id`),
  KEY `idx_system_configs_key` (`config_key`),
  KEY `idx_system_configs_group` (`config_group`),
  KEY `idx_system_configs_builtin` (`is_system_builtin`),
  KEY `idx_config_project` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_constants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `constant_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `default_value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'string',
  `group_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `min_value` bigint DEFAULT NULL,
  `max_value` bigint DEFAULT NULL,
  `require_restart` tinyint(1) NOT NULL DEFAULT '0',
  `system_builtin` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `constant_key` (`constant_key`),
  KEY `idx_sc_key` (`constant_key`),
  KEY `idx_sc_group` (`group_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `token_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usage_date` date NOT NULL DEFAULT (curdate()),
  `input_tokens` bigint DEFAULT '0',
  `output_tokens` bigint DEFAULT '0',
  `total_tokens` bigint DEFAULT '0',
  `call_count` int DEFAULT '0',
  `estimated_cost` decimal(10,4) DEFAULT '0.0000',
  `cost` decimal(10,4) DEFAULT '0.0000',
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_token_usage_agent_id` (`agent_id`),
  KEY `idx_token_usage_created_at` (`created_at`),
  KEY `idx_token_usage_agent_created` (`agent_id`,`created_at`),
  KEY `idx_token_usage_model_created` (`model`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `granted_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `expires_at` timestamp NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_up_user_perm` (`user_id`,`permission`),
  KEY `idx_up_user` (`user_id`),
  KEY `idx_up_perm` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `must_change_password` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_username` (`username`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_status` (`status`),
  KEY `idx_users_role_id` (`role_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始数据
-- ============================================================

-- 表: roles

  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_system` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_roles_name` (`name`),
  KEY `idx_roles_is_system` (`is_system`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `roles` (`id`, `name`, `display_name`, `description`, `is_system`, `created_at`, `updated_at`) VALUES (1,'ADMIN','管理员','系统管理员，拥有所有权限',1,'2026-06-04 01:19:14','2026-06-04 01:19:14'),(2,'PROJECT_MANAGER','项目经理','负责项目管理和 Agent 调度',1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(3,'DEVELOPER','开发者','使用 Agent 进行开发工作',1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(4,'OPS_ENGINEER','运维工程师','负责系统运维和部署',1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(5,'OBSERVER','观察者','只读权限，查看系统状态',1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(6,'USER','普通用户','普通用户，基础权限',1,'2026-06-04 01:24:44','2026-06-04 01:24:44');



-- 表: users

  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `must_change_password` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_username` (`username`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_status` (`status`),
  KEY `idx_users_role_id` (`role_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `users` (`id`, `username`, `password`, `email`, `nickname`, `avatar`, `role_id`, `status`, `must_change_password`, `created_at`, `updated_at`) VALUES (1,'admin','$2a$12$y5hE84SCGuTPyckiAX2s4ezDjkbRpn.8rM8nltfQuIApT7g5pQXFa','chengxun@88.com','管理员',NULL,1,'APPROVED',0,'2026-06-04 01:19:14','2026-06-04 01:21:35');



-- 表: role_permissions

  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission`),
  KEY `idx_role_permissions_role_id` (`role_id`),
  CONSTRAINT `role_permissions_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `role_permissions` (`id`, `role_id`, `permission`, `created_at`) VALUES (1,1,'system:monitor','2026-06-04 01:19:13'),(2,1,'admin:manage','2026-06-04 01:19:13'),(3,1,'agent:view','2026-06-04 01:19:13'),(4,1,'ai:admin','2026-06-04 01:19:13'),(5,1,'skills:manage','2026-06-04 01:19:13'),(6,1,'approval:manage','2026-06-04 01:19:13'),(7,1,'workflow:view','2026-06-04 01:19:13'),(8,1,'agent:manage','2026-06-04 01:19:13'),(9,1,'system:monitor:manage','2026-06-04 01:19:13'),(10,1,'notification:manage','2026-06-04 01:19:13'),(11,1,'pipeline:manage','2026-06-04 01:19:13'),(12,1,'notification:view','2026-06-04 01:19:13'),(13,1,'agents:manage','2026-06-04 01:19:13'),(14,1,'code:review','2026-06-04 01:19:13'),(15,1,'skills:view','2026-06-04 01:19:13'),(16,1,'logs:view','2026-06-04 01:19:13'),(17,1,'roles:manage','2026-06-04 01:19:13'),(18,1,'agents:task','2026-06-04 01:19:13'),(19,1,'pipeline:view','2026-06-04 01:19:13'),(20,1,'ai:use','2026-06-04 01:19:13'),(21,1,'system:config:manage','2026-06-04 01:19:13'),(22,1,'knowledge:manage','2026-06-04 01:19:13'),(23,1,'pipeline:approve','2026-06-04 01:19:13'),(24,1,'log:view','2026-06-04 01:19:13'),(25,1,'dashboard:view','2026-06-04 01:19:13'),(26,1,'terminal:use','2026-06-04 01:19:13'),(27,1,'system:view','2026-06-04 01:19:13'),(28,1,'workflow:manage','2026-06-04 01:19:13'),(29,1,'projects:view','2026-06-04 01:19:13'),(30,1,'pipeline:execute','2026-06-04 01:19:13'),(31,1,'users:manage','2026-06-04 01:19:13'),(32,1,'system:config','2026-06-04 01:19:13'),(33,1,'pipeline:intervene','2026-06-04 01:19:13'),(34,1,'tokens:view','2026-06-04 01:19:13'),(35,1,'projects:manage','2026-06-04 01:19:13'),(36,1,'system:manage','2026-06-04 01:19:13'),(37,1,'approval:view','2026-06-04 01:19:13'),(38,1,'tokens:manage','2026-06-04 01:19:13'),(39,1,'agents:view','2026-06-04 01:19:13'),(40,2,'system:monitor','2026-06-04 01:24:44'),(41,2,'pipeline:approve','2026-06-04 01:24:44'),(42,2,'dashboard:view','2026-06-04 01:24:44'),(43,2,'projects:view','2026-06-04 01:24:44'),(44,2,'workflow:view','2026-06-04 01:24:44'),(45,2,'workflow:manage','2026-06-04 01:24:44'),(46,2,'pipeline:execute','2026-06-04 01:24:44'),(47,2,'projects:edit','2026-06-04 01:24:44'),(48,2,'notification:manage','2026-06-04 01:24:44'),(49,2,'pipeline:manage','2026-06-04 01:24:44'),(50,2,'pipeline:create','2026-06-04 01:24:44'),(51,2,'agents:manage','2026-06-04 01:24:44'),(52,2,'pipeline:intervene','2026-06-04 01:24:44'),(53,2,'code:review','2026-06-04 01:24:44'),(54,2,'skills:view','2026-06-04 01:24:44'),(55,2,'projects:manage','2026-06-04 01:24:44'),(56,2,'agents:task','2026-06-04 01:24:44'),(57,2,'pipeline:view','2026-06-04 01:24:44'),(58,2,'ai:use','2026-06-04 01:24:44'),(59,2,'agents:view','2026-06-04 01:24:44'),(60,3,'dashboard:view','2026-06-04 01:24:44'),(61,3,'code:review','2026-06-04 01:24:44'),(62,3,'projects:view','2026-06-04 01:24:44'),(63,3,'skills:view','2026-06-04 01:24:44'),(64,3,'pipeline:execute','2026-06-04 01:24:44'),(65,3,'agents:task','2026-06-04 01:24:44'),(66,3,'pipeline:view','2026-06-04 01:24:44'),(67,3,'ai:use','2026-06-04 01:24:44'),(68,3,'agents:view','2026-06-04 01:24:44'),(69,4,'system:monitor','2026-06-04 01:24:44'),(70,4,'pipeline:approve','2026-06-04 01:24:44'),(71,4,'dashboard:view','2026-06-04 01:24:44'),(72,4,'projects:view','2026-06-04 01:24:44'),(73,4,'workflow:view','2026-06-04 01:24:44'),(74,4,'workflow:manage','2026-06-04 01:24:44'),(75,4,'pipeline:execute','2026-06-04 01:24:44'),(76,4,'system:monitor:manage','2026-06-04 01:24:44'),(77,4,'pipeline:manage','2026-06-04 01:24:44'),(78,4,'pipeline:create','2026-06-04 01:24:44'),(79,4,'pipeline:intervene','2026-06-04 01:24:44'),(80,4,'pipeline:view','2026-06-04 01:24:44'),(81,4,'agents:view','2026-06-04 01:24:44'),(82,5,'system:monitor','2026-06-04 01:24:44'),(83,5,'dashboard:view','2026-06-04 01:24:44'),(84,5,'projects:view','2026-06-04 01:24:44'),(85,5,'skills:view','2026-06-04 01:24:44'),(86,5,'pipeline:view','2026-06-04 01:24:44'),(87,5,'ai:use','2026-06-04 01:24:44'),(88,5,'agents:view','2026-06-04 01:24:44'),(89,6,'dashboard:view','2026-06-04 01:24:44'),(90,6,'projects:view','2026-06-04 01:24:44');



-- 表: permission_definitions

  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `system` tinyint(1) NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission_key` (`permission_key`),
  KEY `idx_pd_key` (`permission_key`),
  KEY `idx_pd_category` (`category`),
  KEY `idx_pd_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `permission_definitions` (`id`, `permission_key`, `name`, `description`, `category`, `enabled`, `system`, `sort_order`, `created_at`, `updated_at`) VALUES (1,'PERM_agents:manage','Agent 管理','创建、删除、配置 Agent','Agent',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(2,'PERM_agents:view','Agent 查看','查看 Agent 状态','Agent',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(3,'PERM_agents:task','Agent 任务','向 Agent 发送任务','Agent',1,1,3,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(4,'PERM_skills:view','技能查看','查看技能列表','技能',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(5,'PERM_skills:manage','技能管理','创建、编辑、删除技能','技能',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(6,'PERM_tokens:view','Token 查看','查看 Token 列表','Token',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(7,'PERM_tokens:manage','Token 管理','创建、分配 Token','Token',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(8,'PERM_projects:view','项目查看','查看项目列表','项目',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(9,'PERM_projects:manage','项目管理','创建、删除项目','项目',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(10,'PERM_system:monitor','系统监控','查看系统状态、上下文健康','系统',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(11,'PERM_system:manage','系统管理','配置系统参数','系统',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(12,'PERM_ai:use','AI助手使用','使用AI助手问答功能','AI',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(13,'PERM_ai:admin','AI助手管理','管理AI知识库','AI',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(14,'PERM_approval:view','审批查看','查看审批请求','审批',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(15,'PERM_approval:manage','审批管理','批准、拒绝审批请求','审批',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(16,'PERM_notification:view','通知查看','查看通知','通知',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(17,'PERM_notification:manage','通知管理','管理通知模板','通知',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(18,'PERM_admin:manage','管理员权限','所有功能','管理',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(19,'PERM_roles:manage','角色管理','创建、编辑角色','管理',1,1,2,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(20,'PERM_users:manage','用户管理','创建、编辑、删除用户','管理',1,1,3,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(21,'PERM_logs:view','日志查看','查看操作日志','管理',1,1,4,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(22,'PERM_code:review','代码审查','审查代码提交','项目',1,1,3,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(23,'PERM_pipeline:view','流水线查看','查看CICD流水线','项目',1,1,4,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(24,'PERM_pipeline:manage','流水线管理','管理CICD流水线','项目',1,1,5,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(25,'PERM_pipeline:execute','流水线执行','执行CICD流水线','项目',1,1,6,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(26,'PERM_pipeline:approve','流水线审批','审批CICD流水线','项目',1,1,7,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(27,'PERM_pipeline:intervene','流水线干预','干预CICD流水线','项目',1,1,8,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(28,'PERM_workflow:view','工作流查看','查看工作流','项目',1,1,9,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(29,'PERM_workflow:manage','工作流管理','管理工作流','项目',1,1,10,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(30,'PERM_dashboard:view','仪表盘查看','查看仪表盘','工作台',1,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(31,'PERM_terminal:use','终端使用','使用系统终端','系统',1,1,3,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(32,'PERM_system:view','系统查看','查看系统配置','系统',1,1,4,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(33,'PERM_system:config','系统配置','查看系统配置','系统',1,1,5,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(34,'PERM_system:config:manage','系统配置管理','管理系统配置','系统',1,1,6,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(35,'PERM_system:monitor:manage','系统监控管理','管理系统监控','系统',1,1,7,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(36,'PERM_knowledge:manage','知识库管理','管理知识库','项目',1,1,11,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(37,'PERM_log:view','日志查看','查看日志','管理',1,1,5,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(38,'PERM_agent:view','Agent查看','查看Agent状态','Agent',1,1,4,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(39,'PERM_agent:manage','Agent管理','管理Agent','Agent',1,1,5,'2026-06-04 01:24:44','2026-06-04 01:24:44');



-- 表: system_configs

  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci,
  `config_group` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `value_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'string',
  `is_system_builtin` tinyint(1) DEFAULT '0',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属项目 ID（null=全局默认）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  UNIQUE KEY `uk_config_key_project` (`config_key`,`project_id`),
  KEY `idx_system_configs_key` (`config_key`),
  KEY `idx_system_configs_group` (`config_group`),
  KEY `idx_system_configs_builtin` (`is_system_builtin`),
  KEY `idx_config_project` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `system_configs` (`id`, `config_key`, `config_value`, `config_group`, `value_type`, `is_system_builtin`, `description`, `created_at`, `updated_at`, `project_id`) VALUES (1,'system.name','ChengXun Game Maker','system','string',0,'系统名称','2026-06-04 01:18:41','2026-06-04 01:18:41',NULL),(2,'security.jwt.secret','REDACTED_JWT_SECRET','security','string',0,'JWT 密钥','2026-06-04 01:18:41','2026-06-04 01:18:41',NULL),(3,'claude.api.key','REDACTED_CLAUDE_API_KEY','agent','string',0,'Claude API Key','2026-06-04 01:18:58','2026-06-04 01:18:58',NULL),(4,'claude.api.url','https://token-plan-cn.xiaomimimo.com/anthropic','agent','string',0,'Claude API URL','2026-06-04 01:18:58','2026-06-04 01:18:58',NULL),(5,'claude.model','mimo-v2.5-pro[1m]','agent','string',0,'Claude 模型','2026-06-04 01:18:58','2026-06-04 01:18:58',NULL),(6,'security.password.min-length','8','security','number',1,'密码最小长度','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(7,'security.password.require-uppercase','true','security','boolean',1,'密码是否需要大写字母','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(8,'security.password.require-lowercase','true','security','boolean',1,'密码是否需要小写字母','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(9,'security.password.require-digit','true','security','boolean',1,'密码是否需要数字','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(10,'security.password.require-special','true','security','boolean',1,'密码是否需要特殊字符','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(11,'security.session.timeout-minutes','30','security','number',1,'会话超时时间（分钟）','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(12,'security.session.max-concurrent','1','security','number',1,'最大并发会话数','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(13,'security.login.max-attempts','5','security','number',1,'最大登录尝试次数','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(14,'security.login.lockout-minutes','15','security','number',1,'登录锁定时间（分钟）','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(15,'agent.task.max-retry','3','agent','number',1,'任务最大重试次数','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(16,'agent.task.retry-delay-ms','5000','agent','number',1,'任务重试延迟（毫秒）','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(17,'agent.task.max-queue-size','100','agent','number',1,'任务队列最大长度','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(18,'agent.message.max-size','10000','agent','number',1,'消息最大长度','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(19,'email.verification.expire-minutes','10','email','number',1,'验证码过期时间（分钟）','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(20,'email.verification.code-length','6','email','number',1,'验证码长度','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(21,'notification.enabled.channels','feishu,email','notification','string',1,'启用的通知渠道（逗号分隔）','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(22,'system.pagination.default-size','20','system','number',1,'默认分页大小','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL),(23,'system.pagination.max-size','100','system','number',1,'最大分页大小','2026-06-04 01:24:44','2026-06-04 01:24:44',NULL);



-- 表: system_constants

  `id` bigint NOT NULL AUTO_INCREMENT,
  `constant_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `default_value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'string',
  `group_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `min_value` bigint DEFAULT NULL,
  `max_value` bigint DEFAULT NULL,
  `require_restart` tinyint(1) NOT NULL DEFAULT '0',
  `system_builtin` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `constant_key` (`constant_key`),
  KEY `idx_sc_key` (`constant_key`),
  KEY `idx_sc_group` (`group_name`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `system_constants` (`id`, `constant_key`, `display_name`, `description`, `value`, `default_value`, `value_type`, `group_name`, `unit`, `min_value`, `max_value`, `require_restart`, `system_builtin`, `created_at`, `updated_at`) VALUES (1,'agent.max-message-backlog','最大消息积压','Agent 消息队列最大积压数量，超过此数视为上下文异常','50','50','int','agent','条',1,1000,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(2,'agent.max-idle-minutes','最大空闲时间','Agent 最大无响应时间，超过视为上下文失效','30','30','int','agent','分钟',1,1440,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(3,'agent.max-recovery-attempts','最大恢复尝试','上下文恢复最大尝试次数，超过则停止 Agent','3','3','int','agent','次',1,10,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(4,'agent.task-queue-size','任务队列大小','Agent 任务队列最大容量','500','500','int','agent','条',10,5000,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(5,'agent.message-queue-size','消息队列大小','Agent 消息队列最大容量','1000','1000','int','agent','条',10,10000,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(6,'agent.max-retry-count','最大重试次数','任务/消息失败后最大重试次数','3','3','int','agent','次',0,10,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(7,'agent.history-size','历史记录大小','任务/消息历史保留数量','1000','1000','int','agent','条',100,10000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(8,'agent.scheduler-interval-ms','调度间隔','Agent 调度器执行间隔','300000','300000','long','agent','毫秒',10000,3600000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(9,'agent.max-warnings','最大警告次数','绩效管理最大警告次数，超过触发解雇流程','3','3','int','agent','次',1,10,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(10,'agent.max-learned-knowledge','最大知识条数','Agent 最大学习知识数量','100','100','int','agent','条',10,1000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(11,'file.max-size-mb','单文件大小限制','单个文件最大上传大小','50','50','int','file','MB',1,500,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(12,'file.quota-mb','Agent 存储配额','每个 Agent 的文件存储配额','500','500','int','file','MB',10,5000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(13,'file.storage-dir','存储目录','文件存储根目录','data/files','data/files','string','file','',NULL,NULL,1,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(14,'security.max-login-attempts','最大登录尝试','登录失败最大尝试次数，超过锁定账户','5','5','int','security','次',1,20,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(15,'security.jwt-expiration-ms','JWT 过期时间','JWT Token 有效期','86400000','86400000','long','security','毫秒',3600000,604800000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(16,'security.session-timeout-minutes','会话超时','Web 会话超时时间','30','30','int','security','分钟',5,480,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(17,'rate-limit.global','全局限流','全局 API 每分钟请求限制','120','120','int','rate-limit','次/分钟',10,1000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(18,'rate-limit.auth','认证限流','登录接口每分钟请求限制','10','10','int','rate-limit','次/分钟',1,100,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(19,'rate-limit.write','写操作限流','写操作每分钟请求限制','60','60','int','rate-limit','次/分钟',5,500,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(20,'performance.max-backups','最大备份数','文件备份最大保留数量','10','10','int','performance','份',1,100,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(21,'performance.max-output-size','最大输出大小','沙箱执行最大输出大小','1048576','1048576','long','performance','字节',1024,10485760,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(22,'performance.ws-session-timeout','WebSocket 超时','终端 WebSocket 会话超时时间','1800000','1800000','long','performance','毫秒',60000,86400000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(23,'performance.context-compact-threshold','上下文压缩阈值','触发自动压缩的消息数','50','50','int','performance','条',10,500,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(24,'notification.batch-size','批量通知大小','批量发送通知的批次大小','100','100','int','notification','条',10,1000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(25,'mcp.discovery-timeout-ms','MCP 发现超时','MCP 工具发现超时时间','10000','10000','long','mcp','毫秒',1000,60000,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44'),(26,'file.version-limit','版本保留数','同名文件保留的最大版本数','10','10','int','file','个',1,100,0,1,'2026-06-04 01:24:44','2026-06-04 01:24:44');



-- 表: notification_templates

  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码（唯一）',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知渠道：EMAIL, FEISHU, SYSTEM',
  `category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：ALERT, TASK, AGENT, SYSTEM',
  `subject` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主题（邮件主题/飞书标题）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容模板',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `system_builtin` tinyint(1) DEFAULT '0' COMMENT '是否系统内置',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_channel` (`channel`),
  KEY `idx_category` (`category`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

INSERT INTO `notification_templates` (`id`, `template_code`, `template_name`, `channel`, `category`, `subject`, `content`, `description`, `enabled`, `system_builtin`, `created_at`, `updated_at`) VALUES (1,'TASK_FEISHU','任务飞书模板','FEISHU','TASK','任务通知','**任务通知**\n\n任务：${taskTitle}\n状态：${content}\n时间：${time}','用于发送任务相关飞书通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(2,'TASK_SYSTEM','任务站内通知模板','SYSTEM','TASK','任务通知：${taskTitle}','任务状态：${content}','用于发送任务站内通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(3,'AGENT_FEISHU','Agent飞书模板','FEISHU','AGENT','Agent通知','**Agent通知**\n\nAgent：${agentName}\n内容：${content}\n时间：${time}','用于发送Agent相关飞书通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(4,'AGENT_SYSTEM','Agent站内通知模板','SYSTEM','AGENT','Agent通知：${agentName}','通知内容：${content}','用于发送Agent站内通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(5,'RECOVERY_FEISHU','恢复通知飞书模板','FEISHU','ALERT','告警恢复','**告警恢复**\n\n规则：${ruleName}\n当前值：${triggerValue}\n时间：${time}','用于发送告警恢复飞书通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(6,'RECOVERY_DINGTALK','恢复通知钉钉模板','DINGTALK','ALERT','告警恢复','### 告警恢复\n\n**规则名称**：${ruleName}\n**当前值**：${triggerValue}\n**恢复时间**：${time}\n\n告警已恢复，系统运行正常。','用于发送告警恢复钉钉通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(7,'TASK_DINGTALK','任务钉钉模板','DINGTALK','TASK','任务通知','### 任务通知\n\n**任务标题**：${taskTitle}\n**任务状态**：${content}\n**通知时间**：${time}','用于发送任务钉钉通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37'),(8,'AGENT_DINGTALK','Agent钉钉模板','DINGTALK','AGENT','Agent通知','### Agent通知\n\n**Agent名称**：${agentName}\n**通知内容**：${content}\n**通知时间**：${time}','用于发送Agent钉钉通知',1,1,'2026-06-04 09:18:37','2026-06-04 09:18:37');



