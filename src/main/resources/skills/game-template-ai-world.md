---
name: AI程序化世界游戏开发模板
description: AI程序化世界游戏开发模板，适用于AI生成地图、动态生态、探索冒险类游戏
trigger: AI生成, 程序化, 世界, 探索, procedural, world, generation, 生态
examples: No Man's Sky|Minecraft|Dwarf Fortress|Terraria|Starbound
---

# AI 程序化世界游戏开发模板

## 游戏设计核心原则

### 核心循环（持续进行）
```
探索新区域 → 发现新资源 → 遇到新生物 → 建造基地 → 继续探索
```
- **无限世界**：每次探索都是新世界
- **动态生态**：生物之间有生态关系
- **程序生成**：AI 生成独特内容

### 玩家心理学
- **"探索未知"的好奇心**：发现新地形、新生物
- **"无限可能"的期待感**：每次都不一样
- **"建造世界"的成就感**：在程序生成的世界中建造
- **"发现秘密"的惊喜感**：发现隐藏内容

### 程序化世界设计要点
```
程序化世界核心：
1. 种子系统：相同种子生成相同世界
2. 生物群落：不同区域有不同生态
3. 动态生态：生物之间有食物链
4. 天气系统：天气影响环境
```

## 核心系统设计

### 1. 世界生成系统
```javascript
class WorldGenerator {
  constructor(seed) {
    this.seed = seed;
    this.noise = new SimplexNoise(seed);
    this.biomes = [];
  }
  
  generateChunk(chunkX, chunkZ) {
    const chunk = new Chunk(chunkX, chunkZ);
    
    // 生成地形
    for (let x = 0; x < 16; x++) {
      for (let z = 0; z < 16; z++) {
        const worldX = chunkX * 16 + x;
        const worldZ = chunkZ * 16 + z;
        
        // 使用噪声生成高度
        const height = this.getHeight(worldX, worldZ);
        
        // 确定生物群落
        const biome = this.getBiome(worldX, worldZ);
        
        // 生成方块
        this.generateBlocks(chunk, x, z, height, biome);
      }
    }
    
    // 生成结构
    this.generateStructures(chunk);
    
    // 生成生物
    this.generateMobs(chunk);
    
    return chunk;
  }
  
  getHeight(x, z) {
    const scale = 0.01;
    const height = this.noise.noise2D(x * scale, z * scale);
    return Math.floor(50 + height * 30);
  }
  
  getBiome(x, z) {
    const temperature = this.noise.noise2D(x * 0.005, z * 0.005);
    const humidity = this.noise.noise2D(x * 0.005 + 1000, z * 0.005 + 1000);
    
    if (temperature > 0.3) return 'desert';
    if (temperature < -0.3) return 'tundra';
    if (humidity > 0.3) return 'forest';
    if (humidity < -0.3) return 'plains';
    return 'grassland';
  }
  
  generateBlocks(chunk, x, z, height, biome) {
    for (let y = 0; y < height; y++) {
      if (y === 0) {
        chunk.setBlock(x, y, z, 'bedrock');
      } else if (y < height - 4) {
        chunk.setBlock(x, y, z, 'stone');
      } else if (y < height) {
        chunk.setBlock(x, y, z, biome.surfaceBlock);
      } else {
        chunk.setBlock(x, y, z, 'air');
      }
    }
  }
}

const BIOMES = {
  forest: { surfaceBlock: 'grass', trees: 0.05, animals: ['deer', 'rabbit', 'wolf'] },
  desert: { surfaceBlock: 'sand', trees: 0.01, animals: ['scorpion', 'snake'] },
  tundra: { surfaceBlock: 'snow', trees: 0.02, animals: ['polar_bear', 'wolf'] },
  plains: { surfaceBlock: 'grass', trees: 0.02, animals: ['cow', 'horse', 'chicken'] }
};
```

