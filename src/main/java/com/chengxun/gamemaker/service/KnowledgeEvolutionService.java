package com.chengxun.gamemaker.service;

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

    /** 已处理的文档哈希，避免重复处理 */
    private final Map<String, String> processedDocuments = new ConcurrentHashMap<>();

    /** 已学习的模式 */
    private final Map<String, LearnedPattern> learnedPatterns = new ConcurrentHashMap<>();

    public KnowledgeEvolutionService(GameKnowledgeBase knowledgeBase,
                                     GameTemplateService templateService,
                                     MemoryManager memoryManager,
                                     SkillManager skillManager) {
        this.knowledgeBase = knowledgeBase;
        this.templateService = templateService;
        this.memoryManager = memoryManager;
        this.skillManager = skillManager;
    }

    /**
     * 初始化知识进化服务
     */
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

            log.info("知识库整理完成");

        } catch (Exception e) {
            log.error("整理知识库失败", e);
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

            log.info("知识自进化完成");

        } catch (Exception e) {
            log.error("知识自进化失败", e);
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
     */
    private Map<String, List<ProjectExperience>> collectProjectExperiences() {
        Map<String, List<ProjectExperience>> experiences = new HashMap<>();

        // 从知识库中提取项目经验
        // 这里可以扩展为从数据库或文件中读取

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

            if (projectExperiences.size() >= 2) { // 至少2个项目有类似经验
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

        // 分析成功因素
        Map<String, Integer> factorCount = new HashMap<>();
        for (ProjectExperience exp : experiences) {
            if (exp.getSuccessFactor() != null) {
                factorCount.merge(exp.getSuccessFactor(), 1, Integer::sum);
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
     */
    private void mergeDuplicateKnowledge() {
        log.debug("合并重复知识...");
    }

    /**
     * 清理过期知识
     */
    private void cleanupExpiredKnowledge() {
        log.debug("清理过期知识...");
    }

    /**
     * 优化知识索引
     */
    private void optimizeKnowledgeIndex() {
        log.debug("优化知识索引...");
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
        private int frequency;
        private List<String> sources = new ArrayList<>();

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
