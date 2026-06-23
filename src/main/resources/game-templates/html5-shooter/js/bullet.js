/**
 * 射击游戏 - 子弹类
 * 管理子弹的位置、移动和渲染
 */

class Bullet {
    /**
     * 构造函数 - 初始化子弹
     * @param {number} x - 初始x坐标
     * @param {number} y - 初始y坐标
     * @param {number} angle - 发射角度（弧度，默认向上）
     */
    constructor(x, y, angle = -Math.PI / 2) {
        // 位置坐标
        this.x = x;
        this.y = y;

        // 速度分量（根据角度计算）
        this.vx = Math.cos(angle) * BULLET_SPEED;
        this.vy = Math.sin(angle) * BULLET_SPEED;

        // 尺寸
        this.radius = BULLET_RADIUS;

        // 存活状态
        this.alive = true;
    }

    /**
     * 更新子弹位置
     * 根据速度移动子弹
     */
    update() {
        // 更新位置
        this.x += this.vx;
        this.y += this.vy;

        // 检查是否超出屏幕
        if (this.isOffScreen()) {
            this.alive = false;
        }
    }

    /**
     * 渲染子弹
     * 绘制发光的小圆点
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     */
    render(ctx) {
        if (!this.alive) return;

        // 外发光效果
        const gradient = ctx.createRadialGradient(
            this.x, this.y, 0,
            this.x, this.y, this.radius * 3
        );
        gradient.addColorStop(0, 'rgba(255, 215, 0, 0.8)');
        gradient.addColorStop(1, 'rgba(255, 215, 0, 0)');

        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius * 3, 0, Math.PI * 2);
        ctx.fill();

        // 子弹主体
        ctx.fillStyle = COLORS.BULLET;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fill();

        // 子弹中心高光
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius * 0.5, 0, Math.PI * 2);
        ctx.fill();
    }

    /**
     * 检查子弹是否超出屏幕
     * @returns {boolean} 是否超出屏幕
     */
    isOffScreen() {
        return (
            this.x < -this.radius ||
            this.x > CANVAS_WIDTH + this.radius ||
            this.y < -this.radius ||
            this.y > CANVAS_HEIGHT + this.radius
        );
    }

    /**
     * 检测与敌人的碰撞
     * @param {Enemy} enemy - 敌人对象
     * @returns {boolean} 是否发生碰撞
     */
    hits(enemy) {
        const dx = this.x - enemy.x;
        const dy = this.y - enemy.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.radius + enemy.size);
    }
}
