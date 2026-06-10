---
name: AI NPC对话冒险游戏开发模板
description: AI NPC对话冒险游戏开发模板，适用于AI驱动NPC对话、动态剧情类游戏
trigger: AI NPC, 对话, 冒险, 角色扮演, 剧情, NPC, AI对话, dynamic dialogue
examples: AI Dungeon|Character.AI|ChatGPT游戏|AI角色扮演
---

# AI NPC 对话冒险游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
与 NPC 对话 → 获得信息 → 做出选择 → 推进剧情 → 与新 NPC 对话
```
- **自由对话**：玩家可以自由输入
- **动态剧情**：剧情根据对话生成
- **角色扮演**：玩家扮演特定角色

### 玩家心理学
- **"自由对话"的沉浸感**：和 NPC 自由对话
- **"剧情分支"的选择感**：每个选择都有影响
- **"角色扮演"的代入感**：觉得自己就是主角
- **"探索世界"的好奇心**：发现新 NPC、新剧情

### AI NPC 设计要点
```
AI NPC 核心：
1. 独立性格：每个 NPC 有独特性格
2. 记忆系统：NPC 记住之前的对话
3. 关系系统：NPC 之间有关系
4. 动态反应：NPC 根据玩家行为反应
```

## 核心系统设计

### 1. AI NPC 系统
```javascript
class AINPC {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.personality = config.personality;
    this.background = config.background;
    this.memory = [];
    this.relationships = {};
    this.currentMood = 'neutral';
  }
  
  async chat(playerMessage) {
    // 构建上下文
    const context = this.buildContext(playerMessage);
    
    // 调用 AI 生成回复
    const response = await this.generateResponse(context);
    
    // 记录对话
    this.memory.push({
      player: playerMessage,
      npc: response.text,
      timestamp: Date.now()
    });
    
    // 更新关系
    this.updateRelationship(response.relationshipChange);
    
    // 更新心情
    this.currentMood = response.mood;
    
    return response;
  }
  
  buildContext(playerMessage) {
    return {
      personality: this.personality,
      background: this.background,
      recentMemory: this.memory.slice(-10),
      currentMood: this.currentMood,
      relationships: this.relationships,
      playerMessage
    };
  }
  
  async generateResponse(context) {
    const prompt = this.buildPrompt(context);
    
    const response = await fetch('https://api.example.com/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt })
    });
    
    const result = await response.json();
    
    return {
      text: result.text,
      mood: result.mood,
      relationshipChange: result.relationshipChange,
      actions: result.actions
    };
  }
  
  buildPrompt(context) {
    return `你是一个名叫${this.name}的NPC。
性格：${this.personality}
背景：${this.background}
当前心情：${context.currentMood}

最近的对话：
${context.recentMemory.map(m => `玩家：${m.player}\n${this.name}：${m.npc}`).join('\n')}

玩家说：${context.playerMessage}

请用${this.personality}的语气回复，并返回JSON格式：
{
  "text": "你的回复",
  "mood": "你的心情",
  "relationshipChange": 0,
  "actions": []
}`;
  }
  
  updateRelationship(change) {
    // 更新与玩家的关系
    this.relationships.player = (this.relationships.player || 0) + change;
  }
}

const NPCS = {
  merchant: {
    name: '商人',
    personality: '精明、友善、健谈',
    background: '一个经验丰富的旅行商人，见过很多世面'
  },
  guard: {
    name: '守卫',
    personality: '严肃、忠诚、寡言',
    background: '城门守卫，对陌生人保持警惕'
  },
  wizard: {
    name: '巫师',
    personality: '神秘、智慧、古怪',
    background: '研究古代魔法的老巫师'
  }
};
```

### 2. 记忆系统
```javascript
class MemorySystem {
  constructor() {
    this.shortTerm = []; // 短期记忆（最近对话）
    this.longTerm = []; // 长期记忆（重要事件）
    this.entityMemory = {}; // 实体记忆（记住特定事物）
  }
  
  addShortTerm(memory) {
    this.shortTerm.push(memory);
    
    // 保持最近 20 条
    if (this.shortTerm.length > 20) {
      this.shortTerm.shift();
    }
  }
  
  addLongTerm(memory) {
    this.longTerm.push(memory);
  }
  
  rememberEntity(entityId, info) {
    this.entityMemory[entityId] = {
      ...this.entityMemory[entityId],
      ...info,
      lastSeen: Date.now()
    };
  }
  
  getEntityInfo(entityId) {
    return this.entityMemory[entityId];
  }
  
  getRelevantMemories(context) {
    // 根据上下文获取相关记忆
    const relevant = [];
    
    for (const memory of this.longTerm) {
      if (this.isRelevant(memory, context)) {
        relevant.push(memory);
      }
    }
    
    return relevant;
  }
  
