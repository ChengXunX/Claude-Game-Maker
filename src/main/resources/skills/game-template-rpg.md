---
name: game-template-rpg
description: RPG游戏模板 - 提供完整的RPG游戏项目骨架
category: game-template
triggerPattern: RPG, role-playing, 角色扮演, JRPG, adventure
---

# RPG 游戏模板

## 概述

这是一个完整的 RPG 游戏模板，基于 Phaser 3 引擎。包含：
- 角色系统（属性、装备、技能）
- 战斗系统（回合制战斗）
- 对话系统（NPC 对话）
- 任务系统（主线/支线任务）
- 背包系统（物品管理）
- 地图系统（世界地图、城镇、迷宫）

## 项目结构

```
game-project/
├── index.html
├── package.json
├── vite.config.js
├── src/
│   ├── main.js
│   ├── config.js
│   ├── scenes/
│   │   ├── BootScene.js
│   │   ├── MenuScene.js
│   │   ├── WorldScene.js      # 世界地图
│   │   ├── BattleScene.js     # 战斗场景
│   │   ├── DialogScene.js     # 对话场景
│   │   └── InventoryScene.js  # 背包场景
│   ├── entities/
│   │   ├── Character.js       # 角色基类
│   │   ├── Player.js          # 玩家角色
│   │   ├── NPC.js             # NPC
│   │   └── Monster.js         # 怪物
│   ├── systems/
│   │   ├── BattleSystem.js    # 战斗系统
│   │   ├── QuestSystem.js     # 任务系统
│   │   ├── DialogSystem.js    # 对话系统
│   │   └── InventorySystem.js # 背包系统
│   ├── data/
│   │   ├── items.js           # 物品数据
│   │   ├── skills.js          # 技能数据
│   │   ├── monsters.js        # 怪物数据
│   │   └── quests.js          # 任务数据
│   └── ui/
│       ├── HUD.js
│       ├── Menu.js
│       └── DialogBox.js
└── assets/
    ├── images/
    ├── sounds/
    └── data/
```

## 核心代码模板

### 1. 角色系统 (Character.js)

```javascript
export class Character {
  constructor(config) {
    this.name = config.name
    this.level = config.level || 1
    this.exp = 0
    this.expToNextLevel = 100

    // 基础属性
    this.stats = {
      hp: config.hp || 100,
      maxHp: config.hp || 100,
      mp: config.mp || 50,
      maxMp: config.mp || 50,
      attack: config.attack || 10,
      defense: config.defense || 5,
      speed: config.speed || 10,
      magic: config.magic || 8
    }

    // 装备
    this.equipment = {
      weapon: null,
      armor: null,
      accessory: null
    }

    // 技能
    this.skills = config.skills || []

    // 状态效果
    this.statusEffects = []
  }

  // 获取实际属性（包含装备加成）
  getActualStat(statName) {
    let base = this.stats[statName]

    // 装备加成
    Object.values(this.equipment).forEach(item => {
      if (item && item.bonuses && item.bonuses[statName]) {
        base += item.bonuses[statName]
      }
    })

    // 状态效果
    this.statusEffects.forEach(effect => {
      if (effect.stat === statName) {
        base += effect.value
      }
    })

    return Math.max(0, base)
  }

  // 获得经验值
  gainExp(amount) {
    this.exp += amount
    while (this.exp >= this.expToNextLevel) {
      this.levelUp()
    }
  }

  // 升级
  levelUp() {
    this.level++
    this.exp -= this.expToNextLevel
    this.expToNextLevel = Math.floor(this.expToNextLevel * 1.5)

    // 提升属性
    this.stats.maxHp += 10
    this.stats.hp = this.stats.maxHp
    this.stats.maxMp += 5
    this.stats.mp = this.stats.maxMp
    this.stats.attack += 2
    this.stats.defense += 1
    this.stats.speed += 1
    this.stats.magic += 2

    console.log(`${this.name} 升级到 ${this.level} 级！`)
  }

  // 装备物品
  equip(item) {
    if (!item.equipSlot) return false

    // 卸下旧装备
    const oldItem = this.equipment[item.equipSlot]
    if (oldItem) {
      this.unequip(item.equipSlot)
    }

    // 装备新物品
    this.equipment[item.equipSlot] = item
    return true
  }

  // 卸下装备
  unequip(slot) {
    const item = this.equipment[slot]
    if (item) {
      this.equipment[slot] = null
      return item
    }
    return null
  }

  // 使用技能
  useSkill(skillIndex, target) {
    const skill = this.skills[skillIndex]
    if (!skill) return false

    // 检查 MP
    if (this.stats.mp < skill.mpCost) {
      console.log('MP 不足！')
      return false
    }

    // 消耗 MP
    this.stats.mp -= skill.mpCost

    // 执行技能效果
    return skill.execute(this, target)
  }

  // 受到伤害
  takeDamage(damage) {
    const actualDamage = Math.max(1, damage - this.getActualStat('defense'))
    this.stats.hp = Math.max(0, this.stats.hp - actualDamage)
    return actualDamage
  }

  // 恢复 HP
  heal(amount) {
    this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + amount)
  }

  // 恢复 MP
  restoreMp(amount) {
    this.stats.mp = Math.min(this.stats.maxMp, this.stats.mp + amount)
  }

  // 检查是否存活
  isAlive() {
    return this.stats.hp > 0
  }
}
```

