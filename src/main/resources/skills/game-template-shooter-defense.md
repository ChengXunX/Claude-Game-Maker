---
name: 射击防御游戏开发模板
description: 射击防御游戏开发模板，适用于射击+塔防混合类游戏
trigger: shooter, defense, 射击, 防御, zombie, 僵尸, monster, 怪物, wave, 波次
examples: 僵尸防御|怪物猎人|射击塔防|Bloons TD|Plants vs Zombies
---

# 射击防御游戏开发模板

## 游戏设计核心原则

### 核心循环（每波 1-3 分钟）
```
准备阶段 → 战斗阶段 → 升级阶段 → 下一波
```
- **射击快感**：手动瞄准射击
- **防御策略**：建造防御工事
- **波次挑战**：越来越强的敌人

### 玩家心理学
- **"射击爽感"**：击杀敌人的满足感
- -"建造防御"的策略感**：布置防御工事
- **"波次挑战"的紧张感**：越来越强的敌人
- **"升级成长"的期待感**：武器越来越强

### 射击防御设计要点
```
射击防御核心：
1. 射击手感：射击要爽快
2. 防御策略：建造防御工事
3. 波次系统：敌人越来越强
4. 升级系统：武器越来越强
```

## 核心系统设计

### 1. 射击系统
```javascript
class ShootingSystem {
  constructor() {
    this.weapons = [];
    this.selectedWeapon = 0;
    this.ammo = {};
  }
  
  addWeapon(config) {
    this.weapons.push({
      id: config.id,
      name: config.name,
      damage: config.damage,
      fireRate: config.fireRate,
      bulletSpeed: config.bulletSpeed,
      spread: config.spread,
      ammoType: config.ammoType
    });
    
    this.ammo[config.id] = config.maxAmmo;
  }
  
  shoot(direction) {
    const weapon = this.weapons[this.selectedWeapon];
    
    if (!weapon) return null;
    
    if (this.ammo[weapon.id] <= 0) {
      this.reload();
      return null;
    }
    
    this.ammo[weapon.id]--;
    
    const bullet = {
      position: { ...this.player.position },
      velocity: {
        x: Math.cos(direction) * weapon.bulletSpeed,
        y: Math.sin(direction) * weapon.bulletSpeed
      },
      damage: weapon.damage,
      spread: weapon.spread
    };
    
    return bullet;
  }
  
  reload() {
    const weapon = this.weapons[this.selectedWeapon];
    this.ammo[weapon.id] = weapon.maxAmmo;
  }
  
  switchWeapon(index) {
    if (index >= 0 && index < this.weapons.length) {
      this.selectedWeapon = index;
    }
  }
}

const WEAPONS = [
  { id: 'pistol', name: '手枪', damage: 10, fireRate: 300, bulletSpeed: 500, spread: 0, maxAmmo: 12 },
  { id: 'shotgun', name: '霰弹枪', damage: 8, fireRate: 800, bulletSpeed: 400, spread: 30, maxAmmo: 6 },
  { id: 'rifle', name: '步枪', damage: 15, fireRate: 100, bulletSpeed: 600, spread: 5, maxAmmo: 30 },
  { id: 'sniper', name: '狙击枪', damage: 50, fireRate: 1500, bulletSpeed: 800, spread: 0, maxAmmo: 5 }
];
```

