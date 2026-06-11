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
            initPlayerPsychology();
            initGameFeel();
            initRetentionDesign();
            initRandomnessDesign();
            initAdvancedLevelDesign();
            initEconomySystems();
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
     * 玩家心理学知识
     * 让 Agent 理解为什么某些设计让游戏"好玩"
     */
    private void initPlayerPsychology() {
        knowledgeBase.recordSolution("psychology", "心流理论（Flow Theory）",
            "心流 = 完全沉浸、忘记时间的状态。触发条件：\n" +
            "1. 挑战与技能平衡：太简单→无聊，太难→焦虑\n" +
            "2. 明确目标：玩家随时知道该做什么\n" +
            "3. 即时反馈：每个动作都有可见结果\n" +
            "4. 控制感：玩家感觉是自己在主导\n" +
            "游戏设计应用：难度动态调整，让玩家始终处于'刚好够挑战'的区间。");

        knowledgeBase.recordSolution("psychology", "斯金纳箱与变比率强化",
            "行为心理学核心：不确定的奖励比固定奖励更让人上瘾。\n" +
            "- 固定比率（每10次给奖）→ 适合任务系统\n" +
            "- 变比率（平均10次但随机）→ 适合抽卡、掉落\n" +
            "- 变间隔（随机时间给奖）→ 适合每日惊喜\n" +
            "应用：不要让玩家预测奖励时机，保持期待感。但要有保底，避免无限挫败。");

        knowledgeBase.recordSolution("psychology", "损失厌恶与沉没成本",
            "人对'失去'的敏感度是'获得'的2倍。\n" +
            "游戏应用：\n" +
            "1. 已有进度即将失去 → 玩家更愿意付费/花时间保住\n" +
            "2. 限时活动制造'不参与就错过'的紧迫感\n" +
            "3. 连续登录奖励中断 → 损失厌恶驱动继续登录\n" +
            "注意：过度使用会让玩家感到被操纵，适度使用。");

        knowledgeBase.recordSolution("psychology", "蔡格尼克效应（未完成感）",
            "人对未完成的事记忆更深，完成后的满足感驱动继续。\n" +
            "游戏应用：\n" +
            "1. 进度条差一点就满 → 玩家不愿停下\n" +
            "2. 收集系统（99/100）→ 差一个的焦虑感\n" +
            "3. 章节结尾留悬念 → 驱动下一章\n" +
            "4. 每日任务差一个完成 → 催促回来做完");

        knowledgeBase.recordSolution("psychology", "自我决定理论（内在动机）",
            "内在动机三要素：自主性、胜任感、关联性。\n" +
            "- 自主性：让玩家有选择权，不要强制线性\n" +
            "- 胜任感：让玩家感觉'我变强了'，而非数值膨胀\n" +
            "- 关联性：多人互动、排行榜、公会系统\n" +
            "最好的游戏让玩家因为'想玩'而玩，不是'不得不玩'。");
    }

    /**
     * 游戏感觉（Juice）知识
     * 让游戏从'能玩'变成'好玩'的关键
     */
    private void initGameFeel() {
        knowledgeBase.recordSolution("game_feel", "Juice效果（游戏调味料）",
            "Juice = 让游戏手感爆炸的各种反馈效果。没有Juice的游戏像白开水。\n" +
            "基础Juice清单：\n" +
            "1. 屏幕震动：爆炸、受击时轻微shake\n" +
            "2. 顿帧（Hitlag）：击中敌人时暂停2-3帧，增强打击感\n" +
            "3. 粒子效果：消除、得分、升级时的粒子爆发\n" +
            "4. 缩放弹跳：UI元素出现时的弹性动画\n" +
            "5. 数字弹出：伤害数字、得分数字飞出\n" +
            "6. 慢动作：关键时刻的子弹时间\n" +
            "实现：所有效果都应该可叠加、可调节强度。");

        knowledgeBase.recordSolution("game_feel", "音效设计原则",
            "好音效 = 50%的游戏体验。设计原则：\n" +
            "1. 即时性：操作和音效必须同步，延迟>50ms就能感知\n" +
            "2. 层次感：背景音乐(低音量) + 环境音(中) + 交互音(高)\n" +
            "3. 变化性：同一动作多次触发时音调微变，避免机械感\n" +
            "4. 情绪匹配：紧张时音乐加速，胜利时音效明亮\n" +
            "5. 音效反馈：每个按钮点击都要有声音，没有声音=没点到\n" +
            "Web实现：使用Web Audio API，预加载音效，支持音量控制。");

        knowledgeBase.recordSolution("game_feel", "视觉反馈设计",
            "视觉反馈让玩家'看到'自己的操作效果：\n" +
            "1. 颜色变化：受击闪白、危险闪红、治疗闪绿\n" +
            "2. 形变效果：角色跳跃时拉伸、落地时压扁\n" +
            "3. 拖尾效果：快速移动时的残影\n" +
            "4. 镜头效果：攻击时镜头微微推进，Boss出现时镜头震动\n" +
            "5. UI动画：金币飞入背包、血条减少时的延迟动画\n" +
            "关键：所有反馈都要快（<100ms），夸张但不干扰操作。");

        knowledgeBase.recordSolution("game_feel", "操作手感优化",
            "操作手感 = 输入响应 + 物理反馈 + 视觉确认。\n" +
            "1. 输入缓冲：在动画未结束时按下一次，动画结束后自动执行\n" +
            "2. 土狼时间（Coyote Time）：离开平台后仍有0.1秒可跳跃\n" +
            "3. 辅助瞄准：射击方向自动微调，增加命中感\n" +
            "4. 输入预测：客户端先播放动画，再等服务端确认\n" +
            "5. 连招简化：连续攻击的输入窗口要宽松（>200ms）\n" +
            "核心原则：宁可让操作变得'太容易'，也不要'太难按'。");
    }

    /**
     * 留存设计知识
     */
    private void initRetentionDesign() {
        knowledgeBase.recordSolution("retention", "留存设计框架",
            "留存 = 让玩家回来。分三层：\n" +
            "1. 次日留存（D1）：新手引导必须丝滑，首次体验30秒内必须成功一次\n" +
            "2. 7日留存（D7）：每日奖励+解锁新内容+社交绑定\n" +
            "3. 30日留存（D30）：深度系统+公会+竞技+长期目标\n" +
            "关键指标：D1>40%, D7>15%, D30>5% 是及格线。");

        knowledgeBase.recordSolution("retention", "每日奖励系统",
            "每日奖励设计要点：\n" +
            "1. 递增奖励：第1天10金币，第7天100金币，断签重来\n" +
            "2. 里程碑奖励：连续7天额外大奖\n" +
            "3. 补签机制：允许花少量资源补签，减少挫败\n" +
            "4. 视觉反馈：签到动画、进度条、倒计时\n" +
            "5. 推送提醒：接近断签时提醒（可选）\n" +
            "注意：奖励必须有意义，1金币的每日奖励不如没有。");

        knowledgeBase.recordSolution("retention", "成就与收集系统",
            "成就系统驱动长期留存：\n" +
            "1. 分类：主线成就(剧情)、挑战成就(高难)、收集成就(全收集)\n" +
            "2. 进度可见：显示'47/100'而非只有'未完成'\n" +
            "3. 阶段奖励：每25%进度给小奖励，100%给大奖\n" +
            "4. 稀有度分级：普通/稀有/史诗/传说，带动炫耀心理\n" +
            "5. 隐藏成就：探索型玩家的惊喜\n" +
            "实现：成就弹出时要有仪式感（音效+动画+分享按钮）。");
    }

    /**
     * 随机与概率设计知识
     */
    private void initRandomnessDesign() {
        knowledgeBase.recordSolution("randomness", "保底机制设计",
            "抽卡/开箱必须有保底，否则玩家会感到被欺骗：\n" +
            "1. 硬保底：N次必出（如90抽必出SSR）\n" +
            "2. 软保底：接近保底时概率递增（如75抽后概率上升）\n" +
            "3. 天井机制：累计N次可自选（如300抽自选一个）\n" +
            "4. 概率公示：必须显示真实概率，法律要求\n" +
            "设计原则：让玩家感觉'再抽几次就出了'，而非'永远抽不到'。");

        knowledgeBase.recordSolution("randomness", "伪随机 vs 真随机",
            "真随机（Random）会产生极端情况，玩家体验差。\n" +
            "伪随机（Pseudo-Random Distribution, PRD）：\n" +
            "- 连续不出时概率递增，出了之后概率重置\n" +
            "- 实际概率和标称一致，但体验更平滑\n" +
            "- 适合：暴击、掉落、触发效果\n" +
            "实现：维护一个递增的C值，每次未触发时C增加，触发后重置。\n" +
            "暴击率20% → 实际期望5次攻击暴击1次，但不会出现连续10次不暴击。");

        knowledgeBase.recordSolution("randomness", "随机性与策略性的平衡",
            "好的随机性增加重玩价值，坏的随机性让人砸手机。\n" +
            "好的随机：\n" +
            "- 随机地图/关卡 → 每次不同体验\n" +
            "- 随机事件/遭遇 → 增加变化\n" +
            "- 随机掉落 → 保持期待\n" +
            "坏的随机：\n" +
            "- 关键操作靠运气 → 玩家感到无力\n" +
            "- 随机性决定胜负 → 不公平\n" +
            "原则：随机性应该增加选择，而非取代选择。");
    }

    /**
     * 进阶关卡设计知识
     */
    private void initAdvancedLevelDesign() {
        knowledgeBase.recordSolution("level_design", "教学关设计原则",
            "教学关不是教程文字，是让玩家自己发现玩法：\n" +
            "1. 安全环境：没有死亡风险，让玩家自由尝试\n" +
            "2. 单一机制：每关只教一个新东西\n" +
            "3. 先展示再操作：先看到NPC做，再自己做\n" +
            "4. 即时应用：学完立刻用上，不要学了不用\n" +
            "5. 无文字教程：用环境引导，而非弹窗说明\n" +
            "反面教材：开头弹出10页教程文字 = 劝退。");

        knowledgeBase.recordSolution("level_design", "Boss设计模式",
            "Boss = 测试玩家是否掌握了之前所有技能。\n" +
            "设计框架：\n" +
            "1. 阶段制：Boss分2-3个阶段，每阶段用不同招式\n" +
            "2. 前摇提示：每个攻击前有明确的视觉提示（0.5-1秒）\n" +
            "3. 弱点窗口：特定攻击后有安全输出时间\n" +
            "4. 学习曲线：第一阶段简单，后续阶段叠加复杂度\n" +
            "5. 环境互动：利用场景中的元素对抗Boss\n" +
            "关键：Boss战失败后，玩家应该知道'下次我该怎么做'。");

        knowledgeBase.recordSolution("level_design", "难度节奏设计",
            "难度不是一直上升，而是波浪式：\n" +
            "紧张（高难）→ 放松（低难/奖励）→ 更紧张 → 更放松\n" +
            "具体做法：\n" +
            "1. 每3-5个紧张关卡后放一个轻松关卡\n" +
            "2. 新机制引入时降低难度\n" +
            "3. Boss战前给一个存档/补给点\n" +
            "4. 连续失败后暗中降低难度（动态难度调整, DDA）\n" +
            "5. 玩家主动选择难度（简单/普通/困难）比强制更好");
    }

    /**
     * 经济系统设计知识
     */
    private void initEconomySystems() {
        knowledgeBase.recordSolution("economy", "游戏经济系统设计",
            "经济系统 = 货币的产出（水龙头）和消耗（水池）。\n" +
            "核心原则：产出必须 < 消耗，否则通胀。\n" +
            "常见货币设计：\n" +
            "1. 软货币（金币）：游戏内获取，用于基础操作\n" +
            "2. 硬货币（钻石）：稀缺，部分可付费获取\n" +
            "3. 体力/精力：限制每日产出，控制节奏\n" +
            "4. 贡献/荣誉：不可购买，只能通过行为获取\n" +
            "关键：不同货币之间不要直接兑换，防止刷金。");

        knowledgeBase.recordSolution("economy", "付费平衡设计",
            "付费不能破坏游戏平衡，否则免费玩家流失。\n" +
            "可接受的付费：\n" +
            "1. 外观/皮肤：不影响数值\n" +
            "2. 加速/便利：省时间但不增加上限\n" +
            "3. 额外内容：新关卡/新角色/新剧情\n" +
            "不可接受的付费：\n" +
            "1. 直接买数值：付费碾压免费\n" +
            "2. 付费独占强力装备\n" +
            "3. 付费跳过核心玩法\n" +
            "原则：付费玩家应该'更快'而非'更强'。");

        knowledgeBase.recordSolution("economy", "通胀控制策略",
            "游戏经济通胀 = 货币贬值，物价飞涨。\n" +
            "控制方法：\n" +
            "1. 强消耗：强化/升级有失败率，消耗材料\n" +
            "2. 限时消耗：装备耐久度、赛季重置\n" +
            "3. 稀缺资源：高级材料每日限量获取\n" +
            "4. 货币回收：交易税、维修费、建筑维护\n" +
            "5. 赛季制：每赛季重置部分进度，重置经济\n" +
            "监控指标：追踪每个玩家的净收入/支出比，异常时告警。");
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
