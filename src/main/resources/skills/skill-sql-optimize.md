---
name: SQL优化
description: 分析和优化SQL查询语句，提升数据库性能
trigger: SQL优化,查询优化,慢查询,数据库性能,索引优化
examples: 优化查询|慢查询分析|索引建议|执行计划分析
---

# SQL优化技能

## 优化流程

### 1. 分析查询
```sql
-- 查看执行计划
EXPLAIN SELECT * FROM users WHERE status = 'ACTIVE';

-- 查看实际执行情况
EXPLAIN ANALYZE SELECT * FROM users WHERE status = 'ACTIVE';
```

### 2. 常见优化策略

#### 索引优化
```sql
-- 添加索引
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);

-- 复合索引遵循最左前缀
-- 好: WHERE user_id = 1 AND created_at > '2024-01-01'
-- 差: WHERE created_at > '2024-01-01' (无法使用索引)
```

#### 查询重写
```sql
-- 避免 SELECT *
SELECT id, username, email FROM users WHERE id = 1;

-- 使用 EXISTS 代替 IN
-- 差
SELECT * FROM users WHERE id IN (SELECT user_id FROM orders);
-- 好
SELECT * FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- 分页优化
-- 差
SELECT * FROM orders ORDER BY id LIMIT 10 OFFSET 10000;
-- 好
SELECT * FROM orders WHERE id > 10000 ORDER BY id LIMIT 10;
```

### 3. 性能检查清单
- [ ] WHERE条件字段是否有索引
- [ ] 是否避免了全表扫描
- [ ] JOIN字段是否有索引
- [ ] 是否使用了合适的分页方式
- [ ] 是否避免了在WHERE中使用函数
