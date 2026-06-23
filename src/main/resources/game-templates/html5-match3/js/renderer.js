/**
 * 三消游戏渲染器类
 * 负责使用 Canvas 2D API 绘制游戏画面
 */

class Renderer {
    /**
     * 构造函数
     * @param {HTMLCanvasElement} canvas - Canvas 元素
     */
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');

        // 设置画布尺寸
        this.canvas.width = GameConfig.CANVAS_WIDTH;
        this.canvas.height = GameConfig.CANVAS_HEIGHT;

        // 动画相关
        this.animationFrame = null;
        this.particles = [];
    }

    /**
     * 清屏
     */
    clear() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    /**
     * 绘制整个棋盘
     * @param {Board} board - 棋盘对象
     */
    drawBoard(board) {
        this.clear();

        // 绘制背景网格
        this.drawGrid();

        // 绘制所有方块
        for (let row = 0; row < board.rows; row++) {
            for (let col = 0; col < board.cols; col++) {
                const type = board.getBlock(row, col);
                if (type !== -1) {
                    this.drawBlock(row, col, type);
                }
            }
        }
    }

    /**
     * 绘制背景网格
     */
    drawGrid() {
        const ctx = this.ctx;
        const padding = GameConfig.CANVAS_PADDING;
        const blockSize = GameConfig.BLOCK_SIZE;
        const gap = GameConfig.BLOCK_GAP;

        ctx.fillStyle = 'rgba(0, 0, 0, 0.05)';

        for (let row = 0; row < GameConfig.GRID_ROWS; row++) {
            for (let col = 0; col < GameConfig.GRID_COLS; col++) {
                const x = padding + col * (blockSize + gap);
                const y = padding + row * (blockSize + gap);

                // 绘制圆角矩形背景
                this.drawRoundRect(x, y, blockSize, blockSize, GameConfig.BLOCK_RADIUS);
                ctx.fill();
            }
        }
    }

    /**
     * 绘制单个方块
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @param {number} type - 方块类型
     * @param {number} [offsetX=0] - X 偏移量（用于动画）
     * @param {number} [offsetY=0] - Y 偏移量（用于动画）
     * @param {number} [alpha=1] - 透明度
     */
    drawBlock(row, col, type, offsetX = 0, offsetY = 0, alpha = 1) {
        const ctx = this.ctx;
        const padding = GameConfig.CANVAS_PADDING;
        const blockSize = GameConfig.BLOCK_SIZE;
        const gap = GameConfig.BLOCK_GAP;

        const x = padding + col * (blockSize + gap) + offsetX;
        const y = padding + row * (blockSize + gap) + offsetY;
        const radius = GameConfig.BLOCK_RADIUS;

        ctx.save();
        ctx.globalAlpha = alpha;

        // 绘制阴影
        ctx.shadowColor = 'rgba(0, 0, 0, 0.2)';
        ctx.shadowBlur = 6;
        ctx.shadowOffsetX = 2;
        ctx.shadowOffsetY = 2;

        // 绘制方块主体
        ctx.fillStyle = GameConfig.COLORS[type];
        this.drawRoundRect(x, y, blockSize, blockSize, radius);
        ctx.fill();

        // 移除阴影绘制高光
        ctx.shadowColor = 'transparent';
        ctx.shadowBlur = 0;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 0;

        // 绘制高光效果（左上角）
        const gradient = ctx.createLinearGradient(x, y, x + blockSize, y + blockSize);
        gradient.addColorStop(0, 'rgba(255, 255, 255, 0.4)');
        gradient.addColorStop(0.5, 'rgba(255, 255, 255, 0.1)');
        gradient.addColorStop(1, 'rgba(0, 0, 0, 0.1)');

        ctx.fillStyle = gradient;
        this.drawRoundRect(x + 2, y + 2, blockSize - 4, blockSize - 4, radius - 1);
        ctx.fill();

        // 绘制内部装饰（小圆形）
        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.beginPath();
        ctx.arc(x + blockSize * 0.3, y + blockSize * 0.3, blockSize * 0.12, 0, Math.PI * 2);
        ctx.fill();

        ctx.restore();
    }

    /**
     * 绘制选中效果
     * @param {number} row - 行号
     * @param {number} col - 列号
     */
    drawSelection(row, col) {
        const ctx = this.ctx;
        const padding = GameConfig.CANVAS_PADDING;
        const blockSize = GameConfig.BLOCK_SIZE;
        const gap = GameConfig.BLOCK_GAP;

        const x = padding + col * (blockSize + gap);
        const y = padding + row * (blockSize + gap);
        const radius = GameConfig.BLOCK_RADIUS;

        // 绘制高亮边框
        ctx.strokeStyle = '#FFD700';
        ctx.lineWidth = 4;
        ctx.shadowColor = '#FFD700';
        ctx.shadowBlur = 10;

        this.drawRoundRect(x - 2, y - 2, blockSize + 4, blockSize + 4, radius + 2);
        ctx.stroke();

        // 清除阴影
        ctx.shadowColor = 'transparent';
        ctx.shadowBlur = 0;

        // 绘制脉冲动画效果
        const time = Date.now() % 1000;
        const scale = 1 + Math.sin(time * Math.PI / 500) * 0.03;

        ctx.save();
        ctx.translate(x + blockSize / 2, y + blockSize / 2);
        ctx.scale(scale, scale);
        ctx.translate(-(x + blockSize / 2), -(y + blockSize / 2));

        ctx.strokeStyle = 'rgba(255, 215, 0, 0.5)';
        ctx.lineWidth = 2;
        this.drawRoundRect(x - 5, y - 5, blockSize + 10, blockSize + 10, radius + 3);
        ctx.stroke();

        ctx.restore();
    }

    /**
     * 绘制圆角矩形路径
     * @param {number} x - X 坐标
     * @param {number} y - Y 坐标
     * @param {number} width - 宽度
     * @param {number} height - 高度
     * @param {number} radius - 圆角半径
     */
    drawRoundRect(x, y, width, height, radius) {
        const ctx = this.ctx;
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + width - radius, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
        ctx.lineTo(x + width, y + height - radius);
        ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
        ctx.lineTo(x + radius, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
    }

    /**
     * 绘制分数
     * @param {number} score - 当前分数
     * @param {number} [x] - X 坐标（默认右上角）
     * @param {number} [y] - Y 坐标（默认右上角）
     */
    drawScore(score, x, y) {
        // 分数显示在 HTML 元素中，这里可以添加额外的画布效果
    }

    /**
     * 绘制连击效果
     * @param {number} combo - 连击数
     * @param {number} centerX - 中心 X 坐标
     * @param {number} centerY - 中心 Y 坐标
     */
    drawCombo(combo, centerX, centerY) {
        const ctx = this.ctx;

        ctx.save();

        // 连击文字
        const text = `${combo} 连击!`;
        const fontSize = 30 + combo * 2;

        ctx.font = `bold ${fontSize}px 'Microsoft YaHei', sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        // 绘制文字阴影
        ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
        ctx.fillText(text, centerX + 2, centerY + 2);

        // 绘制文字
        ctx.fillStyle = '#FFD700';
        ctx.strokeStyle = '#FF6B00';
        ctx.lineWidth = 3;
        ctx.strokeText(text, centerX, centerY);
        ctx.fillText(text, centerX, centerY);

        ctx.restore();
    }

    /**
     * 绘制消除动画
     * @param {Array} positions - 消除位置数组
     * @param {number} progress - 动画进度 (0-1)
     */
    drawEliminationAnimation(positions, progress) {
        const ctx = this.ctx;

        for (const pos of positions) {
            const type = pos.type;
            const scale = 1 - progress;
            const alpha = 1 - progress;

            // 计算缩放后的尺寸和位置
            const padding = GameConfig.CANVAS_PADDING;
            const blockSize = GameConfig.BLOCK_SIZE;
            const gap = GameConfig.BLOCK_GAP;

            const centerX = padding + pos.col * (blockSize + gap) + blockSize / 2;
            const centerY = padding + pos.row * (blockSize + gap) + blockSize / 2;

            const scaledSize = blockSize * scale;

            ctx.save();
            ctx.globalAlpha = alpha;
            ctx.translate(centerX, centerY);
            ctx.scale(scale, scale);

            // 绘制缩小的方块
            ctx.fillStyle = GameConfig.COLORS[type];
            this.drawRoundRect(-blockSize / 2, -blockSize / 2, blockSize, blockSize, GameConfig.BLOCK_RADIUS);
            ctx.fill();

            ctx.restore();

            // 绘制消除粒子效果
            this.drawEliminationParticles(centerX, centerY, type, progress);
        }
    }

    /**
     * 绘制消除粒子效果
     * @param {number} x - 中心 X 坐标
     * @param {number} y - 中心 Y 坐标
     * @param {number} type - 方块类型
     * @param {number} progress - 动画进度
     */
    drawEliminationParticles(x, y, type, progress) {
        const ctx = this.ctx;
        const particleCount = 8;
        const color = GameConfig.COLORS[type];

        for (let i = 0; i < particleCount; i++) {
            const angle = (i / particleCount) * Math.PI * 2;
            const distance = progress * 40;
            const particleX = x + Math.cos(angle) * distance;
            const particleY = y + Math.sin(angle) * distance;
            const particleSize = (1 - progress) * 6;

            ctx.save();
            ctx.globalAlpha = 1 - progress;
            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.arc(particleX, particleY, particleSize, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
        }
    }

    /**
     * 绘制交换动画
     * @param {Object} block1 - 第一个方块信息 {row, col, type}
     * @param {Object} block2 - 第二个方块信息 {row, col, type}
     * @param {number} progress - 动画进度 (0-1)
     * @param {boolean} isReverse - 是否是反向交换
     */
    drawSwapAnimation(block1, block2, progress, isReverse = false) {
        const padding = GameConfig.CANVAS_PADDING;
        const blockSize = GameConfig.BLOCK_SIZE;
        const gap = GameConfig.BLOCK_GAP;

        // 计算起始和结束位置
        const x1 = padding + block1.col * (blockSize + gap);
        const y1 = padding + block1.row * (blockSize + gap);
        const x2 = padding + block2.col * (blockSize + gap);
        const y2 = padding + block2.row * (blockSize + gap);

        // 使用缓动函数
        const easedProgress = this.easeInOutQuad(progress);

        if (isReverse) {
            // 反向交换
            const offsetX1 = (x2 - x1) * (1 - easedProgress);
            const offsetY1 = (y2 - y1) * (1 - easedProgress);
            const offsetX2 = (x1 - x2) * (1 - easedProgress);
            const offsetY2 = (y1 - y2) * (1 - easedProgress);

            this.drawBlock(block1.row, block1.col, block1.type, offsetX1, offsetY1);
            this.drawBlock(block2.row, block2.col, block2.type, offsetX2, offsetY2);
        } else {
            // 正向交换
            const offsetX1 = (x2 - x1) * easedProgress;
            const offsetY1 = (y2 - y1) * easedProgress;
            const offsetX2 = (x1 - x2) * easedProgress;
            const offsetY2 = (y1 - y2) * easedProgress;

            this.drawBlock(block1.row, block1.col, block1.type, offsetX1, offsetY1);
            this.drawBlock(block2.row, block2.col, block2.type, offsetX2, offsetY2);
        }
    }

    /**
     * 绘制下落动画
     * @param {Array} drops - 下落信息数组
     * @param {number} progress - 动画进度 (0-1)
     */
    drawDropAnimation(drops, progress) {
        const easedProgress = this.easeOutBounce(progress);

        for (const drop of drops) {
            const offsetY = (drop.toRow - drop.fromRow) *
                           (GameConfig.BLOCK_SIZE + GameConfig.BLOCK_GAP) *
                           easedProgress;

            this.drawBlock(drop.fromRow, drop.fromCol, drop.type, 0, offsetY);
        }
    }

    /**
     * 绘制新方块出现动画
     * @param {Array} newBlocks - 新方块信息数组
     * @param {number} progress - 动画进度 (0-1)
     */
    drawNewBlockAnimation(newBlocks, progress) {
        const easedProgress = this.easeOutBack(progress);

        for (const block of newBlocks) {
            const scale = easedProgress;
            const alpha = progress;

            const padding = GameConfig.CANVAS_PADDING;
            const blockSize = GameConfig.BLOCK_SIZE;
            const gap = GameConfig.BLOCK_GAP;

            const centerX = padding + block.col * (blockSize + gap) + blockSize / 2;
            const centerY = padding + block.row * (blockSize + gap) + blockSize / 2;

            const ctx = this.ctx;
            ctx.save();
            ctx.globalAlpha = alpha;
            ctx.translate(centerX, centerY);
            ctx.scale(scale, scale);
            ctx.translate(-centerX, -centerY);

            this.drawBlock(block.row, block.col, block.type);

            ctx.restore();
        }
    }

    /**
     * 绘制动画帧（通用）
     * @param {Board} board - 棋盘对象
     * @param {Array} animatingBlocks - 动画中的方块信息
     */
    drawAnimation(board, animatingBlocks) {
        this.clear();
        this.drawGrid();

        // 绘制不在动画中的方块
        for (let row = 0; row < board.rows; row++) {
            for (let col = 0; col < board.cols; col++) {
                const type = board.getBlock(row, col);
                if (type !== -1) {
                    const isAnimating = animatingBlocks.some(
                        b => b.row === row && b.col === col
                    );
                    if (!isAnimating) {
                        this.drawBlock(row, col, type);
                    }
                }
            }
        }
    }

    /**
     * 绘制游戏结束遮罩
     * @param {number} score - 最终分数
     */
    drawGameOver(score) {
        const ctx = this.ctx;

        // 绘制半透明遮罩
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // 绘制游戏结束文字
        ctx.fillStyle = '#FFFFFF';
        ctx.font = 'bold 48px "Microsoft YaHei", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('游戏结束', this.canvas.width / 2, this.canvas.height / 2 - 40);

        // 绘制最终分数
        ctx.font = 'bold 32px "Microsoft YaHei", sans-serif';
        ctx.fillStyle = '#FFD700';
        ctx.fillText(`最终分数: ${score}`, this.canvas.width / 2, this.canvas.height / 2 + 20);

        // 绘制重新开始提示
        ctx.font = '24px "Microsoft YaHei", sans-serif';
        ctx.fillStyle = '#CCCCCC';
        ctx.fillText('点击重新开始按钮', this.canvas.width / 2, this.canvas.height / 2 + 70);
    }

    /**
     * 绘制没有可用移动提示
     */
    drawNoMoves() {
        const ctx = this.ctx;

        // 绘制半透明遮罩
        ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
        ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // 绘制提示文字
        ctx.fillStyle = '#FF6B6B';
        ctx.font = 'bold 36px "Microsoft YaHei", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('没有可用的移动', this.canvas.width / 2, this.canvas.height / 2 - 20);

        ctx.font = '24px "Microsoft YaHei", sans-serif';
        ctx.fillStyle = '#CCCCCC';
        ctx.fillText('正在重新洗牌...', this.canvas.width / 2, this.canvas.height / 2 + 30);
    }

    /**
     * 缓动函数 - 二次缓入缓出
     * @param {number} t - 进度 (0-1)
     * @returns {number} 缓动后的值
     */
    easeInOutQuad(t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    /**
     * 缓动函数 - 弹性缓出
     * @param {number} t - 进度 (0-1)
     * @returns {number} 缓动后的值
     */
    easeOutBounce(t) {
        if (t < 1 / 2.75) {
            return 7.5625 * t * t;
        } else if (t < 2 / 2.75) {
            return 7.5625 * (t -= 1.5 / 2.75) * t + 0.75;
        } else if (t < 2.5 / 2.75) {
            return 7.5625 * (t -= 2.25 / 2.75) * t + 0.9375;
        } else {
            return 7.5625 * (t -= 2.625 / 2.75) * t + 0.984375;
        }
    }

    /**
     * 缓动函数 - 回弹缓出
     * @param {number} t - 进度 (0-1)
     * @returns {number} 缓动后的值
     */
    easeOutBack(t) {
        const c1 = 1.70158;
        const c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    /**
     * 获取指定网格位置的像素坐标
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @returns {Object} {x, y} 像素坐标
     */
    getPixelPos(row, col) {
        return {
            x: GameConfig.CANVAS_PADDING + col * (GameConfig.BLOCK_SIZE + GameConfig.BLOCK_GAP),
            y: GameConfig.CANVAS_PADDING + row * (GameConfig.BLOCK_SIZE + GameConfig.BLOCK_GAP)
        };
    }
}