### 2. 生态系统
```javascript
class EcosystemSystem {
  constructor() {
    this.foodChain = {};
    this.populations = {};
  }
  
  addSpecies(config) {
    this.foodChain[config.id] = {
      name: config.name,
      diet: config.diet, // herbivore, carnivore, omnivore
      predators: config.predators || [],
      prey: config.prey || [],
      reproductionRate: config.reproductionRate,
      maxPopulation: config.maxPopulation
    };
    
    this.populations[config.id] = {
      count: config.initialPopulation || 10,
      lastUpdate: Date.now()
    };
  }
  
  update(delta) {
    // 更新每个物种的数量
    for (const [speciesId, species] of Object.entries(this.foodChain)) {
      const population = this.populations[speciesId];
      
      // 检查食物
      const foodAvailable = this.checkFood(speciesId);
      
      // 检查天敌
      const predatorsPresent = this.checkPredators(speciesId);
      
      // 计算人口变化
      let change = 0;
      
      if (foodAvailable && !predatorsPresent) {
        // 食物充足，没有天敌，增长
        change = Math.floor(population.count * species.reproductionRate * delta);
      } else if (!foodAvailable || predatorsPresent) {
        // 食物不足或有天敌，减少
        change = -Math.floor(population.count * 0.1 * delta);
      }
      
      // 应用变化
      population.count = Math.max(0, Math.min(species.maxPopulation, population.count + change));
    }
  }
  
  checkFood(speciesId) {
    const species = this.foodChain[speciesId];
    
    if (species.diet === 'herbivore') {
      // 草食动物检查植物
      return true; // 假设植物总是充足的
    }
    
    if (species.diet === 'carnivore') {
      // 肉食动物检查猎物
      for (const preyId of species.prey) {
        if (this.populations[preyId].count > 0) {
          return true;
        }
      }
      return false;
    }
    
    return true;
  }
  
  checkPredators(speciesId) {
    const species = this.foodChain[speciesId];
    
    for (const predatorId of species.predators) {
      if (this.populations[predatorId].count > 0) {
        return true;
      }
    }
    
    return false;
  }
}

const SPECIES = [
  { id: 'rabbit', name: '兔子', diet: 'herbivore', predators: ['wolf'], reproductionRate: 0.1, maxPopulation: 100 },
  { id: 'wolf', name: '狼', diet: 'carnivore', prey: ['rabbit'], reproductionRate: 0.05, maxPopulation: 30 },
  { id: 'deer', name: '鹿', diet: 'herbivore', predators: ['wolf'], reproductionRate: 0.08, maxPopulation: 80 }
];
```

### 3. 天气系统
```javascript
class WeatherSystem {
  constructor() {
    this.currentWeather = 'clear';
    this.weatherTimer = 0;
    this.weatherDuration = 300; // 5 分钟
    this.temperature = 20;
    this.humidity = 50;
  }
  
  update(delta) {
    this.weatherTimer += delta;
    
    if (this.weatherTimer >= this.weatherDuration) {
      this.changeWeather();
      this.weatherTimer = 0;
    }
    
    // 更新温度和湿度
    this.updateClimate(delta);
  }
  
  changeWeather() {
    const weathers = ['clear', 'cloudy', 'rain', 'storm', 'snow'];
    const weights = this.getWeatherWeights();
    
    this.currentWeather = this.weightedRandom(weathers, weights);
  }
  
  getWeatherWeights() {
    // 根据生物群落和季节返回天气权重
    const biome = this.getCurrentBiome();
    const season = this.getCurrentSeason();
    
    const weights = {
      clear: 30,
      cloudy: 25,
      rain: 20,
      storm: 10,
      snow: 15
    };
    
    // 根据生物群落调整
    if (biome === 'desert') {
      weights.rain = 5;
      weights.snow = 0;
    }
    
    if (biome === 'tundra') {
      weights.snow = 40;
      weights.rain = 10;
    }
    
    return weights;
  }
  
  updateClimate(delta) {
    // 根据天气更新温度和湿度
    switch (this.currentWeather) {
      case 'clear':
        this.temperature += 0.1 * delta;
        this.humidity -= 0.05 * delta;
        break;
      case 'rain':
        this.temperature -= 0.05 * delta;
        this.humidity += 0.1 * delta;
        break;
      case 'storm':
        this.temperature -= 0.1 * delta;
        this.humidity += 0.2 * delta;
        break;
      case 'snow':
        this.temperature -= 0.15 * delta;
        this.humidity += 0.1 * delta;
        break;
    }
    
    // 限制范围
    this.temperature = Math.max(-20, Math.min(50, this.temperature));
    this.humidity = Math.max(0, Math.min(100, this.humidity));
  }
  
  getEffects() {
    return {
      visibility: this.currentWeather === 'storm' ? 0.5 : 1,
      movementSpeed: this.currentWeather === 'snow' ? 0.8 : 1,
      temperature: this.temperature,
      humidity: this.humidity
    };
  }
}
```

