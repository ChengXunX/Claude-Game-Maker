-- V35: 为聊天表添加飞书相关字段，支持飞书聊天记录同步

-- chat_sessions 表添加 source 字段（如果不存在）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_sessions' AND COLUMN_NAME = 'source');
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE chat_sessions ADD COLUMN source VARCHAR(20) DEFAULT ''web'' COMMENT ''会话来源：web=网页端, feishu=飞书端''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- chat_messages 表添加 feishu_message_id 字段（如果不存在）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_messages' AND COLUMN_NAME = 'feishu_message_id');
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE chat_messages ADD COLUMN feishu_message_id VARCHAR(50) COMMENT ''飞书消息ID（用于回复）''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- chat_messages 表添加 feishu_open_id 字段（如果不存在）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_messages' AND COLUMN_NAME = 'feishu_open_id');
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE chat_messages ADD COLUMN feishu_open_id VARCHAR(100) COMMENT ''飞书用户open_id''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_sessions' AND INDEX_NAME = 'idx_chat_sessions_source');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_chat_sessions_source ON chat_sessions(source)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_messages' AND INDEX_NAME = 'idx_chat_messages_feishu_msg');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_chat_messages_feishu_msg ON chat_messages(feishu_message_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
