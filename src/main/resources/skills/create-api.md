---
name: 创建API
description: 创建 RESTful API 端点
trigger: API,接口,rest,endpoint,创建接口
examples: 创建用户 API|create REST endpoint|添加接口
---

你是一位 API 设计专家。请帮助创建高质量的 RESTful API。

## API 设计原则

### 1. RESTful 规范
- 使用正确的 HTTP 方法
- 使用名词而非动词
- 使用复数形式
- 使用合适的状态码

### 2. URL 设计
```
GET    /api/resources          # 获取列表
GET    /api/resources/{id}     # 获取单个
POST   /api/resources          # 创建
PUT    /api/resources/{id}     # 更新
DELETE /api/resources/{id}     # 删除
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
POST   /api/player/login           # 登录
GET    /api/player/profile         # 获取玩家信息
PUT    /api/player/profile         # 更新玩家信息
GET    /api/player/inventory       # 获取背包
POST   /api/player/inventory/use   # 使用物品
GET    /api/player/achievements    # 获取成就
```

### 2. 游戏API
```
GET    /api/game/state             # 获取游戏状态
POST   /api/game/action            # 执行游戏操作
GET    /api/game/leaderboard       # 获取排行榜
POST   /api/game/report            # 上报游戏数据
GET    /api/game/config            # 获取游戏配置
```

### 3. 社交API
```
GET    /api/friends                # 获取好友列表
POST   /api/friends/add            # 添加好友
DELETE /api/friends/{id}           # 删除好友
GET    /api/chat/messages          # 获取聊天记录
POST   /api/chat/send              # 发送消息
```

## API实现模板

### 1. 控制器模板
```java
@RestController
@RequestMapping("/api/resources")
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

### 3. 数据层模板
```java
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
    List<Resource> findByName(String name);
    
    List<Resource> findByStatus(String status);
    
    @Query("SELECT r FROM Resource r WHERE r.createdAt > :date")
    List<Resource> findRecent(@Param("date") LocalDateTime date);
}
```

## API安全

### 1. 认证授权
```java
@RestController
@RequestMapping("/api/protected")
public class ProtectedController {

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('PERM_data:view')")
    public ResponseEntity<Data> getData() {
        // 需要权限才能访问
        return ResponseEntity.ok(data);
    }
}
```

### 2. 输入验证
```java
public class ResourceRequest {
    
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称不能超过100字符")
    private String name;
    
    @NotNull(message = "类型不能为空")
    private String type;
    
    @Min(value = 0, message = "数量不能为负")
    private Integer quantity;
}
```

### 3. 限流
```java
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @GetMapping
    @RateLimiter(name = "resourceApi", fallbackMethod = "fallback")
    public ResponseEntity<List<Resource>> getAll() {
        return ResponseEntity.ok(resourceService.getAll());
    }

    public ResponseEntity<List<Resource>> fallback(Exception e) {
        return ResponseEntity.status(429).body(null);
    }
}
```

## API文档

### 1. Swagger注解
```java
@Tag(name = "资源管理", description = "资源CRUD操作")
@RestController
@RequestMapping("/api/resources")
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

## API测试

### 1. 单元测试
```java
@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetAllResources() throws Exception {
        mockMvc.perform(get("/api/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldCreateResource() throws Exception {
        Resource resource = new Resource("test", "type");
        
        mockMvc.perform(post("/api/resources")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resource)))
            .andExpect(status().isCreated());
    }
}
```

### 2. 集成测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndRetrieveResource() {
        Resource resource = new Resource("test", "type");
        
        ResponseEntity<Resource> createResponse = restTemplate.postForEntity(
            "/api/resources", resource, Resource.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        ResponseEntity<Resource> getResponse = restTemplate.getForEntity(
            "/api/resources/" + createResponse.getBody().getId(), Resource.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("test");
    }
}
```

## 常见错误

1. **URL设计不规范**：要遵循RESTful规范
2. **状态码使用不当**：要使用正确的状态码
3. **输入验证不足**：要验证所有输入
4. **错误处理不当**：要提供有用的错误信息
5. **文档不完整**：要提供完整的API文档
