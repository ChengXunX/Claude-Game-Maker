package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.RolePromptLibrary;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识进化服务
 * 负责将 Agent 创建的文档和技能自动加入知识库，支持自进化自更新
 *
 * 主要功能：
 * - 监控 Agent 创建的文档，自动提取知识
 * - 监控 Agent 创建的技能，自动加入技能库
 * - 从成功的游戏生成中学习，更新模板和最佳实践
 * - 定期整理和优化知识库
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class KnowledgeEvolutionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeEvolutionService.class);

    /** 知识库存储目录 */
    private static final String KB_DIR = "data/knowledge-base";
    private static final String DOCS_DIR = KB_DIR + "/documents";
    private static final String LEARNED_SKILLS_DIR = KB_DIR + "/learned-skills";
    private static final String PATTERNS_DIR = KB_DIR + "/patterns";

    private final GameKnowledgeBase knowledgeBase;
    private final GameTemplateService templateService;
    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final RolePromptLibrary rolePromptLibrary;

    /** 已处理的文档哈希，避免重复处理 */
    private final Map<String, String> processedDocuments = new ConcurrentHashMap<>();

    /** 已学习的模式 */
    private final Map<String, LearnedPattern> learnedPatterns = new ConcurrentHashMap<>();

    /** 进化次数计数器 */
    private final java.util.concurrent.atomic.AtomicInteger evolutionCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public KnowledgeEvolutionService(GameKnowledgeBase knowledgeBase,
                                     GameTemplateService templateService,
                                     MemoryManager memoryManager,
                                     SkillManager skillManager,
                                     RolePromptLibrary rolePromptLibrary) {
        this.knowledgeBase = knowledgeBase;
        this.templateService = templateService;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
        this.rolePromptLibrary = rolePromptLibrary;
    }

    /**
     * 初始化知识进化服务
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(KB_DIR));
            Files.createDirectories(Path.of(DOCS_DIR));
            Files.createDirectories(Path.of(LEARNED_SKILLS_DIR));
            Files.createDirectories(Path.of(PATTERNS_DIR));

            loadLearnedPatterns();
            log.info("知识进化服务初始化完成");
        } catch (IOException e) {
            log.error("初始化知识进化服务失败", e);
        }
    }

    /**
     * 处理 Agent 创建的文档
     * 从文档中提取知识并加入知识库
     *
     * @param agentId Agent ID
     * @param documentPath 文档路径
     * @param documentContent 文档内容
     */
    public void processAgentDocument(String agentId, String documentPath, String documentContent) {
        String docHash = generateHash(documentContent);

        // 检查是否已处理过
        if (docHash.equals(processedDocuments.get(documentPath))) {
            log.debug("文档已处理过: {}", documentPath);
            return;
        }

        log.info("处理 Agent 文档: agent={}, path={}", agentId, documentPath);

        try {
            // 提取知识
            DocumentKnowledge knowledge = extractKnowledge(documentContent);

            // 保存到知识库
            saveDocumentKnowledge(agentId, documentPath, knowledge);

            // 记录已处理
            processedDocuments.put(documentPath, docHash);

            // 如果是游戏相关文档，尝试学习模式
            if (knowledge.isGameRelated()) {
                learnFromDocument(knowledge);
            }

            log.info("文档处理完成: agent={}, path={},知识点={}",
                agentId, documentPath, knowledge.getKeywords().size());

        } catch (Exception e) {
            log.error("处理文档失败: agent={}, path={}", agentId, documentPath, e);
        }
    }

    /**
     * 处理 Agent 创建的技能
     * 将技能加入知识库和技能管理器
     *
     * @param agentId Agent ID
     * @param skill 技能对象
     */
    public void processAgentSkill(String agentId, Skill skill) {
        log.info("处理 Agent 技能: agent={}, skill={}", agentId, skill.getId());

        try {
            // 保存到知识库
            saveSkillToKnowledgeBase(agentId, skill);

            // 注册到技能管理器
            skillManager.registerGlobalSkill(skill);
            skillManager.saveGlobalSkillToFile(skill);

            // 学习技能模式
            learnFromSkill(skill);

            log.info("技能处理完成: agent={}, skill={}", agentId, skill.getId());

        } catch (Exception e) {
            log.error("处理技能失败: agent={}, skill={}", agentId, skill.getId(), e);
        }
    }

    /**
     * 从成功的游戏生成中学习
     *
     * @param gameDescription 游戏描述
     * @param templateId 使用的模板
     * @param generatedCode 生成的代码
     * @param success 是否成功
     */
    public void learnFromGameGeneration(String gameDescription, String templateId,
                                        String generatedCode, boolean success) {
        log.info("从游戏生成中学习: template={}, success={}", templateId, success);

        // 记录模板使用
        knowledgeBase.recordTemplateUsage(templateId, gameDescription, success, 0);

        // 记录游戏学习事件
        knowledgeBase.recordGameLearning(templateId, gameDescription, success);

        if (success) {
            // 提取成功模式
            GamePattern pattern = extractGamePattern(gameDescription, generatedCode, templateId);
            saveGamePattern(pattern);

            // 更新最佳实践
            updateBestPractices(pattern);

            log.info("从成功案例中学习: template={}, 模式={}", templateId, pattern.getPatternType());
        } else {
            // 记录失败原因，用于改进
            log.info("记录失败案例: template={}", templateId);
        }
    }

    /**
     * 从 Agent 记忆中提取知识
     *
     * @param agentId Agent ID
     * @param project 项目
     */
    public void extractKnowledgeFromMemory(String agentId, GameProject project) {
        log.info("从 Agent 记忆中提取知识: agent={}, project={}", agentId,
            project != null ? project.getId() : "null");

        if (project == null) {
            log.warn("无法提取知识：项目为空");
            return;
        }

        try {
            // 加载 Agent 的所有记忆
            Map<String, String> memories = memoryManager.getAllMemory(project, agentId);

            for (Map.Entry<String, String> entry : memories.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 提取有价值的知识
                if (isValuableKnowledge(key, value)) {
                    saveExtractedKnowledge(agentId, key, value);
                }
            }

            log.info("从 Agent 记忆中提取知识完成: agent={}", agentId);

        } catch (Exception e) {
            log.error("从 Agent 记忆中提取知识失败: agent={}", agentId, e);
        }
    }

    /**
     * 定期整理知识库（每天凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void organizeKnowledgeBase() {
        log.info("开始整理知识库...");

        try {
            // 合并重复知识
            mergeDuplicateKnowledge();

            // 清理过期知识
            cleanupExpiredKnowledge();

            // 优化知识索引
            optimizeKnowledgeIndex();

            // 保存学习到的模式
            saveLearnedPatterns();

            evolutionCount.incrementAndGet();

            // 记录进化使用事件
            knowledgeBase.recordEvolution("organize", "知识库整理完成，累计 " + evolutionCount.get() + " 次", true);

            log.info("知识库整理完成，累计进化次数: {}", evolutionCount.get());

        } catch (Exception e) {
            log.error("整理知识库失败", e);
            knowledgeBase.recordEvolution("organize", "知识库整理失败: " + e.getMessage(), false);
        }
    }

    /**
     * 定期自进化（每6小时）
     */
    @Scheduled(fixedRate = 21600000)
    public void selfEvolve() {
        log.info("开始知识自进化...");

        try {
            // 分析成功模式
            analyzeSuccessPatterns();

            // 更新模板推荐
            updateTemplateRecommendations();

            // 生成新的最佳实践
            generateNewBestPractices();

            // 项目经验全局优化
            optimizeGloballyFromProjects();

            // 进化角色提示词
            evolveRolePrompts();

            // 保存学习到的模式（每次进化后都保存，不等凌晨 2 点）
            saveLearnedPatterns();

            evolutionCount.incrementAndGet();

            // 记录进化使用事件
            knowledgeBase.recordEvolution("self_evolve", "自进化完成，累计 " + evolutionCount.get() + " 次", true);

            log.info("知识自进化完成，累计进化次数: {}", evolutionCount.get());

        } catch (Exception e) {
            log.error("知识自进化失败", e);
            knowledgeBase.recordEvolution("self_evolve", "自进化失败: " + e.getMessage(), false);
        }
    }

    /**
     * 从项目经验中全局优化
     * 将各项目的成功经验应用到全局知识库
     */
    public void optimizeGloballyFromProjects() {
        log.info("开始从项目经验中全局优化...");

        try {
            // 1. 收集所有项目的成功经验
            Map<String, List<ProjectExperience>> allExperiences = collectProjectExperiences();

            // 2. 分析共性模式
            Map<String, CommonPattern> commonPatterns = analyzeCommonPatterns(allExperiences);

            // 3. 更新全局技能
            updateGlobalSkillsFromPatterns(commonPatterns);

            // 4. 更新全局模板
            updateGlobalTemplatesFromPatterns(commonPatterns);

            // 5. 更新最佳实践
            updateBestPracticesFromPatterns(commonPatterns);

            log.info("项目经验全局优化完成，发现 {} 个共性模式", commonPatterns.size());

        } catch (Exception e) {
            log.error("项目经验全局优化失败", e);
        }
    }

    /**
     * 收集所有项目的成功经验
     * 从 learned-skills 文件中提取项目经验
     */
    private Map<String, List<ProjectExperience>> collectProjectExperiences() {
        Map<String, List<ProjectExperience>> experiences = new HashMap<>();

        try {
            Path skillsDir = Path.of(LEARNED_SKILLS_DIR);
            if (!Files.exists(skillsDir)) return experiences;

            Files.list(skillsDir)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String description = extractFrontmatterField(content, "description");
                        String learnedFrom = extractFrontmatterField(content, "learned-from");
                        String name = extractFrontmatterField(content, "name");

                        if (description != null && learnedFrom != null) {
                            // learnedFrom 格式: projectId:agentRole
                            String projectId = learnedFrom.contains(":")
                                ? learnedFrom.substring(0, learnedFrom.indexOf(':'))
                                : "global";

                            ProjectExperience exp = new ProjectExperience();
                            exp.setProjectId(projectId);
                            exp.setDescription(description);
                            exp.setSource(learnedFrom);

                            experiences.computeIfAbsent(projectId, k -> new ArrayList<>()).add(exp);
                        }
                    } catch (IOException e) {
                        log.debug("读取项目经验文件失败: {}", path);
                    }
                });
        } catch (IOException e) {
            log.warn("读取项目经验失败: {}", e.getMessage());
        }

        return experiences;
    }

    /**
     * 分析共性模式
     */
    private Map<String, CommonPattern> analyzeCommonPatterns(Map<String, List<ProjectExperience>> experiences) {
        Map<String, CommonPattern> patterns = new HashMap<>();

        // 分析成功案例中的共性
        for (Map.Entry<String, List<ProjectExperience>> entry : experiences.entrySet()) {
            String category = entry.getKey();
            List<ProjectExperience> projectExperiences = entry.getValue();

            if (!projectExperiences.isEmpty()) {
                CommonPattern pattern = extractCommonPattern(projectExperiences);
                if (pattern != null) {
                    patterns.put(category, pattern);
                }
            }
        }

        return patterns;
    }

    /**
     * 从模式中更新全局技能
     */
    private void updateGlobalSkillsFromPatterns(Map<String, CommonPattern> patterns) {
        for (Map.Entry<String, CommonPattern> entry : patterns.entrySet()) {
            CommonPattern pattern = entry.getValue();

            if (pattern.getSkillTemplate() != null) {
                Skill skill = createSkillFromPattern(pattern);
                if (skill != null) {
                    skillManager.registerGlobalSkill(skill);
                    skillManager.saveGlobalSkillToFile(skill);
                    log.info("从项目经验中创建全局技能: {}", skill.getId());
                }
            }
        }
    }

    /**
     * 从模式中更新全局模板
     */
    private void updateGlobalTemplatesFromPatterns(Map<String, CommonPattern> patterns) {
        // 更新游戏模板推荐
        for (Map.Entry<String, CommonPattern> entry : patterns.entrySet()) {
            CommonPattern pattern = entry.getValue();

            if (pattern.getTemplateRecommendation() != null) {
                // 更新模板推荐权重
                log.info("更新模板推荐: {}", pattern.getTemplateRecommendation());
            }
        }
    }

    /**
     * 从模式中更新最佳实践
     */
    private void updateBestPracticesFromPatterns(Map<String, CommonPattern> patterns) {
        for (Map.Entry<String, CommonPattern> entry : patterns.entrySet()) {
            CommonPattern pattern = entry.getValue();

            if (pattern.getBestPractice() != null) {
                saveBestPractice(entry.getKey(), pattern.getBestPractice());
                log.info("更新最佳实践: {}", entry.getKey());
            }
        }
    }

    /**
     * 从模式创建技能
     */
    private Skill createSkillFromPattern(CommonPattern pattern) {
        if (pattern.getSkillTemplate() == null) return null;

        Skill skill = new Skill();
        skill.setId("learned-" + pattern.getPatternType() + "-" + System.currentTimeMillis());
        skill.setName(pattern.getPatternName());
        skill.setDescription(pattern.getDescription());
        skill.setCategory("learned");
        skill.setPrompt(pattern.getSkillTemplate());
        skill.setTriggerPattern(pattern.getTriggerKeywords());

        return skill;
    }

    /**
     * 项目经验类
     */
    public static class ProjectExperience {
        private String projectId;
        private String projectType;
        private String successFactor;
        private String teamConfiguration;
        private String technicalStack;
        private long duration;
        private boolean success;
        private String description;
        private String source;

        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getProjectType() { return projectType; }
        public void setProjectType(String projectType) { this.projectType = projectType; }
        public String getSuccessFactor() { return successFactor; }
        public void setSuccessFactor(String successFactor) { this.successFactor = successFactor; }
        public String getTeamConfiguration() { return teamConfiguration; }
        public void setTeamConfiguration(String teamConfiguration) { this.teamConfiguration = teamConfiguration; }
        public String getTechnicalStack() { return technicalStack; }
        public void setTechnicalStack(String technicalStack) { this.technicalStack = technicalStack; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    /**
     * 共性模式类
     */
    public static class CommonPattern {
        private String patternType;
        private String patternName;
        private String description;
        private String skillTemplate;
        private String triggerKeywords;
        private String templateRecommendation;
        private String bestPractice;
        private int occurrenceCount;

        // Getters and Setters
        public String getPatternType() { return patternType; }
        public void setPatternType(String patternType) { this.patternType = patternType; }
        public String getPatternName() { return patternName; }
        public void setPatternName(String patternName) { this.patternName = patternName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSkillTemplate() { return skillTemplate; }
        public void setSkillTemplate(String skillTemplate) { this.skillTemplate = skillTemplate; }
        public String getTriggerKeywords() { return triggerKeywords; }
        public void setTriggerKeywords(String triggerKeywords) { this.triggerKeywords = triggerKeywords; }
        public String getTemplateRecommendation() { return templateRecommendation; }
        public void setTemplateRecommendation(String templateRecommendation) { this.templateRecommendation = templateRecommendation; }
        public String getBestPractice() { return bestPractice; }
        public void setBestPractice(String bestPractice) { this.bestPractice = bestPractice; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    }

    /**
     * 提取共性模式
     */
    private CommonPattern extractCommonPattern(List<ProjectExperience> experiences) {
        if (experiences.isEmpty()) return null;

        CommonPattern pattern = new CommonPattern();
        pattern.setOccurrenceCount(experiences.size());

        // 分析成功因素（优先用 successFactor，没有则用 description）
        Map<String, Integer> factorCount = new HashMap<>();
        for (ProjectExperience exp : experiences) {
            String factor = exp.getSuccessFactor() != null ? exp.getSuccessFactor() : exp.getDescription();
            if (factor != null) {
                factorCount.merge(factor, 1, Integer::sum);
            }
        }

        // 找出最常见的成功因素
        String mostCommonFactor = factorCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        if (mostCommonFactor != null) {
            pattern.setPatternType(mostCommonFactor);
            pattern.setPatternName("成功模式: " + mostCommonFactor);
            pattern.setDescription("基于 " + experiences.size() + " 个项目的成功经验");
        }

        return pattern;
    }

    // ===== 内部方法 =====

    /**
     * 从文档中提取知识
     */
    private DocumentKnowledge extractKnowledge(String content) {
        DocumentKnowledge knowledge = new DocumentKnowledge();

        // 提取标题
        String[] lines = content.split("\n");
        if (lines.length > 0) {
            knowledge.setTitle(lines[0].replaceAll("^#+\\s*", "").trim());
        }

        // 提取关键词
        List<String> keywords = extractKeywords(content);
        knowledge.setKeywords(keywords);

        // 判断是否游戏相关
        boolean gameRelated = content.contains("游戏") || content.contains("game") ||
            content.contains("player") || content.contains("玩家") ||
            content.contains("level") || content.contains("关卡");
        knowledge.setGameRelated(gameRelated);

        // 提取代码片段
        List<String> codeSnippets = extractCodeSnippets(content);
        knowledge.setCodeSnippets(codeSnippets);

        return knowledge;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        List<String> keywords = new ArrayList<>();

        // 简单的关键词提取
        String[] commonKeywords = {
            "template", "模板", "skill", "技能", "game", "游戏",
            "player", "玩家", "enemy", "敌人", "level", "关卡",
            "score", "分数", "health", "生命", "attack", "攻击",
            "defense", "防御", "weapon", "武器", "armor", "装甲"
        };

        String lowerContent = content.toLowerCase();
        for (String keyword : commonKeywords) {
            if (lowerContent.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        return keywords;
    }

    /**
     * 提取代码片段
     */
    private List<String> extractCodeSnippets(String content) {
        List<String> snippets = new ArrayList<>();

        // 提取 ``` 包裹的代码块
        int start = 0;
        while (true) {
            int begin = content.indexOf("```", start);
            if (begin == -1) break;

            int end = content.indexOf("```", begin + 3);
            if (end == -1) break;

            String snippet = content.substring(begin + 3, end).trim();
            if (!snippet.isEmpty()) {
                snippets.add(snippet);
            }

            start = end + 3;
        }

        return snippets;
    }

    /**
     * 保存文档知识
     */
    private void saveDocumentKnowledge(String agentId, String documentPath, DocumentKnowledge knowledge) {
        try {
            Path kbPath = Path.of(DOCS_DIR, agentId, knowledge.getTitle() + ".json");
            Files.createDirectories(kbPath.getParent());

            // 简化实现：保存为文本格式
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(knowledge.getTitle()).append("\n\n");
            sb.append("来源: ").append(documentPath).append("\n");
            sb.append("Agent: ").append(agentId).append("\n");
            sb.append("时间: ").append(LocalDateTime.now()).append("\n\n");
            sb.append("关键词: ").append(String.join(", ", knowledge.getKeywords())).append("\n\n");

            Files.writeString(kbPath, sb.toString());

        } catch (IOException e) {
            log.error("保存文档知识失败", e);
        }
    }

    /**
     * 保存技能到知识库
     */
    private void saveSkillToKnowledgeBase(String agentId, Skill skill) {
        try {
            Path kbPath = Path.of(LEARNED_SKILLS_DIR, skill.getId() + ".md");
            Files.createDirectories(kbPath.getParent());

            StringBuilder sb = new StringBuilder();
            sb.append("---\n");
            sb.append("name: ").append(skill.getId()).append("\n");
            sb.append("description: ").append(skill.getDescription()).append("\n");
            sb.append("learned-from: ").append(agentId).append("\n");
            sb.append("learned-at: ").append(LocalDateTime.now()).append("\n");
            sb.append("---\n\n");
            sb.append("# ").append(skill.getName()).append("\n\n");
            sb.append(skill.getDescription()).append("\n");

            Files.writeString(kbPath, sb.toString());

        } catch (IOException e) {
            log.error("保存技能到知识库失败", e);
        }
    }

    /**
     * 从文档中学习
     */
    private void learnFromDocument(DocumentKnowledge knowledge) {
        // 提取游戏模式
        for (String keyword : knowledge.getKeywords()) {
            LearnedPattern pattern = learnedPatterns.get(keyword);
            if (pattern == null) {
                pattern = new LearnedPattern(keyword);
                learnedPatterns.put(keyword, pattern);
            }
            pattern.incrementFrequency();
            pattern.addSource(knowledge.getTitle());
        }
    }

    /**
     * 从技能中学习
     */
    private void learnFromSkill(Skill skill) {
        String patternKey = "skill:" + skill.getId();
        LearnedPattern pattern = learnedPatterns.get(patternKey);
        if (pattern == null) {
            pattern = new LearnedPattern(patternKey);
            learnedPatterns.put(patternKey, pattern);
        }
        pattern.incrementFrequency();
        pattern.addSource("skill:" + skill.getId());
    }

    /**
     * 提取游戏模式
     */
    private GamePattern extractGamePattern(String description, String code, String templateId) {
        GamePattern pattern = new GamePattern();
        pattern.setTemplateId(templateId);
        pattern.setDescription(description);
        pattern.setCreatedAt(LocalDateTime.now());

        // 分析代码中的模式
        if (code.contains("Phaser.Physics.Arcade")) {
            pattern.setPatternType("arcade-physics");
        } else if (code.contains("Phaser.GameObjects")) {
            pattern.setPatternType("game-objects");
        } else if (code.contains("socket.io")) {
            pattern.setPatternType("multiplayer");
        }

        return pattern;
    }

    /**
     * 保存游戏模式
     */
    private void saveGamePattern(GamePattern pattern) {
        try {
            Path patternPath = Path.of(PATTERNS_DIR, pattern.getPatternType() + ".json");
            Files.createDirectories(patternPath.getParent());

            StringBuilder sb = new StringBuilder();
            sb.append("模板: ").append(pattern.getTemplateId()).append("\n");
            sb.append("类型: ").append(pattern.getPatternType()).append("\n");
            sb.append("描述: ").append(pattern.getDescription()).append("\n");
            sb.append("时间: ").append(pattern.getCreatedAt()).append("\n");

            Files.writeString(patternPath, sb.toString());

        } catch (IOException e) {
            log.error("保存游戏模式失败", e);
        }
    }

    /**
     * 更新最佳实践
     */
    private void updateBestPractices(GamePattern pattern) {
        knowledgeBase.recordBestPractice(
            pattern.getPatternType(),
            "成功模式: " + pattern.getPatternType(),
            "从成功的游戏生成中学习到的模式，模板: " + pattern.getTemplateId()
        );
    }

    /**
     * 保存最佳实践
     *
     * @param category 分类
     * @param content 内容
     */
    private void saveBestPractice(String category, String content) {
        try {
            Path bpPath = Path.of(PATTERNS_DIR, "best-practices", category.replace("/", "_") + ".md");
            Files.createDirectories(bpPath.getParent());
            Files.writeString(bpPath, content);
            log.info("保存最佳实践: {}", category);
        } catch (IOException e) {
            log.error("保存最佳实践失败: {}", category, e);
        }
    }

    /**
     * 判断是否有价值的知识
     */
    private boolean isValuableKnowledge(String key, String value) {
        // 过滤掉无价值的知识
        if (key.startsWith("temp/") || key.startsWith("debug/")) {
            return false;
        }

        // 包含代码的知识通常有价值
        if (value.contains("function") || value.contains("class") || value.contains("import")) {
            return true;
        }

        // 包含解决方案的知识有价值
        if (value.contains("solution") || value.contains("fix") || value.contains("解决")) {
            return true;
        }

        return value.length() > 100; // 长文档通常有价值
    }

    /**
     * 保存提取的知识
     */
    private void saveExtractedKnowledge(String agentId, String key, String value) {
        try {
            Path kbPath = Path.of(DOCS_DIR, "extracted", agentId, key.replace("/", "_") + ".md");
            Files.createDirectories(kbPath.getParent());
            Files.writeString(kbPath, value);
        } catch (IOException e) {
            log.error("保存提取的知识失败", e);
        }
    }

    /**
     * 生成哈希值
     */
    private String generateHash(String content) {
        return String.valueOf(content.hashCode());
    }

    /**
     * 合并重复知识
     * 委托给 GameKnowledgeBase 执行解决方案去重合并
     */
    private void mergeDuplicateKnowledge() {
        log.info("合并重复解决方案...");
        knowledgeBase.mergeDuplicateSolutions();
    }

    /**
     * 清理过期知识
     * 委托给 GameKnowledgeBase 执行过期归档
     */
    private void cleanupExpiredKnowledge() {
        log.info("归档过期解决方案...");
        knowledgeBase.archiveExpiredSolutions();
    }

    /**
     * 优化知识索引
     * 委托给 GameKnowledgeBase 生成精华摘要
     */
    private void optimizeKnowledgeIndex() {
        log.info("生成精华摘要...");
        knowledgeBase.generateEssences();
    }

    /**
     * 加载学习到的模式
     */
    private void loadLearnedPatterns() {
        Path patternsFile = Path.of(PATTERNS_DIR, "learned-patterns.json");
        if (!Files.exists(patternsFile)) {
            log.debug("No learned patterns file found");
            return;
        }

        try {
            String content = Files.readString(patternsFile);
            // 简单解析：每行一个 patternKey|frequency|sources
            for (String line : content.split("\n")) {
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 2) {
                    LearnedPattern pattern = new LearnedPattern(parts[0]);
                    pattern.frequency = Integer.parseInt(parts[1]);
                    if (parts.length > 2) {
                        for (String src : parts[2].split(",")) {
                            pattern.addSource(src);
                        }
                    }
                    learnedPatterns.put(parts[0], pattern);
                }
            }
            log.info("Loaded {} learned patterns", learnedPatterns.size());
        } catch (Exception e) {
            log.warn("Failed to load learned patterns: {}", e.getMessage());
        }
    }

    /**
     * 保存学习到的模式
     */
    private void saveLearnedPatterns() {
        try {
            Files.createDirectories(Path.of(PATTERNS_DIR));
            Path patternsFile = Path.of(PATTERNS_DIR, "learned-patterns.json");

            StringBuilder sb = new StringBuilder();
            for (LearnedPattern pattern : learnedPatterns.values()) {
                sb.append(pattern.getKey()).append("|")
                  .append(pattern.getFrequency()).append("|")
                  .append(String.join(",", pattern.getSources()))
                  .append("\n");
            }

            Files.writeString(patternsFile, sb.toString());
            log.info("Saved {} learned patterns", learnedPatterns.size());
        } catch (Exception e) {
            log.warn("Failed to save learned patterns: {}", e.getMessage());
        }
    }

    /**
     * 分析成功模式
     */
    private void analyzeSuccessPatterns() {
        log.debug("分析成功模式...");
    }

    /**
     * 更新模板推荐
     */
    private void updateTemplateRecommendations() {
        log.debug("更新模板推荐...");
    }

    /**
     * 生成新的最佳实践
     */
    private void generateNewBestPractices() {
        log.debug("生成新的最佳实践...");
    }

    // ===== 内部类 =====

    /**
     * 文档知识
     */
    private static class DocumentKnowledge {
        private String title;
        private List<String> keywords = new ArrayList<>();
        private List<String> codeSnippets = new ArrayList<>();
        private boolean gameRelated;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        public List<String> getCodeSnippets() { return codeSnippets; }
        public void setCodeSnippets(List<String> codeSnippets) { this.codeSnippets = codeSnippets; }
        public boolean isGameRelated() { return gameRelated; }
        public void setGameRelated(boolean gameRelated) { this.gameRelated = gameRelated; }
    }

    /**
     * 学习到的模式
     */
    private static class LearnedPattern {
        private final String key;
        private String patternType;
        private int frequency;
        private List<String> sources = new ArrayList<>();
        private List<String> examples = new ArrayList<>();
        private String solution;
        private LocalDateTime lastOccurrence;

        public LearnedPattern() {
            this.key = "";
            this.frequency = 0;
        }

        public LearnedPattern(String key) {
            this.key = key;
            this.frequency = 0;
        }

        public void incrementFrequency() { this.frequency++; }
        public void addSource(String source) { this.sources.add(source); }
        public String getKey() { return key; }
        public int getFrequency() { return frequency; }
        public List<String> getSources() { return sources; }
    }

    // ===== Agent 查询接口 =====

    /**
     * 查询与任务相关的知识
     * 搜索已学习的模式、解决方案和最佳实践，返回可用于 prompt 注入的知识文本
     *
     * @param query 任务描述或查询关键词
     * @param agentRole Agent 角色（用于角色相关知识过滤）
     * @return 格式化的知识文本，可直接注入 prompt
     */
    /** 知识查询最大输出长度 */
    private static final int MAX_KNOWLEDGE_QUERY_LENGTH = 1000;

    public String queryRelevantKnowledge(String query, String agentRole) {
        if (query == null || query.isEmpty()) return "";

        StringBuilder knowledge = new StringBuilder();
        String lowerQuery = query.toLowerCase();

        // 1. 搜索已学习的模式（最多 3 条）
        List<LearnedPattern> matchedPatterns = learnedPatterns.values().stream()
            .filter(p -> {
                String key = p.getKey().toLowerCase();
                return lowerQuery.contains(key) || key.contains(lowerQuery.split("\\s+")[0]);
            })
            .sorted((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()))
            .limit(3)
            .toList();

        if (!matchedPatterns.isEmpty()) {
            knowledge.append("### 学习到的模式\n\n");
            for (LearnedPattern pattern : matchedPatterns) {
                knowledge.append(String.format("- **%s** (频率: %d)\n",
                    pattern.getKey(), pattern.getFrequency()));
            }
            knowledge.append("\n");
        }

        // 2. 搜索解决方案（优先使用精华摘要，fallback 到 Top 3 精选方案）
        Map<String, List<GameKnowledgeBase.Solution>> allSolutions = knowledgeBase.getAllSolutions();
        for (Map.Entry<String, List<GameKnowledgeBase.Solution>> entry : allSolutions.entrySet()) {
            String problemType = entry.getKey();
            if (lowerQuery.contains(problemType.toLowerCase()) || problemType.toLowerCase().contains(lowerQuery.split("\\s+")[0])) {
                // 先尝试精华摘要
                String essence = knowledgeBase.getEssence(problemType);
                if (essence != null && !essence.isEmpty()) {
                    knowledge.append("### 已知方案: ").append(problemType).append(" (精华)\n\n");
                    // 精华内容已较精简，取前 500 字符
                    String trimmed = essence.length() > 500 ? essence.substring(0, 500) + "..." : essence;
                    knowledge.append(trimmed).append("\n\n");
                } else {
                    // fallback 到精选 Top 3
                    List<GameKnowledgeBase.Solution> topSols = knowledgeBase.getTopSolutions(problemType, 3);
                    if (!topSols.isEmpty()) {
                        knowledge.append("### 已知方案: ").append(problemType).append("\n\n");
                        for (GameKnowledgeBase.Solution sol : topSols) {
                            knowledge.append(String.format("- **%s**: %s\n",
                                sol.getProblemDescription().length() > 60 ? sol.getProblemDescription().substring(0, 60) + "..." : sol.getProblemDescription(),
                                sol.getSolution().length() > 150 ? sol.getSolution().substring(0, 150) + "..." : sol.getSolution()));
                        }
                        knowledge.append("\n");
                    }
                }
            }
        }

        // 3. 搜索最佳实践（最多 2 条）
        List<GameKnowledgeBase.BestPractice> matchedPractices = knowledgeBase.getAllBestPractices().stream()
            .filter(bp -> {
                String combined = (bp.getCategory() + " " + bp.getTitle() + " " + bp.getContent()).toLowerCase();
                for (String word : lowerQuery.split("\\s+")) {
                    if (word.length() > 2 && combined.contains(word)) return true;
                }
                return false;
            })
            .limit(2)
            .toList();

        if (!matchedPractices.isEmpty()) {
            knowledge.append("### 最佳实践\n\n");
            for (GameKnowledgeBase.BestPractice bp : matchedPractices) {
                knowledge.append(String.format("- **%s** (%s): %s\n",
                    bp.getTitle(), bp.getCategory(),
                    bp.getContent().length() > 150 ? bp.getContent().substring(0, 150) + "..." : bp.getContent()));
            }
            knowledge.append("\n");
        }

        // 总体截断保护
        String result = knowledge.toString();
        if (result.length() > MAX_KNOWLEDGE_QUERY_LENGTH) {
            result = result.substring(0, MAX_KNOWLEDGE_QUERY_LENGTH) + "\n...(已截断)";
        }
        return result;
    }

    /**
     * 将项目级学习的技能提升为全局技能
     * 使其他项目也能使用该技能
     *
     * @param skill 要提升的技能
     * @param agentId 学习该技能的 Agent ID
     */
    public void promoteLearnedSkill(Skill skill, String agentId) {
        if (skill == null) return;

        log.info("Promoting learned skill to global: {} (learned by {})", skill.getId(), agentId);

        try {
            // 注册为全局技能
            skillManager.registerGlobalSkill(skill);
            skillManager.saveGlobalSkillToFile(skill);

            // 保存到知识库的 learned-skills 目录
            saveSkillToKnowledgeBase(agentId, skill);

            log.info("Skill promoted to global: {}", skill.getId());
        } catch (Exception e) {
            log.error("Failed to promote skill to global: {}", skill.getId(), e);
        }
    }

    /**
     * 从任务完成中提取洞察并加入知识库
     * 成功的任务提取模式，失败的任务记录解决方案
     *
     * @param agentId Agent ID
     * @param taskDescription 任务描述
     * @param result 执行结果
     * @param success 是否成功
     */
    public void extractInsightsFromTaskCompletion(String agentId, String taskDescription,
                                                   String result, boolean success) {
        if (taskDescription == null || taskDescription.isEmpty()) return;

        log.info("Extracting insights from task completion: agent={}, success={}", agentId, success);

        try {
            if (success) {
                // 成功：提取模式
                String patternKey = extractPatternKey(taskDescription);
                LearnedPattern pattern = learnedPatterns.get(patternKey);
                if (pattern == null) {
                    pattern = new LearnedPattern(patternKey);
                    learnedPatterns.put(patternKey, pattern);
                }
                pattern.incrementFrequency();
                pattern.addSource(agentId + ":" + System.currentTimeMillis());

                // 如果模式频率达到阈值，生成最佳实践
                if (pattern.getFrequency() >= 3) {
                    String bestPractice = String.format(
                        "Based on %d successful completions of similar tasks. Key pattern: %s",
                        pattern.getFrequency(), patternKey);
                    saveBestPractice("task_pattern", bestPractice);
                }
            } else {
                // 失败：记录解决方案（如果有）
                if (result != null && result.length() > 50) {
                    String problemType = extractPatternKey(taskDescription);
                    knowledgeBase.recordSolution(problemType, taskDescription,
                        result.length() > 500 ? result.substring(0, 500) : result);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract insights from task completion", e);
        }
    }

    /**
     * 从游戏验证结果中学习
     * 将验证通过/失败的经验保存到知识库，供后续项目参考
     *
     * @param agentId 发起验证的 Agent ID
     * @param projectId 项目 ID
     * @param projectName 项目名称
     * @param overallScore 总分 (0-100)
     * @param dimensionScores 各维度评分 Map
     * @param issues 发现的问题列表
     * @param suggestions 改进建议列表
     * @param passed 是否通过验证
     */
    public void learnFromGameVerification(String agentId, String projectId, String projectName,
                                           int overallScore, Map<String, Integer> dimensionScores,
                                           List<String> issues, List<String> suggestions, boolean passed) {
        log.info("从游戏验证中学习: project={}, score={}, passed={}", projectName, overallScore, passed);

        try {
            // 1. 记录验证模式
            String patternKey = "game_verification_" + (passed ? "pass" : "fail");
            LearnedPattern pattern = learnedPatterns.get(patternKey);
            if (pattern == null) {
                pattern = new LearnedPattern(patternKey);
                learnedPatterns.put(patternKey, pattern);
            }
            pattern.incrementFrequency();
            pattern.addSource(projectId + ":" + agentId + ":" + System.currentTimeMillis());

            // 2. 如果验证失败，记录问题和解决方案
            if (!passed && issues != null && !issues.isEmpty()) {
                String problemType = "game_quality_issues";
                StringBuilder problemDesc = new StringBuilder();
                problemDesc.append(String.format("项目 [%s] 验证未通过 (总分: %d/100)\n", projectName, overallScore));
                problemDesc.append("问题:\n");
                issues.forEach(issue -> problemDesc.append("- ").append(issue).append("\n"));

                StringBuilder solution = new StringBuilder();
                if (suggestions != null && !suggestions.isEmpty()) {
                    solution.append("改进建议:\n");
                    suggestions.forEach(s -> solution.append("- ").append(s).append("\n"));
                }

                knowledgeBase.recordSolution(problemType, problemDesc.toString(), solution.toString());

                // 记录各维度低分的解决方案
                if (dimensionScores != null) {
                    for (Map.Entry<String, Integer> entry : dimensionScores.entrySet()) {
                        if (entry.getValue() < 60) {
                            String dimProblem = "game_" + entry.getKey() + "_low";
                            knowledgeBase.recordSolution(dimProblem,
                                String.format("%s 评分不足: %d/100", entry.getKey(), entry.getValue()),
                                String.format("需要改进 %s 方面，参考建议: %s",
                                    entry.getKey(),
                                    suggestions != null ? String.join("; ", suggestions) : "无"));
                        }
                    }
                }
            }

            // 3. 如果验证通过，提取成功模式
            if (passed) {
                String successPattern = "game_verification_success";
                LearnedPattern successP = learnedPatterns.get(successPattern);
                if (successP == null) {
                    successP = new LearnedPattern(successPattern);
                    learnedPatterns.put(successPattern, successP);
                }
                successP.incrementFrequency();
                successP.addSource(projectId + ":" + overallScore);

                // 如果多次验证成功，生成最佳实践
                if (successP.getFrequency() >= 3) {
                    String bestPractice = String.format(
                        "基于 %d 次成功验证的经验，游戏项目质量关键因素：\n" +
                        "- 总分通常在 %d 分以上\n" +
                        "- 各维度均衡发展\n" +
                        "- 核心玩法完整且可玩",
                        successP.getFrequency(), overallScore);
                    saveBestPractice("game_verification", bestPractice);
                }
            }

            // 4. 保存验证历史到知识库文档
            saveVerificationHistory(agentId, projectId, projectName, overallScore, dimensionScores, issues, suggestions, passed);

            log.info("游戏验证学习完成: project={}, passed={}", projectName, passed);

        } catch (Exception e) {
            log.error("从游戏验证中学习失败", e);
        }
    }

    /**
     * 保存验证历史到知识库文档
     */
    private void saveVerificationHistory(String agentId, String projectId, String projectName,
                                          int overallScore, Map<String, Integer> dimensionScores,
                                          List<String> issues, List<String> suggestions, boolean passed) {
        try {
            Path historyPath = Path.of(KB_DIR, "verification-history", projectId + ".md");
            Files.createDirectories(historyPath.getParent());

            StringBuilder sb = new StringBuilder();

            // 如果文件已存在，读取现有内容
            if (Files.exists(historyPath)) {
                sb.append(Files.readString(historyPath));
                sb.append("\n---\n\n");
            }

            sb.append(String.format("## 验证记录 - %s\n\n", LocalDateTime.now()));
            sb.append(String.format("- 项目: %s\n", projectName));
            sb.append(String.format("- 总分: %d/100\n", overallScore));
            sb.append(String.format("- 结果: %s\n", passed ? "✅ 通过" : "❌ 未通过"));
            sb.append(String.format("- 发起者: %s\n\n", agentId));

            if (dimensionScores != null && !dimensionScores.isEmpty()) {
                sb.append("### 各维度评分\n\n");
                for (Map.Entry<String, Integer> entry : dimensionScores.entrySet()) {
                    String status = entry.getValue() >= 60 ? "✅" : "⚠️";
                    sb.append(String.format("- %s %s: %d/100\n", status, entry.getKey(), entry.getValue()));
                }
                sb.append("\n");
            }

            if (issues != null && !issues.isEmpty()) {
                sb.append("### 发现的问题\n\n");
                issues.forEach(issue -> sb.append("- ").append(issue).append("\n"));
                sb.append("\n");
            }

            if (suggestions != null && !suggestions.isEmpty()) {
                sb.append("### 改进建议\n\n");
                suggestions.forEach(s -> sb.append("- ").append(s).append("\n"));
                sb.append("\n");
            }

            Files.writeString(historyPath, sb.toString());

        } catch (IOException e) {
            log.error("保存验证历史失败", e);
        }
    }

    /**
     * 查询游戏验证相关的知识
     * 返回与游戏质量、验证相关的最佳实践和解决方案
     *
     * @param query 查询关键词
     * @return 格式化的知识文本
     */
    public String queryVerificationKnowledge(String query) {
        StringBuilder knowledge = new StringBuilder();

        // 1. 查询验证相关的解决方案（优先精华，fallback 精选）
        Map<String, List<GameKnowledgeBase.Solution>> allSolutions = knowledgeBase.getAllSolutions();
        for (Map.Entry<String, List<GameKnowledgeBase.Solution>> entry : allSolutions.entrySet()) {
            String problemType = entry.getKey();
            if (problemType.contains("game_") || problemType.contains("verification")) {
                String essence = knowledgeBase.getEssence(problemType);
                if (essence != null && !essence.isEmpty()) {
                    knowledge.append(String.format("### %s (精华)\n\n", problemType));
                    String trimmed = essence.length() > 400 ? essence.substring(0, 400) + "..." : essence;
                    knowledge.append(trimmed).append("\n\n");
                } else {
                    List<GameKnowledgeBase.Solution> topSols = knowledgeBase.getTopSolutions(problemType, 3);
                    if (!topSols.isEmpty()) {
                        knowledge.append(String.format("### %s\n\n", problemType));
                        for (GameKnowledgeBase.Solution sol : topSols) {
                            knowledge.append(String.format("- **问题**: %s\n  **解决方案**: %s\n\n",
                                sol.getProblemDescription().length() > 100
                                    ? sol.getProblemDescription().substring(0, 100) + "..." : sol.getProblemDescription(),
                                sol.getSolution().length() > 200
                                    ? sol.getSolution().substring(0, 200) + "..." : sol.getSolution()));
                        }
                    }
                }
            }
        }

        // 2. 查询验证相关的最佳实践
        List<GameKnowledgeBase.BestPractice> bestPractices = knowledgeBase.getAllBestPractices();
        for (GameKnowledgeBase.BestPractice bp : bestPractices) {
            if (bp.getCategory().contains("game") || bp.getCategory().contains("verification")) {
                knowledge.append(String.format("### 最佳实践: %s\n\n%s\n\n", bp.getTitle(), bp.getContent()));
            }
        }

        return knowledge.toString();
    }

    /**
     * 进化角色提示词
     * 分析项目经验中的成功模式，将最佳实践融入角色提示词
     * 优化后的提示词自动持久化到数据库
     */
    private void evolveRolePrompts() {
        log.info("开始进化角色提示词...");

        try {
            // 收集所有角色的成功经验
            Map<String, List<String>> roleExperiences = collectRoleExperiences();

            for (Map.Entry<String, List<String>> entry : roleExperiences.entrySet()) {
                String roleId = entry.getKey();
                List<String> experiences = entry.getValue();

                if (experiences.isEmpty()) continue;

                // 获取当前提示词
                String currentPrompt = rolePromptLibrary.getPrompt(roleId);
                if (currentPrompt == null) continue;

                // 分析经验，提取可增强的规范
                String enhancement = analyzeExperiencesForEnhancement(roleId, experiences);
                if (enhancement == null || enhancement.isEmpty()) continue;

                // 检查是否已有此增强（避免重复添加）
                if (currentPrompt.contains(enhancement)) continue;

                // 追加增强内容到提示词
                String evolvedPrompt = currentPrompt + "\n\n## 经验积累（自动生成）\n\n" + enhancement;

                // 保存到数据库
                String name = rolePromptLibrary.getRoleName(roleId);
                String notifyTargets = String.join(",", rolePromptLibrary.getNotifyTargets(roleId));
                String reviewer = rolePromptLibrary.getReviewer(roleId);
                rolePromptLibrary.saveToDatabase(roleId, evolvedPrompt, name, notifyTargets, reviewer, "evolution");

                log.info("角色 {} 提示词已进化，新增经验规范", roleId);
            }

            log.info("角色提示词进化完成");
        } catch (Exception e) {
            log.error("角色提示词进化失败", e);
        }
    }

    /**
     * 收集各角色的项目经验
     * 从 learnedPatterns 和 learned-skills 文件中收集
     *
     * @return 角色ID -> 经验列表
     */
    private Map<String, List<String>> collectRoleExperiences() {
        Map<String, List<String>> roleExperiences = new HashMap<>();

        // 1. 从已学习的模式中收集高频经验
        List<String> patternInsights = new ArrayList<>();
        for (LearnedPattern pattern : learnedPatterns.values()) {
            if (pattern.getFrequency() >= 2 && !pattern.getSources().isEmpty()) {
                String insight = pattern.getKey() + "（出现 " + pattern.getFrequency() + " 次）";
                patternInsights.add(insight);
            }
        }

        // 2. 从 learned-skills 文件中收集经验
        List<String> skillInsights = loadLearnedSkillInsights();

        // 3. 合并所有经验
        List<String> allInsights = new ArrayList<>();
        allInsights.addAll(patternInsights);
        allInsights.addAll(skillInsights);

        if (!allInsights.isEmpty()) {
            // 所有角色共享相同的经验库
            for (String roleId : rolePromptLibrary.getAllRoles()) {
                roleExperiences.put(roleId, allInsights);
            }
        }

        return roleExperiences;
    }

    /**
     * 从 learned-skills 文件中加载经验摘要
     *
     * @return 经验列表
     */
    private List<String> loadLearnedSkillInsights() {
        List<String> insights = new ArrayList<>();
        try {
            Path skillsDir = Path.of(LEARNED_SKILLS_DIR);
            if (!Files.exists(skillsDir)) return insights;

            Files.list(skillsDir)
                .filter(p -> p.toString().endsWith(".md"))
                .sorted((a, b) -> { // 按修改时间倒序，取最新的
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .limit(20) // 最多读取 20 个最新技能
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        // 提取 YAML frontmatter 中的 description
                        String description = extractFrontmatterField(content, "description");
                        String learnedFrom = extractFrontmatterField(content, "learned-from");
                        if (description != null && !description.isEmpty()) {
                            String insight = description;
                            if (learnedFrom != null) {
                                insight += "（来源: " + learnedFrom + "）";
                            }
                            insights.add(insight);
                        }
                    } catch (IOException e) {
                        log.debug("读取 learned-skill 文件失败: {}", path);
                    }
                });
        } catch (IOException e) {
            log.warn("读取 learned-skills 目录失败: {}", e.getMessage());
        }
        return insights;
    }

    /**
     * 从 YAML frontmatter 中提取指定字段
     */
    private String extractFrontmatterField(String content, String fieldName) {
        if (content == null) return null;
        String[] lines = content.split("\n");
        boolean inFrontmatter = false;
        for (String line : lines) {
            if (line.trim().equals("---")) {
                if (inFrontmatter) break;
                inFrontmatter = true;
                continue;
            }
            if (inFrontmatter && line.startsWith(fieldName + ":")) {
                return line.substring(fieldName.length() + 1).trim();
            }
        }
        return null;
    }

    /**
     * 分析经验并提取可增强的规范
     *
     * @param roleId      角色ID
     * @param experiences 经验列表
     * @return 增强规范文本，无增强返回 null
     */
    private String analyzeExperiencesForEnhancement(String roleId, List<String> experiences) {
        // 合并经验内容
        StringBuilder enhancement = new StringBuilder();
        enhancement.append("以下是从项目经验中自动提取的最佳实践：\n\n");

        int added = 0;
        for (String exp : experiences) {
            if (exp == null || exp.length() < 20) continue;
            // 截取关键信息
            String summary = exp.length() > 200 ? exp.substring(0, 200) + "..." : exp;
            enhancement.append("- ").append(summary).append("\n");
            added++;
            if (added >= 10) break; // 最多追加 10 条
        }

        return added > 0 ? enhancement.toString() : null;
    }

    /**
     * 记录 Dream 知识提取结果
     * 更新已处理文档计数，使前端能正确显示提取结果
     *
     * @param agentId Agent ID
     * @param projectId 项目 ID
     * @param savedCount 保存的知识条数
     */
    public void recordDreamExtraction(String agentId, String projectId, int savedCount) {
        String key = "dream_" + projectId + "_" + agentId;
        processedDocuments.put(key, String.valueOf(System.currentTimeMillis()));
        log.info("Dream 提取已记录: agent={}, project={}, saved={}", agentId, projectId, savedCount);
    }

    /**
     * 获取知识进化统计信息
     *
     * @return 统计数据 Map
     */
    public Map<String, Object> getEvolutionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("learnedPatternsCount", learnedPatterns.size());
        stats.put("totalPatternFrequency", learnedPatterns.values().stream().mapToInt(LearnedPattern::getFrequency).sum());
        stats.put("solutionsCount", knowledgeBase.getAllSolutions().values().stream().mapToInt(List::size).sum());
        stats.put("bestPracticesCount", knowledgeBase.getAllBestPractices().size());
        stats.put("processedDocumentsCount", processedDocuments.size());
        stats.put("learnedSkillsCount", skillManager.getAllGlobalSkills().stream()
            .filter(s -> "learned".equals(s.getCategory())).count());
        stats.put("evolutionCount", evolutionCount.get());
        return stats;
    }

    /**
     * 获取已学习的模式列表（供前端展示）
     *
     * @return 模式列表
     */
    public List<Map<String, Object>> getLearnedPatternsList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (LearnedPattern pattern : learnedPatterns.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", pattern.getKey());
            item.put("frequency", pattern.getFrequency());
            item.put("sources", pattern.getSources());
            list.add(item);
        }
        // 按频率降序
        list.sort((a, b) -> Integer.compare((int) b.get("frequency"), (int) a.get("frequency")));
        return list;
    }

    /**
     * 获取已学习的技能列表（供前端展示）
     * 从 learned-skills 目录读取文件，提取元数据
     *
     * @return 技能列表
     */
    public List<Map<String, Object>> getLearnedSkillsList() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Path skillsDir = Path.of(LEARNED_SKILLS_DIR);
            if (!Files.exists(skillsDir)) return list;

            Files.list(skillsDir)
                .filter(p -> p.toString().endsWith(".md"))
                .sorted((a, b) -> {
                    try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
                    catch (IOException e) { return 0; }
                })
                .limit(50)
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        Map<String, Object> item = new LinkedHashMap<>();
                        String name = extractFrontmatterField(content, "name");
                        String description = extractFrontmatterField(content, "description");
                        String learnedFrom = extractFrontmatterField(content, "learned-from");
                        String learnedAt = extractFrontmatterField(content, "learned-at");

                        item.put("id", name != null ? name : path.getFileName().toString().replace(".md", ""));
                        item.put("description", description != null ? description : "");
                        item.put("learnedFrom", learnedFrom != null ? learnedFrom : "");
                        item.put("learnedAt", learnedAt != null ? learnedAt : "");
                        item.put("fileName", path.getFileName().toString());

                        // 从文件名中提取项目信息
                        if (learnedFrom != null && learnedFrom.contains(":")) {
                            item.put("projectId", learnedFrom.substring(0, learnedFrom.indexOf(':')));
                            item.put("agentRole", learnedFrom.substring(learnedFrom.indexOf(':') + 1));
                        }

                        list.add(item);
                    } catch (IOException e) {
                        log.debug("读取 learned-skill 文件失败: {}", path);
                    }
                });
        } catch (IOException e) {
            log.warn("读取 learned-skills 目录失败: {}", e.getMessage());
        }
        return list;
    }

    /**
     * 从任务描述中提取模式关键词
     * 取前3个有意义的词作为模式标识
     */
    private String extractPatternKey(String taskDescription) {
        if (taskDescription == null) return "unknown";
        String[] words = taskDescription.toLowerCase().split("[\\s,，。、]+");
        return Arrays.stream(words)
            .filter(w -> w.length() > 2)
            .limit(3)
            .collect(Collectors.joining("_"));
    }

    // ===== 玩家反馈驱动的知识提取 =====

    /**
     * 从玩家反馈中提取知识
     * 当项目有测试反馈、用户评价时，自动提取改进知识
     *
     * @param projectId 项目 ID
     * @param feedbackType 反馈类型：bug_report, gameplay_feedback, ux_feedback, suggestion
     * @param feedbackContent 反馈内容
     * @param severity 严重程度：critical, major, minor, suggestion
     */
    public void learnFromPlayerFeedback(String projectId, String feedbackType,
                                         String feedbackContent, String severity) {
        if (feedbackContent == null || feedbackContent.trim().isEmpty()) return;

        log.info("从玩家反馈中学习: project={}, type={}, severity={}", projectId, feedbackType, severity);

        try {
            // 根据反馈类型提取知识
            String category = mapFeedbackCategory(feedbackType);
            String knowledgeKey = "feedback." + projectId + "." + System.currentTimeMillis();

            // 构建知识内容
            StringBuilder content = new StringBuilder();
            content.append("项目: ").append(projectId).append("\n");
            content.append("反馈类型: ").append(feedbackType).append("\n");
            content.append("严重程度: ").append(severity).append("\n");
            content.append("反馈内容: ").append(feedbackContent).append("\n");

            // 记录到知识库
            knowledgeBase.recordSolution(category, "玩家反馈: " + feedbackType, content.toString());

            // 如果是严重问题，记录为需要优先解决的模式
            if ("critical".equals(severity) || "major".equals(severity)) {
                recordCriticalFeedbackPattern(projectId, feedbackType, feedbackContent);
            }

            log.info("玩家反馈知识已记录: project={}, type={}", projectId, feedbackType);
        } catch (Exception e) {
            log.error("从玩家反馈中学习失败", e);
        }
    }

    /**
     * 映射反馈类型到知识类别
     */
    private String mapFeedbackCategory(String feedbackType) {
        if (feedbackType == null) return "player_feedback";
        return switch (feedbackType.toLowerCase()) {
            case "bug_report" -> "bug_pattern";
            case "gameplay_feedback" -> "game_design";
            case "ux_feedback" -> "game_feel";
            case "suggestion" -> "player_suggestion";
            default -> "player_feedback";
        };
    }

    /**
     * 记录严重反馈模式（需要优先解决）
     */
    private void recordCriticalFeedbackPattern(String projectId, String feedbackType, String content) {
        String patternKey = "critical_" + feedbackType + "_" + projectId;
        LearnedPattern pattern = new LearnedPattern();
        pattern.patternType = "critical_feedback";
        pattern.frequency = 1;
        pattern.examples = new ArrayList<>(List.of(content));
        pattern.lastOccurrence = LocalDateTime.now();
        learnedPatterns.put(patternKey, pattern);
        saveLearnedPatterns();
    }

    // ===== 跨项目知识迁移 =====

    /**
     * 获取跨项目推荐知识
     * 当一个项目遇到问题时，查找其他项目是否有类似经验
     *
     * @param projectId 当前项目 ID
     * @param problemDescription 问题描述
     * @param gameType 游戏类型
     * @return 推荐的知识列表（来自其他项目的经验）
     */
    public List<CrossProjectKnowledge> getCrossProjectKnowledge(String projectId,
                                                                 String problemDescription,
                                                                 String gameType) {
        List<CrossProjectKnowledge> recommendations = new ArrayList<>();

        // 从学习到的模式中查找相关经验
        for (Map.Entry<String, LearnedPattern> entry : learnedPatterns.entrySet()) {
            LearnedPattern pattern = entry.getValue();

            // 跳过当前项目的模式
            if (entry.getKey().contains(projectId)) continue;

            // 检查是否相关
            if (isPatternRelevant(pattern, problemDescription, gameType)) {
                CrossProjectKnowledge knowledge = new CrossProjectKnowledge();
                knowledge.sourceKey = entry.getKey();
                knowledge.patternType = pattern.patternType;
                knowledge.frequency = pattern.frequency;
                knowledge.description = pattern.examples.isEmpty() ? "" : pattern.examples.get(0);
                knowledge.relevanceScore = calculateRelevance(pattern, problemDescription, gameType);
                recommendations.add(knowledge);
            }
        }

        // 按相关性排序
        recommendations.sort((a, b) -> Double.compare(b.relevanceScore, a.relevanceScore));

        // 返回 top 5
        return recommendations.stream().limit(5).toList();
    }

    /**
     * 判断模式是否与当前问题相关
     */
    private boolean isPatternRelevant(LearnedPattern pattern, String problemDescription, String gameType) {
        if (problemDescription == null) return false;

        String problemLower = problemDescription.toLowerCase();
        String patternType = pattern.patternType != null ? pattern.patternType.toLowerCase() : "";

        // 模式类型匹配
        if (problemLower.contains(patternType) || patternType.contains(problemLower.substring(0, Math.min(10, problemLower.length())))) {
            return true;
        }

        // 示例内容匹配
        for (String example : pattern.examples) {
            if (example != null) {
                String exampleLower = example.toLowerCase();
                // 简单关键词匹配
                String[] keywords = problemLower.split("[\\s,，。、]+");
                for (String keyword : keywords) {
                    if (keyword.length() > 2 && exampleLower.contains(keyword)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 计算模式与当前问题的相关性分数
     */
    private double calculateRelevance(LearnedPattern pattern, String problemDescription, String gameType) {
        double score = 0;

        // 频率越高越相关
        score += Math.min(pattern.frequency * 2, 20);

        // 最近出现的更相关
        if (pattern.lastOccurrence != null) {
            long daysSince = java.time.Duration.between(pattern.lastOccurrence, LocalDateTime.now()).toDays();
            if (daysSince < 7) score += 15;
            else if (daysSince < 30) score += 10;
            else if (daysSince < 90) score += 5;
        }

        // 有解决方案的更相关
        if (pattern.solution != null && !pattern.solution.isEmpty()) {
            score += 10;
        }

        return score;
    }

    /**
     * 导出项目知识供其他项目使用
     * 在项目完成或里程碑达成时调用
     *
     * @param projectId 项目 ID
     * @param gameType 游戏类型
     * @param successFactors 成功因素描述
     */
    public void exportProjectKnowledge(String projectId, String gameType, String successFactors) {
        if (successFactors == null || successFactors.trim().isEmpty()) return;

        log.info("导出项目知识: project={}, gameType={}", projectId, gameType);

        try {
            String patternKey = "exported_" + projectId + "_" + System.currentTimeMillis();
            LearnedPattern pattern = new LearnedPattern();
            pattern.patternType = gameType != null ? gameType : "general";
            pattern.frequency = 1;
            pattern.examples = new ArrayList<>(List.of(successFactors));
            pattern.lastOccurrence = LocalDateTime.now();

            // 记录成功因素作为解决方案
            pattern.solution = successFactors;

            learnedPatterns.put(patternKey, pattern);
            saveLearnedPatterns();

            log.info("项目知识已导出: project={}", projectId);
        } catch (Exception e) {
            log.error("导出项目知识失败", e);
        }
    }

    // ===== 跨项目模式学习增强 =====

    /**
     * 从已完成项目中提取成功模式
     * 分析项目的关键决策、技术选型、里程碑拆分方式等
     *
     * @param projectId 项目ID
     * @param gameType 游戏类型
     * @param milestones 里程碑列表
     * @param projectGoal 项目目标
     */
    public void extractProjectPatterns(String projectId, String gameType,
                                        List<GameProject.GoalMilestone> milestones, String projectGoal) {
        if (projectId == null || milestones == null) return;

        log.info("提取项目成功模式: project={}, gameType={}", projectId, gameType);

        try {
            // 1. 提取里程碑拆分模式
            int milestoneCount = milestones.size();
            long completedCount = milestones.stream()
                .filter(m -> m.getStatus() == GameProject.MilestoneStatus.COMPLETED)
                .count();

            if (completedCount > 0) {
                String patternKey = "milestone_pattern_" + (gameType != null ? gameType : "general");
                LearnedPattern pattern = learnedPatterns.get(patternKey);
                if (pattern == null) {
                    pattern = new LearnedPattern();
                    pattern.patternType = "milestone_decomposition";
                    learnedPatterns.put(patternKey, pattern);
                }
                pattern.frequency++;
                pattern.lastOccurrence = LocalDateTime.now();
                pattern.examples.add(String.format("项目[%s]: %d个里程碑完成%d个，目标: %s",
                    projectId, milestoneCount, completedCount,
                    projectGoal != null && projectGoal.length() > 50 ? projectGoal.substring(0, 50) + "..." : projectGoal));
            }

            // 2. 提取任务拆分模式
            for (GameProject.GoalMilestone milestone : milestones) {
                if (milestone.getTasks() != null && !milestone.getTasks().isEmpty()) {
                    int taskCount = milestone.getTasks().size();
                    String patternKey = "task_split_pattern_" + taskCount + "_tasks";
                    LearnedPattern pattern = learnedPatterns.get(patternKey);
                    if (pattern == null) {
                        pattern = new LearnedPattern();
                        pattern.patternType = "task_splitting";
                        learnedPatterns.put(patternKey, pattern);
                    }
                    pattern.frequency++;
                    pattern.lastOccurrence = LocalDateTime.now();
                    pattern.examples.add(String.format("里程碑[%s]拆分为%d个任务",
                        milestone.getTitle() != null ? milestone.getTitle() : "未知", taskCount));
                }
            }

            // 3. 提取角色分配模式
            Map<String, Integer> roleDistribution = new HashMap<>();
            for (GameProject.GoalMilestone milestone : milestones) {
                String role = milestone.getAssignedAgentRole();
                if (role != null) {
                    roleDistribution.merge(role, 1, Integer::sum);
                }
            }
            if (!roleDistribution.isEmpty()) {
                String patternKey = "role_distribution_" + (gameType != null ? gameType : "general");
                LearnedPattern pattern = learnedPatterns.get(patternKey);
                if (pattern == null) {
                    pattern = new LearnedPattern();
                    pattern.patternType = "role_assignment";
                    learnedPatterns.put(patternKey, pattern);
                }
                pattern.frequency++;
                pattern.lastOccurrence = LocalDateTime.now();
                pattern.examples.add(String.format("角色分配: %s",
                    roleDistribution.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .reduce((a, b) -> a + ", " + b).orElse("无")));
            }

            saveLearnedPatterns();
            log.info("项目模式提取完成: project={}", projectId);

        } catch (Exception e) {
            log.error("提取项目模式失败: project={}", projectId, e);
        }
    }

    /**
     * 查询相似项目的成功经验
     * 根据游戏类型搜索其他项目的成功模式和最佳实践
     *
     * @param gameType 游戏类型
     * @param currentProjectId 当前项目ID（排除自身）
     * @return 格式化的经验文本，可直接注入prompt
     */
    public String querySimilarProjectExperiences(String gameType, String currentProjectId) {
        if (gameType == null || gameType.isEmpty()) return "";

        StringBuilder experience = new StringBuilder();
        String lowerType = gameType.toLowerCase();

        // 1. 搜索同类型项目的里程碑模式
        String milestonePatternKey = "milestone_pattern_" + gameType;
        LearnedPattern milestonePattern = learnedPatterns.get(milestonePatternKey);
        if (milestonePattern != null && milestonePattern.frequency > 0) {
            experience.append("### 同类项目里程碑经验\n\n");
            experience.append(String.format("共有 %d 个同类项目的经验数据：\n", milestonePattern.frequency));
            for (String example : milestonePattern.examples.stream().limit(3).toList()) {
                experience.append("- ").append(example).append("\n");
            }
            experience.append("\n");
        }

        // 2. 搜索任务拆分经验
        String[] taskSplitKeys = {"task_split_pattern_3_tasks", "task_split_pattern_5_tasks", "task_split_pattern_8_tasks"};
        for (String key : taskSplitKeys) {
            LearnedPattern pattern = learnedPatterns.get(key);
            if (pattern != null && pattern.frequency > 0) {
                if (experience.indexOf("任务拆分经验") < 0) {
                    experience.append("### 任务拆分经验\n\n");
                }
                for (String example : pattern.examples.stream().limit(2).toList()) {
                    experience.append("- ").append(example).append("\n");
                }
            }
        }
        if (experience.indexOf("任务拆分经验") >= 0) experience.append("\n");

        // 3. 搜索角色分配经验
        String rolePatternKey = "role_distribution_" + gameType;
        LearnedPattern rolePattern = learnedPatterns.get(rolePatternKey);
        if (rolePattern != null && rolePattern.frequency > 0) {
            experience.append("### 角色分配经验\n\n");
            for (String example : rolePattern.examples.stream().limit(2).toList()) {
                experience.append("- ").append(example).append("\n");
            }
            experience.append("\n");
        }

        // 4. 搜索最佳实践
        List<GameKnowledgeBase.BestPractice> matchedPractices = knowledgeBase.getAllBestPractices().stream()
            .filter(bp -> {
                String combined = (bp.getCategory() + " " + bp.getTitle() + " " + bp.getContent()).toLowerCase();
                return combined.contains(lowerType);
            })
            .limit(3)
            .toList();

        if (!matchedPractices.isEmpty()) {
            experience.append("### 同类游戏最佳实践\n\n");
            for (GameKnowledgeBase.BestPractice bp : matchedPractices) {
                experience.append(String.format("- **%s** (%s): %s\n",
                    bp.getTitle(), bp.getCategory(),
                    bp.getContent().length() > 200 ? bp.getContent().substring(0, 200) + "..." : bp.getContent()));
            }
            experience.append("\n");
        }

        // 5. 搜索失败教训（优先精华，fallback 精选）
        Map<String, List<GameKnowledgeBase.Solution>> allSolutions = knowledgeBase.getAllSolutions();
        for (Map.Entry<String, List<GameKnowledgeBase.Solution>> entry : allSolutions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(lowerType) || lowerType.contains(entry.getKey().toLowerCase())) {
                String essence = knowledgeBase.getEssence(entry.getKey());
                if (essence != null && !essence.isEmpty()) {
                    experience.append("### 同类项目经验 (精华)\n\n");
                    String trimmed = essence.length() > 300 ? essence.substring(0, 300) + "..." : essence;
                    experience.append(trimmed).append("\n\n");
                } else {
                    List<GameKnowledgeBase.Solution> topSols = knowledgeBase.getTopSolutions(entry.getKey(), 2);
                    if (!topSols.isEmpty()) {
                        experience.append("### 同类项目失败教训\n\n");
                        for (GameKnowledgeBase.Solution sol : topSols) {
                            experience.append(String.format("- 问题: %s\n  解决: %s\n",
                                sol.getProblemDescription().length() > 80 ? sol.getProblemDescription().substring(0, 80) + "..." : sol.getProblemDescription(),
                                sol.getSolution().length() > 150 ? sol.getSolution().substring(0, 150) + "..." : sol.getSolution()));
                        }
                        experience.append("\n");
                    }
                }
            }
        }

        return experience.toString();
    }

    /**
     * 构建跨项目模式摘要
     * 用于注入到ProducerAgent的决策上下文中
     *
     * @param gameType 游戏类型
     * @param currentProjectId 当前项目ID
     * @return 格式化的模式摘要
     */
    /** 跨项目模式摘要最大长度 */
    private static final int MAX_PATTERN_SUMMARY_LENGTH = 800;

    public String buildCrossProjectPatternSummary(String gameType, String currentProjectId) {
        StringBuilder summary = new StringBuilder();

        // 获取跨项目知识推荐（最多 3 条）
        List<CrossProjectKnowledge> recommendations = getCrossProjectKnowledge(
            currentProjectId, gameType, gameType);

        if (!recommendations.isEmpty()) {
            summary.append("### 跨项目成功模式\n\n");
            int limit = Math.min(recommendations.size(), 3);
            for (int i = 0; i < limit; i++) {
                CrossProjectKnowledge knowledge = recommendations.get(i);

                // 跳过无效的知识记录
                if (knowledge.patternType == null || knowledge.description == null) {
                    continue;
                }

                String desc = knowledge.description.length() > 80
                    ? knowledge.description.substring(0, 80) + "..." : knowledge.description;
                summary.append(String.format("- **%s** (频率: %d): %s\n",
                    knowledge.patternType, knowledge.frequency, desc));
            }
            summary.append("\n");
        }

        // 添加相似项目经验（截断保护）
        String similarExperiences = querySimilarProjectExperiences(gameType, currentProjectId);
        if (!similarExperiences.isEmpty()) {
            if (similarExperiences.length() > 500) {
                similarExperiences = similarExperiences.substring(0, 500) + "...";
            }
            summary.append(similarExperiences);
        }

        // 总体截断保护
        String result = summary.toString();
        if (result.length() > MAX_PATTERN_SUMMARY_LENGTH) {
            result = result.substring(0, MAX_PATTERN_SUMMARY_LENGTH) + "\n...(已截断)";
        }
        return result;
    }

    /**
     * 跨项目知识推荐
     */
    public static class CrossProjectKnowledge {
        /** 来源标识 */
        public String sourceKey;
        /** 模式类型 */
        public String patternType;
        /** 出现频率 */
        public int frequency;
        /** 描述 */
        public String description;
        /** 相关性分数 */
        public double relevanceScore;
    }

    /**
     * 游戏模式
     */
    private static class GamePattern {
        private String templateId;
        private String patternType;
        private String description;
        private LocalDateTime createdAt;

        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getPatternType() { return patternType; }
        public void setPatternType(String patternType) { this.patternType = patternType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
