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
```
- 使用名词，不用动词
- 使用复数形式
- 使用小写字母
- 使用连字符分隔
- 使用层级关系
```

## 设计原则

### 1. RESTful规范
```
- 使用正确的HTTP方法
- 使用合适的状态码
- 使用统一的响应格式
- 使用版本控制
```

### 2. 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 3. 状态码使用
```
200 OK                    # 成功
201 Created              # 创建成功
204 No Content           # 删除成功
400 Bad Request          # 请求错误
401 Unauthorized         # 未认证
403 Forbidden            # 无权限
404 Not Found            # 资源不存在
409 Conflict             # 冲突
500 Internal Server Error # 服务器错误
```

## 游戏API设计

### 1. 玩家API
```
POST   /api/v1/player/login           # 登录
GET    /api/v1/player/profile         # 获取玩家信息
PUT    /api/v1/player/profile         # 更新玩家信息
GET    /api/v1/player/inventory       # 获取背包
POST   /api/v1/player/inventory/use   # 使用物品
GET    /api/v1/player/achievements    # 获取成就
```

### 2. 游戏API
```
GET    /api/v1/game/state             # 获取游戏状态
POST   /api/v1/game/action            # 执行游戏操作
GET    /api/v1/game/leaderboard       # 获取排行榜
POST   /api/v1/game/report            # 上报游戏数据
GET    /api/v1/game/config            # 获取游戏配置
```

### 3. 社交API
```
GET    /api/v1/friends                # 获取好友列表
POST   /api/v1/friends/add            # 添加好友
DELETE /api/v1/friends/{id}           # 删除好友
GET    /api/v1/chat/messages          # 获取聊天记录
POST   /api/v1/chat/send              # 发送消息
```

## API实现模板

### 1. 控制器模板
```java
@RestController
@RequestMapping("/api/v1/resources")
@Tag(name = "资源管理", description = "资源CRUD操作")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    @Operation(summary = "获取资源列表")
    public ResponseEntity<List<Resource>> getAll() {
        return ResponseEntity.ok(resourceService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个资源")
    public ResponseEntity<Resource> getById(@PathVariable Long id) {
        Resource resource = resourceService.getById(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resource);
    }

    @PostMapping
    @Operation(summary = "创建资源")
    public ResponseEntity<Resource> create(@RequestBody Resource resource) {
        Resource created = resourceService.create(resource);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新资源")
    public ResponseEntity<Resource> update(@PathVariable Long id, @RequestBody Resource resource) {
        Resource updated = resourceService.update(id, resource);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资源")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 2. 服务层模板
```java
@Service
public class ResourceService {

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    public List<Resource> getAll() {
        return repository.findAll();
    }

    public Resource getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Resource create(Resource resource) {
        return repository.save(resource);
    }

    public Resource update(Long id, Resource resource) {
        if (!repository.existsById(id)) {
            return null;
        }
        resource.setId(id);
        return repository.save(resource);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

## API文档

### 1. Swagger注解
```java
@Tag(name = "资源管理", description = "资源CRUD操作")
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    @Operation(summary = "获取资源列表", description = "获取所有资源的列表")
    @ApiResponse(responseCode = "200", description = "成功获取资源列表")
    @GetMapping
    public ResponseEntity<List<Resource>> getAll() {
        // ...
    }
}
```

### 2. API文档生成
```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## 常见错误

1. **URL设计不规范**：要遵循RESTful规范
2. **状态码使用不当**：要使用正确的状态码
3. **输入验证不足**：要验证所有输入
4. **错误处理不当**：要提供有用的错误信息
5. **文档不完整**：要提供完整的API文档
