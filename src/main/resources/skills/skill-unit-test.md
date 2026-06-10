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
public void shouldCalculateDamage() {
  // Arrange - 准备测试数据
  int attack = 100;
  int defense = 50;
  
  // Act - 执行被测试功能
  int damage = calculateDamage(attack, defense);
  
  // Assert - 验证结果
  assertEquals(50, damage);
}
```

## 测试模板

### 1. Java单元测试
```java
@SpringBootTest
class BattleSystemTest {
  
  @Autowired
  private BattleSystem battleSystem;
  
  @Test
  void shouldCalculateDamage() {
    // Given
    Player player = new Player(100, 500);
    Enemy enemy = new Enemy(50, 200, 30);
    
    // When
    int damage = battleSystem.calculateDamage(player, enemy);
    
    // Then
    assertEquals(70, damage);
  }
  
  @Test
  void shouldHandleCriticalHit() {
    // Given
    Player player = new Player(100, 500);
    Enemy enemy = new Enemy(50, 200, 30);
    
    // When
    int damage = battleSystem.calculateDamage(player, enemy, true);
    
    // Then
    assertEquals(140, damage);
  }
  
  @Test
  void shouldThrowExceptionWhenPlayerIsNull() {
    // Given
    Enemy enemy = new Enemy(50, 200, 30);
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      battleSystem.calculateDamage(null, enemy);
    });
  }
}
```

### 2. JavaScript单元测试
```javascript
describe('BattleSystem', () => {
  let battle;
  let player;
  let enemy;
  
  beforeEach(() => {
    battle = new BattleSystem();
    player = new Player({ attack: 100, hp: 500 });
    enemy = new Enemy({ attack: 50, hp: 200, defense: 30 });
  });
  
  it('should calculate damage correctly', () => {
    const damage = battle.calculateDamage(player, enemy);
    expect(damage).toBe(70);
  });
  
  it('should handle critical hit', () => {
    const damage = battle.calculateDamage(player, enemy, true);
    expect(damage).toBe(140);
  });
  
  it('should throw error when player is null', () => {
    expect(() => battle.calculateDamage(null, enemy)).toThrow();
  });
});
```

## 游戏测试

### 1. 物理系统测试
```javascript
describe('PhysicsSystem', () => {
  it('should detect collision', () => {
    const obj1 = { x: 0, y: 0, width: 10, height: 10 };
    const obj2 = { x: 5, y: 5, width: 10, height: 10 };
    
    expect(checkCollision(obj1, obj2)).toBe(true);
  });
  
  it('should not detect collision when far apart', () => {
    const obj1 = { x: 0, y: 0, width: 10, height: 10 };
    const obj2 = { x: 100, y: 100, width: 10, height: 10 };
    
    expect(checkCollision(obj1, obj2)).toBe(false);
  });
  
  it('should apply gravity', () => {
    const physics = new PhysicsSystem();
    const obj = { x: 0, y: 0, velocity: { x: 0, y: 0 } };
    
    physics.update(obj, 1); // 1秒
    
    expect(obj.velocity.y).toBe(9.8);
  });
});
```

### 2. 状态机测试
```javascript
describe('StateMachine', () => {
  it('should transition between states', () => {
    const sm = new StateMachine();
    sm.addState('idle', { enter: jest.fn() });
    sm.addState('running', { enter: jest.fn() });
    
    sm.changeState('idle');
    expect(sm.currentState).toBe('idle');
    
    sm.changeState('running');
    expect(sm.currentState).toBe('running');
  });
  
  it('should call enter/exit on transition', () => {
    const sm = new StateMachine();
    const idleEnter = jest.fn();
    const idleExit = jest.fn();
    
    sm.addState('idle', { enter: idleEnter, exit: idleExit });
    sm.addState('running', { enter: jest.fn() });
    
    sm.changeState('idle');
    expect(idleEnter).toHaveBeenCalled();
    
    sm.changeState('running');
    expect(idleExit).toHaveBeenCalled();
  });
});
```

### 3. 经济系统测试
```javascript
describe('EconomySystem', () => {
  it('should add currency', () => {
    const economy = new EconomySystem();
    economy.addCurrency('gold', 100);
    expect(economy.getCurrency('gold')).toBe(100);
  });
  
  it('should subtract currency', () => {
    const economy = new EconomySystem();
    economy.addCurrency('gold', 100);
    economy.subtractCurrency('gold', 50);
    expect(economy.getCurrency('gold')).toBe(50);
  });
  
  it('should not allow negative currency', () => {
    const economy = new EconomySystem();
    expect(() => economy.subtractCurrency('gold', 100)).toThrow();
  });
});
```

## Mock技术

### 1. Mock对象
```java
@Mock
private EnemyRepository enemyRepository;

@InjectMocks
private EnemyService enemyService;

@Test
void shouldFindEnemyById() {
  // Given
  Long enemyId = 1L;
  Enemy enemy = new Enemy("Zombie", 100, 50);
  when(enemyRepository.findById(enemyId)).thenReturn(Optional.of(enemy));
  
  // When
  Enemy result = enemyService.findById(enemyId);
  
  // Then
  assertNotNull(result);
  assertEquals("Zombie", result.getName());
  verify(enemyRepository).findById(enemyId);
}
```

### 2. Mock函数
```javascript
// Mock函数
const mockFn = jest.fn();
mockFn.mockReturnValue(42);
mockFn.mockImplementation((x) => x * 2);

// 验证调用
expect(mockFn).toHaveBeenCalled();
expect(mockFn).toHaveBeenCalledWith(1, 2);
expect(mockFn).toHaveBeenCalledTimes(1);
```

## 测试覆盖率

### 1. 覆盖率目标
```
- 语句覆盖率：80%+
- 分支覆盖率：70%+
- 函数覆盖率：90%+
```

### 2. 覆盖率报告
```bash
# Java
mvn jacoco:report

# JavaScript
npm run test:coverage
```

## 测试最佳实践

### 1. 测试命名
```
格式：[被测试功能]_[场景]_[预期结果]

示例：
- calculateDamage_withCriticalHit_shouldDoubleDamage
- login_withValidCredentials_shouldReturnToken
- addToInventory_whenFull_shouldThrowError
```

### 2. 测试数据
```
- 使用有意义的测试数据
- 避免魔法数字
- 使用工厂函数创建测试数据
```

### 3. 测试隔离
```
- 每个测试独立
- 不依赖外部状态
- 使用mock隔离依赖
```

## 常见错误

1. **测试不独立**：测试之间相互依赖
2. **测试不全面**：只测正常流程
3. **测试不稳定**：测试结果不稳定
4. **测试不及时**：测试编写不及时
5. **测试不维护**：测试代码质量差
