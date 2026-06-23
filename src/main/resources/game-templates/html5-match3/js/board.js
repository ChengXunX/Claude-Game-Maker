/**
 * 三消游戏棋盘类
 * 负责管理游戏棋盘的状态和逻辑
 */

class Board {
    /**
     * 构造函数
     * @param {number} rows - 网格行数
     * @param {number} cols - 网格列数
     */
    constructor(rows, cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = [];
        this.init();
    }

    /**
     * 初始化棋盘
     * 创建网格并随机填充方块，确保初始状态无匹配
     */
    init() {
        this.grid = [];
        for (let row = 0; row < this.rows; row++) {
            this.grid[row] = [];
            for (let col = 0; col < this.cols; col++) {
                this.grid[row][col] = this.getRandomBlockType(row, col);
            }
        }
    }

    /**
     * 获取随机方块类型（避免初始匹配）
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @returns {number} 方块类型索引
     */
    getRandomBlockType(row, col) {
        let type;
        let attempts = 0;
        const maxAttempts = 50;

        do {
            type = Math.floor(Math.random() * GameConfig.BLOCK_TYPES);
            attempts++;
        } while (attempts < maxAttempts && this.wouldMatch(row, col, type));

        return type;
    }

    /**
     * 检查在指定位置放置方块是否会产生匹配
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @param {number} type - 方块类型
     * @returns {boolean} 是否会产生匹配
     */
    wouldMatch(row, col, type) {
        // 检查水平方向（左边两个）
        if (col >= 2) {
            if (this.grid[row][col - 1] === type &&
                this.grid[row][col - 2] === type) {
                return true;
            }
        }

        // 检查垂直方向（上面两个）
        if (row >= 2) {
            if (this.grid[row - 1] &&
                this.grid[row - 1][col] === type &&
                this.grid[row - 2] &&
                this.grid[row - 2][col] === type) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取指定位置的方块类型
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @returns {number|null} 方块类型，越界返回 null
     */
    getBlock(row, col) {
        if (this.isValidPos(row, col)) {
            return this.grid[row][col];
        }
        return null;
    }

    /**
     * 设置指定位置的方块类型
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @param {number} type - 方块类型
     */
    setBlock(row, col, type) {
        if (this.isValidPos(row, col)) {
            this.grid[row][col] = type;
        }
    }

    /**
     * 检查坐标是否有效
     * @param {number} row - 行号
     * @param {number} col - 列号
     * @returns {boolean} 是否有效
     */
    isValidPos(row, col) {
        return row >= 0 && row < this.rows && col >= 0 && col < this.cols;
    }

    /**
     * 交换两个方块
     * @param {number} r1 - 第一个方块行号
     * @param {number} c1 - 第一个方块列号
     * @param {number} r2 - 第二个方块行号
     * @param {number} c2 - 第二个方块列号
     */
    swap(r1, c1, r2, c2) {
        if (!this.isValidPos(r1, c1) || !this.isValidPos(r2, c2)) {
            return;
        }

        const temp = this.grid[r1][c1];
        this.grid[r1][c1] = this.grid[r2][c2];
        this.grid[r2][c2] = temp;
    }

    /**
     * 查找所有匹配的方块
     * @returns {Array} 匹配的方块数组，每个元素包含位置和类型信息
     */
    findMatches() {
        const matches = [];
        const matched = new Set();

        // 检查水平匹配
        for (let row = 0; row < this.rows; row++) {
            for (let col = 0; col < this.cols - 2; col++) {
                const type = this.grid[row][col];
                if (type === -1) continue;

                // 向右检查连续相同方块
                let matchLength = 1;
                while (col + matchLength < this.cols &&
                       this.grid[row][col + matchLength] === type) {
                    matchLength++;
                }

                // 如果匹配数量达到要求
                if (matchLength >= GameConfig.MIN_MATCH) {
                    const match = {
                        type: type,
                        positions: []
                    };

                    for (let i = 0; i < matchLength; i++) {
                        const pos = `${row},${col + i}`;
                        if (!matched.has(pos)) {
                            matched.add(pos);
                            match.positions.push({
                                row: row,
                                col: col + i
                            });
                        }
                    }

                    if (match.positions.length > 0) {
                        matches.push(match);
                    }

                    col += matchLength - 1;
                }
            }
        }

        // 检查垂直匹配
        for (let col = 0; col < this.cols; col++) {
            for (let row = 0; row < this.rows - 2; row++) {
                const type = this.grid[row][col];
                if (type === -1) continue;

                // 向下检查连续相同方块
                let matchLength = 1;
                while (row + matchLength < this.rows &&
                       this.grid[row + matchLength][col] === type) {
                    matchLength++;
                }

                // 如果匹配数量达到要求
                if (matchLength >= GameConfig.MIN_MATCH) {
                    const match = {
                        type: type,
                        positions: []
                    };

                    for (let i = 0; i < matchLength; i++) {
                        const pos = `${row + i},${col}`;
                        if (!matched.has(pos)) {
                            matched.add(pos);
                            match.positions.push({
                                row: row + i,
                                col: col
                            });
                        }
                    }

                    if (match.positions.length > 0) {
                        matches.push(match);
                    }

                    row += matchLength - 1;
                }
            }
        }

        return matches;
    }

    /**
     * 消除匹配的方块
     * @param {Array} matches - 匹配数组
     * @returns {number} 消除的方块数量
     */
    eliminate(matches) {
        let count = 0;

        for (const match of matches) {
            for (const pos of match.positions) {
                if (this.grid[pos.row][pos.col] !== -1) {
                    this.grid[pos.row][pos.col] = -1;
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 方块下落
     * @returns {Array} 下落信息数组，用于动画
     */
    drop() {
        const drops = [];

        // 逐列处理
        for (let col = 0; col < this.cols; col++) {
            let emptyRow = this.rows - 1;

            // 从底部向上扫描
            for (let row = this.rows - 1; row >= 0; row--) {
                if (this.grid[row][col] !== -1) {
                    // 如果需要下落
                    if (row !== emptyRow) {
                        drops.push({
                            fromRow: row,
                            fromCol: col,
                            toRow: emptyRow,
                            toCol: col,
                            type: this.grid[row][col]
                        });

                        this.grid[emptyRow][col] = this.grid[row][col];
                        this.grid[row][col] = -1;
                    }
                    emptyRow--;
                }
            }
        }

        return drops;
    }

    /**
     * 填充空位
     * @returns {Array} 新方块信息数组，用于动画
     */
    fillEmpty() {
        const newBlocks = [];

        for (let row = 0; row < this.rows; row++) {
            for (let col = 0; col < this.cols; col++) {
                if (this.grid[row][col] === -1) {
                    const type = Math.floor(Math.random() * GameConfig.BLOCK_TYPES);
                    this.grid[row][col] = type;
                    newBlocks.push({
                        row: row,
                        col: col,
                        type: type
                    });
                }
            }
        }

        return newBlocks;
    }

    /**
     * 判断交换是否有效（会产生匹配）
     * @param {number} r1 - 第一个方块行号
     * @param {number} c1 - 第一个方块列号
     * @param {number} r2 - 第二个方块行号
     * @param {number} c2 - 第二个方块列号
     * @returns {boolean} 是否有效
     */
    isValidSwap(r1, c1, r2, c2) {
        // 检查是否相邻
        const dr = Math.abs(r1 - r2);
        const dc = Math.abs(c1 - c2);
        if (!((dr === 1 && dc === 0) || (dr === 0 && dc === 1))) {
            return false;
        }

        // 临时交换
        this.swap(r1, c1, r2, c2);

        // 检查是否有匹配
        const matches = this.findMatches();

        // 交换回来
        this.swap(r1, c1, r2, c2);

        return matches.length > 0;
    }

    /**
     * 检查是否有可用的移动
     * @returns {boolean} 是否有可用移动
     */
    hasValidMoves() {
        // 尝试所有可能的交换
        for (let row = 0; row < this.rows; row++) {
            for (let col = 0; col < this.cols; col++) {
                // 尝试向右交换
                if (col < this.cols - 1 && this.isValidSwap(row, col, row, col + 1)) {
                    return true;
                }
                // 尝试向下交换
                if (row < this.rows - 1 && this.isValidSwap(row, col, row + 1, col)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取棋盘的字符串表示（调试用）
     * @returns {string} 棋盘字符串
     */
    toString() {
        let str = '';
        for (let row = 0; row < this.rows; row++) {
            str += this.grid[row].map(cell => cell === -1 ? '.' : cell).join(' ') + '\n';
        }
        return str;
    }
}
