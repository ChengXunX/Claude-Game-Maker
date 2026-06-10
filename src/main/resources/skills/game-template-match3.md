---
name: 三消游戏开发模板
description: 三消游戏开发模板，适用于消除类、三消类益智游戏
trigger: 三消游戏,消除游戏,match 3,宝石消除,开心消消乐
examples: Candy Crush|Bejeweled|开心消消乐|Puzzle Quest|Homescapes
---

# 三消游戏开发模板

## 游戏设计核心原则

### 核心循环（30秒一轮）
```
观察棋盘 → 选择交换 → 触发消除 → 看反馈 → 继续观察
```
- 每次消除必须有**即时反馈**（粒子效果 + 分数弹出 + 音效）
- 消除后棋盘自动下落+填充，形成**连锁反应**的期待感
- 玩家的决策时间应在 3-10 秒之间，太快无趣，太慢焦虑

### 玩家心理学
- **损失厌恶**：步数/时间限制让玩家珍惜每一步
- **随机奖励**：偶尔出现的特殊消除比固定奖励更让人上瘾
- **进度可见**：分数、星级、进度条让玩家知道"快成功了"
- **社交比较**：排行榜、好友分数激发竞争欲

### 难度曲线设计
```
关卡1-5:   教程关，每关只教一个概念（交换、消除、特殊方块）
关卡6-15:  入门关，简单目标，步数充裕
关卡16-30: 进阶关，引入障碍物，步数适中
关卡31-50: 挑战关，复杂目标，步数紧张
关卡51+:   精英关，需要策略和运气
```

## 核心系统设计

### 1. 棋盘系统
```
Board {
  width: 8
  height: 8
  grid: [
    [gem1, gem2, gem3, ...],
    [gem4, gem5, gem6, ...],
    ...
  ]
}
```

### 2. 宝石类型
| 类型 | 颜色 | 特殊效果 | 获取方式 |
|------|------|----------|----------|
| 普通 | 6种颜色 | 无 | 基础 |
| 炸弹 | 特殊 | 消除3×3范围 | 4个同色L形消除 |
| 闪电 | 特殊 | 消除整行/列 | 5个同色直线消除 |
| 彩虹 | 特殊 | 消除同色全部 | 7个以上同色消除 |

### 3. 匹配检测算法
```javascript
function findMatches(board) {
  const matches = [];
  
  // 横向检测
  for (let y = 0; y < board.height; y++) {
    for (let x = 0; x < board.width - 2; x++) {
      if (board[y][x] === board[y][x+1] && board[y][x] === board[y][x+2]) {
        // 找到连续3个相同颜色，继续向右延伸
        let length = 3;
        while (x + length < board.width && board[y][x] === board[y][x+length]) {
          length++;
        }
        matches.push({type: 'horizontal', x, y, length});
        x += length - 1; // 跳过已检测的部分
      }
    }
  }
  
  // 纵向检测（同理）
  for (let x = 0; x < board.width; x++) {
    for (let y = 0; y < board.height - 2; y++) {
      if (board[y][x] === board[y+1][x] && board[y][x] === board[y+2][x]) {
        let length = 3;
        while (y + length < board.height && board[y][x] === board[y+length][x]) {
          length++;
        }
        matches.push({type: 'vertical', x, y, length});
        y += length - 1;
      }
    }
  }
  
  return matches;
}
```

### 4. 特殊方块生成规则
```javascript
function createSpecialGem(match) {
  if (match.length >= 7) {
    return new RainbowGem(); // 彩虹：消除同色全部
  } else if (match.length === 5) {
    return new LightningGem(match.type); // 闪电：消除整行/列
  } else if (match.length === 4) {
    return new BombGem(); // 炸弹：3×3范围
  }
  return null; // 3个匹配不生成特殊方块
}
```

## 关键技术实现

### 交换动画
```javascript
async function swapGems(gem1, gem2) {
  // 1. 检查是否相邻
  if (!isAdjacent(gem1, gem2)) return false;
  
  // 2. 执行交换动画（200ms）
  await animateSwap(gem1, gem2);
  
  // 3. 检查是否有匹配
  const matches = findMatches(board);
  if (matches.length === 0) {
    // 无匹配，交换回来（带抖动提示）
    await animateSwap(gem2, gem1);
    shakeBoard(); // 轻微抖动告诉玩家"这个交换无效"
    return false;
  }
  
  // 4. 处理消除（带连锁）
  await processMatches(matches);
  return true;
}
```

