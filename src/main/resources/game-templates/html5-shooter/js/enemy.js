/**
 * 射击游戏 - 敌人类
 * 管理敌人的位置、移动和渲染
 */

class Enemy {
    /**
     * 构造函数 - 初始化敌人
     * @param {number} x - 初始x坐标
     * @param {number} y - 初始y坐标
     */
    constructor(x, y) {
        // 位置坐标
        this.x = x;
        this.y = y;

        // 尺寸
        this.size = ENEMY_SIZE;

        // 移动速度（略微随机化）
        this.speed = ENEMY_SPEED + Math.random() * 1;

        // 水平移动方向（用于左右摆动）
        this.vx = (Math.random() - 0.5) * 2;

        // 旋转角度
        this.angle = 0;
        this.rotationSpeed = (Math.random() - 0.5) * 0.1;

        // 存活状态
        this.alive = true;

        // 生命值
        this.health = 1;
    }

    /**
     * 更新敌人状态
     * 向下移动并左右摆动
     */
    update() {
        // 向下移动
        this.y += this.speed;

        // 左右摆动
        this.x += this.vx;

        // 边界反弹
        if (this.x < this.size || this.x > CANVAS_WIDTH - this.size) {
            this.vx = -this.vx;
        }

        // 旋转
        this.angle += this.rotationSpeed;

        // 超出屏幕底部则标记为死亡
        if (this.y > CANVAS_HEIGHT + this.size) {
            this.alive = false;
        }
    }

    /**
     * 渲染敌人
     * 绘制红色方块（带旋转）
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     */
    render(ctx) {
        if (!this.alive) return;

        // 保存绘图状态
        ctx.save();

        // 移动到敌人位置并旋转
        ctx.translate(this.x, this.y);
        ctx.rotate(this.angle);

        // 绘制敌人主体（正方形）
        ctx.fillStyle = COLORS.ENEMY;
        ctx.fillRect(-this.size, -this.size, this.size * 2, this.size * 2);

        // 绘制边框
        ctx.strokeStyle = COLORS.ENEMY_ACCENT;
        ctx.lineWidth = 2;
        ctx.strokeRect(-this.size, -this.size, this.size * 2, this.size * 2);

        // 绘制内部装饰（X形）
        ctx.strokeStyle = COLORS.ENEMY_ACCENT;
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(-this.size * 0.5, -this.size * 0.5);
        ctx.lineTo(this.size * 0.5, this.size * 0.5);
        ctx.moveTo(this.size * 0.5, -this.size * 0.5);
        ctx.lineTo(-this.size * 0.5, this.size * 0.5);
        ctx.stroke();

        // 绘制中心点
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath();
        ctx.arc(0, 0, this.size * 0.2, 0, Math.PI * 2);
        ctx.fill();

        // 恢复绘图状态
        ctx.restore();
    }

    /**
     * 检测与子弹的碰撞
     * @param {Bullet} bullet - 子弹对象
     * @returns {boolean} 是否发生碰撞
     */
    collidesWith(bullet) {
        const dx = this.x - bullet.x;
        const dy = this.y - bullet.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.size + bullet.radius);
    }

    /**
     * 受到伤害
     * 减少生命值，生命值为0时标记为死亡
     */
    takeDamage() {
        this.health--;
        if (this.health <= 0) {
            this.alive = false;
        }
    }
}
