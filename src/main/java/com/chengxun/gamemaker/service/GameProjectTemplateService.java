package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.AgentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 游戏项目模板服务
 * 提供各种游戏类型的项目模板和Agent配置
 *
 * 支持的游戏类型：
 * - RPG（角色扮演）
 * - 射击游戏
 * - 策略游戏
 * - 休闲游戏
 * - 模拟经营
 * - 卡牌游戏
 * - 棋牌游戏
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GameProjectTemplateService {

    private static final Logger log = LoggerFactory.getLogger(GameProjectTemplateService.class);

    /**
     * 游戏类型枚举
     */
    public enum GameType {
        RPG("角色扮演游戏", "Role-Playing Game"),
        SHOOTER("射击游戏", "Shooter Game"),
        STRATEGY("策略游戏", "Strategy Game"),
        CASUAL("休闲游戏", "Casual Game"),
        SIMULATION("模拟经营", "Simulation Game"),
        CARD("卡牌游戏", "Card Game"),
        CHESS("棋牌游戏", "Chess/Board Game"),
        PUZZLE("益智游戏", "Puzzle Game"),
        RACING("赛车游戏", "Racing Game"),
        SPORTS("体育游戏", "Sports Game"),
        ADVENTURE("冒险游戏", "Adventure Game"),
        SANDBOX("沙盒游戏", "Sandbox Game"),
        TOWER_DEFENSE("塔防游戏", "Tower Defense Game"),
        PLANTS_VS_ZOMBIES("植物大战僵尸", "Plants vs Zombies Style");

        private final String chineseName;
        private final String englishName;

        GameType(String chineseName, String englishName) {
            this.chineseName = chineseName;
            this.englishName = englishName;
        }

        public String getChineseName() { return chineseName; }
        public String getEnglishName() { return englishName; }
    }

    /**
     * 获取所有支持的游戏类型
     */
    public List<Map<String, String>> getSupportedGameTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        for (GameType type : GameType.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("id", type.name());
            info.put("name", type.getChineseName());
            info.put("englishName", type.getEnglishName());
            info.put("description", getGameTypeDescription(type));
            types.add(info);
        }
        return types;
    }

    /**
     * 获取游戏类型描述
     */
    private String getGameTypeDescription(GameType type) {
        return switch (type) {
            case RPG -> "包含角色成长、任务系统、战斗系统、装备系统等";
            case SHOOTER -> "包含射击机制、关卡设计、敌人AI、武器系统等";
            case STRATEGY -> "包含资源管理、单位控制、战术决策、回合制/即时制等";
            case CASUAL -> "简单易上手、碎片化时间、休闲娱乐为主";
            case SIMULATION -> "模拟真实场景、经营管理、资源调配等";
            case CARD -> "卡牌收集、卡组构建、策略对战等";
            case CHESS -> "棋盘游戏、规则引擎、AI对弈等";
            case PUZZLE -> "解谜逻辑、关卡设计、提示系统等";
            case RACING -> "赛车操控、赛道设计、物理引擎、多人竞赛等";
            case SPORTS -> "体育规则、球员管理、赛事系统等";
            case ADVENTURE -> "剧情驱动、探索解谜、角色互动等";
            case SANDBOX -> "开放世界、自由建造、生存探索等";
            case TOWER_DEFENSE -> "塔防策略、防御塔布局、敌人波次、资源管理等";
            case PLANTS_VS_ZOMBIES -> "植物DIY、僵尸对抗、策略布局、资源阳光管理，支持玩家自定义植物设计";
        };
    }

    /**
     * 获取游戏类型的默认Agent配置
     *
     * @param gameType 游戏类型
     * @param projectWorkDir 项目工作目录
     * @param supportsImageGeneration 是否支持图片生成
     * @return Agent定义列表
     */
    public List<AgentDefinition> getDefaultAgentConfigs(GameType gameType, String projectWorkDir,
                                                         boolean supportsImageGeneration) {
        List<AgentDefinition> agents = new ArrayList<>();

        // 所有游戏类型都需要的基础Agent
        agents.add(AgentDefinition.createPlannerAgent(
            "planner-1", "系统策划", projectWorkDir));

        agents.add(AgentDefinition.createServerAgent(
            "server-1", "服务端开发", projectWorkDir));

        agents.add(AgentDefinition.createClientAgent(
            "client-1", "客户端开发", projectWorkDir));

        // UI Agent根据API能力配置
        agents.add(AgentDefinition.createUiAgent(
            "ui-1", "UI设计", projectWorkDir, supportsImageGeneration));

        agents.add(AgentDefinition.createTesterAgent(
            "tester-1", "测试工程师", projectWorkDir));

        // 根据游戏类型添加特殊Agent
        switch (gameType) {
            case RPG -> {
                // RPG需要数值策划
                agents.add(createNumericalPlanner("数值策划", projectWorkDir));
                // RPG需要关卡策划
                agents.add(createLevelDesigner("关卡策划", projectWorkDir));
            }
            case SHOOTER -> {
                // 射击游戏需要关卡策划
                agents.add(createLevelDesigner("关卡策划", projectWorkDir));
                // 需要3D相关能力标记
                agents.get(2).addCapability("3d_basics");
            }
            case STRATEGY -> {
                // 策略游戏需要数值策划
                agents.add(createNumericalPlanner("数值策划", projectWorkDir));
                // AI策划
                agents.add(createAiDesigner("AI策划", projectWorkDir));
            }
            case CARD -> {
                // 卡牌游戏需要数值策划
                agents.add(createNumericalPlanner("数值策划", projectWorkDir));
                // 卡牌设计师
                agents.add(createCardDesigner("卡牌设计师", projectWorkDir));
            }
            case SIMULATION -> {
                // 模拟经营需要数值策划
                agents.add(createNumericalPlanner("数值策划", projectWorkDir));
            }
            case TOWER_DEFENSE -> {
                // 塔防游戏需要数值策划
                agents.add(createNumericalPlanner("数值策划", projectWorkDir));
                // 关卡策划
                agents.add(createLevelDesigner("关卡策划", projectWorkDir));
                // AI策划（敌人路径AI）
                agents.add(createAiDesigner("AI策划", projectWorkDir));
            }
            case PLANTS_VS_ZOMBIES -> {
                // 植物大战僵尸类型需要特殊配置
                agents.add(createNumericalPlanner("数值策划", projectWorkDir));
                agents.add(createLevelDesigner("关卡策划", projectWorkDir));
                agents.add(createAiDesigner("AI策划", projectWorkDir));
                // 植物设计师（核心特色）
                agents.add(createPlantDesigner("植物设计师", projectWorkDir));
                // 僵尸设计师
                agents.add(createZombieDesigner("僵尸设计师", projectWorkDir));
                // DIY系统设计师
                agents.add(createDiySystemDesigner("DIY系统设计师", projectWorkDir));
            }
            default -> {
                // 其他类型使用基础配置
            }
        }

        // 设置标签
        for (AgentDefinition agent : agents) {
            agent.setTag("gameType", gameType.name());
            agent.setTag("projectDir", projectWorkDir);
        }

        log.info("Created {} agent configs for game type: {}", agents.size(), gameType.getChineseName());
        return agents;
    }

    /**
     * 创建数值策划Agent
     */
    private AgentDefinition createNumericalPlanner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("numerical-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("numerical-planner")
            .description("数值策划，负责游戏数值平衡、经济系统、成长曲线设计")
            .workDir(workDir)
            .capability("numerical_design")
            .capability("balance_tuning")
            .capability("economy_design")
            .supportedFileType("xlsx")
            .supportedFileType("csv")
            .supportedFileType("json")
            .tag("department", "design")
            .tag("speciality", "numerical")
            .maxContextSize(120000)
            .build();
    }

    /**
     * 创建关卡策划Agent
     */
    private AgentDefinition createLevelDesigner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("level-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("level-designer")
            .description("关卡策划，负责关卡设计、地图编辑、难度曲线")
            .workDir(workDir)
            .capability("level_design")
            .capability("map_design")
            .capability("difficulty_design")
            .supportedFileType("json")
            .supportedFileType("tmx")
            .supportedFileType("yaml")
            .tag("department", "design")
            .tag("speciality", "level")
            .maxContextSize(100000)
            .build();
    }

    /**
     * 创建AI策划Agent
     */
    private AgentDefinition createAiDesigner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("ai-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("ai-designer")
            .description("AI策划，负责敌人AI、NPC行为、决策树设计")
            .workDir(workDir)
            .capability("ai_design")
            .capability("behavior_tree")
            .capability("pathfinding")
            .supportedFileType("json")
            .supportedFileType("lua")
            .supportedFileType("py")
            .tag("department", "design")
            .tag("speciality", "ai")
            .maxContextSize(100000)
            .build();
    }

    /**
     * 创建卡牌设计师Agent
     */
    private AgentDefinition createCardDesigner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("card-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("card-designer")
            .description("卡牌设计师，负责卡牌设计、技能效果、卡组平衡")
            .workDir(workDir)
            .capability("card_design")
            .capability("skill_design")
            .capability("deck_balance")
            .supportedFileType("json")
            .supportedFileType("xlsx")
            .tag("department", "design")
            .tag("speciality", "card")
            .maxContextSize(100000)
            .build();
    }

    /**
     * 创建植物设计师Agent（植物大战僵尸专用）
     */
    private AgentDefinition createPlantDesigner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("plant-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("plant-designer")
            .description("植物设计师，负责植物设计、技能效果、植物平衡、DIY植物模板")
            .workDir(workDir)
            .capability("plant_design")
            .capability("skill_effect_design")
            .capability("plant_balance")
            .capability("plant_template_design")
            .supportedFileType("json")
            .supportedFileType("png")
            .supportedFileType("svg")
            .supportedFileType("atlas")
            .tag("department", "design")
            .tag("speciality", "plant")
            .tag("gameType", "PLANTS_VS_ZOMBIES")
            .maxContextSize(120000)
            .build();
    }

    /**
     * 创建僵尸设计师Agent（植物大战僵尸专用）
     */
    private AgentDefinition createZombieDesigner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("zombie-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("zombie-designer")
            .description("僵尸设计师，负责僵尸设计、僵尸AI、僵尸波次、Boss设计")
            .workDir(workDir)
            .capability("zombie_design")
            .capability("zombie_ai_design")
            .capability("wave_design")
            .capability("boss_design")
            .supportedFileType("json")
            .supportedFileType("png")
            .supportedFileType("svg")
            .tag("department", "design")
            .tag("speciality", "zombie")
            .tag("gameType", "PLANTS_VS_ZOMBIES")
            .maxContextSize(120000)
            .build();
    }

    /**
     * 创建DIY系统设计师Agent（植物大战僵尸专用）
     */
    private AgentDefinition createDiySystemDesigner(String name, String workDir) {
        return AgentDefinition.builder()
            .id("diy-" + UUID.randomUUID().toString().substring(0, 8))
            .name(name)
            .role("diy-system-designer")
            .description("DIY系统设计师，负责植物DIY系统、编辑器设计、组件系统、分享系统")
            .workDir(workDir)
            .capability("diy_system_design")
            .capability("editor_design")
            .capability("component_system")
            .capability("sharing_system")
            .supportedFileType("json")
            .supportedFileType("ts")
            .supportedFileType("js")
            .tag("department", "system")
            .tag("speciality", "diy")
            .tag("gameType", "PLANTS_VS_ZOMBIES")
            .maxContextSize(150000)
            .build();
    }

    /**
     * 获取项目目录结构模板
     */
    public Map<String, List<String>> getProjectDirectoryStructure(GameType gameType) {
        Map<String, List<String>> structure = new LinkedHashMap<>();

        // 基础目录结构
        structure.put("design", Arrays.asList(
            "docs/",
            "specs/",
            "balance/",
            "levels/"
        ));

        structure.put("server", Arrays.asList(
            "src/",
            "config/",
            "scripts/",
            "sql/"
        ));

        structure.put("client", Arrays.asList(
            "src/",
            "assets/",
            "prefabs/",
            "scenes/"
        ));

        structure.put("shared", Arrays.asList(
            "proto/",
            "config/",
            "common/"
        ));

        structure.put("tools", Arrays.asList(
            "build/",
            "scripts/",
            "templates/"
        ));

        // 根据游戏类型添加特殊目录
        switch (gameType) {
            case RPG -> {
                structure.put("client/assets", Arrays.asList(
                    "characters/",
                    "items/",
                    "skills/",
                    "maps/",
                    "ui/",
                    "audio/"
                ));
            }
            case SHOOTER -> {
                structure.put("client/assets", Arrays.asList(
                    "weapons/",
                    "characters/",
                    "maps/",
                    "effects/",
                    "ui/",
                    "audio/"
                ));
            }
            case CARD -> {
                structure.put("design/cards", Arrays.asList(
                    "definitions/",
                    "images/",
                    "effects/"
                ));
            }
            case PLANTS_VS_ZOMBIES -> {
                // 植物大战僵尸专用目录结构
                structure.put("design/plants", Arrays.asList(
                    "templates/",           // 植物模板
                    "skills/",              // 技能配置
                    "balance/",             // 平衡性数据
                    "diy-components/"       // DIY组件库
                ));
                structure.put("design/zombies", Arrays.asList(
                    "definitions/",         // 僵尸定义
                    "ai/",                  // 僵尸AI配置
                    "waves/",               // 波次配置
                    "bosses/"               // Boss配置
                ));
                structure.put("design/levels", Arrays.asList(
                    "maps/",                // 地图配置
                    "difficulty/",          // 难度曲线
                    "rewards/"              // 奖励配置
                ));
                structure.put("client/assets", Arrays.asList(
                    "plants/",              // 植物资源
                    "zombies/",             // 僵尸资源
                    "projectiles/",         // 投射物资源
                    "effects/",             // 特效资源
                    "maps/",                // 地图资源
                    "ui/",                  // UI资源
                    "audio/"                // 音频资源
                ));
                structure.put("client/prefabs", Arrays.asList(
                    "plants/",              // 植物预制体
                    "zombies/",             // 僵尸预制体
                    "projectiles/",         // 投射物预制体
                    "effects/"              // 特效预制体
                ));
                structure.put("client/scripts", Arrays.asList(
                    "plants/",              // 植物脚本
                    "zombies/",             // 僵尸脚本
                    "diy/",                 // DIY系统脚本
                    "ui/",                  // UI脚本
                    "managers/"             // 管理器脚本
                ));
                structure.put("diy-system", Arrays.asList(
                    "editor/",              // DIY编辑器
                    "components/",          // 组件库
                    "templates/",           // 模板库
                    "sharing/",             // 分享系统
                    "validation/"           // 验证系统
                ));
                structure.put("server/config", Arrays.asList(
                    "plants/",              // 植物配置
                    "zombies/",             // 僵尸配置
                    "levels/",              // 关卡配置
                    "diy/"                  // DIY配置
                ));
            }
            case TOWER_DEFENSE -> {
                // 塔防游戏通用目录
                structure.put("design/towers", Arrays.asList(
                    "definitions/",
                    "skills/",
                    "balance/"
                ));
                structure.put("design/enemies", Arrays.asList(
                    "definitions/",
                    "ai/",
                    "waves/"
                ));
                structure.put("client/assets", Arrays.asList(
                    "towers/",
                    "enemies/",
                    "projectiles/",
                    "effects/",
                    "maps/",
                    "ui/",
                    "audio/"
                ));
            }
            default -> {
                structure.put("client/assets", Arrays.asList(
                    "sprites/",
                    "ui/",
                    "audio/",
                    "data/"
                ));
            }
        }

        return structure;
    }

    /**
     * 获取游戏类型的技术栈建议
     */
    public Map<String, Object> getTechStackRecommendation(GameType gameType) {
        Map<String, Object> recommendation = new HashMap<>();

        // 通用技术栈
        recommendation.put("server", "Java/Python/Go");
        recommendation.put("database", "MySQL/PostgreSQL");
        recommendation.put("cache", "Redis");
        recommendation.put("communication", "WebSocket/gRPC");

        // 根据游戏类型推荐客户端技术
        switch (gameType) {
            case RPG, STRATEGY, SIMULATION -> {
                recommendation.put("client", "Unity/Cocos2d-x/Godot");
                recommendation.put("ui", "UGUI/FairyGUI");
                recommendation.put("language", "C#/C++/Lua");
            }
            case SHOOTER, RACING, SPORTS -> {
                recommendation.put("client", "Unity/Unreal Engine");
                recommendation.put("ui", "UGUI/UMG");
                recommendation.put("language", "C#/C++");
                recommendation.put("physics", "内置物理引擎");
            }
            case CASUAL, PUZZLE -> {
                recommendation.put("client", "Cocos Creator/Unity/Laya");
                recommendation.put("ui", "内置UI系统");
                recommendation.put("language", "TypeScript/C#");
            }
            case CARD, CHESS -> {
                recommendation.put("client", "Unity/Cocos Creator");
                recommendation.put("ui", "FairyGUI/UGUI");
                recommendation.put("language", "C#/TypeScript");
                recommendation.put("ai", "行为树/状态机");
            }
            case SANDBOX -> {
                recommendation.put("client", "Unity/Unreal Engine");
                recommendation.put("ui", "UGUI/UMG");
                recommendation.put("language", "C#/C++");
                recommendation.put("world", "程序化生成");
            }
            case TOWER_DEFENSE -> {
                recommendation.put("client", "Unity/Cocos Creator/Godot");
                recommendation.put("ui", "UGUI/FairyGUI");
                recommendation.put("language", "C#/TypeScript/GDScript");
                recommendation.put("ai", "A*寻路/状态机");
                recommendation.put("gameplay", "波次系统/资源管理");
            }
            case PLANTS_VS_ZOMBIES -> {
                recommendation.put("client", "Unity/Cocos Creator");
                recommendation.put("ui", "UGUI/FairyGUI");
                recommendation.put("language", "C#/TypeScript");
                recommendation.put("ai", "行为树/状态机/寻路算法");
                recommendation.put("gameplay", "波次系统/阳光经济/植物DIY");
                recommendation.put("diy", "组件化设计/可视化编辑器/模板系统");
                recommendation.put("sharing", "UGC社区/云端存储/作品展示");
            }
            default -> {
                recommendation.put("client", "Unity/Cocos Creator");
                recommendation.put("language", "C#/TypeScript");
            }
        }

        recommendation.put("versionControl", "Git");
        recommendation.put("ci", "Jenkins/GitHub Actions");
        recommendation.put("monitoring", "Prometheus/Grafana");

        return recommendation;
    }

    /**
     * 获取植物大战僵尸游戏的DIY系统设计指南
     *
     * @return DIY系统设计指南
     */
    public Map<String, Object> getPlantsVsZombiesDiyGuide() {
        Map<String, Object> guide = new HashMap<>();

        // 植物组件系统
        Map<String, Object> plantComponents = new HashMap<>();
        plantComponents.put("base", Arrays.asList(
            "外观组件 (AppearanceComponent): 精灵图、动画、特效",
            "生命组件 (HealthComponent): HP、护甲、回复",
            "攻击组件 (AttackComponent): 伤害、攻速、范围",
            "技能组件 (SkillComponent): 主动技能、被动技能",
            "阳光组件 (SunComponent): 产出阳光、消耗阳光"
        ));
        plantComponents.put("advanced", Arrays.asList(
            "投射物组件 (ProjectileComponent): 子弹类型、弹道",
            "范围组件 (AreaComponent): AOE范围、作用区域",
            "控制组件 (ControlComponent): 减速、眩晕、击退",
            "增益组件 (BuffComponent): 攻击加成、防御加成",
            "特殊组件 (SpecialComponent): 自定义特殊效果"
        ));
        guide.put("plantComponents", plantComponents);

        // DIY编辑器功能
        Map<String, Object> diyEditor = new HashMap<>();
        diyEditor.put("visualEditor", Arrays.asList(
            "拖拽式组件组装",
            "实时预览效果",
            "属性面板调节",
            "动画时间轴编辑"
        ));
        diyEditor.put("templateSystem", Arrays.asList(
            "基础模板库（向日葵、豌豆射手、坚果墙等）",
            "模板继承和修改",
            "自定义模板保存",
            "模板分享功能"
        ));
        diyEditor.put("validationSystem", Arrays.asList(
            "属性范围检查",
            "平衡性验证",
            "资源引用检查",
            "兼容性测试"
        ));
        guide.put("diyEditor", diyEditor);

        // 分享系统
        Map<String, Object> sharingSystem = new HashMap<>();
        sharingSystem.put("features", Arrays.asList(
            "植物作品上传",
            "作品展示画廊",
            "点赞和评论",
            "热门排行",
            "官方推荐",
            "导入导出功能"
        ));
        sharingSystem.put("dataFormat", Arrays.asList(
            "JSON格式存储",
            "版本兼容处理",
            "资源打包",
            "云端同步"
        ));
        guide.put("sharingSystem", sharingSystem);

        // 平衡性设计
        Map<String, Object> balanceDesign = new HashMap<>();
        balanceDesign.put("principles", Arrays.asList(
            "阳光成本与价值平衡",
            "攻击与防御的权衡",
            "特殊能力的限制",
            "冷却时间设计",
            "植物间的协同与克制"
        ));
        balanceDesign.put("metrics", Arrays.asList(
            "DPS (每秒伤害)",
            "DPD (每阳光伤害)",
            "生存能力评分",
            "功能性评分",
            "综合性价比"
        ));
        guide.put("balanceDesign", balanceDesign);

        return guide;
    }

    /**
     * 获取植物大战僵尸的默认植物模板列表
     *
     * @return 默认植物模板列表
     */
    public List<Map<String, Object>> getDefaultPlantTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        // 向日葵模板
        Map<String, Object> sunflower = new HashMap<>();
        sunflower.put("id", "sunflower");
        sunflower.put("name", "向日葵");
        sunflower.put("description", "产出阳光的基础植物");
        sunflower.put("sunCost", 50);
        sunflower.put("health", 300);
        sunflower.put("sunProduction", 25);
        sunflower.put("productionInterval", 24);
        sunflower.put("category", "production");
        sunflower.put("rarity", "common");
        templates.add(sunflower);

        // 豌豆射手模板
        Map<String, Object> peashooter = new HashMap<>();
        peashooter.put("id", "peashooter");
        peashooter.put("name", "豌豆射手");
        peashooter.put("description", "发射豌豆的基础攻击植物");
        peashooter.put("sunCost", 100);
        peashooter.put("health", 300);
        peashooter.put("damage", 20);
        peashooter.put("attackSpeed", 1.5);
        peashooter.put("range", "straight");
        peashooter.put("category", "attack");
        peashooter.put("rarity", "common");
        templates.add(peashooter);

        // 坚果墙模板
        Map<String, Object> wallNut = new HashMap<>();
        wallNut.put("id", "wallnut");
        wallNut.put("name", "坚果墙");
        wallNut.put("description", "高血量的防御植物");
        wallNut.put("sunCost", 50);
        wallNut.put("health", 4000);
        wallNut.put("category", "defense");
        wallNut.put("rarity", "common");
        templates.add(wallNut);

        // 寒冰射手模板
        Map<String, Object> snowPea = new HashMap<>();
        snowPea.put("id", "snowpea");
        snowPea.put("name", "寒冰射手");
        snowPea.put("description", "发射冰豌豆，减缓僵尸移动");
        snowPea.put("sunCost", 175);
        snowPea.put("health", 300);
        snowPea.put("damage", 20);
        snowPea.put("attackSpeed", 1.5);
        snowPea.put("slowEffect", 0.5);
        snowPea.put("slowDuration", 10);
        snowPea.put("category", "control");
        snowPea.put("rarity", "uncommon");
        templates.add(snowPea);

        // 双发射手模板
        Map<String, Object> repeater = new HashMap<>();
        repeater.put("id", "repeater");
        repeater.put("name", "双发射手");
        repeater.put("description", "一次发射两颗豌豆");
        repeater.put("sunCost", 200);
        repeater.put("health", 300);
        repeater.put("damage", 20);
        repeater.put("attackSpeed", 1.5);
        repeater.put("projectileCount", 2);
        repeater.put("category", "attack");
        repeater.put("rarity", "uncommon");
        templates.add(repeater);

        // 火炬树桩模板
        Map<String, Object> torchwood = new HashMap<>();
        torchwood.put("id", "torchwood");
        torchwood.put("name", "火炬树桩");
        torchwood.put("description", "将经过的豌豆变为火球，造成范围伤害");
        torchwood.put("sunCost", 175);
        torchwood.put("health", 300);
        torchwood.put("damageMultiplier", 2.0);
        torchwood.put("areaDamage", 30);
        torchwood.put("areaRadius", 50);
        torchwood.put("category", "support");
        torchwood.put("rarity", "rare");
        templates.add(torchwood);

        // 樱桃炸弹模板
        Map<String, Object> cherryBomb = new HashMap<>();
        cherryBomb.put("id", "cherrybomb");
        cherryBomb.put("name", "樱桃炸弹");
        cherryBomb.put("description", "瞬间爆炸，消灭范围内所有僵尸");
        cherryBomb.put("sunCost", 150);
        cherryBomb.put("health", 300);
        cherryBomb.put("damage", 1800);
        cherryBomb.put("areaRadius", 115);
        cherryBomb.put("isInstantUse", true);
        cherryBomb.put("category", "instant");
        cherryBomb.put("rarity", "rare");
        templates.add(cherryBomb);

        return templates;
    }
}
