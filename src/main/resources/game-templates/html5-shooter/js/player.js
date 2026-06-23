/**
 * 射击游戏 - 玩家类
 * 管理玩家的位置、移动和射击
 */

class Player {
    /**
     * 构造函数 - 初始化玩家
     */
    constructor() {
        // 位置坐标（屏幕底部中央）
        this.x = CANVAS_WIDTH / 2;
        this.y = CANVAS_HEIGHT - 50;

        // 尺寸
        this.size = PLAYER_SIZE;

        // 射击计时器
        this.lastFireTime = 0;

        // 存活状态
        this.alive = true;
    }

    /**
     * 更新玩家状态
     * 处理键盘输入，移动玩家
     * @param {Object} keys - 按键状态对象
     */
    update(keys) {
        // 水平移动
        if (keys['ArrowLeft'] || keys['KeyA']) {
            this.x -= PLAYER_SPEED;
        }
        if (keys['ArrowRight'] || keys['KeyD']) {
            this.x += PLAYER_SPEED;
        }

        // 垂直移动
        if (keys['ArrowUp'] || keys['KeyW']) {
            this.y -= PLAYER_SPEED;
        }
        if (keys['ArrowDown'] || keys['KeyS']) {
            this.y += PLAYER_SPEED;
        }

        // 边界检查（保持在屏幕内）
        if (this.x < this.size) this.x = this.size;
        if (this.x > CANVAS_WIDTH - this.size) this.x = CANVAS_WIDTH - this.size;
        if (this.y < this.size) this.y = this.size;
        if (this.y > CANVAS_HEIGHT - this.size) this.y = CANVAS_HEIGHT - this.size;
    }

    /**
     * 渲染玩家
     * 绘制三角形飞船
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     */
    render(ctx) {
        if (!this.alive) return;

        // 保存绘图状态
        ctx.save();

        // 绘制飞船主体（三角形）
        ctx.fillStyle = COLORS.PLAYER;
        ctx.beginPath();
        ctx.moveTo(this.x, this.y - this.size);  // 顶部
        ctx.lineTo(this.x - this.size * 0.8, this.y + this.size * 0.6);  // 左下
        ctx.lineTo(this.x + this.size * 0.8, this.y + this.size * 0.6);  // 右下
        ctx.closePath();
        ctx.fill();

        // 绘制飞船边框
        ctx.strokeStyle = COLORS.PLAYER_ACCENT;
        ctx.lineWidth = 2;
        ctx.stroke();

        // 绘制驾驶舱
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size * 0.25, 0, Math.PI * 2);
        ctx.fill();

        // 绘制引擎火焰
        ctx.fillStyle = '#FF6600';
        ctx.beginPath();
        ctx.moveTo(this.x - this.size * 0.3, this.y + this.size * 0.6);
        ctx.lineTo(this.x, this.y + this.size * 1.2);
        ctx.lineTo(this.x + this.size * 0.3, this.y + this.size * 0.6);
        ctx.closePath();
        ctx.fill();

        // 恢复绘图状态
        ctx.restore();
    }

    /**
     * 射击
     * 检查射击冷却时间，创建子弹
     * @param {number} currentTime - 当前时间
     * @returns {Bullet|null} 新创建的子弹，或null（冷却中）
     */
    shoot(currentTime) {
        // 检查射击冷却
        if (currentTime - this.lastFireTime < FIRE_RATE) {
            return null;
        }

        // 更新射击时间
        this.lastFireTime = currentTime;

        // 创建子弹（从飞船顶部发射）
        return new Bullet(this.x, this.y - this.size);
    }

    /**
     * 检测与敌人的碰撞
     * @param {Enemy} enemy - 敌人对象
     * @returns {boolean} 是否发生碰撞
     */
    collidesWith(enemy) {
        const dx = this.x - enemy.x;
        const dy = this.y - enemy.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.size + enemy.size) * 0.7;
    }
}
