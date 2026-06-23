/**
 * 三消游戏输入处理器类
 * 负责处理用户的鼠标和触摸输入
 */

class InputHandler {
    /**
     * 构造函数
     * @param {HTMLCanvasElement} canvas - Canvas 元素
     */
    constructor(canvas) {
        this.canvas = canvas;

        // 选中的方块
        this.selectedBlock = null;

        // 回调函数
        this.onSwapCallback = null;
        this.onSelectCallback = null;
        this.onRestartCallback = null;

        // 是否正在处理动画
        this.isAnimating = false;

        // 绑定事件
        this.bindEvents();
    }

    /**
     * 绑定事件监听器
     */
    bindEvents() {
        // 鼠标事件
        this.canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
        this.canvas.addEventListener('mouseup', (e) => this.onMouseUp(e));

        // 触摸事件（移动端支持）
        this.canvas.addEventListener('touchstart', (e) => {
            e.preventDefault();
            const touch = e.touches[0];
            this.onMouseDown(touch);
        });

        this.canvas.addEventListener('touchend', (e) => {
            e.preventDefault();
            const touch = e.changedTouches[0];
            this.onMouseUp(touch);
        });

        // 防止触摸时页面滚动
        this.canvas.addEventListener('touchmove', (e) => {
            e.preventDefault();
        });
    }

    /**
     * 鼠标按下事件处理
     * @param {Event} e - 鼠标事件对象
     */
    onMouseDown(e) {
        // 如果正在播放动画，忽略输入
        if (this.isAnimating) {
            return;
        }

        const pos = this.getMousePos(e);
        const gridPos = this.getGridPos(pos.x, pos.y);

        if (gridPos) {
            if (this.selectedBlock) {
                // 已经有选中的方块，检查是否点击了相邻方块
                const dr = Math.abs(this.selectedBlock.row - gridPos.row);
                const dc = Math.abs(this.selectedBlock.col - gridPos.col);

                if ((dr === 1 && dc === 0) || (dr === 0 && dc === 1)) {
                    // 点击了相邻方块，触发交换
                    if (this.onSwapCallback) {
                        this.onSwapCallback(
                            this.selectedBlock.row,
                            this.selectedBlock.col,
                            gridPos.row,
                            gridPos.col
                        );
                    }
                    this.selectedBlock = null;
                } else if (dr === 0 && dc === 0) {
                    // 点击了同一个方块，取消选中
                    this.selectedBlock = null;
                    if (this.onSelectCallback) {
                        this.onSelectCallback(null);
                    }
                } else {
                    // 点击了不相邻的方块，重新选中
                    this.selectedBlock = gridPos;
                    if (this.onSelectCallback) {
                        this.onSelectCallback(gridPos);
                    }
                }
            } else {
                // 没有选中的方块，选中当前点击的方块
                this.selectedBlock = gridPos;
                if (this.onSelectCallback) {
                    this.onSelectCallback(gridPos);
                }
            }
        } else {
            // 点击了棋盘外，取消选中
            this.selectedBlock = null;
            if (this.onSelectCallback) {
                this.onSelectCallback(null);
            }
        }
    }

    /**
     * 鼠标释放事件处理
     * @param {Event} e - 鼠标事件对象
     */
    onMouseUp(e) {
        // 目前不需要处理释放事件
        // 可以在这里添加拖拽交换的支持
    }

    /**
     * 获取鼠标在 Canvas 上的位置
     * @param {Event} e - 鼠标事件对象
     * @returns {Object} {x, y} Canvas 坐标
     */
    getMousePos(e) {
        const rect = this.canvas.getBoundingClientRect();
        const scaleX = this.canvas.width / rect.width;
        const scaleY = this.canvas.height / rect.height;

        return {
            x: (e.clientX - rect.left) * scaleX,
            y: (e.clientY - rect.top) * scaleY
        };
    }

    /**
     * 将 Canvas 坐标转换为网格位置
     * @param {number} x - Canvas X 坐标
     * @param {number} y - Canvas Y 坐标
     * @returns {Object|null} {row, col} 网格位置，无效位置返回 null
     */
    getGridPos(x, y) {
        const padding = GameConfig.CANVAS_PADDING;
        const blockSize = GameConfig.BLOCK_SIZE;
        const gap = GameConfig.BLOCK_GAP;

        // 计算网格坐标
        const col = Math.floor((x - padding) / (blockSize + gap));
        const row = Math.floor((y - padding) / (blockSize + gap));

        // 检查是否在有效范围内
        if (row >= 0 && row < GameConfig.GRID_ROWS &&
            col >= 0 && col < GameConfig.GRID_COLS) {

            // 检查是否在方块内部（排除间隙）
            const blockX = padding + col * (blockSize + gap);
            const blockY = padding + row * (blockSize + gap);

            if (x >= blockX && x <= blockX + blockSize &&
                y >= blockY && y <= blockY + blockSize) {
                return { row, col };
            }
        }

        return null;
    }

    /**
     * 设置交换回调函数
     * @param {Function} callback - 回调函数 (r1, c1, r2, c2)
     */
    onSwap(callback) {
        this.onSwapCallback = callback;
    }

    /**
     * 设置选中回调函数
     * @param {Function} callback - 回调函数 ({row, col} | null)
     */
    onSelect(callback) {
        this.onSelectCallback = callback;
    }

    /**
     * 设置重新开始按钮事件
     * @param {Function} callback - 回调函数
     */
    onRestart(callback) {
        this.onRestartCallback = callback;
        const restartBtn = document.getElementById('restart-btn');
        if (restartBtn) {
            restartBtn.addEventListener('click', () => {
                if (this.onRestartCallback) {
                    this.onRestartCallback();
                }
            });
        }
    }

    /**
     * 设置动画状态
     * @param {boolean} isAnimating - 是否正在播放动画
     */
    setAnimating(isAnimating) {
        this.isAnimating = isAnimating;
    }

    /**
     * 清除选中状态
     */
    clearSelection() {
        this.selectedBlock = null;
    }

    /**
     * 获取当前选中的方块
     * @returns {Object|null} {row, col} 或 null
     */
    getSelectedBlock() {
        return this.selectedBlock;
    }
}
