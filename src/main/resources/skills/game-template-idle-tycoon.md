---
name: game-template-idle-tycoon
description: 数值养成经营游戏模板 - 店铺经营、员工培养、数值成长、自动收益
category: game-template
triggerPattern: idle, 放置, tycoon, 经营, 养成, 数值, 店铺, 商业, 经理, manager
---

# 数值养成经营游戏模板

## 概述

完整的数值养成经营游戏模板，包含：
- **店铺系统**：多种店铺类型、升级扩张
- **员工系统**：招聘、培养、技能提升
- **数值成长**：收入、声望、等级递增
- **离线收益**：退出游戏后自动积累
- **任务系统**：主线/支线任务
- **成就系统**：达成目标获得奖励

## 核心代码

### 1. 游戏配置 (config.js)

```javascript
export const GAME_CONFIG = {
  tickInterval: 1000,          // 游戏循环间隔（毫秒）
  offlineMultiplier: 0.5,      // 离线收益倍率
  maxOfflineHours: 24,         // 最大离线时间
  baseIncome: 10,              // 基础收入
  prestigeMultiplier: 1.5,     // 转生倍率
  maxLevel: 100,               // 最大等级
  autoSaveInterval: 30000      // 自动保存间隔
}
```

### 2. 店铺系统 (Shop.js)

```javascript
export class Shop {
  constructor(config) {
    this.id = config.id
    this.name = config.name
    this.level = 1
    this.baseIncome = config.baseIncome
    this.baseCost = config.baseCost
    this.incomeMultiplier = 1
    this.costMultiplier = 1
    this.employees = []
    this.lastCollectTime = Date.now()
  }

  getIncome() {
    const base = this.baseIncome * Math.pow(1.1, this.level - 1)
    const employeeBonus = this.employees.reduce((sum, e) => sum + e.getBonus(), 0)
    return Math.floor(base * this.incomeMultiplier + employeeBonus)
  }

  getUpgradeCost() {
    return Math.floor(this.baseCost * Math.pow(1.15, this.level - 1) * this.costMultiplier)
  }

  upgrade() {
    this.level++
    return this.level
  }

  hireEmployee(employee) {
    this.employees.push(employee)
  }

  getOfflineEarnings(offlineMs) {
    const hours = Math.min(offlineMs / 3600000, GAME_CONFIG.maxOfflineHours)
    return Math.floor(this.getIncome() * (hours * 3600) * GAME_CONFIG.offlineMultiplier)
  }
}
```

### 3. 员工系统 (Employee.js)

```javascript
export class Employee {
  constructor(config) {
    this.id = config.id
    this.name = config.name
    this.role = config.role
    this.level = 1
    this.exp = 0
    this.skills = config.skills || []
    this.salary = config.salary || 100
  }

  getBonus() {
    const levelBonus = this.level * 5
    const skillBonus = this.skills.reduce((sum, s) => sum + s.value, 0)
    return levelBonus + skillBonus
  }

  addExp(amount) {
    this.exp += amount
    const required = this.getRequiredExp()
    if (this.exp >= required) {
      this.levelUp()
      return true
    }
    return false
  }

  levelUp() {
    this.level++
    this.exp -= this.getRequiredExp()
    this.salary = Math.floor(this.salary * 1.1)
  }

  getRequiredExp() {
    return Math.floor(100 * Math.pow(1.5, this.level - 1))
  }
}
```

### 4. 数值管理器 (NumericalManager.js)

```javascript
export class NumericalManager {
  constructor() {
    this.level = 1
    this.exp = 0
    this.gold = 100
    this.gems = 0
    this.prestigeCount = 0
    this.prestigeMultiplier = 1
  }

  addExp(amount) {
    this.exp += amount * this.prestigeMultiplier
    while (this.exp >= this.getRequiredExp() && this.level < GAME_CONFIG.maxLevel) {
      this.levelUp()
    }
  }

  levelUp() {
    this.level++
    this.exp -= this.getRequiredExp()
    this.gold += this.level * 50
  }

  getRequiredExp() {
    return Math.floor(100 * Math.pow(1.2, this.level - 1))
  }

  canPrestige() {
    return this.level >= 50
  }

  prestige() {
    if (!this.canPrestige()) return false
    this.prestigeCount++
    this.prestigeMultiplier = 1 + (this.prestigeCount * 0.1)
    this.level = 1
    this.exp = 0
    return true
  }

  addGold(amount) {
    this.gold += Math.floor(amount * this.prestigeMultiplier)
  }

  addGems(amount) {
    this.gems += amount
  }
}
```

### 5. 任务系统 (QuestSystem.js)

```javascript
export class QuestSystem {
  constructor() {
    this.activeQuests = []
    this.completedQuests = []
    this.achievements = []
  }

  getAvailableQuests(playerLevel) {
    return QUESTS.filter(q =>
      q.requiredLevel <= playerLevel &&
      !this.completedQuests.includes(q.id)
    ).slice(0, 3)
  }

  checkProgress(questId, condition) {
    const quest = this.activeQuests.find(q => q.id === questId)
    if (!quest) return

    quest.progress++
    if (quest.progress >= quest.target) {
      this.completeQuest(questId)
    }
  }

  completeQuest(questId) {
    const quest = this.activeQuests.find(q => q.id === questId)
    if (!quest) return null

    this.activeQuests = this.activeQuests.filter(q => q.id !== questId)
    this.completedQuests.push(questId)

    return quest.rewards
  }
}
```

## 游戏玩法

1. **经营店铺**：升级店铺提升收入
2. **招聘员工**：雇佣员工提升效率
3. **数值成长**：通过经营获得经验值升级
4. **离线收益**：退出游戏后回来领取离线收益
5. **转生系统**：达到一定等级后重置获得永久加成
6. **完成任务**：完成任务获得额外奖励

## 扩展点

- 添加新店铺类型：在 `ShopConfig` 中定义
- 添加新员工类型：在 `EmployeeConfig` 中定义
- 添加新任务：在 `QuestConfig` 中定义
- 添加成就系统：创建 `AchievementManager`
- 添加排行榜：创建 `LeaderboardManager`
