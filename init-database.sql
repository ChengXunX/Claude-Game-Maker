-- ============================================================
-- ChengXun Game Maker 数据库初始化脚本
-- 版本: 2.0.0
-- 使用前请确保MySQL服务已启动
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS game_maker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE game_maker;

-- 清除 Flyway 历史（如果是重新初始化）
DROP TABLE IF EXISTS flyway_schema_history;

-- 执行初始化脚本
SOURCE src/main/resources/db/migration/V1__init_schema.sql;

-- 显示数据库信息
SELECT 'Database initialized successfully' AS status;
