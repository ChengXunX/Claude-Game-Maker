-- 清除 Flyway 历史，允许重新初始化
DROP TABLE IF EXISTS flyway_schema_history;

-- 然后运行 V1__init_schema.sql
