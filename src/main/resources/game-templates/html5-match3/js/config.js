/**
 * 三消游戏配置文件
 * 包含所有游戏常量和配置参数
 */

// 使用 IIFE 避免全局污染
const GameConfig = (function() {
    'use strict';

    // 网格配置
    const GRID_ROWS = 8;                    // 网格行数
    const GRID_COLS = 8;                    // 网格列数
    const BLOCK_SIZE = 60;                  // 方块尺寸（像素）
    const BLOCK_GAP = 4;                    // 方块间距（像素）

    // 方块类型配置
    const BLOCK_TYPES = 6;                  // 方块类型数量

    // 方块颜色配置 - 使用鲜艳的颜色便于区分
    const COLORS = [
        '#FF6B6B',   // 红色
        '#4ECDC4',   // 青色
        '#45B7D1',   // 蓝色
        '#96CEB4',   // 绿色
        '#FFEAA7',   // 黄色
        '#DDA0DD'    // 紫色
    ];

    // 方块高亮颜色（用于选中效果）
    const COLORS_HIGHLIGHT = [
        '#FF8E8E',   // 浅红
        '#6EDDD4',   // 浅青
        '#65C7E1',   // 浅蓝
        '#B6DEC4',   // 浅绿
        '#FFF4C7',   // 浅黄
        '#EABFEA'    // 浅紫
    ];

    // 方块深色（用于阴影效果）
    const COLORS_DARK = [
        '#CC5555',   // 深红
        '#3EA3A3',   // 深青
        '#3597B1',   // 深蓝
        '#76AE94',   // 深绿
        '#CCBB77',   // 深黄
        '#B080B0'    // 深紫
    ];

    // 动画配置
    const ANIMATION_SPEED = 15;             // 消除动画速度（毫秒/帧）
    const SWAP_SPEED = 200;                 // 交换动画时长（毫秒）
    const DROP_SPEED = 150;                 // 下落动画时长（毫秒）
    const MATCH_FLASH_DURATION = 300;       // 匹配闪烁时长（毫秒）

    // 游戏规则配置
    const MIN_MATCH = 3;                    // 最小匹配数量
    const BASE_SCORE = 10;                  // 基础分数（每个方块）
    const COMBO_MULTIPLIER = 1.5;           // 连击分数倍率
    const MAX_COMBO = 10;                   // 最大连击数

    // 画布配置
    const CANVAS_PADDING = 20;              // 画布内边距
    const CANVAS_WIDTH = GRID_COLS * (BLOCK_SIZE + BLOCK_GAP) + CANVAS_PADDING * 2;
    const CANVAS_HEIGHT = GRID_ROWS * (BLOCK_SIZE + BLOCK_GAP) + CANVAS_PADDING * 2;

    // 方块圆角配置
    const BLOCK_RADIUS = 10;                // 方块圆角半径

    // 返回公共配置
    return {
        // 网格
        GRID_ROWS: GRID_ROWS,
        GRID_COLS: GRID_COLS,
        BLOCK_SIZE: BLOCK_SIZE,
        BLOCK_GAP: BLOCK_GAP,

        // 方块
        BLOCK_TYPES: BLOCK_TYPES,
        COLORS: COLORS,
        COLORS_HIGHLIGHT: COLORS_HIGHLIGHT,
        COLORS_DARK: COLORS_DARK,

        // 动画
        ANIMATION_SPEED: ANIMATION_SPEED,
        SWAP_SPEED: SWAP_SPEED,
        DROP_SPEED: DROP_SPEED,
        MATCH_FLASH_DURATION: MATCH_FLASH_DURATION,

        // 规则
        MIN_MATCH: MIN_MATCH,
        BASE_SCORE: BASE_SCORE,
        COMBO_MULTIPLIER: COMBO_MULTIPLIER,
        MAX_COMBO: MAX_COMBO,

        // 画布
        CANVAS_PADDING: CANVAS_PADDING,
        CANVAS_WIDTH: CANVAS_WIDTH,
        CANVAS_HEIGHT: CANVAS_HEIGHT,
        BLOCK_RADIUS: BLOCK_RADIUS
    };
})();
