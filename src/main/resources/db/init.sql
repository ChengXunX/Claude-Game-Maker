-- ============================================================
-- ChengXun Game Maker - 数据库初始化脚本
-- 版本: 3.0.0
-- 日期: 2026-06-06
-- 说明: 自动从生产环境导出，包含全部 57 张表结构和初始数据
-- 用法: mysql -u root -p game_maker < init.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: game_maker
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.22.04.1

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

--
-- Table structure for table `agent_capabilities`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_capabilities` (
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
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_files`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL DEFAULT '0',
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` enum('USER_UPLOAD','AGENT_GENERATED','MCP','AGENT_TRANSFER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `direction` enum('INBOUND','OUTBOUND') COLLATE utf8mb4_unicode_ci NOT NULL,
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

--
-- Table structure for table `agent_health`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_health` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `health_status` enum('HEALTHY','WARNING','UNHEALTHY','OFFLINE') COLLATE utf8mb4_unicode_ci NOT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_interventions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_interventions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `intervention_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intervention_type` enum('INSTRUCTION','DECISION_OVERRIDE','DIRECTION_CHANGE','PAUSE','RESUME','PRIORITY_CHANGE','TASK_CANCEL','TASK_REASSIGN','URGENT_INSTRUCTION','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `instruction` text COLLATE utf8mb4_unicode_ci,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `original_decision` text COLLATE utf8mb4_unicode_ci,
  `new_decision` text COLLATE utf8mb4_unicode_ci,
  `status` enum('PENDING','ACKNOWLEDGED','EXECUTING','COMPLETED','REJECTED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
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
  `version` bigint DEFAULT NULL,
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

--
-- Table structure for table `agent_logs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `log_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'INFO',
  `message` text COLLATE utf8mb4_unicode_ci,
  `details` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `decision` text COLLATE utf8mb4_unicode_ci,
  `detail` text COLLATE utf8mb4_unicode_ci,
  `duration_ms` bigint DEFAULT NULL,
  `level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `summary` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_agent_logs_agent_id` (`agent_id`),
  KEY `idx_agent_logs_log_level` (`log_level`),
  KEY `idx_agent_logs_created_at` (`created_at`),
  KEY `idx_agent_logs_agent_level` (`agent_id`,`log_level`),
  KEY `idx_agent_logs_created_level` (`created_at`,`log_level`),
  KEY `idx_agent_log_agent_id` (`agent_id`),
  KEY `idx_agent_log_action` (`action`),
  KEY `idx_agent_log_created` (`created_at`),
  KEY `idx_agent_log_level` (`level`)
) ENGINE=InnoDB AUTO_INCREMENT=3614 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_mcp_bindings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_mcp_bindings` (
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

--
-- Table structure for table `agent_performance`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_performance` (
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

--
-- Table structure for table `agent_presets`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `agent_presets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `api_provider` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `capabilities` text COLLATE utf8mb4_unicode_ci,
  `max_context_size` int DEFAULT NULL,
  `reasoning_depth` int DEFAULT NULL,
  `role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_agent_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `supported_file_types` text COLLATE utf8mb4_unicode_ci,
  `supports_code_execution` tinyint(1) DEFAULT '0',
  `supports_file_operations` tinyint(1) DEFAULT '0',
  `supports_image_generation` tinyint(1) DEFAULT '0',
  `is_system` tinyint(1) DEFAULT '0',
  `tags` text COLLATE utf8mb4_unicode_ci,
  `unsupported_features` text COLLATE utf8mb4_unicode_ci,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alert_records`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `alert_records` (
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
  `trigger_value` double DEFAULT NULL,
  `threshold_value` double DEFAULT NULL,
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
  `notified` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  KEY `idx_alert_record_rule_id` (`rule_id`),
  KEY `idx_alert_record_priority` (`priority`),
  KEY `idx_alert_record_created` (`created_at`),
  CONSTRAINT `alert_records_ibfk_1` FOREIGN KEY (`rule_id`) REFERENCES `alert_rules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alert_rules`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `alert_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rule_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM',
  `metric_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `condition_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '>',
  `threshold` double NOT NULL,
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
  KEY `idx_alert_rule_priority` (`priority`),
  KEY `idx_alert_rule_type` (`rule_type`),
  KEY `idx_alert_rule_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `api_tokens`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `api_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `token_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `api_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `api_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `max_tokens` int DEFAULT NULL,
  `context_window` int DEFAULT 200000,
  `priority` int DEFAULT NULL,
  `agent_tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assigned_agent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assigned_agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usage_count` bigint DEFAULT NULL,
  `total_tokens_used` bigint DEFAULT '0',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','EXHAUSTED','DISABLED','EXPIRED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `approval_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requester_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_data` text COLLATE utf8mb4_unicode_ci,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` enum('PENDING','APPROVED','REJECTED','EXPIRED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `approver_id` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approval_comment` text COLLATE utf8mb4_unicode_ci,
  `priority` int NOT NULL DEFAULT '5',
  `approved_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `requester_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ar_project` (`project_id`),
  KEY `idx_ar_status` (`status`),
  KEY `idx_ar_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `capability_invocation_logs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `capability_invocation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `capability_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `params` text COLLATE utf8mb4_unicode_ci,
  `result` text COLLATE utf8mb4_unicode_ci,
  `status` enum('PENDING_APPROVAL','APPROVED','EXECUTED','FAILED','REJECTED','TIMEOUT','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=95 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_messages`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_chat_messages_session` (`session_id`),
  CONSTRAINT `chat_messages_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_sessions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `chat_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL DEFAULT '新对话',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_sessions_user` (`user_id`),
  KEY `idx_chat_sessions_updated` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `code_reviews`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `code_reviews` (
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
  `changed_files` text COLLATE utf8mb4_unicode_ci,
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
  KEY `idx_review_created` (`created_at`),
  KEY `idx_review_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `device_trusts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `device_trusts` (
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
  UNIQUE KEY `UKebr7sk6ytax5xicmlly3k5smf` (`user_id`,`device_fingerprint`),
  KEY `idx_device_trusts_user_id` (`user_id`),
  KEY `idx_device_trusts_fingerprint` (`device_fingerprint`),
  KEY `idx_device_trusts_expires_at` (`expires_at`),
  KEY `idx_device_trusts_fingerprint_user` (`device_fingerprint`,`user_id`),
  KEY `idx_device_trusts_expires` (`expires_at`),
  CONSTRAINT `device_trusts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dismissal_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `dismissal_requests` (
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

--
-- Table structure for table `document_index`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `document_index` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `doc_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `keywords` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `content_hash` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_di_agent` (`agent_id`),
  KEY `idx_di_project` (`project_id`),
  KEY `idx_doc_path` (`file_path`),
  KEY `idx_doc_type` (`doc_type`),
  KEY `idx_doc_agent` (`agent_id`),
  KEY `idx_doc_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `game_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `game_templates` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `builtin` bit(1) DEFAULT NULL,
  `config_json` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `game_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_game_tpl_builtin` (`builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `git_repositories`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `git_repositories` (
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
  KEY `idx_git_repositories_status_updated` (`status`,`updated_at`),
  KEY `idx_git_project` (`project_id`),
  KEY `idx_git_type` (`repository_type`),
  KEY `idx_git_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `interventions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `interventions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `type` enum('GUIDANCE','CORRECTION','PRIORITY','CONSTRAINT','CONTEXT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `urgency` enum('NORMAL','HIGH','CRITICAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `duration` enum('ONCE','SESSION','PERMANENT') COLLATE utf8mb4_unicode_ci NOT NULL,
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

--
-- Table structure for table `knowledge_base`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_base` (
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
  `usage_count` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kb_category` (`category`),
  KEY `idx_kb_project` (`project_id`),
  KEY `idx_kb_access` (`access_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mcp_servers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `mcp_servers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `transport_type` enum('STDIO','SSE','STREAMABLE_HTTP') COLLATE utf8mb4_unicode_ci NOT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mcp_tools`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `mcp_tools` (
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

--
-- Table structure for table `notification_preferences`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `notification_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` enum('IN_APP','EMAIL','DINGTALK','FEISHU','WEBHOOK') COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `do_not_disturb` tinyint(1) NOT NULL DEFAULT '0',
  `quiet_start` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quiet_end` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_np_user_type_channel` (`user_id`,`notification_type`,`channel`),
  UNIQUE KEY `idx_np_user_type` (`user_id`,`notification_type`,`channel`),
  KEY `idx_np_user` (`user_id`),
  KEY `idx_np_type` (`notification_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `notification_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码（唯一）',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `channel` enum('EMAIL','FEISHU','DINGTALK','SYSTEM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` enum('ALERT','TASK','AGENT','SYSTEM') COLLATE utf8mb4_unicode_ci NOT NULL,
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
  KEY `idx_enabled` (`enabled`),
  KEY `idx_template_channel` (`channel`),
  KEY `idx_template_category` (`category`),
  KEY `idx_template_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `type` enum('INFO','WARNING','ERROR','SUCCESS','TASK','AGENT','SYSTEM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel` enum('SYSTEM','EMAIL','FEISHU','DINGTALK') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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

--
-- Table structure for table `operation_logs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `operation_logs` (
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
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_target` (`target_type`),
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_level` (`level`),
  CONSTRAINT `operation_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `performance_metrics`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `performance_metrics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `metric_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metric_value` double NOT NULL,
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
  KEY `idx_metric_created` (`created_at`),
  KEY `idx_metric_name` (`metric_name`),
  KEY `idx_metric_type` (`metric_type`)
) ENGINE=InnoDB AUTO_INCREMENT=4993 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `performance_reviews`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `performance_reviews` (
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

--
-- Table structure for table `permission_definitions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `permission_definitions` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `permission_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
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

--
-- Table structure for table `pipeline_stages`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `pipeline_stages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_id` bigint NOT NULL,
  `stage_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `order_index` int NOT NULL DEFAULT '0',
  `config` text COLLATE utf8mb4_unicode_ci,
  `started_at` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `commands` text COLLATE utf8mb4_unicode_ci,
  `continue_on_failure` bit(1) DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_seconds` bigint DEFAULT NULL,
  `log` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_order` int NOT NULL,
  `timeout_seconds` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ps_pipeline` (`pipeline_id`),
  KEY `idx_stage_pipeline` (`pipeline_id`),
  KEY `idx_stage_order` (`stage_order`),
  CONSTRAINT `fk_ps_pipeline` FOREIGN KEY (`pipeline_id`) REFERENCES `pipelines` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipelines`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `pipelines` (
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
  `duration_seconds` bigint DEFAULT NULL,
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

--
-- Table structure for table `producer_replacements`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `producer_replacements` (
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
  `executed_at` datetime(6) DEFAULT NULL,
  `old_producer_created_at` datetime(6) DEFAULT NULL,
  `old_producer_history` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `replacement_no` (`replacement_no`),
  KEY `idx_replacement_project` (`project_id`),
  KEY `idx_replacement_created` (`created_at`),
  KEY `idx_replacement_old` (`old_producer_id`),
  KEY `idx_replacement_new` (`new_producer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_alert_config`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_alert_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `custom_duration_seconds` int DEFAULT NULL,
  `custom_priority` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `custom_threshold` double DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rule_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8l6y1rskxxm7rqul7pu04wheo` (`project_id`,`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_alert_configs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_alert_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alert_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `threshold` double DEFAULT NULL,
  `notify_channels` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pac_project_type` (`project_id`,`alert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_members`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `role` enum('OWNER','MANAGER','DEVELOPER','VIEWER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pm_project_user` (`project_id`,`user_id`),
  UNIQUE KEY `UKaydweb1re2g5786xaugww4u0` (`project_id`,`user_id`),
  KEY `idx_pm_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_notification_config`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_notification_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_override` enum('EMAIL','FEISHU','DINGTALK','SYSTEM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `notify_users` text COLLATE utf8mb4_unicode_ci,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rate_limit_seconds` int DEFAULT NULL,
  `recipients` text COLLATE utf8mb4_unicode_ci,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1484hslqqycymht0yuj6cliei` (`project_id`,`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_notification_configs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_notification_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notification_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `config` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pnc_project_type` (`project_id`,`notification_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_token_binding`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_token_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_default` bit(1) DEFAULT NULL,
  `priority` int DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `token_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2h8okvew463pkbdwyxt14p9it` (`project_id`,`token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_token_bindings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `project_token_bindings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ptb_project_token` (`project_id`,`token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recruitment_requests`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `recruitment_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `producer_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `producer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approval_comment` text COLLATE utf8mb4_unicode_ci,
  `approved_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `capabilities` text COLLATE utf8mb4_unicode_ci,
  `created_agent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `employee_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `executed_at` datetime(6) DEFAULT NULL,
  `is_custom_role` bit(1) DEFAULT NULL,
  `original_request_id` bigint DEFAULT NULL,
  `project_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `revision_count` int DEFAULT NULL,
  `supported_file_types` text COLLATE utf8mb4_unicode_ci,
  `work_dir` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rr_producer` (`producer_id`),
  KEY `idx_rr_status` (`status`),
  KEY `idx_recruit_status` (`status`),
  KEY `idx_recruit_producer` (`producer_id`),
  KEY `idx_recruit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_permissions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission`),
  KEY `idx_role_permissions_role_id` (`role_id`),
  CONSTRAINT `role_permissions_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `roles` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_configs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `system_configs` (
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
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_constants`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `system_constants` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `token_usage`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `token_usage` (
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
  `estimated_cost` double DEFAULT NULL,
  `cost` decimal(10,4) DEFAULT '0.0000',
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_token_usage_agent_id` (`agent_id`),
  KEY `idx_token_usage_created_at` (`created_at`),
  KEY `idx_token_usage_agent_created` (`agent_id`,`created_at`),
  KEY `idx_token_usage_model_created` (`model`,`created_at`),
  KEY `idx_token_usage_date` (`usage_date`),
  KEY `idx_token_usage_agent` (`agent_id`),
  KEY `idx_token_usage_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_permissions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `user_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` enum('GRANTED','APPROVED') COLLATE utf8mb4_unicode_ci NOT NULL,
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

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','DISABLED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_approvals`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `workflow_approvals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci,
  `decided_at` datetime(6) DEFAULT NULL,
  `instance_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `step_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wfa_instance` (`instance_id`),
  KEY `idx_wfa_status` (`status`),
  KEY `idx_wfa_approver` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_audit_logs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `workflow_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `detail_json` text COLLATE utf8mb4_unicode_ci,
  `instance_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `step_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wfal_instance` (`instance_id`),
  KEY `idx_wfal_action` (`action`),
  KEY `idx_wfal_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_instances`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `workflow_instances` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `context_json` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `parameters_json` text COLLATE utf8mb4_unicode_ci,
  `project_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wfi_template` (`template_id`),
  KEY `idx_wfi_project` (`project_id`),
  KEY `idx_wfi_status` (`status`),
  KEY `idx_wfi_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_step_executions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `workflow_step_executions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `input_data_json` text COLLATE utf8mb4_unicode_ci,
  `instance_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_retries` int DEFAULT NULL,
  `output_data_json` text COLLATE utf8mb4_unicode_ci,
  `retry_count` int DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `step_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `timeout_minutes` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wfse_instance_step` (`instance_id`,`step_id`),
  KEY `idx_wfse_instance` (`instance_id`),
  KEY `idx_wfse_status` (`status`),
  KEY `idx_wfse_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `workflow_templates` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `builtin` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `steps_json` text COLLATE utf8mb4_unicode_ci,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wf_tpl_builtin` (`builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-06 13:11:48

-- ============================================================
-- 初始数据
-- ============================================================

-- 表: roles
INSERT IGNORE INTO `roles` (`id`, `name`, `display_name`, `description`, `is_system`, `created_at`, `updated_at`) VALUES (1,'ADMIN','管理员','系统管理员，拥有所有权限',1,'2026-06-04 10:39:07','2026-06-04 10:39:07'),(2,'PROJECT_MANAGER','项目经理','负责项目管理和 Agent 调度',1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(3,'DEVELOPER','开发者','使用 Agent 进行开发工作',1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(4,'OPS_ENGINEER','运维工程师','负责系统运维和部署',1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(5,'OBSERVER','观察者','只读权限，查看系统状态',1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(6,'USER','普通用户','普通用户，基础权限',1,'2026-06-04 10:56:41','2026-06-04 10:56:41');

-- 表: users
INSERT IGNORE INTO `users` (`id`, `username`, `password`, `email`, `nickname`, `avatar`, `role_id`, `status`, `must_change_password`, `created_at`, `updated_at`) VALUES (1,'admin','$2b$12$L4WCT4ajZm8YK8W6iHPZ.uLtkw1rp.Lb5fVySmD1G/BNc.F7oo2Ci','chengxun@88.com','管理员',NULL,1,'APPROVED',0,'2026-06-04 10:39:07','2026-06-05 13:16:17');

-- 表: role_permissions
INSERT IGNORE INTO `role_permissions` (`id`, `role_id`, `permission`, `created_at`) VALUES (1,1,'system:monitor','2026-06-04 10:39:06'),(2,1,'admin:manage','2026-06-04 10:39:06'),(3,1,'agent:view','2026-06-04 10:39:06'),(4,1,'approval:manage','2026-06-04 10:39:06'),(5,1,'ai:admin','2026-06-04 10:39:06'),(6,1,'skills:manage','2026-06-04 10:39:06'),(7,1,'workflow:view','2026-06-04 10:39:06'),(8,1,'agent:manage','2026-06-04 10:39:06'),(9,1,'system:monitor:manage','2026-06-04 10:39:06'),(10,1,'notification:manage','2026-06-04 10:39:06'),(11,1,'pipeline:manage','2026-06-04 10:39:06'),(12,1,'notification:view','2026-06-04 10:39:06'),(13,1,'agents:manage','2026-06-04 10:39:06'),(14,1,'code:review','2026-06-04 10:39:06'),(15,1,'skills:view','2026-06-04 10:39:06'),(16,1,'logs:view','2026-06-04 10:39:06'),(17,1,'roles:manage','2026-06-04 10:39:06'),(18,1,'agents:task','2026-06-04 10:39:06'),(19,1,'pipeline:view','2026-06-04 10:39:06'),(20,1,'ai:use','2026-06-04 10:39:06'),(21,1,'system:config:manage','2026-06-04 10:39:06'),(22,1,'knowledge:manage','2026-06-04 10:39:06'),(23,1,'pipeline:approve','2026-06-04 10:39:06'),(24,1,'log:view','2026-06-04 10:39:06'),(25,1,'dashboard:view','2026-06-04 10:39:06'),(26,1,'terminal:use','2026-06-04 10:39:06'),(27,1,'system:view','2026-06-04 10:39:06'),(28,1,'workflow:manage','2026-06-04 10:39:06'),(29,1,'projects:view','2026-06-04 10:39:06'),(30,1,'pipeline:execute','2026-06-04 10:39:06'),(31,1,'users:manage','2026-06-04 10:39:06'),(32,1,'system:config','2026-06-04 10:39:06'),(33,1,'pipeline:intervene','2026-06-04 10:39:06'),(34,1,'tokens:view','2026-06-04 10:39:06'),(35,1,'projects:manage','2026-06-04 10:39:06'),(36,1,'system:manage','2026-06-04 10:39:06'),(37,1,'approval:view','2026-06-04 10:39:06'),(38,1,'tokens:manage','2026-06-04 10:39:06'),(39,1,'agents:view','2026-06-04 10:39:06'),(91,1,'iteration:view','2026-06-11 12:00:00'),(92,1,'iteration:manage','2026-06-11 12:00:00'),(93,1,'supervision:view','2026-06-11 12:00:00'),(40,2,'system:monitor','2026-06-04 10:56:41'),(41,2,'pipeline:approve','2026-06-04 10:56:41'),(42,2,'dashboard:view','2026-06-04 10:56:41'),(43,2,'projects:view','2026-06-04 10:56:41'),(44,2,'workflow:view','2026-06-04 10:56:41'),(45,2,'workflow:manage','2026-06-04 10:56:41'),(46,2,'pipeline:execute','2026-06-04 10:56:41'),(47,2,'projects:edit','2026-06-04 10:56:41'),(48,2,'notification:manage','2026-06-04 10:56:41'),(49,2,'pipeline:manage','2026-06-04 10:56:41'),(50,2,'pipeline:create','2026-06-04 10:56:41'),(51,2,'agents:manage','2026-06-04 10:56:41'),(52,2,'pipeline:intervene','2026-06-04 10:56:41'),(53,2,'code:review','2026-06-04 10:56:41'),(54,2,'skills:view','2026-06-04 10:56:41'),(55,2,'projects:manage','2026-06-04 10:56:41'),(56,2,'agents:task','2026-06-04 10:56:41'),(57,2,'pipeline:view','2026-06-04 10:56:41'),(58,2,'ai:use','2026-06-04 10:56:41'),(59,2,'agents:view','2026-06-04 10:56:41'),(94,2,'iteration:view','2026-06-11 12:00:00'),(95,2,'iteration:manage','2026-06-11 12:00:00'),(96,2,'supervision:view','2026-06-11 12:00:00'),(60,3,'dashboard:view','2026-06-04 10:56:41'),(61,3,'code:review','2026-06-04 10:56:41'),(62,3,'projects:view','2026-06-04 10:56:41'),(63,3,'skills:view','2026-06-04 10:56:41'),(64,3,'pipeline:execute','2026-06-04 10:56:41'),(65,3,'agents:task','2026-06-04 10:56:41'),(66,3,'pipeline:view','2026-06-04 10:56:41'),(67,3,'ai:use','2026-06-04 10:56:41'),(68,3,'agents:view','2026-06-04 10:56:41'),(97,3,'iteration:view','2026-06-11 12:00:00'),(98,3,'supervision:view','2026-06-11 12:00:00'),(69,4,'system:monitor','2026-06-04 10:56:41'),(70,4,'pipeline:approve','2026-06-04 10:56:41'),(71,4,'dashboard:view','2026-06-04 10:56:41'),(72,4,'projects:view','2026-06-04 10:56:41'),(73,4,'workflow:view','2026-06-04 10:56:41'),(74,4,'workflow:manage','2026-06-04 10:56:41'),(75,4,'pipeline:execute','2026-06-04 10:56:41'),(76,4,'system:monitor:manage','2026-06-04 10:56:41'),(77,4,'pipeline:manage','2026-06-04 10:56:41'),(78,4,'pipeline:create','2026-06-04 10:56:41'),(79,4,'pipeline:intervene','2026-06-04 10:56:41'),(80,4,'pipeline:view','2026-06-04 10:56:41'),(81,4,'agents:view','2026-06-04 10:56:41'),(99,4,'iteration:view','2026-06-11 12:00:00'),(100,4,'supervision:view','2026-06-11 12:00:00'),(82,5,'system:monitor','2026-06-04 10:56:41'),(83,5,'dashboard:view','2026-06-04 10:56:41'),(84,5,'projects:view','2026-06-04 10:56:41'),(85,5,'skills:view','2026-06-04 10:56:41'),(86,5,'pipeline:view','2026-06-04 10:56:41'),(87,5,'ai:use','2026-06-04 10:56:41'),(88,5,'agents:view','2026-06-04 10:56:41'),(101,5,'iteration:view','2026-06-11 12:00:00'),(102,5,'supervision:view','2026-06-11 12:00:00'),(89,6,'dashboard:view','2026-06-04 10:56:41'),(90,6,'projects:view','2026-06-04 10:56:41'),(103,6,'iteration:view','2026-06-11 12:00:00'),(104,6,'supervision:view','2026-06-11 12:00:00');

-- 表: permission_definitions
INSERT IGNORE INTO `permission_definitions` (`id`, `permission_key`, `name`, `description`, `category`, `enabled`, `system`, `sort_order`, `created_at`, `updated_at`) VALUES (1,'PERM_agents:manage','Agent 管理','创建、删除、配置 Agent','Agent',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(2,'PERM_agents:view','Agent 查看','查看 Agent 状态','Agent',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(3,'PERM_agents:task','Agent 任务','向 Agent 发送任务','Agent',1,1,3,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(4,'PERM_skills:view','技能查看','查看技能列表','技能',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(5,'PERM_skills:manage','技能管理','创建、编辑、删除技能','技能',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(6,'PERM_tokens:view','Token 查看','查看 Token 列表','Token',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(7,'PERM_tokens:manage','Token 管理','创建、分配 Token','Token',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(8,'PERM_projects:view','项目查看','查看项目列表','项目',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(9,'PERM_projects:manage','项目管理','创建、删除项目','项目',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(10,'PERM_system:monitor','系统监控','查看系统状态、上下文健康','系统',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(11,'PERM_system:manage','系统管理','配置系统参数','系统',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(12,'PERM_ai:use','AI助手使用','使用AI助手问答功能','AI',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(13,'PERM_ai:admin','AI助手管理','管理AI知识库','AI',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(14,'PERM_approval:view','审批查看','查看审批请求','审批',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(15,'PERM_approval:manage','审批管理','批准、拒绝审批请求','审批',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(16,'PERM_notification:view','通知查看','查看通知','通知',1,1,1,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(17,'PERM_notification:manage','通知管理','管理通知模板','通知',1,1,2,'2026-06-04 10:56:41','2026-06-04 10:56:41'),(18,'PERM_admin:manage','管理员权限','所有功能','管理',1,1,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(19,'PERM_roles:manage','角色管理','创建、编辑角色','管理',1,1,2,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(20,'PERM_users:manage','用户管理','创建、编辑、删除用户','管理',1,1,3,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(21,'PERM_logs:view','日志查看','查看操作日志','管理',1,1,4,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(22,'PERM_code:review','代码审查','审查代码提交','项目',1,1,3,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(23,'PERM_pipeline:view','流水线查看','查看CICD流水线','项目',1,1,4,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(24,'PERM_pipeline:manage','流水线管理','管理CICD流水线','项目',1,1,5,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(25,'PERM_pipeline:execute','流水线执行','执行CICD流水线','项目',1,1,6,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(26,'PERM_pipeline:approve','流水线审批','审批CICD流水线','项目',1,1,7,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(27,'PERM_pipeline:intervene','流水线干预','干预CICD流水线','项目',1,1,8,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(28,'PERM_workflow:view','工作流查看','查看工作流','项目',1,1,9,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(29,'PERM_workflow:manage','工作流管理','管理工作流','项目',1,1,10,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(30,'PERM_dashboard:view','仪表盘查看','查看仪表盘','工作台',1,1,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(31,'PERM_terminal:use','终端使用','使用系统终端','系统',1,1,3,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(32,'PERM_system:view','系统查看','查看系统配置','系统',1,1,4,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(33,'PERM_system:config','系统配置','查看系统配置','系统',1,1,5,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(34,'PERM_system:config:manage','系统配置管理','管理系统配置','系统',1,1,6,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(35,'PERM_system:monitor:manage','系统监控管理','管理系统监控','系统',1,1,7,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(36,'PERM_knowledge:manage','知识库管理','管理知识库','项目',1,1,11,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(37,'PERM_log:view','日志查看','查看日志','管理',1,1,5,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(38,'PERM_agent:view','Agent查看','查看Agent状态','Agent',1,1,4,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(39,'PERM_agent:manage','Agent管理','管理Agent','Agent',1,1,5,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(40,'PERM_iteration:view','迭代查看','查看版本迭代记录','项目',1,1,12,'2026-06-11 12:00:00','2026-06-11 12:00:00'),(41,'PERM_iteration:manage','迭代管理','管理版本迭代、回滚','项目',1,1,13,'2026-06-11 12:00:00','2026-06-11 12:00:00'),(42,'PERM_supervision:view','督查查看','查看督查报告','项目',1,1,14,'2026-06-11 12:00:00','2026-06-11 12:00:00');

-- 表: system_configs
INSERT IGNORE INTO `system_configs` (`id`, `config_key`, `config_value`, `config_group`, `value_type`, `is_system_builtin`, `description`, `created_at`, `updated_at`, `project_id`) VALUES (1,'system.name','ChengXun Game Maker','system','string',0,'系统名称','2026-06-04 10:38:36','2026-06-04 10:38:36',NULL),(2,'security.jwt.secret','EJw5M!d3wj9F1Rer$ARFeQUYBnhQxAF8W7^&pOl1OwYRwGz5','security','string',0,'JWT 密钥','2026-06-04 10:38:36','2026-06-04 10:38:36',NULL),(3,'claude.api.key','tp-sl4dez0y6wzcfegiu4pxqq9eko4rl8pc5nix9o1yippa6ffy','agent','string',0,'Claude API Key','2026-06-04 10:38:51','2026-06-05 06:18:33',NULL),(4,'claude.api.url','https://token-plan-sgp.xiaomimimo.com/anthropic','agent','string',0,'Claude API URL','2026-06-04 10:38:51','2026-06-05 06:18:33',NULL),(5,'claude.model','mimo-v2.5-pro[1m]','agent','string',0,'Claude 模型','2026-06-04 10:38:51','2026-06-04 10:38:51',NULL),(6,'security.password.min-length','8','security','number',1,'密码最小长度','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(7,'security.password.require-uppercase','true','security','boolean',1,'密码是否需要大写字母','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(8,'security.password.require-lowercase','true','security','boolean',1,'密码是否需要小写字母','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(9,'security.password.require-digit','true','security','boolean',1,'密码是否需要数字','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(10,'security.password.require-special','true','security','boolean',1,'密码是否需要特殊字符','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(11,'security.session.timeout-minutes','30','security','number',1,'会话超时时间（分钟）','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(12,'security.session.max-concurrent','1','security','number',1,'最大并发会话数','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(13,'security.login.max-attempts','5','security','number',1,'最大登录尝试次数','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(14,'security.login.lockout-minutes','15','security','number',1,'登录锁定时间（分钟）','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(15,'agent.task.max-retry','3','agent','number',1,'任务最大重试次数','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(16,'agent.task.retry-delay-ms','5000','agent','number',1,'任务重试延迟（毫秒）','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(17,'agent.task.max-queue-size','100','agent','number',1,'任务队列最大长度','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(18,'agent.message.max-size','10000','agent','number',1,'消息最大长度','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(19,'email.verification.expire-minutes','10','email','number',1,'验证码过期时间（分钟）','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(20,'email.verification.code-length','6','email','number',1,'验证码长度','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(21,'notification.enabled.channels','feishu,email','notification','string',1,'启用的通知渠道（逗号分隔）','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(22,'system.pagination.default-size','20','system','number',1,'默认分页大小','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(23,'system.pagination.max-size','100','system','number',1,'最大分页大小','2026-06-04 10:56:42','2026-06-04 10:56:42',NULL),(24,'claude.max.tokens','14336','claude','string',0,NULL,'2026-06-05 07:11:37','2026-06-05 07:11:37',NULL),(25,'security.device.trust.enabled','false','security','boolean',0,'是否启用设备信任（陌生设备二次验证）','2026-06-06 01:22:09','2026-06-06 01:22:09',NULL),(26,'security.device.trust.days','7','security','number',0,'设备信任有效期（天）','2026-06-06 01:22:09','2026-06-06 01:22:09',NULL);

-- 表: system_constants
INSERT IGNORE INTO `system_constants` (`id`, `constant_key`, `display_name`, `description`, `value`, `default_value`, `value_type`, `group_name`, `unit`, `min_value`, `max_value`, `require_restart`, `system_builtin`, `created_at`, `updated_at`) VALUES (1,'agent.max-message-backlog','最大消息积压','Agent 消息队列最大积压数量，超过此数视为上下文异常','50','50','int','agent','条',1,1000,1,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(2,'agent.max-idle-minutes','最大空闲时间','Agent 最大无响应时间，超过视为上下文失效','30','30','int','agent','分钟',1,1440,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(3,'agent.max-recovery-attempts','最大恢复尝试','上下文恢复最大尝试次数，超过则停止 Agent','3','3','int','agent','次',1,10,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(4,'agent.task-queue-size','任务队列大小','Agent 任务队列最大容量','500','500','int','agent','条',10,5000,1,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(5,'agent.message-queue-size','消息队列大小','Agent 消息队列最大容量','1000','1000','int','agent','条',10,10000,1,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(6,'agent.max-retry-count','最大重试次数','任务/消息失败后最大重试次数','3','3','int','agent','次',0,10,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(7,'agent.history-size','历史记录大小','任务/消息历史保留数量','1000','1000','int','agent','条',100,10000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(8,'agent.scheduler-interval-ms','调度间隔','Agent 调度器执行间隔','300000','300000','long','agent','毫秒',10000,3600000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(9,'agent.max-warnings','最大警告次数','绩效管理最大警告次数，超过触发解雇流程','3','3','int','agent','次',1,10,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(10,'agent.max-learned-knowledge','最大知识条数','Agent 最大学习知识数量','100','100','int','agent','条',10,1000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(11,'file.max-size-mb','单文件大小限制','单个文件最大上传大小','50','50','int','file','MB',1,500,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(12,'file.quota-mb','Agent 存储配额','每个 Agent 的文件存储配额','500','500','int','file','MB',10,5000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(13,'file.storage-dir','存储目录','文件存储根目录','data/files','data/files','string','file','',NULL,NULL,1,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(14,'security.max-login-attempts','最大登录尝试','登录失败最大尝试次数，超过锁定账户','5','5','int','security','次',1,20,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(15,'security.jwt-expiration-ms','JWT 过期时间','JWT Token 有效期','86400000','86400000','long','security','毫秒',3600000,604800000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(16,'security.session-timeout-minutes','会话超时','Web 会话超时时间','30','30','int','security','分钟',5,480,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(17,'rate-limit.global','全局限流','全局 API 每分钟请求限制','120','120','int','rate-limit','次/分钟',10,1000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(18,'rate-limit.auth','认证限流','登录接口每分钟请求限制','10','10','int','rate-limit','次/分钟',1,100,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(19,'rate-limit.write','写操作限流','写操作每分钟请求限制','60','60','int','rate-limit','次/分钟',5,500,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(20,'performance.max-backups','最大备份数','文件备份最大保留数量','10','10','int','performance','份',1,100,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(21,'performance.max-output-size','最大输出大小','沙箱执行最大输出大小','1048576','1048576','long','performance','字节',1024,10485760,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(22,'performance.ws-session-timeout','WebSocket 超时','终端 WebSocket 会话超时时间','1800000','1800000','long','performance','毫秒',60000,86400000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(23,'performance.context-compact-threshold','上下文压缩阈值','触发自动压缩的消息数','50','50','int','performance','条',10,500,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(24,'notification.batch-size','批量通知大小','批量发送通知的批次大小','100','100','int','notification','条',10,1000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(25,'mcp.discovery-timeout-ms','MCP 发现超时','MCP 工具发现超时时间','10000','10000','long','mcp','毫秒',1000,60000,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),(26,'file.version-limit','版本保留数','同名文件保留的最大版本数','10','10','int','file','个',1,100,0,1,'2026-06-04 10:56:42','2026-06-04 10:56:42'),
(27,'supervision.iteration.timeout.hours','迭代超时阈值','版本迭代超过此时间视为超时，触发预警','72','72','int','supervision','小时',12,720,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(28,'supervision.task.overdue.threshold.hours','任务逾期阈值','任务超过预估工时此时间后视为逾期','24','24','int','supervision','小时',1,168,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(29,'supervision.quality.score.threshold','质量评分阈值','迭代评分低于此值视为质量不达标','6','6','int','supervision','分',1,10,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(30,'supervision.rollback.rate.threshold','回滚率阈值','版本回滚率超过此百分比视为异常','30','30','int','supervision','%',5,100,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(31,'supervision.agent.idle.threshold.hours','Agent空闲阈值','Agent无活动超过此时间视为空闲','4','4','int','supervision','小时',1,48,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(32,'supervision.alert.email.enabled','邮件预警开关','是否通过邮件发送督查预警','false','false','boolean','supervision','',NULL,NULL,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(33,'supervision.alert.feishu.enabled','飞书预警开关','是否通过飞书发送督查预警','true','true','boolean','supervision','',NULL,NULL,0,1,'2026-06-11 12:00:00','2026-06-11 12:00:00');

-- 表: notification_templates
INSERT IGNORE INTO `notification_templates` (`id`, `template_code`, `template_name`, `channel`, `category`, `subject`, `content`, `description`, `enabled`, `system_builtin`, `created_at`, `updated_at`) VALUES (1,'TASK_FEISHU','任务飞书模板','FEISHU','TASK','任务通知','**任务通知**\n\n任务：${taskTitle}\n状态：${content}\n时间：${time}','用于发送任务相关飞书通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(2,'TASK_SYSTEM','任务站内通知模板','SYSTEM','TASK','任务通知：${taskTitle}','任务状态：${content}','用于发送任务站内通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(3,'AGENT_FEISHU','Agent飞书模板','FEISHU','AGENT','Agent通知','**Agent通知**\n\nAgent：${agentName}\n内容：${content}\n时间：${time}','用于发送Agent相关飞书通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(4,'AGENT_SYSTEM','Agent站内通知模板','SYSTEM','AGENT','Agent通知：${agentName}','通知内容：${content}','用于发送Agent站内通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(5,'RECOVERY_FEISHU','恢复通知飞书模板','FEISHU','ALERT','告警恢复','**告警恢复**\n\n规则：${ruleName}\n当前值：${triggerValue}\n时间：${time}','用于发送告警恢复飞书通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(6,'RECOVERY_DINGTALK','恢复通知钉钉模板','DINGTALK','ALERT','告警恢复','### 告警恢复\n\n**规则名称**：${ruleName}\n**当前值**：${triggerValue}\n**恢复时间**：${time}\n\n告警已恢复，系统运行正常。','用于发送告警恢复钉钉通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(7,'TASK_DINGTALK','任务钉钉模板','DINGTALK','TASK','任务通知','### 任务通知\n\n**任务标题**：${taskTitle}\n**任务状态**：${content}\n**通知时间**：${time}','用于发送任务钉钉通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(8,'AGENT_DINGTALK','Agent钉钉模板','DINGTALK','AGENT','Agent通知','### Agent通知\n\n**Agent名称**：${agentName}\n**通知内容**：${content}\n**通知时间**：${time}','用于发送Agent钉钉通知',1,1,'2026-06-04 18:38:31','2026-06-04 18:38:31'),(9,'ALERT_SYSTEM','站内告警通知','SYSTEM','ALERT','[${priorityDesc}] 告警: ${ruleName}','监控告警已触发\n\n规则：${ruleName}\n级别：${priorityDesc}\n当前值：${triggerValue}\n阈值：${thresholdValue}\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(10,'ALERT_EMAIL','邮件告警通知','EMAIL','ALERT','[${priorityDesc}] 告警: ${ruleName}','<h2>监控告警通知</h2><p><b>规则：</b>${ruleName}</p><p><b>级别：</b>${priorityDesc}</p><p><b>当前值：</b>${triggerValue}</p><p><b>阈值：</b>${thresholdValue}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(11,'ALERT_FEISHU','飞书告警通知','FEISHU','ALERT','[${priorityDesc}] 告警: ${ruleName}','**监控告警通知**\n\n- 规则：${ruleName}\n- 级别：${priorityDesc}\n- 当前值：${triggerValue}\n- 阈值：${thresholdValue}\n- 时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(12,'ALERT_DINGTALK','钉钉告警通知','DINGTALK','ALERT','[${priorityDesc}] 告警: ${ruleName}','### 监控告警通知\n\n- 规则：${ruleName}\n- 级别：${priorityDesc}\n- 当前值：${triggerValue}\n- 阈值：${thresholdValue}\n- 时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(13,'RECOVERY_SYSTEM','站内告警恢复','SYSTEM','ALERT','告警恢复: ${ruleName}','告警已恢复\n\n规则：${ruleName}\n当前值：${triggerValue}\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(14,'RECOVERY_EMAIL','邮件告警恢复','EMAIL','ALERT','告警恢复: ${ruleName}','<h2>告警恢复通知</h2><p><b>规则：</b>${ruleName}</p><p><b>当前值：</b>${triggerValue}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(15,'TASK_EMAIL','邮件任务通知','EMAIL','TASK','任务通知: ${taskTitle}','<h2>任务通知</h2><p><b>任务：</b>${taskTitle}</p><p><b>结果：</b>${taskResult}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(16,'AGENT_EMAIL','邮件Agent通知','EMAIL','AGENT','Agent通知: ${agentName}','<h2>Agent通知</h2><p><b>Agent：</b>${agentName}</p><p>${content}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(17,'APPROVAL_SYSTEM','站内审批通知','SYSTEM','SYSTEM','审批通知: ${title}','${content}\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(18,'APPROVAL_EMAIL','邮件审批通知','EMAIL','SYSTEM','审批通知: ${title}','<h2>审批通知</h2><p>${content}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(19,'PERMISSION_SYSTEM','站内权限通知','SYSTEM','SYSTEM','权限通知: ${title}','${content}\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(20,'PERMISSION_EMAIL','邮件权限通知','EMAIL','SYSTEM','权限通知: ${title}','<h2>权限通知</h2><p>${content}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(21,'SYSTEM_MAINTENANCE_SYSTEM','站内系统维护','SYSTEM','SYSTEM','系统维护: ${title}','${content}\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(22,'SYSTEM_MAINTENANCE_EMAIL','邮件系统维护','EMAIL','SYSTEM','系统维护: ${title}','<h2>系统维护通知</h2><p>${content}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(23,'TOKEN_EXHAUSTED_SYSTEM','站内Token耗尽','SYSTEM','SYSTEM','Token 耗尽警告','API Token 配额不足，请及时补充。\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(24,'TOKEN_EXHAUSTED_EMAIL','邮件Token耗尽','EMAIL','SYSTEM','Token 耗尽警告','<h2>Token 耗尽警告</h2><p>API Token 配额不足，请及时补充。</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(25,'PROJECT_SYSTEM','站内项目通知','SYSTEM','SYSTEM','项目通知: ${projectName}','${content}\n项目：${projectName}\n时间：${time}',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),(26,'PROJECT_EMAIL','邮件项目通知','EMAIL','SYSTEM','项目通知: ${projectName}','<h2>项目通知</h2><p><b>项目：</b>${projectName}</p><p>${content}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-06 12:31:00','2026-06-06 12:31:00'),
(27,'VERIFICATION_SYSTEM','站内验证通知','SYSTEM','VERIFICATION','验证报告: ${projectName}','项目验证完成\n\n项目：${projectName}\n结果：${result}\n详情：${details}\n时间：${time}',NULL,1,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(28,'VERIFICATION_EMAIL','邮件验证通知','EMAIL','VERIFICATION','验证报告: ${projectName}','<h2>项目验证报告</h2><p><b>项目：</b>${projectName}</p><p><b>结果：</b>${result}</p><p><b>详情：</b>${details}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(29,'VERIFICATION_ISSUE_SYSTEM','站内验证问题','SYSTEM','VERIFICATION','[${severity}] 验证问题: ${projectName}','发现验证问题\n\n项目：${projectName}\n级别：${severity}\n类别：${category}\n问题：${description}\n建议：${suggestion}\n时间：${time}',NULL,1,1,'2026-06-11 12:00:00','2026-06-11 12:00:00'),
(30,'VERIFICATION_ISSUE_EMAIL','邮件验证问题','EMAIL','VERIFICATION','[${severity}] 验证问题: ${projectName}','<h2>验证问题通知</h2><p><b>项目：</b>${projectName}</p><p><b>级别：</b>${severity}</p><p><b>类别：</b>${category}</p><p><b>问题：</b>${description}</p><p><b>建议：</b>${suggestion}</p><p><b>时间：</b>${time}</p>',NULL,1,1,'2026-06-11 12:00:00','2026-06-11 12:00:00');

-- 表: agent_presets
INSERT IGNORE INTO `agent_presets` (`id`, `api_provider`, `description`, `capabilities`, `max_context_size`, `reasoning_depth`, `role`, `source_agent_id`, `source_project_id`, `supported_file_types`, `supports_code_execution`, `supports_file_operations`, `supports_image_generation`, `is_system`, `tags`, `unsupported_features`, `updated_at`, `created_at`, `name`) VALUES (1,'anthropic','项目制作人，负责协调团队、分配任务、审查工作',NULL,100000,3,'producer',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准制作人'),(2,'anthropic','服务端开发 Agent，负责后端逻辑、API 接口、数据库设计',NULL,100000,3,'server-dev',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准服务端开发'),(3,'anthropic','客户端开发 Agent，负责前端逻辑、交互实现、性能优化',NULL,100000,3,'client-dev',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准客户端开发'),(4,'anthropic','UI/美术开发 Agent，负责界面设计、图标制作、视觉效果',NULL,100000,3,'ui-dev',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准 UI 开发'),(5,'anthropic','系统策划 Agent，负责游戏系统设计、玩法策划、文档编写',NULL,100000,3,'system-planner',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准系统策划'),(6,'anthropic','数值策划 Agent，负责游戏数值设计、经济系统、平衡性',NULL,100000,3,'numerical-planner',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准数值策划'),(7,'anthropic','Git 提交专员 Agent，负责代码提交、分支管理、版本控制',NULL,100000,2,'git-commit',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准 Git 专员'),(8,'anthropic','测试 Agent，负责功能测试、性能测试、Bug 报告',NULL,100000,2,'tester',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','标准测试员'),(9,'anthropic','安全工程师 Agent，负责代码安全审计、漏洞检测、反作弊系统',NULL,100000,3,'security-expert',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','安全工程师'),(10,'anthropic','数据分析师 Agent，负责玩家行为分析、留存分析、付费分析',NULL,100000,3,'data-analyst',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','数据分析师'),(11,'anthropic','技术美术 Agent，负责 Shader 开发、渲染优化、美术工具开发',NULL,100000,3,'tech-artist',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','技术美术'),(12,'anthropic','产品经理 Agent，负责产品规划、需求分析、用户体验设计',NULL,100000,3,'product-manager',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','产品经理'),(13,'anthropic','本地化专员 Agent，负责多语言翻译、文化适配、本地化流程',NULL,100000,2,'localization',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','本地化专员'),(14,'anthropic','AI 工程师 Agent，负责 NPC 行为 AI、寻路算法、对话系统',NULL,100000,3,'ai-engineer',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','AI 工程师'),(15,'anthropic','性能优化师 Agent，负责性能分析、瓶颈定位、优化方案',NULL,100000,3,'performance-engineer',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','性能优化师'),(16,'anthropic','音频设计师 Agent，负责音效设计、背景音乐、音频系统',NULL,100000,2,'audio-dev',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','音频设计师'),(17,'anthropic','剧情策划 Agent，负责世界观构建、角色设定、剧情设计',NULL,100000,3,'narrative-planner',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','剧情策划'),(18,'anthropic','关卡设计师 Agent，负责关卡流程、地图布局、难度曲线',NULL,100000,3,'level-design',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','关卡设计师'),(19,'anthropic','运维工程师 Agent，负责 CI/CD、服务器部署、监控告警',NULL,100000,2,'devops',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-06 02:37:00','2026-06-06 02:37:00','运维工程师'),
(20,'anthropic','验证官 Agent，负责项目约束检查、代码质量验证、设计审查和里程碑验收',NULL,100000,3,'verifier',NULL,NULL,NULL,1,1,0,1,NULL,NULL,'2026-06-11 12:00:00','2026-06-11 12:00:00','验证官');

-- 表: workflow_templates
INSERT IGNORE INTO `workflow_templates` (`id`, `builtin`, `created_at`, `description`, `name`, `steps_json`, `updated_at`) VALUES ('client-only-dev',_binary '','2026-06-06 00:55:50.615020','纯前端项目开发流程，适用于H5游戏、小程序、可视化页面等无服务端的项目','客户端开发流程','[{\"id\":\"plan\",\"name\":\"交互策划\",\"agentRole\":\"system-planner\",\"taskDescription\":\"分析需求，设计页面结构、交互流程和视觉规范\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"design\",\"name\":\"UI开发\",\"agentRole\":\"ui-dev\",\"taskDescription\":\"实现页面布局、组件设计和样式开发\",\"dependencies\":[\"plan\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"dev-client\",\"name\":\"功能开发\",\"agentRole\":\"client-dev\",\"taskDescription\":\"实现交互逻辑、数据绑定和动画效果\",\"dependencies\":[\"design\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"测试验证\",\"agentRole\":\"tester\",\"taskDescription\":\"执行功能测试、兼容性测试和用户体验测试\",\"dependencies\":[\"dev-client\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"发布上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"合并代码并部署到CDN或静态资源服务器\",\"dependencies\":[\"test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.615026'),('code-dev-full',_binary '','2026-06-06 00:55:50.621429','代码从编写、测试、审查到部署的完整流程，确保代码质量','代码开发全流程','[{\"id\":\"analyze\",\"name\":\"需求分析\",\"agentRole\":\"system-planner\",\"taskDescription\":\"分析开发需求，确定技术方案和实现路径\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"code-write\",\"name\":\"代码编写\",\"agentRole\":\"server-dev\",\"taskDescription\":\"根据需求和设计文档编写高质量、可维护的代码，遵循编码规范\",\"dependencies\":[\"analyze\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"unit-test\",\"name\":\"单元测试\",\"agentRole\":\"tester\",\"taskDescription\":\"编写和执行单元测试，确保代码逻辑正确，测试覆盖率达标\",\"dependencies\":[\"code-write\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"integration-test\",\"name\":\"集成测试\",\"agentRole\":\"tester\",\"taskDescription\":\"执行集成测试，验证模块间交互和系统整体功能\",\"dependencies\":[\"unit-test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"code-review\",\"name\":\"代码审查\",\"agentRole\":\"server-dev\",\"taskDescription\":\"对代码进行专业审查，发现潜在问题，提供改进建议。审查不通过则返回修改\",\"dependencies\":[\"integration-test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3},{\"id\":\"code-revise\",\"name\":\"代码修改\",\"agentRole\":\"server-dev\",\"taskDescription\":\"根据审查意见修改代码，解决审查中发现的问题\",\"dependencies\":[\"code-review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"部署上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"合并代码并部署到生产环境，进行线上验证\",\"dependencies\":[\"code-revise\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.621435'),('code-quick-fix',_binary '','2026-06-06 00:55:50.565820','代码快速修复流程，适用于小Bug修复和紧急改动','代码快速修复','[{\"id\":\"fix\",\"name\":\"问题修复\",\"agentRole\":\"server-dev\",\"taskDescription\":\"快速定位并修复代码问题，做最小化改动\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"快速测试\",\"agentRole\":\"tester\",\"taskDescription\":\"快速验证修复是否生效，确认无回归问题\",\"dependencies\":[\"fix\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"快速上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"提交修复代码并快速部署\",\"dependencies\":[\"test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3}]','2026-06-06 00:55:50.565827'),('code-review',_binary '','2026-06-06 00:55:50.607042','标准化的代码审查流程','代码审查流程','[{\"id\":\"submit\",\"name\":\"提交审查\",\"agentRole\":\"git-commit\",\"taskDescription\":\"整理代码变更，准备审查材料\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"review\",\"name\":\"执行审查\",\"agentRole\":\"server-dev\",\"taskDescription\":\"审查代码质量、安全性、规范性\",\"dependencies\":[\"submit\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"merge\",\"name\":\"合并部署\",\"agentRole\":\"git-commit\",\"taskDescription\":\"审查通过后合并代码并触发部署\",\"dependencies\":[\"review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.607056'),('feature-branch',_binary '','2026-06-06 00:55:50.585958','标准的功能开发分支流程','功能分支流程','[{\"id\":\"design\",\"name\":\"功能设计\",\"agentRole\":\"system-planner\",\"taskDescription\":\"设计功能方案、接口定义和数据模型\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"implement\",\"name\":\"功能实现\",\"agentRole\":\"server-dev\",\"taskDescription\":\"在功能分支上实现代码，编写单元测试\",\"dependencies\":[\"design\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"测试验证\",\"agentRole\":\"tester\",\"taskDescription\":\"执行功能测试和回归测试\",\"dependencies\":[\"implement\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"review\",\"name\":\"代码审查\",\"agentRole\":\"server-dev\",\"taskDescription\":\"审查代码质量、设计合理性和测试覆盖率\",\"dependencies\":[\"test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"merge\",\"name\":\"合并上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"审查通过后合并功能分支到主干并部署\",\"dependencies\":[\"review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.585964'),('hotfix',_binary '','2026-06-06 00:55:50.515785','线上问题快速修复流程，精简环节优先恢复服务','紧急修复流程','[{\"id\":\"analyze\",\"name\":\"问题分析\",\"agentRole\":\"system-planner\",\"taskDescription\":\"分析线上问题根因，确定影响范围和修复方案\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"fix\",\"name\":\"紧急修复\",\"agentRole\":\"server-dev\",\"taskDescription\":\"实施最小化修复，不做无关改动\",\"dependencies\":[\"analyze\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"验证测试\",\"agentRole\":\"tester\",\"taskDescription\":\"验证修复是否生效，确认无回归问题\",\"dependencies\":[\"fix\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"紧急上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"合并修复代码并紧急部署到生产环境\",\"dependencies\":[\"test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.515799'),('minimal',_binary '','2026-06-06 00:55:50.592559','极简三步流程','最小可用流程','[{\"id\":\"dev\",\"name\":\"开发\",\"agentRole\":\"server-dev\",\"taskDescription\":\"完成功能开发或改动\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"测试\",\"agentRole\":\"tester\",\"taskDescription\":\"快速验证改动是否生效\",\"dependencies\":[\"dev\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"提交代码并部署\",\"dependencies\":[\"test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3}]','2026-06-06 00:55:50.592565'),('planning-full',_binary '','2026-06-06 00:55:50.573013','策划案从撰写、分析、审核到落实的完整流程，支持审核打回和修改迭代','策划案全流程','[{\"id\":\"plan-write\",\"name\":\"策划案撰写\",\"agentRole\":\"system-planner\",\"taskDescription\":\"根据项目需求撰写完整的游戏策划案，包括核心玩法、系统设计、数值框架、关卡设计等\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"plan-analyze\",\"name\":\"策划案分析\",\"agentRole\":\"numerical-planner\",\"taskDescription\":\"深入分析策划案的可行性、完整性、平衡性和风险点，输出分析报告\",\"dependencies\":[\"plan-write\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"plan-review\",\"name\":\"策划案审核\",\"agentRole\":\"system-planner\",\"taskDescription\":\"对策划案进行专业审核，给出审核意见和是否通过的建议。审核不通过则打回修改\",\"dependencies\":[\"plan-analyze\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3},{\"id\":\"plan-revise\",\"name\":\"策划案修改\",\"agentRole\":\"system-planner\",\"taskDescription\":\"根据审核意见修改策划案，解决审核中发现的问题\",\"dependencies\":[\"plan-review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"plan-implement\",\"name\":\"策划案落实\",\"agentRole\":\"system-planner\",\"taskDescription\":\"将审核通过的策划案转化为可执行的开发任务和技术方案，制定开发计划\",\"dependencies\":[\"plan-revise\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3}]','2026-06-06 00:55:50.573019'),('planning-quick',_binary '','2026-06-06 00:55:50.551236','策划案快速评审流程，精简环节快速完成审核，适用于小型或紧急策划','策划案快速评审','[{\"id\":\"write\",\"name\":\"策划撰写\",\"agentRole\":\"system-planner\",\"taskDescription\":\"快速撰写策划案核心内容，聚焦关键设计点\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"review\",\"name\":\"快速审核\",\"agentRole\":\"system-planner\",\"taskDescription\":\"快速审核策划案，给出通过或打回意见\",\"dependencies\":[\"write\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3},{\"id\":\"implement\",\"name\":\"快速落实\",\"agentRole\":\"system-planner\",\"taskDescription\":\"将策划案快速转化为开发任务\",\"dependencies\":[\"review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3}]','2026-06-06 00:55:50.551245'),('rapid-prototype',_binary '','2026-06-06 00:55:50.579737','快速验证游戏创意的轻量流程，跳过审查直接测试，适合Demo和概念验证','快速原型流程','[{\"id\":\"plan\",\"name\":\"快速策划\",\"agentRole\":\"system-planner\",\"taskDescription\":\"快速分析需求，确定核心玩法和最小功能集\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"dev\",\"name\":\"快速开发\",\"agentRole\":\"server-dev\",\"taskDescription\":\"实现核心功能的最小可用版本，不做过度设计\",\"dependencies\":[\"plan\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"快速测试\",\"agentRole\":\"tester\",\"taskDescription\":\"验证核心功能是否可用，记录明显Bug\",\"dependencies\":[\"dev\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3}]','2026-06-06 00:55:50.579743'),('server-only-dev',_binary '','2026-06-06 00:55:50.558838','纯后端项目开发流程，适用于API服务、微服务、后台管理系统等无客户端的项目','服务端开发流程','[{\"id\":\"plan\",\"name\":\"系统策划\",\"agentRole\":\"system-planner\",\"taskDescription\":\"分析需求，设计API接口、数据库模型和系统架构\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"dev-server\",\"name\":\"服务端开发\",\"agentRole\":\"server-dev\",\"taskDescription\":\"实现业务逻辑、API接口、数据库设计和单元测试\",\"dependencies\":[\"plan\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"测试验证\",\"agentRole\":\"tester\",\"taskDescription\":\"执行接口测试、集成测试和性能测试。如测试失败，需记录问题并通知服务端开发Agent修复后重新测试\",\"dependencies\":[\"dev-server\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"review\",\"name\":\"代码审查\",\"agentRole\":\"server-dev\",\"taskDescription\":\"审查代码质量、API设计合理性、安全性和性能\",\"dependencies\":[\"test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"部署上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"合并代码并部署到生产环境\",\"dependencies\":[\"review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.558845'),('standard-game-dev',_binary '','2026-06-06 00:55:50.599506','完整的游戏开发流程，包含策划审批、技术评审、并行开发、测试验收、bug修复和部署。适用于有客户端和服务端的完整项目','标准游戏开发流程','[{\"id\":\"plan\",\"name\":\"系统策划\",\"agentRole\":\"system-planner\",\"taskDescription\":\"分析游戏需求，撰写完整的系统策划案，包括核心玩法、系统架构、功能模块划分、交互流程和数据模型设计。输出策划文档供数值策划审批\",\"dependencies\":[],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"numerical-review\",\"name\":\"数值策划审批\",\"agentRole\":\"numerical-planner\",\"taskDescription\":\"审查系统策划案的数值可行性、平衡性和完整性。检查数值框架是否合理、成长曲线是否平滑、经济系统是否平衡。审批通过后进入技术评审，不通过则打回修改\",\"dependencies\":[\"plan\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3},{\"id\":\"server-tech-review\",\"name\":\"服务端技术评审\",\"agentRole\":\"server-dev\",\"taskDescription\":\"从服务端角度评审策划案：评估技术可行性、性能瓶颈、数据库设计难度、接口复杂度。输出技术评审意见和风险点\",\"dependencies\":[\"numerical-review\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"client-tech-review\",\"name\":\"客户端技术评审\",\"agentRole\":\"client-dev\",\"taskDescription\":\"从客户端角度评审策划案：评估前端实现难度、渲染性能、交互体验、兼容性问题。输出技术评审意见和风险点\",\"dependencies\":[\"numerical-review\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"server-design\",\"name\":\"服务端设计\",\"agentRole\":\"server-dev\",\"taskDescription\":\"根据策划案和技术评审意见，完成以下设计：\\n1. 数据库设计：用户表、角色表、背包表等核心业务表结构\\n2. 配置表设计：装备配置、技能配置、关卡配置等策划配置表\\n3. 接口文档：RESTful API 接口定义，包含请求/响应格式、错误码\\n输出：数据库DDL、配置表模板、接口文档（Swagger格式）\\n接口文档传递给客户端开发，配置表传递给数值策划\",\"dependencies\":[\"server-tech-review\",\"client-tech-review\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"dev-server\",\"name\":\"服务端开发\",\"agentRole\":\"server-dev\",\"taskDescription\":\"根据接口文档和数据库设计，实现服务端业务逻辑：\\n1. 实现数据库表和ORM映射\\n2. 实现 RESTful API 接口\\n3. 实现核心业务逻辑（战斗、背包、任务等）\\n4. 编写单元测试\\n5. 输出接口联调文档给客户端\",\"dependencies\":[\"server-design\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"dev-client\",\"name\":\"客户端开发\",\"agentRole\":\"client-dev\",\"taskDescription\":\"根据接口文档和策划案，实现客户端功能：\\n1. 实现 UI 界面和交互逻辑\\n2. 对接服务端 API 接口\\n3. 实现游戏核心玩法的前端逻辑\\n4. 实现动画、音效等多媒体资源集成\\n5. 编写自动化测试脚本\",\"dependencies\":[\"server-design\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"dev-numerical\",\"name\":\"数值配置\",\"agentRole\":\"numerical-planner\",\"taskDescription\":\"根据配置表模板和策划案，完成数值配置：\\n1. 填写装备、技能、怪物等配置表数据\\n2. 设计数值成长曲线和平衡参数\\n3. 配置关卡难度和奖励数值\\n4. 输出数值配置文件供服务端加载\",\"dependencies\":[\"server-design\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"server-self-test\",\"name\":\"服务端自测\",\"agentRole\":\"server-dev\",\"taskDescription\":\"服务端自测：验证API接口正确性、数据一致性、性能指标。确保所有接口可正常调用，无明显bug\",\"dependencies\":[\"dev-server\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"client-self-test\",\"name\":\"客户端自测\",\"agentRole\":\"client-dev\",\"taskDescription\":\"客户端自测：验证UI展示正确性、交互流畅性、接口对接无误。确保所有页面可正常访问和操作\",\"dependencies\":[\"dev-client\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"numerical-self-test\",\"name\":\"数值自测\",\"agentRole\":\"numerical-planner\",\"taskDescription\":\"数值自测：验证配置表数据正确性、数值平衡性、成长曲线合理性。确保配置表可正确加载，数值无明显异常\",\"dependencies\":[\"dev-numerical\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"acceptance\",\"name\":\"策划验收\",\"agentRole\":\"system-planner\",\"taskDescription\":\"系统策划验收：对照策划案逐项检查功能实现完整性，确认核心玩法、系统逻辑、交互流程是否符合设计预期。验收不通过则记录问题并打回对应开发方修改\",\"dependencies\":[\"server-self-test\",\"client-self-test\",\"numerical-self-test\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3},{\"id\":\"test\",\"name\":\"测试验证\",\"agentRole\":\"tester\",\"taskDescription\":\"全面测试：\\n1. 功能测试：验证所有功能模块是否正常工作\\n2. 集成测试：验证前后端联调是否正确\\n3. 性能测试：验证响应时间、并发能力、资源占用\\n4. 兼容性测试：验证不同设备和浏览器的兼容性\\n5. 记录所有bug并分配给对应开发方（服务端/客户端/数值）\\n测试不通过则进入bug修复阶段\",\"dependencies\":[\"acceptance\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"fix-server\",\"name\":\"服务端Bug修复\",\"agentRole\":\"server-dev\",\"taskDescription\":\"修复测试阶段发现的服务端bug，确保修复后不影响其他功能\",\"dependencies\":[\"test\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"fix-client\",\"name\":\"客户端Bug修复\",\"agentRole\":\"client-dev\",\"taskDescription\":\"修复测试阶段发现的客户端bug，确保修复后不影响其他功能\",\"dependencies\":[\"test\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"fix-numerical\",\"name\":\"数值Bug修复\",\"agentRole\":\"numerical-planner\",\"taskDescription\":\"修复测试阶段发现的数值配置bug，调整不合理的数值参数\",\"dependencies\":[\"test\"],\"parallel\":true,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"regression\",\"name\":\"回归测试\",\"agentRole\":\"tester\",\"taskDescription\":\"回归测试：重新验证所有已修复的bug，确认修复有效且未引入新问题。执行核心功能回归测试套件，确保系统稳定性\",\"dependencies\":[\"fix-server\",\"fix-client\",\"fix-numerical\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":false,\"maxRetries\":3},{\"id\":\"deploy\",\"name\":\"部署上线\",\"agentRole\":\"git-commit\",\"taskDescription\":\"合并所有代码分支，执行构建流水线，部署到生产环境。进行线上冒烟测试，确认服务正常运行\",\"dependencies\":[\"regression\"],\"parallel\":false,\"timeoutMinutes\":60,\"requiresApproval\":true,\"maxRetries\":3}]','2026-06-06 00:55:50.599512');

SET FOREIGN_KEY_CHECKS = 1;
