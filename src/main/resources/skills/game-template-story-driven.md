---
name: game-template-story-driven
description: 剧情推进游戏模板 - 分支剧情、伏笔系统、角色关系、多结局
category: game-template
triggerPattern: story, 剧情, narrative, 叙事, branch, 分支, ending, 结局, foreshadow, 伏笔, visual novel, 视觉小说
---

# 剧情推进游戏模板

## 概述

完整的剧情推进游戏模板，着重设计剧情推进与伏笔系统，包含：
- **剧情引擎**：分支剧情、条件触发、剧情树
- **伏笔系统**：埋设伏笔、自动回收、伏笔揭示
- **角色系统**：好感度、信任值、关系网
- **选择系统**：关键选择、影响追踪、蝴蝶效应
- **多结局**：根据选择和伏笔触发不同结局
- **存档系统**：剧情回溯、分支存档

## 核心代码

### 1. 剧情引擎 (StoryEngine.js)

```javascript
export class StoryEngine {
  constructor() {
    this.currentScene = null
    this.storyFlags = new Map()      // 剧情标记
    this.foreshadows = new Map()     // 伏笔记录
    this.choices = []                // 选择历史
    this.variables = new Map()       // 剧情变量
  }

  // 加载剧情场景
  loadScene(sceneId) {
    const scene = STORY_SCENES[sceneId]
    if (!scene) return null

    // 检查前置条件
    if (scene.conditions && !this.checkConditions(scene.conditions)) {
      return this.getAlternativeScene(scene)
    }

    // 处理伏笔
    if (scene.foreshadow) {
      this埋设伏笔(scene.foreshadow)
    }

    this.currentScene = scene
    return scene
  }

  // 检查条件
  checkConditions(conditions) {
    return conditions.every(condition => {
      const value = this.storyFlags.get(condition.flag)
      switch (condition.operator) {
        case '==': return value === condition.value
        case '!=': return value !== condition.value
        case '>': return value > condition.value
        case '<': return value < condition.value
        case '>=': return value >= condition.value
        case '<=': return value <= condition.value
        case 'contains': return Array.isArray(value) && value.includes(condition.value)
        default: return false
      }
    })
  }

  // 埋设伏笔
  埋设伏笔(foreshadow) {
    this.foreshadows.set(foreshadow.id, {
      id: foreshadow.id,
      description: foreshadow.description,
      埋设时间: Date.now(),
      揭示条件: foreshadow.revealCondition,
      已揭示: false
    })
  }

  // 检查伏笔揭示
  checkForeshadowReveals() {
    const revealed = []
    this.foreshadows.forEach((foreshadow, id) => {
      if (!foreshadow.已揭示 && this.checkConditions(foreshadow.揭示条件)) {
        foreshadow.已揭示 = true
        revealed.push(foreshadow)
      }
    })
    return revealed
  }

  // 做出选择
  makeChoice(choiceId, optionId) {
    const choice = this.currentScene.choices.find(c => c.id === choiceId)
    if (!choice) return null

    const option = choice.options.find(o => o.id === optionId)
    if (!option) return null

    // 记录选择
    this.choices.push({
      sceneId: this.currentScene.id,
      choiceId,
      optionId,
      timestamp: Date.now()
    })

    // 应用效果
    if (option.effects) {
      this.applyEffects(option.effects)
    }

    // 设置剧情标记
    if (option.flag) {
      this.storyFlags.set(option.flag.key, option.flag.value)
    }

    // 返回下一个场景
    return option.nextScene
  }

  // 应用效果
  applyEffects(effects) {
    effects.forEach(effect => {
      switch (effect.type) {
        case 'flag':
          this.storyFlags.set(effect.key, effect.value)
          break
        case 'variable':
          const current = this.variables.get(effect.key) || 0
          this.variables.set(effect.key, current + effect.value)
          break
        case 'foreshadow':
          this.埋设伏笔(effect)
          break
      }
    })
  }
}
```

### 2. 伏笔系统 (ForeshadowSystem.js)

