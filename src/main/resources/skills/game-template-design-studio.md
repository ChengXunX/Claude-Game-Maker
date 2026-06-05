---
name: game-template-design-studio
description: 游戏设计工作室模板 - 可视化关卡编辑、实时预览、素材管理、作品发布
category: game-template
triggerPattern: design, 设计, editor, 编辑器, creative, 创作, sandbox, 沙盒, build, 建造, maker, 制作
---

# 游戏设计工作室模板

## 概述

让玩家沉浸式设计自己游戏作品的创作平台，包含：
- **可视化编辑器**：拖拽式关卡设计、实时预览
- **素材系统**：内置素材库、自定义素材上传
- **逻辑编辑器**：可视化编程、触发器系统
- **作品管理**：保存、版本控制、发布分享
- **测试环境**：即时测试、调试工具
- **模板系统**：预设模板快速开始

## 核心代码

### 1. 编辑器核心 (EditorCore.js)

```javascript
export class EditorCore {
  constructor(canvas) {
    this.canvas = canvas
    this.ctx = canvas.getContext('2d')
    this.scene = {
      objects: [],
      triggers: [],
      metadata: {
        name: '未命名作品',
        author: '',
        description: '',
        version: '1.0.0'
      }
    }
    this.selectedObject = null
    this.gridSize = 32
    this.showGrid = true
    this.zoom = 1
    this.panX = 0
    this.panY = 0
    this.history = []
    this.historyIndex = -1
    this.maxHistory = 50
  }

  // 添加对象
  addObject(type, x, y, properties = {}) {
    const obj = {
      id: this.generateId(),
      type,
      x,
      y,
      width: properties.width || 32,
      height: properties.height || 32,
      rotation: 0,
      properties: { ...properties },
      visible: true,
      locked: false
    }

    this.scene.objects.push(obj)
    this.saveHistory()
    this.render()
    return obj
  }

  // 删除对象
  removeObject(id) {
    this.scene.objects = this.scene.objects.filter(o => o.id !== id)
    if (this.selectedObject?.id === id) {
      this.selectedObject = null
    }
    this.saveHistory()
    this.render()
  }

  // 选择对象
  selectObject(x, y) {
    const obj = this.findObjectAt(x, y)
    this.selectedObject = obj
    this.render()
    return obj
  }

  // 移动对象
  moveObject(id, x, y) {
    const obj = this.scene.objects.find(o => o.id === id)
    if (obj) {
      obj.x = x
      obj.y = y
      this.saveHistory()
      this.render()
    }
  }

  // 查找指定位置的对象
  findObjectAt(x, y) {
    for (let i = this.scene.objects.length - 1; i >= 0; i--) {
      const obj = this.scene.objects[i]
      if (x >= obj.x && x <= obj.x + obj.width &&
          y >= obj.y && y <= obj.y + obj.height) {
        return obj
      }
    }
    return null
  }

  // 保存历史（撤销/重做）
  saveHistory() {
    this.history = this.history.slice(0, this.historyIndex + 1)
    this.history.push(JSON.stringify(this.scene))
    this.historyIndex = this.history.length - 1

    if (this.history.length > this.maxHistory) {
      this.history.shift()
      this.historyIndex--
    }
  }

  // 撤销
  undo() {
    if (this.historyIndex > 0) {
      this.historyIndex--
      this.scene = JSON.parse(this.history[this.historyIndex])
      this.render()
    }
  }

  // 重做
  redo() {
    if (this.historyIndex < this.history.length - 1) {
      this.historyIndex++
      this.scene = JSON.parse(this.history[this.historyIndex])
      this.render()
    }
  }

  // 渲染编辑器
  render() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height)
    this.ctx.save()
    this.ctx.scale(this.zoom, this.zoom)
    this.ctx.translate(this.panX, this.panY)

    // 绘制网格
    if (this.showGrid) {
      this.drawGrid()
    }

    // 绘制对象
    this.scene.objects.forEach(obj => {
      if (obj.visible) {
        this.drawObject(obj)
      }
    })

    // 绘制选中框
    if (this.selectedObject) {
      this.drawSelection(this.selectedObject)
    }

    this.ctx.restore()
  }

  drawGrid() {
    this.ctx.strokeStyle = '#eee'
    this.ctx.lineWidth = 0.5
    for (let x = 0; x < this.canvas.width / this.zoom; x += this.gridSize) {
      this.ctx.beginPath()
      this.ctx.moveTo(x, 0)
      this.ctx.lineTo(x, this.canvas.height / this.zoom)
      this.ctx.stroke()
    }
    for (let y = 0; y < this.canvas.height / this.zoom; y += this.gridSize) {
      this.ctx.beginPath()
      this.ctx.moveTo(0, y)
      this.ctx.lineTo(this.canvas.width / this.zoom, y)
      this.ctx.stroke()
    }
  }

  drawObject(obj) {
    this.ctx.fillStyle = obj.properties.color || '#4CAF50'
    this.ctx.fillRect(obj.x, obj.y, obj.width, obj.height)

    if (obj.properties.text) {
      this.ctx.fillStyle = '#fff'
      this.ctx.font = '12px Arial'
      this.ctx.textAlign = 'center'
      this.ctx.fillText(obj.properties.text, obj.x + obj.width/2, obj.y + obj.height/2 + 4)
    }
  }

  drawSelection(obj) {
    this.ctx.strokeStyle = '#2196F3'
    this.ctx.lineWidth = 2
    this.ctx.setLineDash([5, 5])
    this.ctx.strokeRect(obj.x - 2, obj.y - 2, obj.width + 4, obj.height + 4)
    this.ctx.setLineDash([])
  }

  generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2, 9)
  }
}
```