### 2. 波次系统
```javascript
class WaveSystem {
  constructor() {
    this.currentWave = 0;
    this.enemiesAlive = 0;
    this.waveTimer = 0;
    this.betweenWaveTime = 10;
  }
  
  startNextWave() {
    this.currentWave++;
    const waveConfig = this.getWaveConfig(this.currentWave);
    
    this.spawnEnemies(waveConfig);
    
    return waveConfig;
  }
  
  getWaveConfig(wave) {
    const baseEnemies = 5;
    const enemiesPerWave = Math.floor(baseEnemies + wave * 2);
    
    const types = ['basic', 'fast', 'tank', 'flying'];
    const availableTypes = types.slice(0, Math.min(types.length, Math.floor(wave / 3) + 1));
    
    const enemies = [];
    for (let i = 0; i < enemiesPerWave; i++) {
      const type = availableTypes[Math.floor(Math.random() * availableTypes.length)];
      enemies.push({
        type,
        hp: this.getEnemyHp(type, wave),
        speed: this.getEnemySpeed(type, wave),
        damage: this.getEnemyDamage(type, wave)
      });
    }
    
    return {
      wave,
      enemies,
      reward: wave * 100
    };
  }
  
  getEnemyHp(type, wave) {
    const baseHp = { basic: 50, fast: 30, tank: 150, flying: 40 };
    return baseHp[type] * (1 + wave * 0.2);
  }
  
  getEnemySpeed(type, wave) {
    const baseSpeed = { basic: 1, fast: 2, tank: 0.5, flying: 1.5 };
    return baseSpeed[type] * (1 + wave * 0.05);
  }
  
  getEnemyDamage(type, wave) {
    const baseDamage = { basic: 10, fast: 5, tank: 20, flying: 15 };
    return baseDamage[type] * (1 + wave * 0.1);
  }
  
  onEnemyKilled() {
    this.enemiesAlive--;
    
    if (this.enemiesAlive <= 0) {
      this.onWaveComplete();
    }
  }
  
  onWaveComplete() {
    // 发放奖励
    const reward = this.getWaveConfig(this.currentWave).reward;
    this.player.addCurrency(reward);
    
    // 进入准备阶段
    this.state = 'preparing';
    this.waveTimer = this.betweenWaveTime;
  }
}
```

### 3. 防御工事系统
```javascript
class DefenseSystem {
  constructor() {
    this.structures = [];
  }
  
  addStructure(config) {
    this.structures.push({
      id: config.id,
      type: config.type, // wall, turret, mine, barricade
      position: config.position,
      hp: config.hp,
      damage: config.damage,
      range: config.range,
      cost: config.cost
    });
  }
  
  buildStructure(type, position) {
    const config = STRUCTURE_TYPES[type];
    
    if (!config) return null;
    
    if (this.player.currency < config.cost) {
      return { success: false, error: '金币不足' };
    }
    
    this.player.currency -= config.cost;
    
    const structure = {
      id: this.generateId(),
      type,
      position,
      hp: config.hp,
      damage: config.damage,
      range: config.range,
      attackTimer: 0
    };
    
    this.structures.push(structure);
    
    return { success: true, structure };
  }
  
  update(delta) {
    for (const structure of this.structures) {
      if (structure.type === 'turret') {
        this.updateTurret(structure, delta);
      }
    }
  }
  
  updateTurret(turret, delta) {
    turret.attackTimer += delta;
    
    const attackInterval = 1000 / turret.fireRate;
    
    if (turret.attackTimer >= attackInterval) {
      const target = this.findNearestEnemy(turret.position, turret.range);
      
      if (target) {
        this.turretAttack(turret, target);
        turret.attackTimer = 0;
      }
    }
  }
  
  turretAttack(turret, target) {
    const bullet = {
      position: { ...turret.position },
      velocity: this.calculateVelocity(turret.position, target.position, 500),
      damage: turret.damage
    };
    
    this.bullets.push(bullet);
  }
}

const STRUCTURE_TYPES = {
  wall: { name: '墙壁', hp: 200, cost: 50, damage: 0, range: 0 },
  turret: { name: '炮塔', hp: 100, cost: 200, damage: 20, range: 200, fireRate: 2 },
  mine: { name: '地雷', hp: 50, cost: 100, damage: 100, range: 50, oneTime: true },
  barricade: { name: '路障', hp: 150, cost: 75, damage: 0, range: 0, slow: 0.5 }
};
```

