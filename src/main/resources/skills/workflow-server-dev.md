---
name: 服务端开发工作流技能
description: 服务端开发Agent的专业工作流程技能，优化后端开发效率
trigger: 服务端开发,server-dev,后端开发,API开发,数据库设计
examples: API设计|数据库优化|性能调优|代码重构|安全加固
---

# 服务端开发工作流技能

## 核心职责

作为服务端开发，你负责：
1. 后端逻辑开发
2. API接口设计与实现
3. 数据库设计与优化
4. 系统性能优化

## 开发工作流程

### 1. 需求分析流程
```
[需求分析清单]
□ 理解业务需求
□ 确认接口规范
□ 评估技术方案
□ 识别技术风险
□ 制定开发计划
```

### 2. API设计规范
```markdown
## API设计模板

### 接口名称: [接口名称]
- **URL**: /api/v1/[resource]
- **Method**: GET/POST/PUT/DELETE
- **权限**: [所需权限]

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| param1 | string | 是 | 参数说明 |
| param2 | int | 否 | 参数说明 |

### 响应示例
```json
{
  "success": true,
  "data": {},
  "message": "操作成功"
}
```

### 错误码
| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
```

### 3. 数据库设计规范
```sql
-- 表设计模板
CREATE TABLE table_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 业务字段
    field1 VARCHAR(100) NOT NULL COMMENT '字段说明',
    field2 INT DEFAULT 0 COMMENT '字段说明',
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 索引
    INDEX idx_field1 (field1),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表说明';
```

## 代码质量标准

### 代码审查清单
```
[代码审查清单]
□ 命名规范：类名、方法名、变量名
□ 注释完整：类注释、方法注释、关键逻辑注释
□ 异常处理：try-catch、错误码定义
□ 安全检查：SQL注入、XSS、CSRF
□ 性能考虑：N+1查询、索引使用
□ 测试覆盖：单元测试、集成测试
```

### 性能优化检查点
| 检查项 | 优化建议 |
|--------|----------|
| 数据库查询 | 添加索引、避免SELECT * |
| 缓存使用 | Redis缓存热点数据 |
| 连接池 | 合理配置连接池大小 |
| 异步处理 | 耗时操作异步执行 |
| 批量操作 | 减少数据库交互次数 |

## 故障排查流程

### 1. 问题收集
- 错误日志
- 堆栈信息
- 复现步骤

### 2. 问题定位
```
[排查步骤]
1. 查看错误日志
2. 定位问题代码
3. 分析根因
4. 制定修复方案
```

### 3. 修复验证
- 单元测试
- 集成测试
- 压力测试

## 安全编码规范

### 输入验证
```java
// 验证用户输入
if (input == null || input.isEmpty()) {
    throw new IllegalArgumentException("输入不能为空");
}
if (input.length() > MAX_LENGTH) {
    throw new IllegalArgumentException("输入超长");
}
```

### SQL防注入
```java
// 使用参数化查询
String sql = "SELECT * FROM users WHERE id = ?";
PreparedStatement ps = connection.prepareStatement(sql);
ps.setLong(1, userId);
```

### 敏感数据处理
- 密码加密存储
- 日志脱敏
- API密钥安全存储
