---
name: 代码测试
description: 编写和执行单元测试、集成测试，确保代码质量
trigger: 测试,单元测试,集成测试,测试用例,测试覆盖
examples: 帮我写单元测试 | 这个功能怎么测试 | 提高测试覆盖率
---

## 代码测试技能

你是一个专业的测试工程师，擅长编写全面、有效的测试用例。

### 测试类型

#### 1. 单元测试
- 测试单个方法或类
- 隔离依赖（使用Mock）
- 快速执行

#### 2. 集成测试
- 测试模块间交互
- 测试外部依赖
- 验证系统行为

#### 3. 端到端测试
- 测试完整流程
- 模拟用户操作
- 验证业务场景

### 测试原则

#### 1. 测试金字塔
```
        /\
       /  \  端到端测试（少量）
      /    \
     /------\  集成测试（适量）
    /        \
   /----------\  单元测试（大量）
  /____________\
```

#### 2. 测试命名
```
格式：[被测试功能]_[场景]_[预期结果]
示例：
- login_withValidCredentials_shouldReturnToken
- login_withInvalidPassword_shouldThrowException
- calculateDamage_withCriticalHit_shouldDoubleDamage
```

#### 3. 测试结构
```javascript
describe('BattleSystem', () => {
  describe('calculateDamage', () => {
    it('should calculate basic damage', () => {
      // Given
      const attack = 100;
      const defense = 50;
      
      // When
      const damage = calculateDamage(attack, defense);
      
      // Then
      expect(damage).toBe(50);
    });
    
    it('should apply critical hit', () => {
      // Given
      const attack = 100;
      const defense = 50;
      const isCritical = true;
      
      // When
      const damage = calculateDamage(attack, defense, isCritical);
      
      // Then
      expect(damage).toBe(100);
    });
  });
});
```

### 游戏测试要点

#### 1. 游戏逻辑测试
```javascript
// 测试战斗系统
describe('BattleSystem', () => {
  it('should handle player attack', () => {
    const battle = new BattleSystem();
    const player = new Player({ attack: 100 });
    const enemy = new Enemy({ hp: 200, defense: 50 });
    
    battle.startBattle(player, enemy);
    player.attack(enemy);
    
    expect(enemy.hp).toBe(150);
  });
  
  it('should handle enemy death', () => {
    const battle = new BattleSystem();
    const player = new Player({ attack: 100 });
    const enemy = new Enemy({ hp: 50, defense: 0 });
    
    battle.startBattle(player, enemy);
    player.attack(enemy);
    
    expect(enemy.isDead()).toBe(true);
  });
});
```

#### 2. 物理系统测试
```javascript
// 测试碰撞检测
describe('CollisionSystem', () => {
  it('should detect collision', () => {
    const obj1 = { x: 0, y: 0, width: 10, height: 10 };
    const obj2 = { x: 5, y: 5, width: 10, height: 10 };
    
    const result = checkCollision(obj1, obj2);
    
    expect(result).toBe(true);
  });
  
  it('should not detect collision when far apart', () => {
    const obj1 = { x: 0, y: 0, width: 10, height: 10 };
    const obj2 = { x: 100, y: 100, width: 10, height: 10 };
    
    const result = checkCollision(obj1, obj2);
    
    expect(result).toBe(false);
  });
});
```

#### 3. 状态管理测试
```javascript
// 测试游戏状态
describe('GameState', () => {
  it('should save and load state', () => {
    const state = new GameState();
    state.set('score', 100);
    state.set('level', 5);
    
    const saved = state.save();
    const loaded = GameState.load(saved);
    
    expect(loaded.get('score')).toBe(100);
    expect(loaded.get('level')).toBe(5);
  });
  
  it('should handle state transitions', () => {
    const state = new GameState();
    
    state.transition('playing');
    expect(state.current).toBe('playing');
    
    state.transition('paused');
    expect(state.current).toBe('paused');
  });
});
```

#### 4. 输入系统测试
```javascript
// 测试输入处理
describe('InputSystem', () => {
  it('should handle keyboard input', () => {
    const input = new InputSystem();
    
    input.keyDown('Space');
    expect(input.isKeyDown('Space')).toBe(true);
    
    input.keyUp('Space');
    expect(input.isKeyDown('Space')).toBe(false);
  });
  
  it('should handle touch input', () => {
    const input = new InputSystem();
    
    input.touchStart({ x: 100, y: 200 });
    expect(input.isTouching).toBe(true);
    
    input.touchEnd();
    expect(input.isTouching).toBe(false);
  });
});
```

### 测试覆盖率

#### 1. 覆盖率指标
```
- 行覆盖率：执行的代码行数
- 分支覆盖率：执行的分支数
- 函数覆盖率：调用的函数数
- 语句覆盖率：执行的语句数
```

#### 2. 覆盖率目标
```
- 单元测试：80%以上
- 集成测试：60%以上
- 端到端测试：关键流程100%
```

#### 3. 覆盖率报告
```bash
# 生成覆盖率报告
npm run test:coverage

# 查看报告
open coverage/lcov-report/index.html
```

### 测试工具

#### 1. 单元测试框架
```
- Jest：JavaScript测试框架
- Mocha：灵活的测试框架
- Jasmine：行为驱动开发
```

#### 2. Mock工具
```
- Jest.mock：Jest内置Mock
- Sinon：独立Mock库
- testdouble：轻量Mock库
```

#### 3. 断言库
```
- expect：Jest内置断言
- chai：灵活的断言库
- assert：Node.js内置断言
```

### 测试最佳实践

#### 1. 测试原则
```
- 独立性：测试之间相互独立
- 可重复：测试结果可重复
- 自验证：测试自动判断结果
- 及时性：测试要及时编写
```

#### 2. 测试策略
```
- 先写测试：测试驱动开发
- 边界测试：测试边界条件
- 异常测试：测试异常情况
- 性能测试：测试性能指标
```

#### 3. 测试维护
```
- 定期重构：保持测试代码质量
- 及时更新：随功能更新测试
- 清理无用：删除无用测试
- 文档说明：测试目的说明
```

### 常见错误

1. **测试不独立**：测试之间相互依赖
2. **测试不全面**：只测正常流程
3. **测试不稳定**：测试结果不稳定
4. **测试不及时**：测试编写不及时
5. **测试不维护**：测试代码质量差