### 4. 升级系统
```javascript
class UpgradeSystem {
  constructor() {
    this.upgrades = {};
  }
  
  addUpgrade(config) {
    this.upgrades[config.id] = {
      id: config.id,
      name: config.name,
      description: config.description,
      cost: config.cost,
      maxLevel: config.maxLevel,
      currentLevel: 0,
      effect: config.effect
    };
  }
  
  purchaseUpgrade(upgradeId) {
    const upgrade = this.upgrades[upgradeId];
    
    if (!upgrade) return { success: false, error: '升级不存在' };
    
    if (upgrade.currentLevel >= upgrade.maxLevel) {
      return { success: false, error: '已达最大等级' };
    }
    
    if (this.player.currency < upgrade.cost) {
      return { success: false, error: '金币不足' };
    }
    
    this.player.currency -= upgrade.cost;
    upgrade.currentLevel++;
    
    this.applyUpgrade(upgrade);
    
    return { success: true, upgrade };
  }
  
  applyUpgrade(upgrade) {
    const effect = upgrade.effect;
    
    switch (effect.type) {
      case 'damage':
        this.player.damage *= effect.multiplier;
        break;
      case 'fireRate':
        this.player.fireRate *= effect.multiplier;
        break;
      case 'hp':
        this.player.maxHp += effect.amount;
        this.player.hp += effect.amount;
        break;
      case 'speed':
        this.player.speed *= effect.multiplier;
        break;
    }
  }
}

const UPGRADES = [
  { id: 'damage', name: '伤害提升', description: '增加武器伤害', cost: 100, maxLevel: 5, effect: { type: 'damage', multiplier: 1.2 } },
  { id: 'fireRate', name: '射速提升', description: '增加射击速度', cost: 150, maxLevel: 5, effect: { type: 'fireRate', multiplier: 1.15 } },
  { id: 'hp', name: '生命提升', description: '增加最大生命值', cost: 200, maxLevel: 3, effect: { type: 'hp', amount: 50 } },
  { id: 'speed', name: '移速提升', description: '增加移动速度', cost: 120, maxLevel: 3, effect: { type: 'speed', multiplier: 1.1 } }
];
```

### 5. 敌人系统
```javascript
class EnemySystem {
  constructor() {
    this.enemies = [];
  }
  
  spawnEnemy(config) {
    this.enemies.push({
      id: this.generateId(),
      type: config.type,
      hp: config.hp,
      maxHp: config.hp,
      speed: config.speed,
      damage: config.damage,
      position: { ...config.spawnPosition },
      target: null,
      state: 'moving'
    });
  }
  
  update(delta) {
    for (const enemy of this.enemies) {
      if (enemy.state === 'dead') continue;
      
      // 寻找目标
      if (!enemy.target) {
        enemy.target = this.findNearestTarget(enemy.position);
      }
      
      // 移动到目标
      if (enemy.target) {
        const dist = this.getDistance(enemy.position, enemy.target.position);
        
        if (dist > 30) {
          this.moveToward(enemy, enemy.target.position, delta);
        } else {
          this.attack(enemy, enemy.target);
        }
      }
    }
  }
  
  takeDamage(enemy, damage) {
    enemy.hp -= damage;
    
    if (enemy.hp <= 0) {
      this.killEnemy(enemy);
    }
  }
  
  killEnemy(enemy) {
    enemy.state = 'dead';
    
    // 掉落奖励
    this.dropLoot(enemy);
    
    // 通知波次系统
    this.waveSystem.onEnemyKilled();
  }
}

const ENEMY_TYPES = {
  basic: { name: '基础敌人', hp: 50, speed: 1, damage: 10, reward: 20 },
  fast: { name: '快速敌人', hp: 30, speed: 2, damage: 5, reward: 15 },
  tank: { name: '坦克敌人', hp: 150, speed: 0.5, damage: 20, reward: 50 },
  flying: { name: '飞行敌人', hp: 40, speed: 1.5, damage: 15, reward: 30 }
};
```

## 迭代策略

### 第一版：基础射击
- 简单射击
- 1 种敌人
- 简单 UI
- 基础物理

### 第二版：波次系统
- 波次系统
- 多种敌人
- 计分系统
- 计时系统

### 第三版：防御系统
- 防御工事
- 建造系统
- 资源系统
- 升级系统

### 第四版：深度玩法
- 10 波敌人
- BOSS 战
- 多种武器
- 成就系统

### 第五版：多人模式
- 多人合作
- 房间系统
- 排行榜
- 社区功能

## 常见错误

1. **射击不爽**：射击手感要好
2. **敌人太弱**：要有挑战性
3. **没有升级**：要有成长感
4. **没有防御**：要有防御系统
5. **波次太单调**：要有多种敌人