  isRelevant(memory, context) {
    // 检查记忆是否与当前上下文相关
    return memory.keywords.some(k => context.includes(k));
  }
}
```

### 3. 关系系统
```javascript
class RelationshipSystem {
  constructor() {
    this.relationships = {};
  }
  
  addRelationship(npc1, npc2, type) {
    const key = this.getKey(npc1, npc2);
    this.relationships[key] = {
      type, // friend, enemy, neutral, family, lover
      strength: 50, // 0-100
      history: []
    };
  }
  
  getRelationship(npc1, npc2) {
    const key = this.getKey(npc1, npc2);
    return this.relationships[key];
  }
  
  changeRelationship(npc1, npc2, change, reason) {
    const key = this.getKey(npc1, npc2);
    const rel = this.relationships[key];
    
    if (rel) {
      rel.strength = Math.max(0, Math.min(100, rel.strength + change));
      rel.history.push({
        change,
        reason,
        timestamp: Date.now()
      });
    }
  }
  
  getKey(npc1, npc2) {
    return [npc1, npc2].sort().join('-');
  }
}
```

### 4. 动态剧情系统
```javascript
class DynamicStorySystem {
  constructor() {
    this.storyNodes = [];
    this.currentNode = null;
    this.flags = {};
  }
  
  async generateStoryNode(context) {
    const prompt = this.buildStoryPrompt(context);
    
    const response = await fetch('https://api.example.com/story', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt })
    });
    
    const result = await response.json();
    
    return {
      id: this.generateId(),
      text: result.text,
      choices: result.choices,
      consequences: result.consequences
    };
  }
  
  buildStoryPrompt(context) {
    return `你是一个故事生成器。

当前情况：
${context.situation}

玩家角色：${context.playerRole}

NPC 状态：
${context.npcs.map(npc => `${npc.name}：${npc.currentMood}`).join('\n')}

最近事件：
${context.recentEvents.join('\n')}

请生成下一个故事节点，包含：
1. 场景描述
2. NPC 反应
3. 玩家可选行动（2-4个选项）

返回JSON格式：
{
  "text": "场景描述",
  "choices": [
    {"id": "1", "text": "选项1"},
    {"id": "2", "text": "选项2"}
  ],
  "consequences": {
    "1": {"effect": "选项1的效果"},
    "2": {"effect": "选项2的效果"}
  }
}`;
  }
  
  makeChoice(choiceId) {
    const consequence = this.currentNode.consequences[choiceId];
    
    // 应用后果
    this.applyConsequence(consequence);
    
    // 设置标志
    if (consequence.setFlags) {
      Object.assign(this.flags, consequence.setFlags);
    }
    
    // 生成下一个节点
    return this.generateStoryNode(this.getContext());
  }
}
```

### 5. 对话 UI 系统
```javascript
class DialogueUI {
  constructor() {
    this.isActive = false;
    this.currentNPC = null;
    this.dialogueHistory = [];
  }
  
  startDialogue(npc) {
    this.isActive = true;
    this.currentNPC = npc;
    this.showDialogueBox();
    this.showNPCInfo(npc);
  }
  
  endDialogue() {
    this.isActive = false;
    this.currentNPC = null;
    this.hideDialogueBox();
  }
  
  addMessage(sender, text) {
    this.dialogueHistory.push({ sender, text, timestamp: Date.now() });
    this.displayMessage(sender, text);
  }
  
  async sendMessage(text) {
    this.addMessage('player', text);
    
    // 获取 NPC 回复
    const response = await this.currentNPC.chat(text);
    
    this.addMessage('npc', response.text);
    
    // 显示选项
    if (response.choices) {
      this.showChoices(response.choices);
    }
  }
  
  showChoices(choices) {
    const container = document.getElementById('choices');
    container.innerHTML = '';
    
    for (const choice of choices) {
      const button = document.createElement('button');
      button.textContent = choice.text;
      button.onclick = () => this.selectChoice(choice.id);
      container.appendChild(button);
    }
  }
}
```

## 迭代策略

### 第一版：基础对话
- 1 个 NPC
- 基础对话
- 简单 UI
- 本地存储

### 第二版：记忆系统
- 短期记忆
- 长期记忆
- 实体记忆
- 多个 NPC

### 第三版：关系系统
- NPC 关系
- 好感度系统
- 动态反应
- 分支剧情

### 第四版：深度玩法
- 10 个 NPC
- 复杂剧情
- 多结局
- 成就系统

### 第五版：社区功能
- NPC 分享
- 剧情分享
- 社区创作
- 排行榜

## 常见错误

1. **NPC 太蠢**：NPC 要有基本的智能
2. **没有记忆**：NPC 要记住之前的对话
3. **没有关系**：NPC 之间要有关系
4. **剧情太线性**：要有分支剧情
5. **没有代入感**：要让玩家有代入感
