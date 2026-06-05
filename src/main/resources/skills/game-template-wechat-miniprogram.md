---
name: game-template-wechat-miniprogram
description: 微信小程序+Java全栈模板 - 提供完整的微信小程序游戏项目骨架
category: game-template
triggerPattern: wechat, miniprogram, 微信, 小程序, wx, 微信游戏, mobile, 手机
---

# 微信小程序 + Java 全栈模板

## 概述

完整的微信小程序游戏全栈模板，包含：
- **前端**：微信小程序原生框架 + Canvas 游戏引擎
- **后端**：Spring Boot + MyBatis + MySQL
- **实时通信**：WebSocket
- **用户系统**：微信登录、用户信息
- **支付系统**：微信支付
- **排行榜**：微信关系链排行

## 项目结构

```
wechat-game/
├── README.md
├── docker-compose.yml
│
├── miniprogram/                # 微信小程序前端
│   ├── app.js
│   ├── app.json
│   ├── app.wxss
│   ├── project.config.json
│   ├── pages/
│   │   ├── index/              # 首页
│   │   │   ├── index.js
│   │   │   ├── index.wxml
│   │   │   ├── index.wxss
│   │   │   └── index.json
│   │   ├── game/               # 游戏页
│   │   │   ├── game.js
│   │   │   ├── game.wxml
│   │   │   ├── game.wxss
│   │   │   └── game.json
│   │   ├── ranking/            # 排行榜
│   │   ├── profile/            # 个人中心
│   │   └── shop/               # 商店
│   ├── components/
│   │   ├── game-canvas/        # 游戏画布组件
│   │   ├── leaderboard/        # 排行榜组件
│   │   └── share-card/         # 分享卡片
│   ├── utils/
│   │   ├── api.js              # API 封装
│   │   ├── websocket.js        # WebSocket 封装
│   │   ├── game-engine.js      # 游戏引擎
│   │   └── storage.js          # 本地存储
│   ├── game/
│   │   ├── engine.js           # Canvas 游戏引擎
│   │   ├── scenes/
│   │   │   ├── MenuScene.js
│   │   │   ├── GameScene.js
│   │   │   └── ResultScene.js
│   │   ├── entities/
│   │   │   ├── Player.js
│   │   │   └── Enemy.js
│   │   └── systems/
│   │       ├── InputSystem.js
│   │       ├── PhysicsSystem.js
│   │       └── RenderSystem.js
│   └── assets/
│       ├── images/
│       └── sounds/
│
├── server/                     # Java 后端
│   ├── pom.xml
│   ├── src/main/java/com/game/
│   │   ├── GameApplication.java
│   │   ├── config/
│   │   │   ├── WxConfig.java           # 微信配置
│   │   │   ├── WebSocketConfig.java     # WebSocket 配置
│   │   │   └── SecurityConfig.java      # 安全配置
│   │   ├── controller/
│   │   │   ├── AuthController.java      # 认证接口
│   │   │   ├── GameController.java      # 游戏接口
│   │   │   ├── RankingController.java   # 排行榜接口
│   │   │   └── PayController.java       # 支付接口
│   │   ├── service/
│   │   │   ├── WxAuthService.java       # 微信登录
│   │   │   ├── GameService.java         # 游戏服务
│   │   │   ├── RankingService.java      # 排行榜服务
│   │   │   └── PayService.java          # 支付服务
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── GameRecord.java
│   │   │   └── Payment.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── GameRecordRepository.java
│   │   │   └── PaymentRepository.java
│   │   └── websocket/
│   │       └── GameWebSocketHandler.java
│   └── src/main/resources/
│       └── application.yml
│
└── database/
    └── init.sql
```

## 核心代码

### 1. 微信登录 (miniprogram/utils/api.js)

```javascript
const BASE_URL = 'https://your-server.com/api'

class ApiClient {
  constructor() {
    this.token = wx.getStorageSync('token') || ''
  }

  // 微信登录
  async wxLogin() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: async (res) => {
          if (res.code) {
            try {
              const data = await this.request('/auth/wx-login', {
                method: 'POST',
                data: { code: res.code }
              })
              this.token = data.token
              wx.setStorageSync('token', data.token)
              resolve(data)
            } catch (error) {
              reject(error)
            }
          } else {
            reject(new Error('登录失败'))
          }
        },
        fail: reject
      })
    })
  }

  // 通用请求
  async request(url, options = {}) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: BASE_URL + url,
        method: options.method || 'GET',
        data: options.data,
        header: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.token}`
        },
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res.data)
          } else if (res.statusCode === 401) {
            this.wxLogin().then(() => this.request(url, options)).then(resolve).catch(reject)
          } else {
            reject(new Error(res.data.message || '请求失败'))
          }
        },
        fail: reject
      })
    })
  }

  // 游戏相关 API
  async submitScore(score, level) {
    return this.request('/game/submit-score', {
      method: 'POST',
      data: { score, level }
    })
  }

  async getRanking(type = 'friends') {
    return this.request(`/ranking/${type}`)
  }

  async getGameConfig() {
    return this.request('/game/config')
  }
}

