/**
 * 射击游戏 - 游戏主类
 * 管理游戏主循环、更新逻辑和渲染
 */

class Game {
    /**
     * 构造函数 - 初始化游戏
     */
    constructor() {
        // 获取Canvas元素
        this.canvas = document.getElementById('gameCanvas');
        this.ctx = this.canvas.getContext('2d');

        // 设置画布尺寸
        this.canvas.width = CANVAS_WIDTH;
        this.canvas.height = CANVAS_HEIGHT;

        // 游戏状态
        this.state = GAME_STATE.PLAYING;

        // 游戏对象
        this.player = new Player();
        this.bullets = [];   // 子弹数组
        this.enemies = [];   // 敌人数组

        // 星星背景
        this.stars = this.createStars();

        // 时间相关
        this.lastTime = 0;
        this.lastEnemySpawn = 0;
        this.animationId = null;

        // 分数
        this.score = 0;

        // 按键状态
        this.keys = {};

        // 绑定事件处理器
        this.bindEvents();
    }

    /**
     * 创建星星背景
     * @returns {Array} 星星数组
     */
    createStars() {
        const stars = [];
        for (let i = 0; i < STAR_COUNT; i++) {
            stars.push({
                x: Math.random() * CANVAS_WIDTH,
                y: Math.random() * CANVAS_HEIGHT,
                speed: STAR_SPEED_MIN + Math.random() * (STAR_SPEED_MAX - STAR_SPEED_MIN),
                size: Math.random() * 2 + 1
            });
        }
        return stars;
    }

    /**
     * 绑定键盘事件
     */
    bindEvents() {
        // 按键按下
        document.addEventListener('keydown', (e) => {
            this.keys[e.code] = true;

            // 重新开始
            if (e.code === 'KeyR' && this.state === GAME_STATE.GAME_OVER) {
                this.restart();
            }

            // 阻止默认行为
            if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space'].includes(e.code)) {
                e.preventDefault();
            }
        });

