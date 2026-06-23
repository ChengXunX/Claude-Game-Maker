/**
 * 三消游戏主控制器类
 * 负责协调游戏各个组件，管理游戏流程
 */

class Game {
    /**
     * 构造函数
     */
    constructor() {
        // 获取 Canvas 元素
        this.canvas = document.getElementById('gameCanvas');

        // 初始化游戏组件
        this.board = new Board(GameConfig.GRID_ROWS, GameConfig.GRID_COLS);
        this.renderer = new Renderer(this.canvas);
        this.input = new InputHandler(this.canvas);

        // 游戏状态
        this.score = 0;
        this.level = 1;
        this.combo = 0;
        this.isRunning = false;
        this.isAnimating = false;

        // 动画相关
        this.animationQueue = [];
        this.currentAnimation = null;
        this.animationStartTime = 0;

        // 绑定回调
        this.input.onSwap((r1, c1, r2, c2) => this.handleSwap(r1, c1, r2, c2));
        this.input.onSelect((pos) => this.handleSelect(pos));
        this.input.onRestart(() => this.restart());

        // 游戏主循环
        this.lastTime = 0;
        this.animationFrameId = null;

        // 显示元素
        this.scoreElement = document.getElementById('score');
        this.levelElement = document.getElementById('level');
        this.messageElement = document.getElementById('message');
    }

    /**
     * 开始游戏
     */
    start() {
        this.isRunning = true;
        this.score = 0;
        this.level = 1;
        this.combo = 0;

        // 初始化棋盘
        this.board.init();

        // 确保初始状态有可用的移动
        while (!this.board.hasValidMoves()) {
            this.board.init();
        }

        // 更新显示
        this.updateScoreDisplay();
        this.updateLevelDisplay();

        // 启动游戏循环
        this.lastTime = performance.now();
        this.loop(this.lastTime);

        // 绘制初始状态
        this.render();
    }

    /**
     * 游戏主循环
     * @param {number} timestamp - 当前时间戳
     */
    loop(timestamp) {
        if (!this.isRunning) return;

        // 计算时间差
        const dt = timestamp - this.lastTime;
        this.lastTime = timestamp;

        // 更新游戏状态
        this.update(dt);

        // 渲染
        this.render();

        // 继续循环
        this.animationFrameId = requestAnimationFrame((t) => this.loop(t));
    }

    /**
     * 更新游戏状态
     * @param {number} dt - 时间差（毫秒）
     */
    update(dt) {
        // 处理动画队列
        if (this.currentAnimation) {
            const elapsed = performance.now() - this.animationStartTime;
            const duration = this.currentAnimation.duration;
            const progress = Math.min(elapsed / duration, 1);

            // 更新动画
            this.currentAnimation.progress = progress;

            // 检查动画是否完成
            if (progress >= 1) {
                this.currentAnimation.onComplete();
                this.currentAnimation = null;

                // 处理下一个动画
                this.processNextAnimation();
            }
        }
    }

    /**
     * 渲染游戏画面
     */
    render() {
        this.renderer.clear();
        this.renderer.drawGrid();

        if (this.currentAnimation) {
            // 播放当前动画
            this.renderCurrentAnimation();
        } else {
            // 正常绘制棋盘
            this.renderer.drawBoard(this.board);

            // 绘制选中效果
            const selected = this.input.getSelectedBlock();
            if (selected) {
                this.renderer.drawSelection(selected.row, selected.col);
            }
        }
    }

    /**
     * 渲染当前动画
     */
    renderCurrentAnimation() {
        const anim = this.currentAnimation;

        switch (anim.type) {
            case 'swap':
                this.renderer.drawBoard(this.board);
                this.renderer.drawSwapAnimation(
                    anim.block1,
                    anim.block2,
                    anim.progress,
                    anim.reverse
                );
                break;

            case 'match':
                this.renderer.drawBoard(this.board);
                this.renderer.drawEliminationAnimation(
                    anim.positions,
                    anim.progress
                );
                break;

            case 'drop':
                // 绘制不在下落中的方块
                this.renderer.drawBoard(this.board);
                this.renderer.drawDropAnimation(anim.drops, anim.progress);
                break;

            case 'fill':
                this.renderer.drawBoard(this.board);
                this.renderer.drawNewBlockAnimation(anim.blocks, anim.progress);
                break;
        }
    }

