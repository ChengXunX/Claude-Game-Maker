---
name: RESTful API设计
description: 设计规范的RESTful API接口
trigger: API设计,接口设计,REST,接口规范
examples: 设计API|接口规范|RESTful|API文档
---

# RESTful API设计技能

## 命名规范

### URL格式
```
GET    /api/v1/users          # 获取列表
GET    /api/v1/users/{id}     # 获取详情
POST   /api/v1/users          # 创建
PUT    /api/v1/users/{id}     # 更新
DELETE /api/v1/users/{id}     # 删除
```

### 命名规则
- 使用复数名词: `/users` 不是 `/user`
- 使用小写字母: `/user-profiles` 不是 `/UserProfiles`
- 使用连字符分隔: `/user-profiles` 不是 `/user_profiles`

## 响应格式

### 成功响应
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin"
  },
  "message": "操作成功"
}
```

### 列表响应
```json
{
  "success": true,
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 错误响应
```json
{
  "success": false,
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "参数验证失败",
  "errors": [
    {"field": "username", "message": "用户名不能为空"}
  ]
}
```

## 状态码使用

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | OK | 成功 |
| 201 | Created | 创建成功 |
| 204 | No Content | 删除成功 |
| 400 | Bad Request | 参数错误 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 冲突 |
| 500 | Internal Error | 服务器错误 |

## 版本管理

```
/api/v1/users
/api/v2/users
```

## 分页参数

```
GET /api/v1/users?page=1&size=20&sort=createdAt,desc
```
