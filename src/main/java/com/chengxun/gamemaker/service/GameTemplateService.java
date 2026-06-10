package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.entity.GameTemplateEntity;
import com.chengxun.gamemaker.web.repository.GameTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏模板服务
 * 管理游戏模板的加载、查询和使用
 *
 * 主要功能：
 * - 加载内置游戏模板
 * - 根据游戏类型推荐模板
 * - 根据用户描述匹配模板
 * - 提供模板详情
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GameTemplateService {

    private static final Logger log = LoggerFactory.getLogger(GameTemplateService.class);

    @Autowired
    private GameTemplateRepository gameTemplateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 游戏模板缓存 */
    private final Map<String, GameTemplate> templates = new ConcurrentHashMap<>();

    /**
     * 初始化游戏模板
     * 加载内置模板定义，并从 markdown 文件读取模板内容
     */
    @PostConstruct
    public void init() {
        // 注册内置模板
        registerTemplate(new GameTemplate(
            "platformer",
            "平台跳跃游戏",
            "横版平台跳跃游戏，包含角色控制、关卡系统、敌人系统",
            Arrays.asList("platformer", "platform game", "跳跃", "横版", "side-scrolling"),
            "game-template-platformer"
        ));

        registerTemplate(new GameTemplate(
            "tower-defense",
            "塔防游戏",
            "塔防策略游戏，包含波次系统、塔防系统、经济系统",
            Arrays.asList("tower defense", "塔防", "defense", "TD"),
            "game-template-tower-defense"
        ));

        registerTemplate(new GameTemplate(
            "rpg",
            "RPG游戏",
            "角色扮演游戏，包含角色系统、战斗系统、对话系统",
            Arrays.asList("RPG", "role-playing", "角色扮演", "JRPG", "adventure"),
            "game-template-rpg"
        ));

        registerTemplate(new GameTemplate(
            "puzzle",
            "益智游戏",
            "三消益智游戏，包含棋盘系统、消除系统、关卡系统",
            Arrays.asList("puzzle", "match-3", "益智", "消除", "三消"),
            "game-template-puzzle"
        ));

        registerTemplate(new GameTemplate(
            "fullstack",
            "全栈游戏",
            "全栈游戏项目，包含前端游戏和后端服务，支持多人在线",
            Arrays.asList("fullstack", "全栈", "multiplayer", "多人", "online", "在线", "server", "后端"),
            "game-template-fullstack"
        ));

        registerTemplate(new GameTemplate(
            "shooter",
            "射击游戏",
            "俯视角射击游戏，包含武器系统、敌人AI、子弹系统",
            Arrays.asList("shooter", "FPS", "射击", "gun", "枪战", "bullet", "子弹"),
            "game-template-shooter"
        ));

        registerTemplate(new GameTemplate(
            "racing",
            "赛车游戏",
            "俯视角赛车游戏，包含车辆物理、赛道系统、AI对手",
            Arrays.asList("racing", "race", "赛车", "car", "汽车", "drift", "漂移"),
            "game-template-racing"
        ));

        registerTemplate(new GameTemplate(
            "strategy",
            "策略游戏",
            "回合制策略游戏，包含六边形网格、单位系统、资源系统",
            Arrays.asList("strategy", "RTS", "策略", "战争", "army", "军队", "empire", "帝国"),
            "game-template-strategy"
        ));

        registerTemplate(new GameTemplate(
            "slg",
            "SLG策略游戏",
            "大地图SLG策略游戏，包含城市系统、科技树、军事系统、外交系统",
            Arrays.asList("SLG", "simulation", "模拟", "策略", "4X", "civilization", "文明", "empire", "帝国", "war", "战争"),
            "game-template-slg"
        ));

        registerTemplate(new GameTemplate(
            "hide-and-seek",
            "躲猫猫游戏",
            "多人躲猫猫游戏，包含躲藏者、寻找者、视野系统、道具系统",
            Arrays.asList("hide and seek", "躲猫猫", "捉迷藏", "捕捉", "追逐", "chase", "seeker", "hider"),
            "game-template-hide-and-seek"
        ));

        registerTemplate(new GameTemplate(
            "wechat-miniprogram",
            "微信小程序游戏",
            "微信小程序+Java全栈游戏，包含微信登录、排行榜、支付",
            Arrays.asList("wechat", "miniprogram", "微信", "小程序", "wx", "微信游戏", "mobile", "手机"),
            "game-template-wechat-miniprogram"
        ));

        registerTemplate(new GameTemplate(
            "party-coop",
            "派对合作游戏",
            "类似胡闹厨房的多人合作游戏，支持多设备联机，包含订单系统、烹饪系统、计分系统",
            Arrays.asList("party", "派对", "coop", "合作", "overcooked", "胡闹厨房", "multiplayer", "联机", "cook", "烹饪", "kitchen", "厨房"),
            "game-template-party-coop"
        ));

        registerTemplate(new GameTemplate(
            "coop-adventure",
            "双人合作闯关",
            "双人合作闯关游戏，支持本地/联机，不同角色不同能力，协作解谜",
            Arrays.asList("coop", "合作", "adventure", "冒险", "闯关", "双人", "two player", "puzzle", "platform", "合作闯关"),
            "game-template-coop-adventure"
        ));

        registerTemplate(new GameTemplate(
            "tile-match",
            "三消堆叠益智游戏",
            "三消堆叠益智游戏，多层堆叠、三消匹配、高难度挑战",
            Arrays.asList("tile", "三消", "堆叠", "match", "mahjong", "麻将", "消除", "puzzle", "益智"),
            "game-template-tile-match"
        ));

        registerTemplate(new GameTemplate(
            "idle-tycoon",
            "数值养成经营游戏",
            "店铺经营、员工培养、数值成长、离线收益、转生系统",
            Arrays.asList("idle", "放置", "tycoon", "经营", "养成", "数值", "店铺", "商业", "经理", "manager"),
            "game-template-idle-tycoon"
        ));

        registerTemplate(new GameTemplate(
            "roguelike",
            "肉鸽游戏",
            "随机地图、永久死亡、道具组合、技能构建、进度系统",
            Arrays.asList("roguelike", "肉鸽", "roguelite", "随机", "地牢", "dungeon", "永久死亡", "permadeath"),
            "game-template-roguelike"
        ));

        registerTemplate(new GameTemplate(
            "shooter-defense",
            "射击防御游戏",
            "射击+塔防混合、怪物波次、武器升级、防御工事",
            Arrays.asList("shooter", "defense", "射击", "防御", "monster", "怪物", "wave", "波次"),
            "game-template-shooter-defense"
        ));

        registerTemplate(new GameTemplate(
            "screw-puzzle",
            "螺丝解谜游戏",
            "拆卸螺丝、分类收集、物理解谜、关卡挑战",
            Arrays.asList("screw", "螺丝", "bolt", "解谜", "puzzle", "拆卸", "分类", "物理"),
            "game-template-screw-puzzle"
        ));

        registerTemplate(new GameTemplate(
            "town-sim",
            "小镇模拟经营游戏",
            "建造房屋、居民管理、资源收集、装饰美化、任务系统",
            Arrays.asList("town", "小镇", "sim", "模拟", "village", "村庄", "build", "建造", "decoration", "装饰"),
            "game-template-town-sim"
        ));

        registerTemplate(new GameTemplate(
            "story-driven",
            "剧情推进游戏",
            "分支剧情、伏笔系统、角色关系、多结局，着重剧情推进与伏笔设计",
            Arrays.asList("story", "剧情", "narrative", "叙事", "branch", "分支", "ending", "结局", "foreshadow", "伏笔", "visual novel", "视觉小说"),
            "game-template-story-driven"
        ));

        registerTemplate(new GameTemplate(
            "design-studio",
            "游戏设计工作室",
            "可视化关卡编辑器、素材管理、逻辑编辑器、作品发布，让玩家沉浸式设计自己的游戏作品",
            Arrays.asList("design", "设计", "editor", "编辑器", "creative", "创作", "sandbox", "沙盒", "build", "建造", "maker", "制作"),
            "game-template-design-studio"
        ));

        // ===== AI 驱动游戏模板 =====

        registerTemplate(new GameTemplate(
            "ai-npc",
            "AI NPC 对话冒险",
            "AI驱动NPC对话、动态剧情、角色扮演，每个NPC有独立性格和记忆",
            Arrays.asList("AI", "NPC", "对话", "冒险", "角色扮演", "剧情", "AI对话", "dynamic", "dialogue", "RPG", "人工智能"),
            "game-template-ai-npc"
        ));

        registerTemplate(new GameTemplate(
            "ai-world",
            "AI 程序化世界",
            "AI生成地图、动态生态、探索冒险，程序化生成的独特世界",
            Arrays.asList("AI", "程序化", "世界", "探索", "procedural", "world", "generation", "生态", "生成", "地图"),
            "game-template-ai-world"
        ));

        registerTemplate(new GameTemplate(
            "ai-chess",
            "AI 对弈棋局",
            "AI对手、多种棋类、难度自适应、复盘分析",
            Arrays.asList("AI", "对弈", "棋类", "chess", "围棋", "象棋", "五子棋", "AI对手", "对战", "策略"),
            "game-template-ai-chess"
        ));

        registerTemplate(new GameTemplate(
            "ai-creative",
            "AI 创意工坊",
            "AI绘画、AI音乐、AI故事生成，释放创造力",
            Arrays.asList("AI", "创作", "绘画", "音乐", "故事", "creative", "art", "music", "story", "生成", "创意"),
            "game-template-ai-creative"
        ));

        // 加载模板内容
        loadTemplateContents();

        // 从数据库加载用户自定义模板
        loadCustomTemplatesFromDatabase();

        log.info("游戏模板初始化完成，共 {} 个模板", templates.size());
    }

    /**
     * 从数据库加载用户自定义的游戏模板
     */
    private void loadCustomTemplatesFromDatabase() {
        try {
            List<GameTemplateEntity> customList = gameTemplateRepository.findByBuiltinFalse();
            for (GameTemplateEntity entity : customList) {
                if (templates.containsKey(entity.getId())) {
                    continue; // 跳过已存在的内置模板
                }
                List<String> keywords = new ArrayList<>();
                if (entity.getConfigJson() != null && !entity.getConfigJson().isEmpty()) {
                    try {
                        Map<String, Object> config = objectMapper.readValue(entity.getConfigJson(), new TypeReference<Map<String, Object>>() {});
                        Object kw = config.get("keywords");
                        if (kw instanceof List) {
                            keywords = (List<String>) kw;
                        }
                    } catch (Exception ignored) {}
                }
                GameTemplate template = new GameTemplate(entity.getId(), entity.getName(), entity.getDescription(), keywords, null);
                template.setContent(entity.getConfigJson());
                templates.put(entity.getId(), template);
                log.info("Loaded custom game template from DB: {}", entity.getId());
            }
            log.info("Loaded {} custom game templates from database", customList.size());
        } catch (Exception e) {
            log.warn("Failed to load custom game templates from database: {}", e.getMessage());
        }
    }

    /**
     * 从 markdown 文件加载模板内容
     */
    private void loadTemplateContents() {
        Path skillsDir = Path.of("src/main/resources/skills");
        if (!Files.exists(skillsDir)) {
            log.warn("技能目录不存在: {}", skillsDir);
            return;
        }

        for (GameTemplate template : templates.values()) {
            Path templatePath = skillsDir.resolve(template.getSkillName() + ".md");
            if (Files.exists(templatePath)) {
                try {
                    String content = Files.readString(templatePath);
                    template.setContent(content);
                    log.debug("加载模板内容: {}", template.getSkillName());
                } catch (IOException e) {
                    log.error("加载模板内容失败: {}", templatePath, e);
                }
            } else {
                log.warn("模板文件不存在: {}", templatePath);
            }
        }
    }

    /**
     * 注册游戏模板
     */
    public void registerTemplate(GameTemplate template) {
        templates.put(template.getId(), template);
    }

    /**
     * 解析模板内容中的目录配置
     * 目录配置格式：
     * ## 目录配置
     * | 目录路径 | 用途 | 可访问角色 | 说明 |
     * |---------|------|-----------|------|
     * | /client | 前端游戏客户端 | client-dev, ui-dev | 游戏主程序 |
     * | /server | 后端服务 | server-dev | API、业务逻辑 |
     * | /config | 配置文件 | | 游戏配置（所有角色可访问） |
     *
     * @param content 模板内容
     * @return 解析出的目录配置列表
     */
    public List<TemplateDirectoryConfig> parseDirectoryConfigs(String content) {
        List<TemplateDirectoryConfig> configs = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return configs;
        }

        // 查找目录配置部分
        String[] lines = content.split("\n");
        boolean inDirectorySection = false;
        boolean headerParsed = false;

        for (String line : lines) {
            line = line.trim();

            // 检测目录配置章节
            if (line.contains("## 目录配置") || line.contains("## Directory Config")) {
                inDirectorySection = true;
                continue;
            }

            // 检测下一个章节（结束目录配置解析）
            if (inDirectorySection && line.startsWith("## ") && !line.contains("目录配置") && !line.contains("Directory Config")) {
                break;
            }

            if (inDirectorySection) {
                // 跳过表头分隔行
                if (line.startsWith("|---") || line.startsWith("| ---")) {
                    headerParsed = true;
                    continue;
                }

                // 跳过表头行
                if (!headerParsed && line.startsWith("|") && (line.contains("目录路径") || line.contains("目录") || line.contains("path"))) {
                    continue;
                }

                // 解析数据行
                if (headerParsed && line.startsWith("|") && !line.isEmpty()) {
                    try {
                        TemplateDirectoryConfig config = parseDirectoryConfigLine(line);
                        if (config != null) {
                            configs.add(config);
                        }
                    } catch (Exception e) {
                        log.debug("解析目录配置行失败: {} - {}", line, e.getMessage());
                    }
                }
            }
        }

        return configs;
    }

    /**
     * 解析单行目录配置
     * 格式：| 路径 | 用途 | 可访问角色 | 说明 |
     *
     * @param line 配置行
     * @return 解析出的目录配置
     */
    private TemplateDirectoryConfig parseDirectoryConfigLine(String line) {
        // 去掉首尾的 | 并分割
        String[] parts = line.split("\\|");
        if (parts.length < 3) {
            return null;
        }

        // 去掉第一个空元素（split 结果的第一个元素是空的）
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                cells.add(trimmed);
            }
        }

        if (cells.size() < 2) {
            return null;
        }

        TemplateDirectoryConfig config = new TemplateDirectoryConfig();
        config.setPath(cells.get(0));
        config.setDescription(cells.size() > 1 ? cells.get(1) : "");

        // 解析可访问角色（逗号分隔）
        if (cells.size() > 2 && !cells.get(2).isEmpty()) {
            String rolesStr = cells.get(2);
            // 支持中英文逗号
            String[] roles = rolesStr.split("[,，]");
            List<String> roleList = new ArrayList<>();
            for (String role : roles) {
                String trimmedRole = role.trim();
                if (!trimmedRole.isEmpty()) {
                    roleList.add(trimmedRole);
                }
            }
            config.setAccessibleRoles(roleList);
        }

        // 解析说明
        if (cells.size() > 3) {
            config.setNotes(cells.get(3));
        }

        return config;
    }

    /**
     * 获取模板的目录配置
     * 如果模板内容已解析，直接返回；否则先解析
     *
     * @param templateId 模板 ID
     * @return 目录配置列表
     */
    public List<TemplateDirectoryConfig> getTemplateDirectoryConfigs(String templateId) {
        GameTemplate template = templates.get(templateId);
        if (template == null) {
            return new ArrayList<>();
        }

        // 如果还没有解析过目录配置，先解析
        if (template.getDirectoryConfigs().isEmpty() && template.getContent() != null) {
            List<TemplateDirectoryConfig> configs = parseDirectoryConfigs(template.getContent());
            template.setDirectoryConfigs(configs);
        }

        return template.getDirectoryConfigs();
    }

    /**
     * 根据模板配置项目目录
     * 从模板中提取目录配置，并设置到项目中
     *
     * @param project 项目
     * @param templateId 模板 ID
     */
    public void configureProjectDirectories(GameProject project, String templateId) {
        List<TemplateDirectoryConfig> templateConfigs = getTemplateDirectoryConfigs(templateId);
        if (templateConfigs.isEmpty()) {
            return;
        }

        // 将模板目录配置转换为项目目录配置
        for (TemplateDirectoryConfig templateConfig : templateConfigs) {
            GameProject.DirectoryConfig dirConfig = new GameProject.DirectoryConfig(
                templateConfig.getPath(),
                templateConfig.getDescription(),
                templateConfig.getNotes()
            );
            project.addDirectoryConfig(dirConfig);
        }

        log.info("已从模板 {} 配置项目目录，共 {} 个目录", templateId, templateConfigs.size());
    }

    /**
     * 获取指定角色在模板中可访问的目录列表
     *
     * @param templateId 模板 ID
     * @param role 角色名称
     * @return 可访问的目录路径列表
     */
    public List<String> getAccessibleDirsForRole(String templateId, String role) {
        List<TemplateDirectoryConfig> configs = getTemplateDirectoryConfigs(templateId);
        List<String> accessibleDirs = new ArrayList<>();

        for (TemplateDirectoryConfig config : configs) {
            if (config.isAccessibleBy(role)) {
                accessibleDirs.add(config.getPath());
            }
        }

        return accessibleDirs;
    }

    /**
     * 获取所有模板
     */
    public List<GameTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    /**
     * 根据ID获取模板
     */
    public GameTemplate getTemplate(String id) {
        return templates.get(id);
    }

    /**
     * 创建游戏模板
     *
     * @param id 模板ID
     * @param name 模板名称
     * @param description 模板描述
     * @param keywords 关键词列表
     * @param skillName 技能名称
     * @param content 模板内容
     * @return 创建的模板
     */
    public GameTemplate createTemplate(String id, String name, String description,
                                        List<String> keywords, String skillName, String content) {
        // 检查ID是否已存在
        if (templates.containsKey(id)) {
            throw new RuntimeException("模板ID已存在: " + id);
        }

        GameTemplate template = new GameTemplate(id, name, description, keywords, skillName);
        template.setContent(content);
        templates.put(id, template);

        // 持久化到数据库
        try {
            GameTemplateEntity entity = new GameTemplateEntity();
            entity.setId(id);
            entity.setName(name);
            entity.setDescription(description);
            entity.setBuiltin(false);
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("keywords", keywords != null ? keywords : new ArrayList<>());
            config.put("skillName", skillName);
            if (content != null) {
                config.put("content", content);
            }
            entity.setConfigJson(objectMapper.writeValueAsString(config));
            gameTemplateRepository.save(entity);
            log.info("创建游戏模板并持久化: {} - {}", id, name);
        } catch (Exception e) {
            log.error("Failed to persist game template: {}", id, e);
        }

        return template;
    }

    /**
     * 更新游戏模板
     *
     * @param id 模板ID
     * @param name 模板名称
     * @param description 模板描述
     * @param keywords 关键词列表
     * @param skillName 技能名称
     * @param content 模板内容
     * @return 更新的模板
     */
    public GameTemplate updateTemplate(String id, String name, String description,
                                        List<String> keywords, String skillName, String content) {
        GameTemplate existing = templates.get(id);
        if (existing == null) {
            throw new RuntimeException("模板不存在: " + id);
        }

        // 创建新的模板对象（因为GameTemplate是不可变的）
        GameTemplate updated = new GameTemplate(id, name, description, keywords, skillName);
        updated.setContent(content);
        templates.put(id, updated);

        log.info("更新游戏模板: {} - {}", id, name);
        return updated;
    }

    /**
     * 删除游戏模板
     *
     * @param id 模板ID
     * @return 是否删除成功
     */
    public boolean deleteTemplate(String id) {
        GameTemplate removed = templates.remove(id);
        if (removed != null) {
            // 从数据库中删除（如果存在）
            try {
                gameTemplateRepository.deleteById(id);
            } catch (Exception e) {
                log.debug("Template not found in database or delete failed: {}", id);
            }
            log.info("删除游戏模板: {}", id);
            return true;
        }
        return false;
    }

    /**
     * 检查模板是否存在
     *
     * @param id 模板ID
     * @return 是否存在
     */
    public boolean templateExists(String id) {
        return templates.containsKey(id);
    }

    /**
     * 根据用户描述匹配模板
     *
     * @param description 用户的游戏描述
     * @return 匹配的模板列表（按匹配度排序）
     */
    public List<GameTemplate> matchTemplates(String description) {
        if (description == null || description.isEmpty()) {
            return getAllTemplates();
        }

        String lowerDesc = description.toLowerCase();
        List<GameTemplate> matched = new ArrayList<>();

        for (GameTemplate template : templates.values()) {
            int score = calculateMatchScore(template, lowerDesc);
            if (score > 0) {
                matched.add(template);
            }
        }

        // 按匹配度排序
        matched.sort((a, b) -> {
            int scoreA = calculateMatchScore(a, lowerDesc);
            int scoreB = calculateMatchScore(b, lowerDesc);
            return Integer.compare(scoreB, scoreA);
        });

        return matched;
    }

    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(GameTemplate template, String description) {
        int score = 0;

        for (String keyword : template.getKeywords()) {
            if (description.contains(keyword.toLowerCase())) {
                score += 10;
            }
        }

        // 标题匹配
        if (description.contains(template.getName().toLowerCase())) {
            score += 20;
        }

        // 描述匹配
        if (description.contains(template.getDescription().toLowerCase())) {
            score += 5;
        }

        return score;
    }

    /**
     * 获取最佳匹配模板
     */
    public GameTemplate getBestMatch(String description) {
        List<GameTemplate> matched = matchTemplates(description);
        return matched.isEmpty() ? null : matched.get(0);
    }

    /**
     * 游戏模板内部类
     */
    public static class GameTemplate {
        private final String id;
        private final String name;
        private final String description;
        private final List<String> keywords;
        private final String skillName;
        private String content;
        /** 模板中的目录配置列表 */
        private List<TemplateDirectoryConfig> directoryConfigs = new ArrayList<>();

        public GameTemplate(String id, String name, String description, List<String> keywords, String skillName) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.keywords = keywords;
            this.skillName = skillName;
        }

        public void setContent(String content) { this.content = content; }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getKeywords() { return keywords; }
        public String getSkillName() { return skillName; }
        public String getContent() { return content; }

        public List<TemplateDirectoryConfig> getDirectoryConfigs() { return directoryConfigs; }
        public void setDirectoryConfigs(List<TemplateDirectoryConfig> directoryConfigs) {
            this.directoryConfigs = directoryConfigs != null ? directoryConfigs : new ArrayList<>();
        }
    }

    /**
     * 模板目录配置
     * 定义模板中的目录结构和各角色的访问权限
     */
    public static class TemplateDirectoryConfig {
        /** 目录路径（如 /client、/server） */
        private String path;
        /** 目录用途描述 */
        private String description;
        /** 可访问该目录的角色列表（为空表示所有角色都可访问） */
        private List<String> accessibleRoles = new ArrayList<>();
        /** 补充说明 */
        private String notes;

        public TemplateDirectoryConfig() {}

        public TemplateDirectoryConfig(String path, String description) {
            this.path = path;
            this.description = description;
        }

        public TemplateDirectoryConfig(String path, String description, List<String> accessibleRoles) {
            this.path = path;
            this.description = description;
            this.accessibleRoles = accessibleRoles != null ? accessibleRoles : new ArrayList<>();
        }

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getAccessibleRoles() { return accessibleRoles; }
        public void setAccessibleRoles(List<String> accessibleRoles) { this.accessibleRoles = accessibleRoles != null ? accessibleRoles : new ArrayList<>(); }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        /**
         * 检查指定角色是否可访问该目录
         *
         * @param role 角色名称
         * @return true 如果角色可访问（包括空角色列表表示所有角色可访问）
         */
        public boolean isAccessibleBy(String role) {
            return accessibleRoles.isEmpty() || accessibleRoles.contains(role);
        }
    }
}