    /**
     * 处理选中方块事件
     * @param {Object|null} pos - 选中位置 {row, col} 或 null
     */
    handleSelect(pos) {
        // 重新绘制以更新选中效果
        if (!this.isAnimating) {
            this.render();
        }
    }

    /**
     * 处理交换逻辑
     * @param {number} r1 - 第一个方块行号
     * @param {number} c1 - 第一个方块列号
     * @param {number} r2 - 第二个方块行号
     * @param {number} c2 - 第二个方块列号
     */
    handleSwap(r1, c1, r2, c2) {
        if (this.isAnimating) return;

        this.isAnimating = true;
        this.input.setAnimating(true);
        this.combo = 0;

        // 获取方块类型
        const type1 = this.board.getBlock(r1, c1);
        const type2 = this.board.getBlock(r2, c2);

        // 添加交换动画
        this.queueAnimation({
            type: 'swap',
            block1: { row: r1, col: c1, type: type1 },
            block2: { row: r2, col: c2, type: type2 },
            duration: GameConfig.SWAP_SPEED,
            progress: 0,
            reverse: false,
            onComplete: () => {
                // 执行实际交换
                this.board.swap(r1, c1, r2, c2);

                // 检查是否有匹配
                const matches = this.board.findMatches();

                if (matches.length > 0) {
                    // 有匹配，处理消除
                    this.processMatches();
                } else {
                    // 无匹配，交换回来
                    this.queueAnimation({
                        type: 'swap',
                        block1: { row: r1, col: c1, type: type2 },
                        block2: { row: r2, col: c2, type: type1 },
                        duration: GameConfig.SWAP_SPEED,
                        progress: 0,
                        reverse: true,
                        onComplete: () => {
                            // 交换回来
                            this.board.swap(r1, c1, r2, c2);
                            this.endTurn();
                        }
                    });
                }
            }
        });
    }

    /**
     * 处理匹配和消除
     */
    processMatches() {
        const matches = this.board.findMatches();

        if (matches.length === 0) {
            // 没有更多匹配，检查游戏状态
            this.checkGameState();
            return;
        }

        // 增加连击数
        this.combo++;

        // 计算分数
        let matchCount = 0;
        for (const match of matches) {
            matchCount += match.positions.length;
        }

        const baseScore = matchCount * GameConfig.BASE_SCORE;
        const comboMultiplier = Math.pow(GameConfig.COMBO_MULTIPLIER, Math.min(this.combo - 1, GameConfig.MAX_COMBO - 1));
        const totalScore = Math.floor(baseScore * comboMultiplier);

        // 更新分数
        this.score += totalScore;
        this.updateScoreDisplay();

        // 显示连击效果
        if (this.combo > 1) {
            this.showCombo(this.combo);
        }

        // 获取所有匹配的位置（用于动画）
        const allPositions = [];
        for (const match of matches) {
            for (const pos of match.positions) {
                allPositions.push({ ...pos, type: match.type });
            }
        }

        // 添加消除动画
        this.queueAnimation({
            type: 'match',
            positions: allPositions,
            duration: GameConfig.MATCH_FLASH_DURATION,
            progress: 0,
            onComplete: () => {
                // 执行消除
                this.board.eliminate(matches);

                // 处理下落
                this.processDrop();
            }
        });
    }

    /**
     * 处理方块下落
     */
    processDrop() {
        const drops = this.board.drop();

        if (drops.length === 0) {
            // 没有下落，直接填充
            this.processFill();
            return;
        }

        // 添加下落动画
        this.queueAnimation({
            type: 'drop',
            drops: drops,
            duration: GameConfig.DROP_SPEED,
            progress: 0,
            onComplete: () => {
                // 处理填充
                this.processFill();
            }
        });
    }

    /**
     * 处理填充新方块
     */
    processFill() {
        const newBlocks = this.board.fillEmpty();

        if (newBlocks.length === 0) {
            // 没有新方块，检查是否还有匹配
            this.processMatches();
            return;
        }

        // 添加填充动画
        this.queueAnimation({
            type: 'fill',
            blocks: newBlocks,
            duration: GameConfig.DROP_SPEED,
            progress: 0,
            onComplete: () => {
                // 检查是否形成新的匹配（连锁）
                this.processMatches();
            }
        });
    }