### 2. 战斗系统 (BattleSystem.js)

```javascript
export class BattleSystem {
  constructor(scene) {
    this.scene = scene
    this.playerParty = []
    this.enemyParty = []
    this.turnOrder = []
    this.currentTurnIndex = 0
    this.isBattleActive = false
  }

  // 开始战斗
  startBattle(playerParty, enemyParty) {
    this.playerParty = playerParty
    this.enemyParty = enemyParty
    this.isBattleActive = true

    // 计算回合顺序（按速度排序）
    this.calculateTurnOrder()

    // 开始第一个回合
    this.nextTurn()
  }

  // 计算回合顺序
  calculateTurnOrder() {
    const allCharacters = [...this.playerParty, ...this.enemyParty]
    this.turnOrder = allCharacters.sort((a, b) => {
      return b.getActualStat('speed') - a.getActualStat('speed')
    })
    this.currentTurnIndex = 0
  }

  // 下一个回合
  nextTurn() {
    if (!this.isBattleActive) return

    // 检查战斗结束
    if (this.checkBattleEnd()) {
      this.endBattle()
      return
    }

    // 获取当前角色
    const currentCharacter = this.turnOrder[this.currentTurnIndex]

    // 跳过已死亡的角色
    if (!currentCharacter.isAlive()) {
      this.currentTurnIndex = (this.currentTurnIndex + 1) % this.turnOrder.length
      this.nextTurn()
      return
    }

    // 根据角色类型决定行动
    if (this.playerParty.includes(currentCharacter)) {
      // 玩家角色 - 显示操作菜单
      this.showPlayerMenu(currentCharacter)
    } else {
      // 敌人 - AI 决策
      this.executeEnemyAI(currentCharacter)
    }
  }

  // 显示玩家操作菜单
  showPlayerMenu(character) {
    this.scene.scene.launch('BattleMenu', {
      character: character,
      onAttack: () => this.playerAttack(character),
      onSkill: (skillIndex) => this.playerUseSkill(character, skillIndex),
      onItem: (item) => this.playerUseItem(character, item),
      onDefend: () => this.playerDefend(character)
    })
  }

  // 玩家普通攻击
  playerAttack(character) {
    const target = this.selectRandomEnemy()
    if (!target) return

    const damage = character.getActualStat('attack')
    const actualDamage = target.takeDamage(damage)

    this.showDamageEffect(target, actualDamage)
    this.showBattleLog(`${character.name} 攻击了 ${target.name}，造成 ${actualDamage} 点伤害`)

    this.endTurn()
  }

  // 玩家使用技能
  playerUseSkill(character, skillIndex) {
    const skill = character.skills[skillIndex]
    if (!skill) return

    const target = skill.targetType === 'enemy' ? this.selectRandomEnemy() : character
    const result = character.useSkill(skillIndex, target)

    if (result) {
      this.showBattleLog(`${character.name} 使用了 ${skill.name}`)
      if (result.damage) {
        this.showDamageEffect(target, result.damage)
      }
      if (result.heal) {
        this.showHealEffect(target, result.heal)
      }
    }

    this.endTurn()
  }

  // 敌人 AI
  executeEnemyAI(character) {
    // 简单 AI：随机攻击一个活着的玩家角色
    const alivePlayers = this.playerParty.filter(p => p.isAlive())
    if (alivePlayers.length === 0) return

    const target = alivePlayers[Math.floor(Math.random() * alivePlayers.length)]
    const damage = character.getActualStat('attack')
    const actualDamage = target.takeDamage(damage)

    this.showDamageEffect(target, actualDamage)
    this.showBattleLog(`${character.name} 攻击了 ${target.name}，造成 ${actualDamage} 点伤害`)

    this.scene.time.delayedCall(1000, () => {
      this.endTurn()
    })
  }

  // 结束回合
  endTurn() {
    this.currentTurnIndex = (this.currentTurnIndex + 1) % this.turnOrder.length

    // 处理状态效果
    this.processStatusEffects()

    // 下一回合
    this.scene.time.delayedCall(500, () => {
      this.nextTurn()
    })
  }

  // 检查战斗结束
  checkBattleEnd() {
    const allPlayersDead = this.playerParty.every(p => !p.isAlive())
    const allEnemiesDead = this.enemyParty.every(e => !e.isAlive())
    return allPlayersDead || allEnemiesDead
  }

  // 结束战斗
  endBattle() {
    this.isBattleActive = false

    const playerWon = this.enemyParty.every(e => !e.isAlive())

    if (playerWon) {
      // 计算奖励
      const expReward = this.calculateExpReward()
      const goldReward = this.calculateGoldReward()

      this.playerParty.forEach(character => {
        character.gainExp(expReward)
      })

      this.showBattleLog(`战斗胜利！获得 ${expReward} 经验值和 ${goldReward} 金币`)
      this.scene.events.emit('battleWon', { exp: expReward, gold: goldReward })
    } else {
      this.showBattleLog('战斗失败...')
      this.scene.events.emit('battleLost')
    }
  }

  // 显示伤害效果
  showDamageEffect(target, damage) {
    const text = this.scene.add.text(target.x, target.y - 20, `-${damage}`, {
      fontSize: '24px',
      fontFamily: 'Arial',
      color: '#ff0000',
      fontStyle: 'bold'
    })

    this.scene.tweens.add({
      targets: text,
      y: target.y - 60,
      alpha: 0,
      duration: 1000,
      onComplete: () => text.destroy()
    })
  }
}
```

