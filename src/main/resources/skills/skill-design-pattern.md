---
name: 设计模式应用
description: 常用设计模式的应用场景和实现
trigger: 设计模式,design pattern,单例,工厂,观察者,策略模式
examples: 设计模式|模式应用|代码重构|架构设计
---

# 设计模式应用技能

## 创建型模式

### 1. 单例模式
```java
public class DatabaseManager {
    private static volatile DatabaseManager instance;
    
    private DatabaseManager() {}
    
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
}
```

**使用场景**：数据库连接池、配置管理器、日志管理器

### 2. 工厂模式
```java
public class EnemyFactory {
    public static Enemy createEnemy(String type) {
        switch (type) {
            case "zombie":
                return new Zombie();
            case "skeleton":
                return new Skeleton();
            case "boss":
                return new Boss();
            default:
                throw new IllegalArgumentException("Unknown enemy type: " + type);
        }
    }
}
```

**使用场景**：创建不同类型的对象、游戏实体创建

### 3. 建造者模式
```java
public class CharacterBuilder {
    private String name;
    private int hp;
    private int attack;
    private int defense;
    
    public CharacterBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public CharacterBuilder hp(int hp) {
        this.hp = hp;
        return this;
    }
    
    public CharacterBuilder attack(int attack) {
        this.attack = attack;
        return this;
    }
    
    public CharacterBuilder defense(int defense) {
        this.defense = defense;
        return this;
    }
    
    public Character build() {
        return new Character(name, hp, attack, defense);
    }
}
```

**使用场景**：复杂对象构建、配置对象创建

## 结构型模式

### 1. 适配器模式
```java
// 旧接口
interface OldPaymentSystem {
    void processPayment(double amount);
}

// 新接口
interface NewPaymentSystem {
    void pay(double amount, String currency);
}

// 适配器
class PaymentAdapter implements NewPaymentSystem {
    private OldPaymentSystem oldSystem;
    
    public PaymentAdapter(OldPaymentSystem oldSystem) {
        this.oldSystem = oldSystem;
    }
    
    @Override
    public void pay(double amount, String currency) {
        // 转换逻辑
        oldSystem.processPayment(amount);
    }
}
```

**使用场景**：接口兼容、第三方库集成

### 2. 装饰器模式
```java
interface Weapon {
    int getDamage();
    String getDescription();
}

class BasicWeapon implements Weapon {
    @Override
    public int getDamage() { return 10; }
    
    @Override
    public String getDescription() { return "Basic Sword"; }
}

abstract class WeaponDecorator implements Weapon {
    protected Weapon weapon;
    
    public WeaponDecorator(Weapon weapon) {
        this.weapon = weapon;
    }
}

class FireEnchant extends WeaponDecorator {
    public FireEnchant(Weapon weapon) {
        super(weapon);
    }
    
    @Override
    public int getDamage() {
        return weapon.getDamage() + 5;
    }
    
    @Override
    public String getDescription() {
        return weapon.getDescription() + " of Fire";
    }
}
```

**使用场景**：动态添加功能、装备强化

### 3. 组合模式
```java
interface Component {
    void render();
    void update();
}

class Leaf implements Component {
    private String name;
    
    @Override
    public void render() {
        System.out.println("Rendering leaf: " + name);
    }
    
    @Override
    public void update() {
        // 更新逻辑
    }
}

class Composite implements Component {
    private List<Component> children = new ArrayList<>();
    
    public void add(Component component) {
        children.add(component);
    }
    
    @Override
    public void render() {
        for (Component child : children) {
            child.render();
        }
    }
    
    @Override
    public void update() {
        for (Component child : children) {
            child.update();
        }
    }
}
```

**使用场景**：UI组件树、游戏对象层级

## 行为型模式

### 1. 观察者模式
```java
interface Observer {
    void update(String event, Object data);
}

class EventBus {
    private Map<String, List<Observer>> observers = new HashMap<>();
    
    public void subscribe(String event, Observer observer) {
        observers.computeIfAbsent(event, k -> new ArrayList<>()).add(observer);
    }
    
    public void publish(String event, Object data) {
        List<Observer> eventObservers = observers.get(event);
        if (eventObservers != null) {
            for (Observer observer : eventObservers) {
                observer.update(event, data);
            }
        }
    }
}
```

**使用场景**：事件系统、消息通知

### 2. 策略模式
```java
interface AttackStrategy {
    void attack(Character attacker, Character target);
}

class MeleeAttack implements AttackStrategy {
    @Override
    public void attack(Character attacker, Character target) {
        int damage = attacker.getAttack();
        target.takeDamage(damage);
    }
}

class RangedAttack implements AttackStrategy {
    @Override
    public void attack(Character attacker, Character target) {
        int damage = attacker.getAttack() / 2;
        target.takeDamage(damage);
    }
}

class Character {
    private AttackStrategy attackStrategy;
    
    public void setAttackStrategy(AttackStrategy strategy) {
        this.attackStrategy = strategy;
    }
    
    public void attack(Character target) {
        attackStrategy.attack(this, target);
    }
}
```

**使用场景**：不同攻击方式、AI行为

### 3. 状态模式
```java
interface State {
    void enter();
    void update();
    void exit();
}

class IdleState implements State {
    @Override
    public void enter() {
        System.out.println("Entering idle state");
    }
    
    @Override
    public void update() {
        // 检查输入，切换到移动状态
    }
    
    @Override
    public void exit() {
        System.out.println("Exiting idle state");
    }
}

class StateMachine {
    private Map<String, State> states = new HashMap<>();
    private State currentState;
    
    public void addState(String name, State state) {
        states.put(name, state);
    }
    
    public void changeState(String name) {
        if (currentState != null) {
            currentState.exit();
        }
        currentState = states.get(name);
        currentState.enter();
    }
    
    public void update() {
        if (currentState != null) {
            currentState.update();
        }
    }
}
```

**使用场景**：角色状态管理、游戏状态管理

## 游戏常用模式

### 1. 对象池模式
```javascript
class ObjectPool {
  constructor(createFn, resetFn, initialSize) {
    this.createFn = createFn;
    this.resetFn = resetFn;
    this.pool = [];
    
    for (let i = 0; i < initialSize; i++) {
      this.pool.push(createFn());
    }
  }
  
  get() {
    if (this.pool.length > 0) {
      return this.pool.pop();
    }
    return this.createFn();
  }
  
  release(obj) {
    this.resetFn(obj);
    this.pool.push(obj);
  }
}
```

### 2. 组件模式
```javascript
class Entity {
  constructor() {
    this.components = {};
  }
  
  addComponent(component) {
    this.components[component.constructor.name] = component;
  }
  
  getComponent(ComponentClass) {
    return this.components[ComponentClass.name];
  }
  
  update(deltaTime) {
    for (const component of Object.values(this.components)) {
      component.update(deltaTime);
    }
  }
}

class PositionComponent {
  constructor(x, y) {
    this.x = x;
    this.y = y;
  }
  
  update(deltaTime) {
    // 更新位置
  }
}

class RenderComponent {
  constructor(sprite) {
    this.sprite = sprite;
  }
  
  update(deltaTime) {
    // 渲染
  }
}
```

## 常见错误

1. **过度设计**：不要过度使用设计模式
2. **模式误用**：要选择合适的模式
3. **不理解意图**：要理解模式的核心思想
4. **不考虑场景**：要结合实际场景
5. **不优化性能**：要考虑性能影响