    /**
     * 检查游戏状态
     */
    checkGameState() {
        // 检查是否有可用的移动
        if (!this.board.hasValidMoves()) {
            // 没有可用的移动，重新洗牌
            this.shuffleBoard();
        } else {
            this.endTurn();
        }
    }

    /**
     * 重新洗牌
     */
    shuffleBoard() {
        this.showMessage('正在重新洗牌...');

        // 短暂延迟后重新初始化
        setTimeout(() => {
            this.board.init();

            // 确保有可用的移动
            while (!this.board.hasValidMoves()) {
                this.board.init();
            }

            this.hideMessage();
            this.endTurn();
        }, 1000);
    }

    /**
     * 结束当前回合
     */
    endTurn() {
        this.isAnimating = false;
        this.input.setAnimating(false);
        this.input.clearSelection();

        // 检查游戏结束条件（例如：达到目标分数）
        this.checkGameOver();
    }

    /**
     * 检查游戏是否结束
     */
    checkGameOver() {
        // 这里可以添加游戏结束条件
        // 例如：达到目标分数、时间限制等
        // 目前游戏不会自动结束
    }

    /**
     * 显示连击效果
     * @param {number} combo - 连击数
     */
    showCombo(combo) {
        const centerX = this.canvas.width / 2;
        const centerY = this.canvas.height / 2;

        // 在画布上绘制连击文字
        this.renderer.drawCombo(combo, centerX, centerY);

        // 短暂显示后继续游戏
        setTimeout(() => {
            // 清除连击显示（通过重新渲染）
        }, 500);
    }

    /**
     * 显示消息
     * @param {string} text - 消息文本
     */
    showMessage(text) {
        if (this.messageElement) {
            this.messageElement.textContent = text;
            this.messageElement.classList.remove('hidden');
        }
    }

    /**
     * 隐藏消息
     */
    hideMessage() {
        if (this.messageElement) {
            this.messageElement.classList.add('hidden');
        }
    }

    /**
     * 更新分数显示
     */
    updateScoreDisplay() {
        if (this.scoreElement) {
            this.scoreElement.textContent = this.score;

            // 添加分数增加动画
            this.scoreElement.classList.remove('score-increase');
            void this.scoreElement.offsetWidth; // 触发重绘
            this.scoreElement.classList.add('score-increase');
        }
    }

    /**
     * 更新关卡显示
     */
    updateLevelDisplay() {
        if (this.levelElement) {
            this.levelElement.textContent = this.level;
        }
    }

    /**
     * 将动画加入队列
     * @param {Object} animation - 动画对象
     */
    queueAnimation(animation) {
        if (!this.currentAnimation) {
            this.currentAnimation = animation;
            this.animationStartTime = performance.now();
        } else {
            this.animationQueue.push(animation);
        }
    }

    /**
     * 处理下一个动画
     */
    processNextAnimation() {
        if (this.animationQueue.length > 0) {
            this.currentAnimation = this.animationQueue.shift();
            this.animationStartTime = performance.now();
        }
    }

    /**
     * 重新开始游戏
     */
    restart() {
        // 停止当前动画
        this.animationQueue = [];
        this.currentAnimation = null;

        // 重置游戏状态
        this.score = 0;
        this.level = 1;
        this.combo = 0;
        this.isAnimating = false;

        // 重新初始化棋盘
        this.board.init();

        // 确保有可用的移动
        while (!this.board.hasValidMoves()) {
            this.board.init();
        }

        // 更新显示
        this.updateScoreDisplay();
        this.updateLevelDisplay();
        this.hideMessage();

        // 重置输入状态
        this.input.setAnimating(false);
        this.input.clearSelection();

        // 重新绘制
        this.render();
    }

    /**
     * 停止游戏
     */
    stop() {
        this.isRunning = false;

        if (this.animationFrameId) {
            cancelAnimationFrame(this.animationFrameId);
            this.animationFrameId = null;
        }
    }
}

// 游戏入口点
document.addEventListener('DOMContentLoaded', () => {
    // 创建游戏实例
    const game = new Game();

    // 开始游戏
    game.start();

    // 将游戏实例挂载到全局（调试用）
    window.game = game;
});