### 3. 对话系统 (DialogSystem.js)

```javascript
export class DialogSystem {
  constructor(scene) {
    this.scene = scene
    this.isActive = false
    this.currentDialog = null
    this.currentLine = 0
    this.onComplete = null
  }

  // 开始对话
  startDialog(dialogData, onComplete) {
    this.currentDialog = dialogData
    this.currentLine = 0
    this.isActive = true
    this.onComplete = onComplete

    this.showDialogBox()
    this.showCurrentLine()
  }

  // 显示对话框
  showDialogBox() {
    // 创建对话框背景
    this.dialogBox = this.scene.add.rectangle(400, 500, 700, 150, 0x000000, 0.8)
    this.dialogBox.setOrigin(0.5)
    this.dialogBox.setDepth(100)

    // 创建角色名
    this.nameText = this.scene.add.text(80, 440, '', {
      fontSize: '18px',
      fontFamily: 'Arial',
      color: '#ffff00',
      fontStyle: 'bold'
    })
    this.nameText.setDepth(101)

    // 创建对话文本
    this.dialogText = this.scene.add.text(80, 470, '', {
      fontSize: '16px',
      fontFamily: 'Arial',
      color: '#ffffff',
      wordWrap: { width: 650 }
    })
    this.dialogText.setDepth(101)

    // 创建继续提示
    this.continueText = this.scene.add.text(650, 530, '点击继续...', {
      fontSize: '14px',
      fontFamily: 'Arial',
      color: '#aaaaaa'
    })
    this.continueText.setDepth(101)

    // 点击继续
    this.scene.input.on('pointerdown', this.advanceDialog, this)
  }

  // 显示当前行
  showCurrentLine() {
    const line = this.currentDialog.lines[this.currentLine]
    if (!line) {
      this.endDialog()
      return
    }

    this.nameText.setText(line.speaker || '')
    this.typewriterEffect(line.text)
  }

  // 打字机效果
  typewriterEffect(text) {
    this.dialogText.setText('')
    let index = 0

    this.scene.time.addEvent({
      delay: 30,
      callback: () => {
        this.dialogText.setText(text.substring(0, index + 1))
        index++
        if (index >= text.length) {
          this.showChoices()
        }
      },
      repeat: text.length - 1
    })
  }

  // 显示选项
  showChoices() {
    const line = this.currentDialog.lines[this.currentLine]
    if (!line.choices || line.choices.length === 0) {
      this.continueText.setVisible(true)
      return
    }

    this.continueText.setVisible(false)

    line.choices.forEach((choice, index) => {
      const choiceText = this.scene.add.text(100, 530 + index * 30, `${index + 1}. ${choice.text}`, {
        fontSize: '16px',
        fontFamily: 'Arial',
        color: '#ffffff'
      })
      choiceText.setDepth(101)
      choiceText.setInteractive()

      choiceText.on('pointerdown', () => {
        this.handleChoice(choice)
      })
    })
  }

  // 处理选项
  handleChoice(choice) {
    if (choice.action) {
      choice.action()
    }

    if (choice.nextDialog) {
      this.currentDialog = choice.nextDialog
      this.currentLine = 0
      this.showCurrentLine()
    } else {
      this.advanceDialog()
    }
  }

  // 推进对话
  advanceDialog() {
    if (!this.isActive) return

    this.currentLine++
    this.showCurrentLine()
  }

  // 结束对话
  endDialog() {
    this.isActive = false

    // 移除对话框
    if (this.dialogBox) this.dialogBox.destroy()
    if (this.nameText) this.nameText.destroy()
    if (this.dialogText) this.dialogText.destroy()
    if (this.continueText) this.continueText.destroy()

    // 移除点击监听
    this.scene.input.off('pointerdown', this.advanceDialog, this)

    // 回调
    if (this.onComplete) {
      this.onComplete()
    }
  }
}
```

## 使用方法

1. 使用此模板创建新项目
2. 在 `data/` 目录中定义物品、技能、怪物、任务数据
3. 在 `assets/images/` 中添加角色和场景图片
4. 在 `assets/sounds/` 中添加音效和音乐
5. 运行 `npm run dev` 预览游戏

## 扩展点

- 添加新职业：在 `Character` 类中添加职业系统
- 添加新技能：在 `data/skills.js` 中定义
- 添加新怪物：在 `data/monsters.js` 中定义
- 添加新任务：在 `data/quests.js` 中定义
- 添加存档系统：创建 `SaveManager`
