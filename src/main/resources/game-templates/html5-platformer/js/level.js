/**
 * 平台跳跃游戏 - 关卡类
 * 管理关卡数据，包括平台、金币和渲染
 */

class Level {
    /**
     * 构造函数 - 初始化关卡数据
     */
    constructor() {
        // 平台数组 - 每个平台包含 x, y, width, height
        this.platforms = [];

        // 金币数组 - 每个金币包含 x, y, radius, collected
        this.coins = [];

        // 初始化关卡
        this.init();
    }

    /**
     * 初始化关卡
     * 创建平台和金币的初始布局
     */
    init() {
        // 清空数据
        this.platforms = [];
        this.coins = [];

        // 地面平台（贯穿整个屏幕底部）
        this.platforms.push({
            x: 0,
            y: CANVAS_HEIGHT - 40,
            width: CANVAS_WIDTH,
            height: 40
        });

        // 浮空平台
        const floatingPlatforms = [
            { x: 100, y: 380, width: 150, height: 20 },
            { x: 350, y: 300, width: 120, height: 20 },
            { x: 550, y: 220, width: 150, height: 20 },
            { x: 200, y: 150, width: 100, height: 20 },
            { x: 450, y: 100, width: 130, height: 20 },
            { x: 650, y: 350, width: 100, height: 20 },
        ];

        this.platforms.push(...floatingPlatforms);

        // 金币位置 - 分散在各平台上
        const coinPositions = [
            { x: 175, y: 350 },   // 第一个平台上
            { x: 410, y: 270 },   // 第二个平台上
            { x: 625, y: 190 },   // 第三个平台上
            { x: 250, y: 120 },   // 第四个平台上
            { x: 515, y: 70 },    // 第五个平台上
            { x: 700, y: 320 },   // 第六个平台上
            { x: 300, y: 430 },   // 地面上
            { x: 500, y: 430 },   // 地面上
        ];

        // 创建金币对象
        for (const pos of coinPositions) {
            this.coins.push({
                x: pos.x,
                y: pos.y,
                radius: COIN_RADIUS,
                collected: false
            });
        }
    }

    /**
     * 获取平台列表
     * @returns {Array} 平台数组
     */
    getPlatforms() {
        return this.platforms;
    }

    /**
     * 获取金币列表
     * @returns {Array} 金币数组
     */
    getCoins() {
        return this.coins;
    }

    /**
     * 获取未收集的金币数量
     * @returns {number} 未收集金币数量
     */
    getRemainingCoins() {
        return this.coins.filter(coin => !coin.collected).length;
    }

    /**
     * 渲染关卡
     * 绘制背景、平台和金币
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     */
    render(ctx) {
        // 绘制背景（天空）
        ctx.fillStyle = COLORS.SKY;
        ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 绘制背景装饰（云朵）
        this.drawClouds(ctx);

        // 绘制平台
        for (const platform of this.platforms) {
            this.drawPlatform(ctx, platform);
        }

        // 绘制金币
        for (const coin of this.coins) {
            if (!coin.collected) {
                this.drawCoin(ctx, coin);
            }
        }
    }

    /**
     * 绘制云朵装饰
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     */
    drawClouds(ctx) {
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';

        // 云朵1
        this.drawCloud(ctx, 100, 50, 40);
        this.drawCloud(ctx, 130, 40, 35);
        this.drawCloud(ctx, 160, 50, 40);

        // 云朵2
        this.drawCloud(ctx, 500, 80, 35);
        this.drawCloud(ctx, 530, 70, 30);
        this.drawCloud(ctx, 555, 80, 35);

        // 云朵3
        this.drawCloud(ctx, 700, 40, 30);
        this.drawCloud(ctx, 725, 35, 25);
        this.drawCloud(ctx, 745, 45, 30);
    }

    /**
     * 绘制单个云朵
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     * @param {number} x - x坐标
     * @param {number} y - y坐标
     * @param {number} radius - 云朵半径
     */
    drawCloud(ctx, x, y, radius) {
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.fill();
    }

    /**
     * 绘制平台
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     * @param {Object} platform - 平台对象
     */
    drawPlatform(ctx, platform) {
        // 平台主体
        ctx.fillStyle = COLORS.PLATFORM;
        ctx.fillRect(platform.x, platform.y, platform.width, platform.height);

        // 平台顶部高光
        ctx.fillStyle = '#A0522D';
        ctx.fillRect(platform.x, platform.y, platform.width, 4);

        // 平台边框
        ctx.strokeStyle = '#654321';
        ctx.lineWidth = 1;
        ctx.strokeRect(platform.x, platform.y, platform.width, platform.height);
    }

    /**
     * 绘制金币
     * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
     * @param {Object} coin - 金币对象
     */
    drawCoin(ctx, coin) {
        // 金币外圈（边框）
        ctx.fillStyle = COLORS.COIN_BORDER;
        ctx.beginPath();
        ctx.arc(coin.x, coin.y, coin.radius, 0, Math.PI * 2);
        ctx.fill();

        // 金币内圈
        ctx.fillStyle = COLORS.COIN;
        ctx.beginPath();
        ctx.arc(coin.x, coin.y, coin.radius - 2, 0, Math.PI * 2);
        ctx.fill();

        // 金币符号 "$"
        ctx.fillStyle = COLORS.COIN_BORDER;
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('$', coin.x, coin.y);
    }

    /**
     * 重置关卡
     * 将所有金币标记为未收集
     */
    reset() {
        for (const coin of this.coins) {
            coin.collected = false;
        }
    }
}