module.exports = new ApiClient()
```

### 2. Canvas 游戏引擎 (miniprogram/game/engine.js)

```javascript
class GameEngine {
  constructor(canvas) {
    this.canvas = canvas
    this.ctx = canvas.getContext('2d')
    this.width = canvas.width
    this.height = canvas.height
    this.scenes = new Map()
    this.currentScene = null
    this.running = false
    this.lastTime = 0
    this.fps = 60
    this.frameTime = 1000 / this.fps
  }

  addScene(name, scene) {
    this.scenes.set(name, scene)
    scene.engine = this
  }

  switchScene(name) {
    if (this.currentScene) {
      this.currentScene.exit?.()
    }
    this.currentScene = this.scenes.get(name)
    this.currentScene?.enter?.()
  }

  start() {
    this.running = true
    this.lastTime = Date.now()
    this.loop()
  }

  stop() {
    this.running = false
  }

  loop() {
    if (!this.running) return

    const now = Date.now()
    const dt = now - this.lastTime

    if (dt >= this.frameTime) {
      this.lastTime = now - (dt % this.frameTime)
      this.update(dt / 1000)
      this.render()
    }

    requestAnimationFrame(() => this.loop())
  }

  update(dt) {
    this.currentScene?.update?.(dt)
  }

  render() {
    this.ctx.clearRect(0, 0, this.width, this.height)
    this.currentScene?.render?.(this.ctx)
  }

  // 输入处理
  bindTouch() {
    this.touches = new Map()

    this.canvas.addEventListener('touchstart', (e) => {
      e.preventDefault()
      for (const touch of e.changedTouches) {
        this.touches.set(touch.identifier, {
          x: touch.clientX,
          y: touch.clientY,
          startX: touch.clientX,
          startY: touch.clientY
        })
      }
      this.currentScene?.onTouchStart?.(this.touches)
    })

    this.canvas.addEventListener('touchmove', (e) => {
      e.preventDefault()
      for (const touch of e.changedTouches) {
        const t = this.touches.get(touch.identifier)
        if (t) {
          t.x = touch.clientX
          t.y = touch.clientY
        }
      }
      this.currentScene?.onTouchMove?.(this.touches)
    })

    this.canvas.addEventListener('touchend', (e) => {
      for (const touch of e.changedTouches) {
        this.touches.delete(touch.identifier)
      }
      this.currentScene?.onTouchEnd?.(this.touches)
    })
  }
}

module.exports = GameEngine
```

### 3. 微信登录服务 (server/src/main/java/com/game/service/WxAuthService.java)

```java
@Service
public class WxAuthService {

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.secret}")
    private String appSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 微信登录
     * @param code 微信登录code
     * @return JWT Token
     */
    public Map<String, Object> wxLogin(String code) {
        // 用code换取openid
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appId, appSecret, code
        );

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        String openid = (String) response.get("openid");
        String sessionKey = (String) response.get("session_key");

        if (openid == null) {
            throw new RuntimeException("微信登录失败");
        }

        // 查找或创建用户
        User user = userRepository.findByOpenid(openid)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setOpenid(openid);
                newUser.setSessionKey(sessionKey);
                newUser.setCreatedAt(LocalDateTime.now());
                return userRepository.save(newUser);
            });

        // 更新session_key
        user.setSessionKey(sessionKey);
        userRepository.save(user);

        // 生成JWT
        String token = jwtUtils.generateToken(user.getId(), user.getOpenid());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        return result;
    }
}
```

### 4. 游戏服务 (server/src/main/java/com/game/service/GameService.java)

```java
@Service
public class GameService {

    @Autowired
    private GameRecordRepository recordRepository;

    @Autowired
    private RankingService rankingService;

    /**
     * 提交游戏分数
     */
    public GameRecord submitScore(Long userId, int score, int level) {
        GameRecord record = new GameRecord();
        record.setUserId(userId);
        record.setScore(score);
        record.setLevel(level);
        record.setCreatedAt(LocalDateTime.now());

        // 验证分数（防作弊）
        if (!validateScore(userId, score, level)) {
            throw new RuntimeException("分数验证失败");
        }

        record = recordRepository.save(record);

        // 更新排行榜
        rankingService.updateRanking(userId, score);

        return record;
    }

    /**
     * 验证分数
     */
    private boolean validateScore(Long userId, int score, int level) {
        // 基本验证
        if (score < 0 || score > 999999) return false
        if (level < 1 || level > 100) return false

        // 检查是否超过历史最高分太多
        Optional<GameRecord> bestRecord = recordRepository.findTopByUserIdOrderByScoreDesc(userId);
        if (bestRecord.isPresent()) {
            int maxExpected = bestRecord.get().getScore() * 2
            if (score > maxExpected && score > 10000) {
                return false
            }
        }

        return true
    }