### 2. 素材管理器 (AssetManager.js)

```javascript
export class AssetManager {
  constructor() {
    this.assets = new Map()
    this.categories = {
      characters: { name: '角色', icon: '👤', assets: [] },
      objects: { name: '物体', icon: '📦', assets: [] },
      backgrounds: { name: '背景', icon: '🖼️', assets: [] },
      effects: { name: '特效', icon: '✨', assets: [] },
      sounds: { name: '音效', icon: '🔊', assets: [] },
      ui: { name: 'UI', icon: '🎨', assets: [] }
    }
    this.loadBuiltinAssets()
  }

  loadBuiltinAssets() {
    // 角色
    this.addAsset('characters', 'hero', '英雄', '/assets/characters/hero.png', { width: 32, height: 48 })
    this.addAsset('characters', 'villain', '反派', '/assets/characters/villain.png', { width: 32, height: 48 })
    this.addAsset('characters', 'npc', 'NPC', '/assets/characters/npc.png', { width: 32, height: 48 })

    // 物体
    this.addAsset('objects', 'coin', '金币', '/assets/objects/coin.png', { width: 16, height: 16 })
    this.addAsset('objects', 'gem', '宝石', '/assets/objects/gem.png', { width: 16, height: 16 })
    this.addAsset('objects', 'key', '钥匙', '/assets/objects/key.png', { width: 16, height: 16 })
    this.addAsset('objects', 'door', '门', '/assets/objects/door.png', { width: 32, height: 48 })
    this.addAsset('objects', 'chest', '宝箱', '/assets/objects/chest.png', { width: 32, height: 32 })

    // 背景
    this.addAsset('backgrounds', 'forest', '森林', '/assets/backgrounds/forest.png', { width: 800, height: 600 })
    this.addAsset('backgrounds', 'cave', '洞穴', '/assets/backgrounds/cave.png', { width: 800, height: 600 })
    this.addAsset('backgrounds', 'city', '城市', '/assets/backgrounds/city.png', { width: 800, height: 600 })

    // 特效
    this.addAsset('effects', 'explosion', '爆炸', '/assets/effects/explosion.png', { width: 64, height: 64 })
    this.addAsset('effects', 'sparkle', '闪光', '/assets/effects/sparkle.png', { width: 32, height: 32 })
  }

  addAsset(category, id, name, url, size) {
    const asset = { id, name, url, size, category }
    this.assets.set(`${category}/${id}`, asset)
    this.categories[category].assets.push(asset)
  }

  getAsset(category, id) {
    return this.assets.get(`${category}/${id}`)
  }

  getCategoryAssets(category) {
    return this.categories[category]?.assets || []
  }

  async uploadAsset(category, file) {
    const id = file.name.replace(/\.[^.]+$/, '')
    const url = URL.createObjectURL(file)
    const size = await this.getImageSize(file)

    this.addAsset(category, id, id, url, size)
    return this.getAsset(category, id)
  }

  getImageSize(file) {
    return new Promise((resolve) => {
      const img = new Image()
      img.onload = () => resolve({ width: img.width, height: img.height })
      img.src = URL.createObjectURL(file)
    })
  }
}
```

