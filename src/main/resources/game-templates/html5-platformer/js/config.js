/**
 * 平台跳跃游戏 - 配置常量
 * 定义游戏的各种参数和常量
 */

// 画布尺寸
const CANVAS_WIDTH = 800;   // 画布宽度（像素）
const CANVAS_HEIGHT = 500;  // 画布高度（像素）

// 物理参数
const GRAVITY = 0.5;        // 重力加速度（像素/帧²）
const JUMP_FORCE = -12;     // 跳跃初速度（像素/帧，负值表示向上）

// 玩家参数
const PLAYER_SPEED = 5;     // 玩家移动速度（像素/帧）
const PLAYER_WIDTH = 30;    // 玩家宽度（像素）
const PLAYER_HEIGHT = 40;   // 玩家高度（像素）

// 地图参数
const TILE_SIZE = 32;       // 地图块尺寸（像素）

// 金币参数
const COIN_RADIUS = 10;     // 金币半径（像素）
const COIN_SCORE = 10;      // 每个金币的得分

// 颜色定义
const COLORS = {
    PLAYER: '#4CAF50',      // 玩家颜色（绿色）
    PLAYER_EYE: '#FFFFFF',  // 玩家眼睛颜色（白色）
    PLATFORM: '#8B4513',    // 平台颜色（棕色）
    COIN: '#FFD700',        // 金币颜色（金色）
    COIN_BORDER: '#DAA520', // 金币边框颜色
    SKY: '#87CEEB',         // 天空颜色（浅蓝色）
    GROUND: '#228B22',      // 地面颜色（绿色）
    TEXT: '#333333',         // 文字颜色
    GAME_OVER: '#FF0000'    // 游戏结束颜色（红色）
};

// 游戏状态
const GAME_STATE = {
    PLAYING: 'playing',     // 游戏进行中
    GAME_OVER: 'game_over', // 游戏结束
    WIN: 'win'              // 胜利
};
