/**
 * 平台跳跃游戏 - 玩家类
 * 管理玩家的位置、速度、渲染和碰撞检测
 */

class Player {
    /**
     * 构造函数 - 初始化玩家
     * @param {number} x - 初始x坐标
     * @param {number} y - 初始y坐标
     */
    constructor(x, y) {
        // 位置坐标
        this.x = x;
        this.y = y;

        // 速度
        this.vx = 0;  // 水平速度
        this.vy = 0;  // 垂直速度

        // 尺寸
        this.width = PLAYER_WIDTH;
        this.height = PLAYER_HEIGHT;

        // 状态标志
        this.onGround = false;  // 是否在地面上
        this.facing = 1;        // 朝向（1: 右, -1: 左）

        // 得分
        this.score = 0;
    }

    /**
     * 更新玩家状态
     * 处理输入、应用重力、更新位置
     * @param {number} dt - 时间增量（帧时间）
     * @param {Object} keys - 按键状态对象
     * @param {Array} platforms - 平台数组，用于碰撞检测
     */
    update(dt, keys, platforms) {
        // 处理水平输入
        this.vx = 0;
        if (keys['ArrowLeft'] || keys['KeyA']) {
            this.vx = -PLAYER_SPEED;
            this.facing = -1;
        }
        if (keys['ArrowRight'] || keys['KeyD']) {
            this.vx = PLAYER_SPEED;
            this.facing = 1;
        }

        // 应用重力
        this.vy += GRAVITY;

        // 更新水平位置
        this.x += this.vx;

        // 水平边界检查
        if (this.x < 0) this.x = 0;
        if (this.x + this.width > CANVAS_WIDTH) this.x = CANVAS_WIDTH - this.width;

        // 水平碰撞检测
        for (const platform of platforms) {
            if (this.collidesWith(platform)) {
                if (this.vx > 0) {
                    // 向右移动时，阻止穿入平台
                    this.x = platform.x - this.width;
                } else if (this.vx < 0) {
                    // 向左移动时，阻止穿入平台
                    this.x = platform.x + platform.width;
                }
                this.vx = 0;
            }
        }

        // 更新垂直位置
        this.y += this.vy;
        this.onGround = false;

        // 垂直碰撞检测
        for (const platform of platforms) {
            if (this.collidesWith(platform)) {
                if (this.vy > 0) {
                    // 下落时，站在平台上
                    this.y = platform.y - this.height;
                    this.vy = 0;
                    this.onGround = true;
                } else if (this.vy < 0) {
                    // 上升时，头撞平台
                    this.y = platform.y + platform.height;
                    this.vy = 0;
                }
            }
        }
    }

    /**
     * 跳跃
     * 只有在地面上才能跳跃
     */
    jump() {
        if (this.onGround) {
            this.vy = JUMP_FORCE;
            this.onGround = false;
        }
    }

    /**
     * 渲染玩家
     * 绘制玩家角色（矩形+眼睛）
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     */
    render(ctx) {
        // 绘制玩家身体
        ctx.fillStyle = COLORS.PLAYER;
        ctx.fillRect(this.x, this.y, this.width, this.height);

        // 绘制眼睛
        const eyeSize = 6;
        const eyeY = this.y + 10;

        // 左眼
        ctx.fillStyle = COLORS.PLAYER_EYE;
        ctx.beginPath();
        ctx.arc(this.x + this.width * 0.35, eyeY, eyeSize, 0, Math.PI * 2);
        ctx.fill();

        // 右眼
        ctx.beginPath();
        ctx.arc(this.x + this.width * 0.65, eyeY, eyeSize, 0, Math.PI * 2);
        ctx.fill();

        // 绘制瞳孔（根据朝向移动）
        ctx.fillStyle = '#000';
        const pupilOffset = this.facing * 2;
        ctx.beginPath();
        ctx.arc(this.x + this.width * 0.35 + pupilOffset, eyeY, 3, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(this.x + this.width * 0.65 + pupilOffset, eyeY, 3, 0, Math.PI * 2);
        ctx.fill();
    }

    /**
     * 碰撞检测
     * 检测玩家是否与平台发生碰撞
     * @param {Object} platform - 平台对象，包含 x, y, width, height
     * @returns {boolean} 是否发生碰撞
     */
    collidesWith(platform) {
        return (
            this.x < platform.x + platform.width &&
            this.x + this.width > platform.x &&
            this.y < platform.y + platform.height &&
            this.y + this.height > platform.y
        );
    }

    /**
     * 收集金币
     * 检测玩家是否与金币发生碰撞
     * @param {Object} coin - 金币对象，包含 x, y, radius
     * @returns {boolean} 是否收集到金币
     */
    collectCoin(coin) {
        const dx = (this.x + this.width / 2) - coin.x;
        const dy = (this.y + this.height / 2) - coin.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.width / 2 + coin.radius);
    }
}