### 3. 触发器系统 (TriggerSystem.js)

```javascript
export class TriggerSystem {
  constructor() {
    this.triggers = []
    this.conditions = {
      'collision': { name: '碰撞', params: ['objectA', 'objectB'] },
      'click': { name: '点击', params: ['objectId'] },
      'timer': { name: '定时', params: ['seconds'] },
      'score': { name: '分数达到', params: ['value'] },
      'health': { name: '生命值', params: ['value', 'operator'] },
      'position': { name: '位置', params: ['objectId', 'x', 'y', 'radius'] },
      'variable': { name: '变量', params: ['name', 'operator', 'value'] }
    }
    this.actions = {
      'move': { name: '移动', params: ['objectId', 'x', 'y', 'speed'] },
      'spawn': { name: '生成', params: ['assetId', 'x', 'y'] },
      'destroy': { name: '销毁', params: ['objectId'] },
      'score': { name: '加分', params: ['value'] },
      'message': { name: '显示消息', params: ['text', 'duration'] },
      'sound': { name: '播放音效', params: ['soundId'] },
      'variable': { name: '设置变量', params: ['name', 'value'] },
      'scene': { name: '切换场景', params: ['sceneId'] }
    }
  }

  addTrigger(config) {
    const trigger = {
      id: this.generateId(),
      name: config.name,
      condition: config.condition,
      actions: config.actions,
      enabled: true,
      oneShot: config.oneShot || false,
      triggered: false
    }
    this.triggers.push(trigger)
    return trigger
  }

  removeTrigger(id) {
    this.triggers = this.triggers.filter(t => t.id !== id)
  }

  checkTriggers(context) {
    this.triggers.forEach(trigger => {
      if (!trigger.enabled) return
      if (trigger.oneShot && trigger.triggered) return

      if (this.evaluateCondition(trigger.condition, context)) {
        trigger.triggered = true
        trigger.actions.forEach(action => {
          this.executeAction(action, context)
        })
      }
    })
  }

  evaluateCondition(condition, context) {
    switch (condition.type) {
      case 'collision':
        return context.checkCollision(condition.objectA, condition.objectB)
      case 'click':
        return context.isClicked(condition.objectId)
      case 'timer':
        return context.getElapsedTime() >= condition.seconds * 1000
      case 'score':
        return context.getScore() >= condition.value
      default:
        return false
    }
  }

  executeAction(action, context) {
    switch (action.type) {
      case 'move':
        context.moveObject(action.objectId, action.x, action.y, action.speed)
        break
      case 'spawn':
        context.spawnObject(action.assetId, action.x, action.y)
        break
      case 'destroy':
        context.destroyObject(action.objectId)
        break
      case 'score':
        context.addScore(action.value)
        break
      case 'message':
        context.showMessage(action.text, action.duration)
        break
    }
  }

  generateId() {
    return 'trigger_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5)
  }
}
```

### 4. 作品管理 (ProjectManager.js)

