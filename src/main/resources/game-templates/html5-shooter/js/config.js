/**
 * 射击游戏 - 配置常量
 * 定义游戏的各种参数和常量
 */

// 画布尺寸
const CANVAS_WIDTH = 800;   // 画布宽度（像素）
const CANVAS_HEIGHT = 600;  // 画布高度（像素）

// 玩家参数
const PLAYER_SPEED = 5;     // 玩家移动速度（像素/帧）
const PLAYER_SIZE = 30;     // 玩家尺寸（像素）

// 子弹参数
const BULLET_SPEED = 8;     // 子弹速度（像素/帧）
const BULLET_RADIUS = 3;    // 子弹半径（像素）
const FIRE_RATE = 200;      // 射击间隔（毫秒）

// 敌人参数
const ENEMY_SPEED = 2;      // 敌人移动速度（像素/帧）
const ENEMY_SIZE = 25;      // 敌人尺寸（像素）
const ENEMY_SPAWN_RATE = 1500; // 敌人生成间隔（毫秒）

// 分数参数
const SCORE_PER_ENEMY = 10; // 每个敌人的分数

// 颜色定义
const COLORS = {
    PLAYER: '#00BFFF',      // 玩家颜色（深天蓝）
    PLAYER_ACCENT: '#0099CC', // 玩家强调色
    BULLET: '#FFD700',      // 子弹颜色（金色）
    ENEMY: '#FF4444',       // 敌人颜色（红色）
    ENEMY_ACCENT: '#CC0000', // 敌人强调色
    BACKGROUND: '#0a0a2e',  // 背景色（深蓝）
    STAR: '#FFFFFF',        // 星星颜色
    TEXT: '#FFFFFF',         // 文字颜色
    SCORE: '#FFD700',       // 分数颜色（金色）
    GAME_OVER: '#FF0000'    // 游戏结束颜色（红色）
};

// 游戏状态
const GAME_STATE = {
    PLAYING: 'playing',     // 游戏进行中
    GAME_OVER: 'game_over'  // 游戏结束
};

// 星星背景参数
const STAR_COUNT = 100;     // 星星数量
const STAR_SPEED_MIN = 0.5; // 星星最小速度
const STAR_SPEED_MAX = 2;   // 星星最大速度
