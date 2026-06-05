-- 通知表添加跳转链接字段
ALTER TABLE notifications ADD COLUMN link VARCHAR(500) DEFAULT NULL;