### 4. 结构生成系统
```javascript
class StructureGenerator {
  constructor() {
    this.structures = [];
  }
  
  addStructure(config) {
    this.structures.push({
      id: config.id,
      name: config.name,
      biome: config.biome,
      rarity: config.rarity,
      size: config.size,
      loot: config.loot,
      mobs: config.mobs
    });
  }
  
  generateForChunk(chunk) {
    for (const structure of this.structures) {
      // 检查是否应该生成
      if (Math.random() < structure.rarity) {
        // 检查生物群落
        if (chunk.biome === structure.biome) {
          // 生成结构
          this.placeStructure(chunk, structure);
        }
      }
    }
  }
  
  placeStructure(chunk, structure) {
    // 找到合适的位置
    const position = this.findSuitablePosition(chunk, structure.size);
    
    if (position) {
      // 放置结构
      chunk.addStructure({
        ...structure,
        position
      });
      
      // 生成战利品
      this.generateLoot(chunk, position, structure.loot);
      
      // 生成怪物
      this.generateMobs(chunk, position, structure.mobs);
    }
  }
}

const STRUCTURES = [
  { id: 'village', name: '村庄', biome: 'plains', rarity: 0.01, size: { x: 50, z: 50 }, loot: ['food', 'tools'], mobs: ['villager'] },
  { id: 'dungeon', name: '地牢', biome: 'any', rarity: 0.005, size: { x: 20, z: 20 }, loot: ['treasure', 'weapons'], mobs: ['zombie', 'skeleton'] },
  { id: 'temple', name: '神殿', biome: 'desert', rarity: 0.008, size: { x: 30, z: 30 }, loot: ['ancient_artifact'], mobs: ['guardian'] }
];
```

### 5. 探索系统
```javascript
class ExplorationSystem {
  constructor() {
    this.exploredChunks = new Set();
    this.discoveredStructures = [];
    this.map = new Map();
  }
  
  exploreChunk(chunkX, chunkZ) {
    const key = `${chunkX},${chunkZ}`;
    
    if (!this.exploredChunks.has(key)) {
      this.exploredChunks.add(key);
      
      // 生成区块
      const chunk = this.worldGenerator.generateChunk(chunkX, chunkZ);
      
      // 更新地图
      this.updateMap(chunk);
      
      // 检查发现
      this.checkDiscoveries(chunk);
    }
  }
  
  updateMap(chunk) {
    // 更新探索地图
    this.map.set(`${chunk.x},${chunk.z}`, {
      biome: chunk.biome,
      height: chunk.averageHeight,
      structures: chunk.structures.length,
      discovered: true
    });
  }
  
  checkDiscoveries(chunk) {
    // 检查是否有新发现
    for (const structure of chunk.structures) {
      if (!this.discoveredStructures.find(s => s.id === structure.id)) {
        this.discoveredStructures.push(structure);
        this.showDiscoveryNotification(structure);
      }
    }
  }
  
  getExplorationPercentage() {
    const totalChunks = this.worldGenerator.getTotalChunks();
    return Math.floor(this.exploredChunks.size / totalChunks * 100);
  }
}
```

## 迭代策略

### 第一版：基础世界
- 简单地形生成
- 基础生物群落
- 基础探索
- 简单 UI

### 第二版：生态系统
- 食物链系统
- 动态生态
- 天气系统
- 结构生成

### 第三版：深度探索
- 多种生物群落
- 隐藏结构
- 探索地图
- 成就系统

### 第四版：建造系统
- 建造系统
- 资源收集
- 制作系统
- 基地建设

### 第五版：多人联机
- 多人探索
- 服务器系统
- 社区功能
- 世界分享

## 常见错误

1. **世界太小**：要有足够大的世界
2. **地形太单调**：要有多种生物群落
3. **没有生态**：生物之间要有关系
4. **没有天气**：天气系统增加沉浸感
5. **没有探索欲**：要有隐藏内容激励探索