```javascript
export class ForeshadowSystem {
  constructor() {
    this.伏笔列表 = new Map()
    this.伏笔链 = []  // 伏笔之间的关联
  }

  // 埋设伏笔
  埋设伏笔(config) {
    const 伏笔 = {
      id: config.id,
      名称: config.name,
      描述: config.description,
      类型: config.type,  // '线索', '暗示', '铺垫', '悬念'
      埋设场景: config.scene,
      埋设时间: Date.now(),
      揭示条件: config.revealCondition,
      揭示场景: null,
      已揭示: false,
      重要程度: config.importance || 'normal'  // 'low', 'normal', 'high', 'critical'
    }

    this.伏笔列表.set(伏笔.id, 伏笔)

    // 记录伏笔链
    if (config.relatedTo) {
      this.伏笔链.push({
        源: config.id,
        目标: config.relatedTo,
        关系: config.relation || '相关'
      })
    }

    return 伏笔
  }

  // 检查伏笔揭示
  checkReveals(storyFlags) {
    const revealed = []

    this.伏笔列表.forEach((伏笔, id) => {
      if (伏笔.已揭示) return

      const shouldReveal = this.evaluateCondition(伏笔.揭示条件, storyFlags)
      if (shouldReveal) {
        伏笔.已揭示 = true
        伏笔.揭示时间 = Date.now()
        revealed.push(伏笔)

        // 检查关联伏笔
        const related = this.getRelatedForeshadows(id)
        related.forEach(r => {
          if (!r.已揭示 && this.canAutoReveal(r, storyFlags)) {
            r.已揭示 = true
            r.揭示时间 = Date.now()
            revealed.push(r)
          }
        })
      }
    })

    return revealed
  }

  // 获取伏笔摘要
  getSummary() {
    const total = this.伏笔列表.size
    const revealed = Array.from(this.伏笔列表.values()).filter(f => f.已揭示).length
    const pending = total - revealed

    return {
      总数: total,
      已揭示: revealed,
      待揭示: pending,
      揭示率: total > 0 ? (revealed / total * 100).toFixed(1) + '%' : '0%'
    }
  }

  // 获取伏笔链
  getForeshadowChain(id) {
    const chain = []
    let current = id

    while (current) {
      const 伏笔 = this.伏笔列表.get(current)
      if (!伏笔) break

      chain.push(伏笔)
      const link = this.伏笔链.find(l => l.目标 === current)
      current = link ? link.源 : null
    }

    return chain.reverse()
  }
}
```

### 3. 角色关系系统 (RelationshipSystem.js)

```javascript
export class RelationshipSystem {
  constructor() {
    this.characters = new Map()
    this.relationships = new Map()  // characterA-characterB -> relationship
  }

  addCharacter(config) {
    this.characters.set(config.id, {
      id: config.id,
      name: config.name,
      description: config.description,
      personality: config.personality || [],
      好感度: 50,
      信任值: 50,
      背景故事: config.backstory || ''
    })
  }

  // 更新好感度
  updateAffinity(characterId, delta, reason) {
    const character = this.characters.get(characterId)
    if (!character) return

    const oldValue = character.好感度
    character.好感度 = Math.max(0, Math.min(100, character.好感度 + delta))

    return {
      characterId,
      oldValue,
      newValue: character.好感度,
      delta,
      reason
    }
  }

  // 更新信任值
  updateTrust(characterId, delta, reason) {
    const character = this.characters.get(characterId)
    if (!character) return

    const oldValue = character.信任值
    character.信任值 = Math.max(0, Math.min(100, character.信任值 + delta))

    return {
      characterId,
      oldValue,
      newValue: character.信任值,
      delta,
      reason
    }
  }

  // 获取关系状态
  getRelationshipStatus(charA, charB) {
    const key = `${charA}-${charB}`
    const relationship = this.relationships.get(key)

    if (!relationship) return '陌生人'

    const affinity = relationship.好感度
    const trust = relationship.信任值

    if (affinity >= 80 && trust >= 80) return '挚友'
    if (affinity >= 60 && trust >= 60) return '好友'
    if (affinity >= 40 && trust >= 40) return '熟人'
    if (affinity < 20 && trust < 20) return '敌对'
    return '普通'
  }

  // 触发关系事件
  triggerRelationshipEvent(charA, charB, event) {
    const key = `${charA}-${charB}`
    let relationship = this.relationships.get(key)

    if (!relationship) {
      relationship = { 好感度: 50, 信任值: 50, 事件历史: [] }
      this.relationships.set(key, relationship)
    }

    relationship.事件历史.push({
      时间: Date.now(),
      事件: event,
      影响: event.effects
    })

    // 应用效果
    if (event.effects) {
      if (event.effects.好感度) relationship.好感度 += event.effects.好感度
      if (event.effects.信任值) relationship.信任值 += event.effects.信任值
    }
  }
}
```

### 4. 剧情场景数据 (storyScenes.js)

