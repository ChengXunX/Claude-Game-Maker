---
name: 三消游戏开发模板
description: 三消游戏开发模板，适用于消除类、三消类益智游戏
trigger: 三消游戏,消除游戏,match 3,宝石消除,开心消消乐
examples: Candy Crush|Bejeweled|开心消消乐|Puzzle Quest|Homescapes
---

# 三消游戏开发模板

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
| 类型 | 颜色 | 特殊效果 |
|------|------|----------|
| 普通 | 6种颜色 | 无 |
| 炸弹 | 特殊 | 消除3×3范围 |
| 闪电 | 特殊 | 消除整行/列 |
| 彩虹 | 特殊 | 消除同色全部 |

### 3. 匹配检测
```javascript
function findMatches(board) {
  const matches = [];
  
  // 横向检测
  for (let y = 0; y < board.height; y++) {
    for (let x = 0; x < board.width - 2; x++) {
      if (board[y][x] === board[y][x+1] && board[y][x] === board[y][x+2]) {
        matches.push({type: 'horizontal', x, y, length: 3});
      }
    }
  }
  
  // 纵向检测
  // ...
  
  return matches;
}
```

## 关键技术实现

### 交换动画
```javascript
async function swapGems(gem1, gem2) {
  // 1. 检查是否相邻
  if (!isAdjacent(gem1, gem2)) return false;
  
  // 2. 执行交换动画
  await animateSwap(gem1, gem2);
  
  // 3. 检查是否有匹配
  const matches = findMatches(board);
  if (matches.length === 0) {
    // 无匹配，交换回来
    await animateSwap(gem2, gem1);
    return false;
  }
  
  // 4. 处理消除
  await processMatches(matches);
  return true;
}
```

### 重力填充
```javascript
async function applyGravity() {
  for (let x = 0; x < board.width; x++) {
    let emptyY = board.height - 1;
    
    for (let y = board.height - 1; y >= 0; y--) {
      if (board[y][x] !== null) {
        if (y !== emptyY) {
          board[emptyY][x] = board[y][x];
          board[y][x] = null;
          await animateFall(board[emptyY][x], y, emptyY);
        }
        emptyY--;
      }
    }
    
    // 填充新宝石
    for (let y = emptyY; y >= 0; y--) {
      board[y][x] = createRandomGem();
      await animateAppear(board[y][x], y);
    }
  }
}
```

## 关卡设计

| 元素 | 说明 |
|------|------|
| 目标 | 消除指定数量/达到分数 |
| 步数限制 | 有限步数完成目标 |
| 障碍物 | 冰块、铁链、石头 |
| 道具 | 锤子、刷新、交换 |

## 变现设计

- 道具购买
- 额外步数
- 生命系统
- 限时活动
