---
name: 日志分析
description: 分析系统日志，定位问题根因
trigger: 日志分析,问题排查,错误定位,故障诊断
examples: 分析日志|定位问题|排查错误|故障分析
---

# 日志分析技能

## 日志级别

| 级别 | 说明 | 使用场景 |
|------|------|----------|
| ERROR | 错误 | 系统错误、异常 |
| WARN | 警告 | 潜在问题 |
| INFO | 信息 | 关键操作 |
| DEBUG | 调试 | 开发调试 |

## 分析流程

### 1. 收集信息
```
[问题信息收集]
□ 问题发生时间
□ 问题现象描述
□ 影响范围
□ 复现步骤
□ 相关日志
```

### 2. 日志过滤
```bash
# 按时间过滤
grep "2024-01-01 10:00" app.log

# 按级别过滤
grep "ERROR" app.log

# 按关键词过滤
grep "Exception" app.log

# 组合过滤
grep "ERROR" app.log | grep "2024-01-01"
```

### 3. 常见错误模式

#### 数据库错误
```
ERROR: Connection refused
原因: 数据库连接失败
解决: 检查数据库服务、连接配置

ERROR: Duplicate entry
原因: 唯一约束冲突
解决: 检查数据唯一性
```

#### 内存错误
```
ERROR: OutOfMemoryError
原因: 内存溢出
解决: 增加内存、优化代码

ERROR: StackOverflowError
原因: 递归过深
解决: 检查递归终止条件
```

#### 网络错误
```
ERROR: Connection timeout
原因: 网络超时
解决: 检查网络、增加超时时间

ERROR: Connection refused
原因: 服务未启动
解决: 检查服务状态
```

## 日志工具

### 常用命令
```bash
# 实时查看日志
tail -f app.log

# 查看最后100行
tail -100 app.log

# 统计错误数量
grep -c "ERROR" app.log

# 查看异常堆栈
grep -A 20 "Exception" app.log
```

### 日志分析工具
| 工具 | 用途 |
|------|------|
| ELK Stack | 日志收集分析 |
| Grafana | 日志可视化 |
| Splunk | 企业级日志分析 |
