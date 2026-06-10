package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 知识库初始化器
 * 在系统启动时预置游戏开发最佳实践和常见问题解决方案
 * 让 Agent 在开发游戏时有知识可参考
 *
 * @author chengxun
 * @since 2.0.0
 */
@Component
public class KnowledgeBaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    @Autowired
    private GameKnowledgeBase knowledgeBase;

    /**
     * 系统启动后初始化知识库
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            initGameDesignPatterns();
            initCommonProblems();
            initTechStackGuides();
            log.info("游戏开发知识库预置完成");
        } catch (Exception e) {
            log.error("初始化知识库失败", e);
        }
    }

    /**
     * 游戏设计模式最佳实践
     */
    private void initGameDesignPatterns() {
        // 核心循环设计
        knowledgeBase.recordSolution("game_design", "核心循环设计",
            "游戏核心循环 = 输入 → 反馈 → 奖励 → 重复。" +
            "确保每次循环都有明确的玩家输入、即时反馈（视觉/音效）、适度奖励（分数/道具/解锁）。" +
            "循环时间控制在 30 秒到 2 分钟之间，太短容易无聊，太长容易疲劳。");

        // 难度曲线
        knowledgeBase.recordSolution("game_design", "难度曲线设计",
            "难度曲线应呈波浪式上升：Easy → Medium → Hard → Easy（奖励关）→ Harder。" +
            "每个新机制引入时降低难度让玩家学习，掌握后再提升。" +
            "前 30 秒必须让玩家成功一次，建立信心。");

        // 反馈系统
        knowledgeBase.recordSolution("game_design", "游戏反馈系统",
            "即时反馈是游戏好玩的关键：\n" +
            "1. 视觉反馈：消除时的粒子效果、得分时的数字弹出\n" +
            "2. 音效反馈：成功音效、失败音效、背景音乐变化\n" +
            "3. 触觉反馈：移动端震动（可选）\n" +
            "4. 进度反馈：分数、等级、进度条、成就解锁");

        // 关卡设计
        knowledgeBase.recordSolution("game_design", "关卡设计原则",
            "关卡设计三要素：目标、挑战、奖励。\n" +
            "- 目标：每个关卡有明确的胜利条件\n" +
            "- 挑战：逐步引入新机制，不要一次性堆砌\n" +
            "- 奖励：通关后给予有意义的奖励（新角色/新能力/剧情推进）\n" +
            "前 3 关作为教程关，难度极低，每关教一个新概念。");

        // UI/UX 设计
        knowledgeBase.recordSolution("game_design", "游戏 UI/UX 设计",
            "游戏 UI 原则：\n" +
            "1. 信息层次：最重要的信息最大最显眼\n" +
            "2. 操作反馈：按钮点击有动画和音效\n" +
            "3. 状态可见：始终显示当前分数、生命值、目标\n" +
            "4. 容错设计：重要操作有确认弹窗，支持撤销\n" +
            "5. 响应式：适配不同屏幕尺寸");
    }

    /**
     * 常见问题解决方案
     */
    private void initCommonProblems() {
        // 游戏卡顿
        knowledgeBase.recordSolution("performance", "游戏卡顿/掉帧",
            "常见原因和解决方案：\n" +
            "1. 渲染过多对象 → 使用对象池、视锥剔除、减少绘制调用\n" +
            "2. 频繁 GC → 减少临时对象创建，使用对象池\n" +
            "3. 物理计算过多 → 降低物理步频、使用简单碰撞体\n" +
            "4. 纹理过大 → 压缩纹理、使用图集\n" +
            "5. 过多定时器 → 合并定时器、使用 requestAnimationFrame");

        // 状态管理
        knowledgeBase.recordSolution("architecture", "游戏状态管理",
            "推荐使用状态机模式管理游戏状态：\n" +
            "MENU → PLAYING → PAUSED → GAME_OVER → MENU\n" +
            "每个状态有独立的 enter/update/exit 方法。" +
            "状态切换时清理前一个状态的资源，初始化新状态。" +
            "避免使用全局变量，通过状态机传递数据。");

        // 数据持久化
        knowledgeBase.recordSolution("architecture", "游戏数据持久化",
            "浏览器游戏使用 localStorage 保存进度：\n" +
            "1. 自动保存：每关通关后自动保存\n" +
            "2. 手动保存：提供保存按钮\n" +
            "3. 数据格式：JSON 序列化\n" +
            "4. 版本兼容：保存数据带版本号，支持迁移\n" +
            "5. 防作弊：关键数据加密存储（可选）");

        // 响应式适配
        knowledgeBase.recordSolution("frontend", "游戏响应式适配",
            "HTML5 游戏适配方案：\n" +
            "1. 使用 viewport meta 标签\n" +
            "2. Canvas 尺寸按比例缩放\n" +
            "3. 使用 rem/vw 单位做 UI\n" +
            "4. 监听 resize 事件重新计算布局\n" +
            "5. 提供横屏/竖屏两种布局（可选）");

        // 音效管理
        knowledgeBase.recordSolution("frontend", "游戏音效管理",
            "Web Audio API 音效管理：\n" +
            "1. 预加载音效文件\n" +
            "2. 使用 AudioContext 管理播放\n" +
            "3. 背景音乐和音效分开控制\n" +
            "4. 提供静音开关\n" +
            "5. 移动端需要用户交互后才能播放");
    }

    /**
     * 技术栈指南
     */
    private void initTechStackGuides() {
        // HTML5 Canvas 游戏
        knowledgeBase.recordSolution("tech_stack", "HTML5 Canvas 游戏开发",
            "技术选型：\n" +
            "- 渲染：原生 Canvas 2D 或 PixiJS\n" +
            "- 物理：Matter.js（简单）或 Planck.js（Box2D）\n" +
            "- 音频：Howler.js\n" +
            "- 构建：Vite（快速）\n" +
            "项目结构：src/game/（核心逻辑）、src/scenes/（场景）、src/entities/（实体）、src/assets/（资源）");

        // Vue + 游戏
        knowledgeBase.recordSolution("tech_stack", "Vue + 游戏混合开发",
            "适合：游戏 + 管理界面混合应用\n" +
            "- 游戏部分用 Canvas 独立渲染\n" +
            "- UI 部分用 Vue + Element Plus\n" +
            "- 通过事件总线通信\n" +
            "注意：Canvas 渲染循环和 Vue 的响应式系统要隔离，避免性能问题");

        // 项目目录规范
        knowledgeBase.recordSolution("tech_stack", "游戏项目目录规范",
            "推荐目录结构：\n" +
            "project/\n" +
            "  src/           # 源代码\n" +
            "    game/        # 游戏核心逻辑\n" +
            "    scenes/      # 场景/关卡\n" +
            "    entities/    # 游戏对象\n" +
            "    systems/     # 系统（输入、物理、渲染）\n" +
            "    utils/       # 工具函数\n" +
            "  assets/        # 资源文件\n" +
            "    images/      # 图片\n" +
            "    sounds/      # 音效\n" +
            "    data/        # 配置数据\n" +
            "  index.html     # 入口\n" +
            "  package.json   # 依赖配置");
    }
}