    /**
     * 获取用户游戏记录
     */
    public List<GameRecord> getUserRecords(Long userId, int page, int size) {
        return recordRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size)
        ).getContent();
    }
}
```

### 5. 排行榜服务 (server/src/main/java/com/game/service/RankingService.java)

```java
@Service
public class RankingService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    private static final String RANKING_KEY = "game:ranking:global";

    /**
     * 更新排行榜
     */
    public void updateRanking(Long userId, int score) {
        redisTemplate.opsForZSet().add(RANKING_KEY, String.valueOf(userId), score);
    }

    /**
     * 获取全球排行榜
     */
    public List<Map<String, Object>> getGlobalRanking(int page, int size) {
        int start = page * size
        int end = start + size - 1

        Set<ZSetOperations.TypedTuple<String>> entries =
            redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, start, end);

        List<Map<String, Object>> ranking = new ArrayList<>();
        int rank = start + 1

        if (entries != null) {
            for (ZSetOperations.TypedTuple<String> entry : entries) {
                Long userId = Long.parseLong(entry.getValue());
                User user = userRepository.findById(userId).orElse(null);

                Map<String, Object> item = new HashMap<>();
                item.put("rank", rank++);
                item.put("userId", userId);
                item.put("nickname", user != null ? user.getNickname() : "未知");
                item.put("avatar", user != null ? user.getAvatar() : "");
                item.put("score", entry.getScore().intValue());
                ranking.add(item);
            }
        }

        return ranking
    }

    /**
     * 获取好友排行榜（需要微信关系链）
     */
    public List<Map<String, Object>> getFriendRanking(Long userId, List<Long> friendIds) {
        List<Long> allIds = new ArrayList<>(friendIds);
        allIds.add(userId);

        // 从Redis批量获取分数
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Long id : allIds) {
            Double score = redisTemplate.opsForZSet().score(RANKING_KEY, String.valueOf(id));
            User user = userRepository.findById(id).orElse(null);

            Map<String, Object> item = new HashMap<>();
            item.put("userId", id);
            item.put("nickname", user != null ? user.getNickname() : "未知");
            item.put("avatar", user != null ? user.getAvatar() : "");
            item.put("score", score != null ? score.intValue() : 0);
            ranking.add(item);
        }

        // 按分数排序
        ranking.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        // 添加排名
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }

        return ranking;
    }
}
```

### 6. 微信小程序游戏页面 (miniprogram/pages/game/game.js)

```javascript
const api = require('../../utils/api')
const GameEngine = require('../../game/engine')
const MenuScene = require('../../game/scenes/MenuScene')
const GameScene = require('../../game/scenes/GameScene')

Page({
  data: {
    score: 0,
    level: 1,
    isPlaying: false,
    showResult: false,
    bestScore: 0
  },

  onLoad() {
    this.initGame()
  },

  initGame() {
    const query = wx.createSelectorQuery()
    query.select('#gameCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')

        canvas.width = 375
        canvas.height = 600

        this.engine = new GameEngine(canvas)
        this.engine.addScene('menu', new MenuScene())
        this.engine.addScene('game', new GameScene())
        this.engine.bindTouch()
        this.engine.switchScene('menu')
        this.engine.start()
      })
  },

  startGame() {
    this.setData({ isPlaying: true, score: 0, level: 1 })
    this.engine.switchScene('game')
  },

  onScoreUpdate(score) {
    this.setData({ score })
  },

  onGameOver() {
    this.setData({ isPlaying: false, showResult: true })
    this.submitScore()
  },

  async submitScore() {
    try {
      await api.submitScore(this.data.score, this.data.level)
      wx.showToast({ title: '分数已提交', icon: 'success' })
    } catch (error) {
      console.error('提交分数失败:', error)
    }
  },

  shareToFriends() {
    wx.shareAppMessage({
      title: `我在游戏中得了${this.data.score}分！来挑战我吧`,
      path: '/pages/index/index',
      imageUrl: '/assets/share-card.png'
    })
  },

  onShareTimeline() {
    return {
      title: `我在游戏中得了${this.data.score}分！`,
      imageUrl: '/assets/share-card.png'
    }
  }
})
```

### 7. Docker Compose 配置

```yaml
version: '3.8'

services:
  server:
    build: ./server
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - REDIS_HOST=redis
      - WECHAT_APPID=${WECHAT_APPID}
      - WECHAT_SECRET=${WECHAT_SECRET}
    depends_on:
      - mysql
      - redis

  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=wechat_game
    volumes:
      - mysql-data:/var/lib/mysql
      - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  mysql-data:
  redis-data:
```

### 8. 数据库初始化 (database/init.sql)

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) UNIQUE NOT NULL,
    session_key VARCHAR(128),
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS game_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    level INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_score (user_id, score DESC)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    wx_transaction_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 使用方法

1. 配置微信小程序 AppID 和 AppSecret
2. 启动后端服务 `docker-compose up -d`
3. 使用微信开发者工具打开 `miniprogram/` 目录
4. 配置服务器地址
5. 预览和调试

## 扩展点

- 添加微信支付：在 `PayService.java` 中实现
- 添加微信分享：在小程序中配置分享回调
- 添加实时对战：扩展 WebSocket 处理
- 添加好友系统：利用微信关系链
- 添加成就系统：创建 `AchievementService`
- 添加每日任务：创建 `DailyTaskService`
