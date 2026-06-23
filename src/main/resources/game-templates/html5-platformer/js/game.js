/**
 * 平台跳跃游戏 - 游戏主类
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

        // 玩家实例
        this.player = new Player(50, CANVAS_HEIGHT - 100);

        // 关卡实例
        this.level = new Level();

        // 按键状态
        this.keys = {};

        // 时间相关
        this.lastTime = 0;
        this.animationId = null;

        // 绑定事件处理器
        this.bindEvents();
    }

    /**
     * 绑定键盘事件
     */
    bindEvents() {
        // 按键按下
        document.addEventListener('keydown', (e) => {
            this.keys[e.code] = true;

            // 跳跃按键
            if ((e.code === 'ArrowUp' || e.code === 'Space' || e.code === 'KeyW') &&
                this.state === GAME_STATE.PLAYING) {
                this.player.jump();
            }

            // 重新开始
            if (e.code === 'KeyR' && this.state !== GAME_STATE.PLAYING) {
                this.restart();
            }

            // 阻止默认行为（防止滚动）
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
        this.loop(this.lastTime);
    }

    /**
     * 游戏主循环
     * 使用 requestAnimationFrame 实现平滑动画
     * @param {number} timestamp - 当前时间戳
     */
    loop(timestamp) {
        // 计算时间增量（秒）
        const dt = (timestamp - this.lastTime) / 1000;
        this.lastTime = timestamp;

        // 更新游戏状态
        if (this.state === GAME_STATE.PLAYING) {
            this.update(dt);
        }

        // 渲染
        this.render();

        // 继续循环
        this.animationId = requestAnimationFrame((ts) => this.loop(ts));
    }

    /**
     * 更新游戏逻辑
     * 处理物理、碰撞和收集
     * @param {number} dt - 时间增量
     */
    update(dt) {
        // 更新玩家
        this.player.update(dt, this.keys, this.level.getPlatforms());

        // 检测金币收集
        for (const coin of this.level.getCoins()) {
            if (!coin.collected && this.player.collectCoin(coin)) {
                coin.collected = true;
                this.player.score += COIN_SCORE;
            }
        }

        // 检查游戏结束条件
        this.checkGameOver();

        // 检查胜利条件
        this.checkWin();
    }

    /**
     * 渲染游戏画面
     */
    render() {
        // 清空画布
        this.ctx.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 渲染关卡（背景、平台、金币）
        this.level.render(this.ctx);

        // 渲染玩家
        this.player.render(this.ctx);

        // 渲染UI（分数、状态）
        this.renderUI();

        // 渲染游戏结束画面
        if (this.state === GAME_STATE.GAME_OVER) {
            this.renderGameOver();
        } else if (this.state === GAME_STATE.WIN) {
            this.renderWin();
        }
    }

    /**
     * 渲染UI信息
     * 显示分数和剩余金币
     */
    renderUI() {
        // 分数
        this.ctx.fillStyle = COLORS.TEXT;
        this.ctx.font = 'bold 20px Arial';
        this.ctx.textAlign = 'left';
        this.ctx.textBaseline = 'top';
        this.ctx.fillText(`分数: ${this.player.score}`, 20, 20);

        // 剩余金币
        const remaining = this.level.getRemainingCoins();
        this.ctx.fillText(`剩余金币: ${remaining}`, 20, 50);

        // 操作提示
        this.ctx.font = '14px Arial';
        this.ctx.fillStyle = '#666';
        this.ctx.textAlign = 'right';
        this.ctx.fillText('← → 移动 | ↑/空格 跳跃', CANVAS_WIDTH - 20, 20);
    }

    /**
     * 检查游戏结束条件
     * 玩家掉出屏幕底部则游戏结束
     */
    checkGameOver() {
        if (this.player.y > CANVAS_HEIGHT) {
            this.state = GAME_STATE.GAME_OVER;
        }
    }

    /**
     * 检查胜利条件
     * 收集所有金币则胜利
     */
    checkWin() {
        if (this.level.getRemainingCoins() === 0) {
            this.state = GAME_STATE.WIN;
        }
    }

    /**
     * 渲染游戏结束画面
     */
    renderGameOver() {
        // 半透明遮罩
        this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        this.ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 游戏结束文字
        this.ctx.fillStyle = COLORS.GAME_OVER;
        this.ctx.font = 'bold 48px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.textBaseline = 'middle';
        this.ctx.fillText('游戏结束!', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 50);

        // 分数显示
        this.ctx.fillStyle = '#FFFFFF';
        this.ctx.font = '24px Arial';
        this.ctx.fillText(`最终分数: ${this.player.score}`, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 10);

        // 重新开始提示
        this.ctx.font = '18px Arial';
        this.ctx.fillText('按 R 键重新开始', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 60);
    }

    /**
     * 渲染胜利画面
     */
    renderWin() {
        // 半透明遮罩
        this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        this.ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 胜利文字
        this.ctx.fillStyle = '#FFD700';
        this.ctx.font = 'bold 48px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.textBaseline = 'middle';
        this.ctx.fillText('恭喜通关!', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 50);

        // 分数显示
        this.ctx.fillStyle = '#FFFFFF';
        this.ctx.font = '24px Arial';
        this.ctx.fillText(`最终分数: ${this.player.score}`, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 10);

        // 重新开始提示
        this.ctx.font = '18px Arial';
        this.ctx.fillText('按 R 键重新开始', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 60);
    }

    /**
     * 重新开始游戏
     * 重置玩家和关卡状态
     */
    restart() {
        // 重置玩家位置和分数
        this.player = new Player(50, CANVAS_HEIGHT - 100);

        // 重置关卡
        this.level.reset();

        // 重置游戏状态
        this.state = GAME_STATE.PLAYING;
    }
}