### 重力填充（带延迟效果）
```javascript
async function applyGravity() {
  for (let x = 0; x < board.width; x++) {
    let emptyY = board.height - 1;
    
    for (let y = board.height - 1; y >= 0; y--) {
      if (board[y][x] !== null) {
        if (y !== emptyY) {
          board[emptyY][x] = board[y][x];
          board[y][x] = null;
          // 延迟动画：越远的方块下落越慢，形成视觉层次
          const delay = (emptyY - y) * 30;
          await animateFall(board[emptyY][x], y, emptyY, delay);
        }
        emptyY--;
      }
    }
    
    // 填充新宝石（从顶部掉入）
    for (let y = emptyY; y >= 0; y--) {
      board[y][x] = createRandomGem();
      await animateAppear(board[y][x], y);
    }
  }
}
```

### 连锁消除（核心乐趣来源）
```javascript
async function processMatches(matches) {
  let combo = 0;
  
  while (matches.length > 0) {
    combo++;
    
    // 1. 消除匹配的方块（带爆炸效果）
    for (const match of matches) {
      await explodeGems(match, combo);
    }
    
    // 2. 显示 combo 数字（x2, x3, x4...）
    if (combo > 1) {
      showComboText(combo);
    }
    
    // 3. 重力下落
    await applyGravity();
    
    // 4. 检查新的匹配（连锁）
    matches = findMatches(board);
  }
  
  // 5. 连锁结束，显示总分
  showTotalScore();
}
```

## 关卡设计

### 目标类型
| 目标类型 | 说明 | 示例 |
|----------|------|------|
| 分数目标 | 达到指定分数 | "获得 10000 分" |
| 消除目标 | 消除指定数量的特定颜色 | "消除 30 个红色方块" |
| 障碍物目标 | 清除所有障碍物 | "清除所有冰块" |
| 收集目标 | 收集指定物品 | "收集 3 个钥匙" |
| 限时目标 | 在时间内完成 | "60 秒内获得 5000 分" |

### 障碍物设计
| 障碍物 | 说明 | 消除方式 |
|--------|------|----------|
| 冰块 | 包裹方块，不能移动 | 消除冰块内的方块 |
| 铁链 | 锁住方块，不能交换 | 消除铁链上的方块 |
| 石头 | 不可消除的障碍 | 消除旁边的方块震碎 |
| 蜂蜜 | 蔓延障碍 | 消除蜂蜜旁的方块 |
| 巧克力 | 每步自动生长 | 消除巧克力旁的方块 |

### 道具设计
| 道具 | 效果 | 获取方式 |
|------|------|----------|
| 锤子 | 消除单个方块 | 关卡奖励/购买 |
| 刷新 | 随机打乱棋盘 | 关卡奖励/购买 |
| 交换 | 任意交换两个方块 | 关卡奖励/购买 |
| +5步 | 增加5步 | 购买 |
| 彩虹炸弹 | 开局自带彩虹方块 | 购买 |

## 变现设计

### 免费玩家体验
- 每天 5 条生命，每 30 分钟恢复 1 条
- 每关免费获得 1 个道具
- 看广告获得额外生命或道具

### 付费点
| 付费项 | 价格 | 说明 |
|--------|------|------|
| 去广告 | ¥12 | 永久去广告 |
| 生命包 | ¥6 | 立即获得 5 条生命 |
| 道具包 | ¥12 | 获得 5 个随机道具 |
| 金币包 | ¥30 | 获得 1000 金币 |

## 迭代策略

### 第一版：最小可玩版本
- 8×8 棋盘，6 种颜色
- 基础消除（3 个匹配）
- 重力下落
- 简单计分
- 3 个教程关

### 第二版：核心玩法
- 添加特殊方块（炸弹、闪电、彩虹）
- 添加 10 个关卡
- 添加目标系统（分数/消除目标）
- 添加基础音效

### 第三版：内容扩展
- 添加障碍物（冰块、铁链）
- 添加道具系统
- 添加 20 个关卡
- 添加星级评价

### 第四版：社交和变现
- 添加排行榜
- 添加生命系统
- 添加道具商店
- 添加每日挑战

### 第五版：打磨
- 优化动画流畅度
- 添加粒子效果
- 优化音效
- 平衡关卡难度

## 常见错误

1. **没有连锁消除**：连锁是三消的核心乐趣，必须实现
2. **消除反馈太弱**：必须有视觉+音效+震动（移动端）
3. **关卡太难**：前 10 关必须让玩家轻松通关
4. **没有随机性**：完全确定的消除会失去乐趣
5. **步数太紧**：让玩家觉得"差一点就成功"比"完全不可能"更好