```javascript
export class ProjectManager {
  constructor() {
    this.projects = []
    this.currentProject = null
  }

  createProject(name, template) {
    const project = {
      id: this.generateId(),
      name,
      template: template || 'blank',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      version: '1.0.0',
      scenes: [],
      assets: [],
      settings: {
        width: 800,
        height: 600,
        backgroundColor: '#f0f0f0'
      }
    }

    this.projects.push(project)
    this.currentProject = project
    this.save()
    return project
  }

  loadProject(id) {
    const project = this.projects.find(p => p.id === id)
    if (project) {
      this.currentProject = project
    }
    return project
  }

  saveProject() {
    if (!this.currentProject) return
    this.currentProject.updatedAt = new Date().toISOString()
    this.save()
  }

  save() {
    localStorage.setItem('game-projects', JSON.stringify(this.projects))
  }

  load() {
    const data = localStorage.getItem('game-projects')
    if (data) {
      this.projects = JSON.parse(data)
    }
  }

  deleteProject(id) {
    this.projects = this.projects.filter(p => p.id !== id)
    if (this.currentProject?.id === id) {
      this.currentProject = null
    }
    this.save()
  }

  exportProject(id) {
    const project = this.projects.find(p => p.id === id)
    if (project) {
      const data = JSON.stringify(project, null, 2)
      const blob = new Blob([data], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${project.name}.json`
      a.click()
      URL.revokeObjectURL(url)
    }
  }

  importProject(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          const project = JSON.parse(e.target.result)
          project.id = this.generateId()
          this.projects.push(project)
          this.save()
          resolve(project)
        } catch (err) {
          reject(err)
        }
      }
      reader.readAsText(file)
    })
  }

  generateId() {
    return 'project_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }
}
```

### 5. 测试运行器 (TestRunner.js)

```javascript
export class TestRunner {
  constructor(editor) {
    this.editor = editor
    this.isRunning = false
    this.gameState = null
    this.startTime = null
  }

  start() {
    this.isRunning = true
    this.startTime = Date.now()
    this.gameState = {
      score: 0,
      health: 100,
      variables: {},
      objects: JSON.parse(JSON.stringify(this.editor.scene.objects))
    }
    return this.gameState
  }

  stop() {
    this.isRunning = false
    this.gameState = null
  }

  update(deltaTime) {
    if (!this.isRunning) return

    // 更新对象状态
    this.gameState.objects.forEach(obj => {
      if (obj.properties.velocity) {
        obj.x += obj.properties.velocity.x * deltaTime
        obj.y += obj.properties.velocity.y * deltaTime
      }

      if (obj.properties.gravity) {
        obj.properties.velocity = obj.properties.velocity || { x: 0, y: 0 }
        obj.properties.velocity.y += obj.properties.gravity * deltaTime
      }
    })
  }

  getStats() {
    return {
      duration: Date.now() - this.startTime,
      score: this.gameState?.score || 0,
      objectCount: this.gameState?.objects.length || 0
    }
  }
}
```

## 设计工具功能

### 可视化编辑器

- **拖拽放置**：从素材库拖拽到画布
- **实时预览**：所见即所得
- **网格对齐**：自动对齐网格
- **多选操作**：框选多个对象
- **撤销/重做**：最多50步历史

### 逻辑编辑器

- **触发器**：碰撞、点击、定时、分数等条件
- **动作**：移动、生成、销毁、加分等操作
- **变量**：全局变量和对象变量
- **条件分支**：if/else 逻辑

### 素材系统

- **内置素材**：角色、物体、背景、特效
- **自定义上传**：支持图片和音效
- **素材分类**：按类型组织
- **素材预览**：缩略图预览

## 使用方法

1. 打开设计工作室
2. 创建新作品或选择模板
3. 拖拽素材到画布
4. 设置触发器和逻辑
5. 测试运行
6. 保存并发布

## 扩展点

- 添加新素材类型：在 `AssetManager` 中扩展
- 添加新触发器：在 `TriggerSystem` 中扩展
- 添加新动作：在 `TriggerSystem` 中扩展
- 添加协作功能：集成 WebSocket
- 添加版本控制：集成 Git