```javascript
export const STORY_SCENES = {
  '序章_开始': {
    id: '序章_开始',
    title: '序章：命运的开始',
    剧情: [
      { speaker: '旁白', text: '在一个平静的早晨，你收到了一封神秘的信件...' },
      { speaker: '旁白', text: '信中写着："你被选中了，今晚午夜，老地方见。"' },
      { speaker: '主角', text: '这是什么？谁寄来的？' }
    ],
    伏笔: {
      id: '神秘信件',
      name: '神秘信件',
      description: '一封来源不明的信件，暗示着某种命运',
      type: '悬念',
      revealCondition: { flag: '知道信件来源', value: true }
    },
    choices: [
      {
        id: '序章_选择1',
        question: '你决定怎么做？',
        options: [
          {
            id: '赴约',
            text: '赴约 - 去老地方看看',
            effects: [{ type: 'flag', key: '选择赴约', value: true }],
            nextScene: '序章_赴约'
          },
          {
            id: '忽略',
            text: '忽略 - 当作恶作剧',
            effects: [{ type: 'flag', key: '选择忽略', value: true }],
            nextScene: '序章_忽略'
          }
        ]
      }
    ]
  },

  '序章_赴约': {
    id: '序章_赴约',
    title: '序章：神秘的会面',
    剧情: [
      { speaker: '旁白', text: '你来到了老地方，一个废弃的仓库...' },
      { speaker: '神秘人', text: '你来了，我等你很久了。' },
      { speaker: '主角', text: '你是谁？这封信是你寄的？' },
      { speaker: '神秘人', text: '我是谁不重要，重要的是你将要面对的...' }
    ],
    伏笔: {
      id: '神秘人身份',
      name: '神秘人身份',
      description: '神秘人似乎知道很多关于你的事情',
      type: '铺垫',
      revealCondition: { flag: '知道神秘人身份', value: true }
    },
    人物影响: [
      { characterId: '神秘人', 好感度: +10, 原因: '选择赴约' }
    ],
    choices: [
      {
        id: '序章_选择2',
        question: '你如何回应？',
        options: [
          {
            id: '询问',
            text: '询问 - 你到底是谁？',
            effects: [
              { type: 'flag', key: '主动询问', value: true },
              { type: 'foreshadow', id: '询问伏笔', description: '你的追问让神秘人更加警惕' }
            ],
            nextScene: '序章_询问'
          },
          {
            id: '倾听',
            text: '倾听 - 让他继续说',
            effects: [{ type: 'flag', key: '选择倾听', value: true }],
            nextScene: '序章_倾听'
          }
        ]
      }
    ]
  }
}
```

## 剧情设计要点

### 伏笔设计原则

| 类型 | 说明 | 示例 |
|------|------|------|
| **线索型** | 直接指向真相的证据 | 一张照片、一段对话 |
| **暗示型** | 间接暗示未来事件 | 角色的一句话、环境描写 |
| **铺垫型** | 为后续剧情做铺垫 | 角色的背景故事、历史事件 |
| **悬念型** | 制造悬念引发好奇 | 神秘信件、失踪事件 |

### 伏笔回收策略

1. **自动回收**：当条件满足时自动揭示
2. **手动回收**：玩家主动触发揭示
3. **渐进回收**：分多次逐步揭示
4. **反转回收**：揭示时带来意想不到的反转

### 选择影响追踪

```javascript
// 选择影响记录
const choiceImpact = {
  '选择赴约': {
    直接影响: '进入主线剧情',
    间接影响: '增加神秘人好感度',
    长期影响: '解锁隐藏结局'
  },
  '选择忽略': {
    直接影响: '错过主线剧情',
    间接影响: '触发平行剧情',
    长期影响: '只能达成普通结局'
  }
}
```

## 游戏玩法

1. **阅读剧情**：跟随故事发展
2. **做出选择**：在关键节点做出决定
3. **收集线索**：发现并记住伏笔
4. **推进剧情**：解锁新场景和角色
5. **揭示伏笔**：条件满足时伏笔自动揭示
6. **达成结局**：根据选择和伏笔触发不同结局

## 扩展点

- 添加新场景：在 `STORY_SCENES` 中定义
- 添加新角色：在 `CharacterConfig` 中定义
- 添加新伏笔类型：在 `ForeshadowSystem` 中扩展
- 添加成就系统：创建 `AchievementManager`
- 添加剧情回溯：创建 `StoryTimeline`
