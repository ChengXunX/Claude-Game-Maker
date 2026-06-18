-- V41: 为 api_tokens 表新增 provider_type 和 resource_type 字段
-- 支持资源生成引擎的 Token 配置

-- 使用存储过程安全添加字段（如果不存在）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition VARCHAR(500)
)
BEGIN
    DECLARE column_count INT;
    SELECT COUNT(*) INTO column_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table_name
    AND COLUMN_NAME = p_column_name;

    IF column_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 添加字段
CALL add_column_if_not_exists('api_tokens', 'provider_type', "VARCHAR(30) DEFAULT 'ANTHROPIC'");
CALL add_column_if_not_exists('api_tokens', 'resource_type', "VARCHAR(20) DEFAULT 'TEXT'");

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
