---
name: 单元测试编写
description: 编写高质量的单元测试代码
trigger: 单元测试,unit test,测试编写,JUnit,测试用例
examples: 编写测试|测试用例|测试覆盖|Mock测试
---

# 单元测试编写技能

## 测试原则

### FIRST原则
- **Fast**: 测试要快
- **Independent**: 测试独立
- **Repeatable**: 可重复
- **Self-validating**: 自我验证
- **Timely**: 及时编写

### AAA模式
```java
@Test
void testCalculateTotal() {
    // Arrange - 准备
    Order order = new Order();
    order.addItem(new Item("apple", 10.0, 2));
    
    // Act - 执行
    double total = order.calculateTotal();
    
    // Assert - 验证
    assertEquals(20.0, total);
}
```

## JUnit5常用注解

```java
@BeforeEach    // 每个测试前执行
@AfterEach     // 每个测试后执行
@BeforeAll     // 所有测试前执行（静态）
@AfterAll      // 所有测试后执行（静态）
@Disabled      // 禁用测试
@RepeatedTest  // 重复测试
@ParameterizedTest  // 参数化测试
```

## Mock测试

### Mockito使用
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testGetUser() {
        // 准备
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(new User("admin")));
        
        // 执行
        User user = userService.getUser(1L);
        
        // 验证
        assertEquals("admin", user.getUsername());
        verify(userRepository).findById(1L);
    }
}
```

## 测试覆盖率

### 覆盖目标
| 类型 | 目标 |
|------|------|
| 行覆盖率 | > 80% |
| 分支覆盖率 | > 70% |
| 方法覆盖率 | > 90% |

### 测试场景
- 正常流程
- 边界条件
- 异常情况
- 空值处理