        // 按键释放
        document.addEventListener('keyup', (e) => {
            this.keys[e.code] = false;
        });
    }

    /**
     * 开始游戏
     * 启动游戏主循环
     */
    start() {
        this.lastTime = performance.now();
        this.lastEnemySpawn = this.lastTime;
        this.loop(this.lastTime);
    }

    /**
     * 游戏主循环
     * 使用 requestAnimationFrame 实现平滑动画
     * @param {number} timestamp - 当前时间戳
     */
    loop(timestamp) {
        // 计算时间增量
        const dt = timestamp - this.lastTime;
        this.lastTime = timestamp;

        // 更新游戏状态
        if (this.state === GAME_STATE.PLAYING) {
            this.update(dt, timestamp);
        }

        // 渲染
        this.render();

        // 继续循环
        this.animationId = requestAnimationFrame((ts) => this.loop(ts));
    }

    /**
     * 更新游戏逻辑
     * 处理移动、射击、碰撞和敌人生成
     * @param {number} dt - 时间增量（毫秒）
     * @param {number} currentTime - 当前时间
     */
    update(dt, currentTime) {
        // 更新玩家
        this.player.update(this.keys);

        // 自动射击（按住任意方向键或自动射击）
        const bullet = this.player.shoot(currentTime);
        if (bullet) {
            this.bullets.push(bullet);
        }

        // 更新子弹
        for (const bullet of this.bullets) {
            bullet.update();
        }

        // 更新敌人
        for (const enemy of this.enemies) {
            enemy.update();
        }

        // 生成敌人
        this.spawnEnemy(currentTime);

        // 碰撞检测：子弹 vs 敌人
        this.checkBulletEnemyCollisions();

        // 碰撞检测：玩家 vs 敌人
        this.checkPlayerEnemyCollisions();

        // 更新星星背景
        this.updateStars();

        // 清理死亡对象
        this.cleanup();
    }

    /**
     * 生成敌人
     * @param {number} currentTime - 当前时间
     */
    spawnEnemy(currentTime) {
        // 检查生成间隔
        if (currentTime - this.lastEnemySpawn < ENEMY_SPAWN_RATE) {
            return;
        }

        // 更新生成时间
        this.lastEnemySpawn = currentTime;

        // 随机x坐标
        const x = ENEMY_SIZE + Math.random() * (CANVAS_WIDTH - ENEMY_SIZE * 2);

        // 创建敌人（从屏幕顶部）
        const enemy = new Enemy(x, -ENEMY_SIZE);
        this.enemies.push(enemy);
    }

    /**
     * 检测子弹与敌人的碰撞
     */
    checkBulletEnemyCollisions() {
        for (const bullet of this.bullets) {
            if (!bullet.alive) continue;

            for (const enemy of this.enemies) {
                if (!enemy.alive) continue;

                if (bullet.hits(enemy)) {
                    // 子弹击中敌人
                    bullet.alive = false;
                    enemy.takeDamage();

                    // 如果敌人死亡，加分
                    if (!enemy.alive) {
                        this.score += SCORE_PER_ENEMY;
                    }
                }
            }
        }
    }

    /**
     * 检测玩家与敌人的碰撞
     */
    checkPlayerEnemyCollisions() {
        for (const enemy of this.enemies) {
            if (!enemy.alive) continue;

            if (this.player.collidesWith(enemy)) {
                // 玩家被击中，游戏结束
                this.state = GAME_STATE.GAME_OVER;
                this.player.alive = false;
            }
        }
    }

    /**
     * 更新星星背景
     */
    updateStars() {
        for (const star of this.stars) {
            // 向下移动
            star.y += star.speed;

            // 超出屏幕则重置到顶部
            if (star.y > CANVAS_HEIGHT) {
                star.y = 0;
                star.x = Math.random() * CANVAS_WIDTH;
            }
        }
    }

    /**
     * 清理死亡对象
     * 移除已死亡的子弹和敌人
     */
    cleanup() {
        this.bullets = this.bullets.filter(bullet => bullet.alive);
        this.enemies = this.enemies.filter(enemy => enemy.alive);
    }

    /**
     * 渲染游戏画面
     */
    render() {
        // 清空画布
        this.ctx.fillStyle = COLORS.BACKGROUND;
        this.ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 渲染星星背景
        this.renderStars();

        // 渲染敌人
        for (const enemy of this.enemies) {
            enemy.render(this.ctx);
        }

        // 渲染子弹
        for (const bullet of this.bullets) {
            bullet.render(this.ctx);
        }

        // 渲染玩家
        this.player.render(this.ctx);

        // 渲染UI
        this.renderUI();

        // 渲染游戏结束画面
        if (this.state === GAME_STATE.GAME_OVER) {
            this.renderGameOver();
        }
    }

    /**
     * 渲染星星背景
     */
    renderStars() {
        this.ctx.fillStyle = COLORS.STAR;
        for (const star of this.stars) {
            this.ctx.beginPath();
            this.ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2);
            this.ctx.fill();
        }
    }

    /**
     * 渲染UI信息
     */
    renderUI() {
        // 分数
        this.ctx.fillStyle = COLORS.SCORE;
        this.ctx.font = 'bold 24px Arial';
        this.ctx.textAlign = 'left';
        this.ctx.textBaseline = 'top';
        this.ctx.fillText(`分数: ${this.score}`, 20, 20);

        // 存活敌人数
        this.ctx.fillStyle = COLORS.TEXT;
        this.ctx.font = '16px Arial';
        this.ctx.fillText(`敌人: ${this.enemies.length}`, 20, 55);

        // 操作提示
        this.ctx.font = '14px Arial';
        this.ctx.textAlign = 'right';
        this.ctx.fillText('方向键/WASD 移动 | 自动射击', CANVAS_WIDTH - 20, 20);
    }

    /**
     * 渲染游戏结束画面
     */
    renderGameOver() {
        // 半透明遮罩
        this.ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
        this.ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 游戏结束文字
        this.ctx.fillStyle = COLORS.GAME_OVER;
        this.ctx.font = 'bold 56px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.textBaseline = 'middle';
        this.ctx.fillText('游戏结束!', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 60);

        // 分数显示
        this.ctx.fillStyle = COLORS.SCORE;
        this.ctx.font = 'bold 32px Arial';
        this.ctx.fillText(`最终分数: ${this.score}`, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 10);

        // 重新开始提示
        this.ctx.fillStyle = COLORS.TEXT;
        this.ctx.font = '20px Arial';
        this.ctx.fillText('按 R 键重新开始', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 70);
    }

    /**
     * 重新开始游戏
     */
    restart() {
        // 重置游戏对象
        this.player = new Player();
        this.bullets = [];
        this.enemies = [];

        // 重置分数
        this.score = 0;

        // 重置游戏状态
        this.state = GAME_STATE.PLAYING;
    }
}
