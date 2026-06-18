package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.GameVerifyResult;
import com.chengxun.gamemaker.web.repository.GameVerifyResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家体验分析器（混合评分版）
 * 从玩家视角评估游戏设计，提供趣味度评分和改进建议
 *
 * 三层评分机制：
 * 1. 关键词层 — 基于项目目标描述的关键词匹配（快速，基准分）
 * 2. 代码特征层 — 扫描项目目录中的实际文件（音效、图片、关卡配置等）
 * 3. AI 深度层 — 复用 GameRuntimeVerifier.analyzeQuality() 的持久化结果
 *
 * 评估维度：
 * 1. 核心循环吸引力 — 游戏核心玩法是否足够有趣、让人上瘾
 * 2. 挑战与难度曲线 — 难度是否合理递增，不会太简单或太难
 * 3. 奖励与反馈 — 玩家行为是否有及时、令人满足的反馈
 * 4. 进度与成长感 — 玩家是否能感受到持续的进步和成长
 * 5. 新颖与惊喜 — 是否有足够的新鲜元素和意外惊喜
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class PlayerExperienceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PlayerExperienceAnalyzer.class);

    /** 项目体验评分缓存：projectId -> FunScore */
    private final Map<String, FunScore> scoreCache = new ConcurrentHashMap<>();

    @Autowired
    private GameVerifyResultRepository verifyResultRepository;

    /** 音效文件扩展名 */
    private static final String[] AUDIO_EXTENSIONS = {".mp3", ".wav", ".ogg", ".flac", ".aac", ".m4a"};
    /** 图片/精灵文件扩展名 */
    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".svg", ".gif", ".webp", ".bmp"};
    /** 动画/特效文件名关键词 */
    private static final String[] ANIMATION_KEYWORDS = {"animation", "anim", "effect", "particle", "特效", "动画", "sprite"};
    /** 关卡/地图文件名关键词 */
    private static final String[] LEVEL_KEYWORDS = {"level", "stage", "map", "关卡", "地图", "scene", "chapter"};
    /** UI 相关文件名关键词 */
    private static final String[] UI_KEYWORDS = {"ui", "hud", "menu", "界面", "dialog", "panel", "widget"};
    /** 排除的目录 */
    private static final String[] EXCLUDED_DIRS = {
        "node_modules", ".git", ".claude", "target", "build", "dist",
        "__pycache__", ".idea", ".vscode", ".gradle", ".mvn", ".game-maker"
    };

    /**
     * 趣味度评分结果
     */
    public static class FunScore {
        private int coreLoopScore;      // 核心循环 (0-100)
        private int challengeScore;     // 挑战感 (0-100)
        private int rewardScore;        // 奖励反馈 (0-100)
        private int progressionScore;   // 进度感 (0-100)
        private int noveltyScore;       // 新颖度 (0-100)
        private int overallScore;       // 综合分 (加权平均)
        private List<String> painPoints = new ArrayList<>();
        private List<String> improvements = new ArrayList<>();
        private LocalDateTime analyzedAt;
        /** 数据来源标识：KEYWORD=纯关键词, CODE_FEATURE=含代码特征, AI_DEEP=含AI深度分析 */
        private String dataSource = "KEYWORD";

        public FunScore() {
            this.analyzedAt = LocalDateTime.now();
        }

        // Getters and Setters
        public int getCoreLoopScore() { return coreLoopScore; }
        public void setCoreLoopScore(int score) { this.coreLoopScore = score; }
        public int getChallengeScore() { return challengeScore; }
        public void setChallengeScore(int score) { this.challengeScore = score; }
        public int getRewardScore() { return rewardScore; }
        public void setRewardScore(int score) { this.rewardScore = score; }
        public int getProgressionScore() { return progressionScore; }
        public void setProgressionScore(int score) { this.progressionScore = score; }
        public int getNoveltyScore() { return noveltyScore; }
        public void setNoveltyScore(int score) { this.noveltyScore = score; }
        public int getOverallScore() { return overallScore; }
        public void setOverallScore(int score) { this.overallScore = score; }
        public List<String> getPainPoints() { return painPoints; }
        public void setPainPoints(List<String> points) { this.painPoints = points; }
        public List<String> getImprovements() { return improvements; }
        public void setImprovements(List<String> improvements) { this.improvements = improvements; }
        public LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }

        /**
         * 计算综合分（加权平均）
         * 核心循环权重最高(30%)，其他各17.5%
         */
        public void calculateOverall() {
            this.overallScore = (int) (coreLoopScore * 0.30
                + challengeScore * 0.175
                + rewardScore * 0.175
                + progressionScore * 0.175
                + noveltyScore * 0.175);
        }

        /**
         * 格式化为可读文本
         */
        /** 最大输出长度（字符数） */
        private static final int MAX_OUTPUT_LENGTH = 500;

        public String toReadableText() {
            StringBuilder sb = new StringBuilder();
            sb.append("## 玩家体验评分\n\n");
            sb.append(String.format("- 综合分: **%d/100**\n", overallScore));
            sb.append(String.format("- 核心循环: %d/100\n", coreLoopScore));
            sb.append(String.format("- 挑战感: %d/100\n", challengeScore));
            sb.append(String.format("- 奖励反馈: %d/100\n", rewardScore));
            sb.append(String.format("- 进度感: %d/100\n", progressionScore));
            sb.append(String.format("- 新颖度: %d/100\n\n", noveltyScore));

            // 痛点最多显示 3 条
            if (!painPoints.isEmpty()) {
                sb.append("### 预测痛点\n");
                int limit = Math.min(painPoints.size(), 3);
                for (int i = 0; i < limit; i++) {
                    sb.append("- ").append(painPoints.get(i)).append("\n");
                }
                sb.append("\n");
            }

            // 改进建议最多显示 3 条
            if (!improvements.isEmpty()) {
                sb.append("### 改进建议\n");
                int limit = Math.min(improvements.size(), 3);
                for (int i = 0; i < limit; i++) {
                    sb.append(String.format("%d. %s\n", i + 1, improvements.get(i)));
                }
                sb.append("\n");
            }

            // 截断保护
            String result = sb.toString();
            if (result.length() > MAX_OUTPUT_LENGTH) {
                result = result.substring(0, MAX_OUTPUT_LENGTH) + "\n...(已截断)";
            }
            return result;
        }
    }

    /**
     * 分析项目的玩家体验（混合评分）
     *
     * 评分流程：
     * 1. 关键词基准分（原有逻辑）
     * 2. 代码特征修正（扫描实际项目文件）
     * 3. AI 深度分析叠加（读取 DB 中的最新分析结果）
     *
     * @param project 项目对象
     * @return 趣味度评分
     */
    public FunScore analyzeProject(GameProject project) {
        if (project == null) return new FunScore();

        String projectId = project.getId();
        // 缓存1小时
        FunScore cached = scoreCache.get(projectId);
        if (cached != null && cached.getAnalyzedAt().plusHours(1).isAfter(LocalDateTime.now())) {
            return cached;
        }

        log.info("分析项目玩家体验: project={}", project.getName());

        FunScore score = new FunScore();
        String goal = project.getGoal() != null ? project.getGoal() : "";
        String lowerGoal = goal.toLowerCase();

        // ===== 第 1 层：关键词基准分 =====
        score.setCoreLoopScore(evaluateCoreLoop(lowerGoal, project));
        score.setChallengeScore(evaluateChallenge(lowerGoal, project));
        score.setRewardScore(evaluateReward(lowerGoal, project));
        score.setProgressionScore(evaluateProgression(lowerGoal, project));
        score.setNoveltyScore(evaluateNovelty(lowerGoal, project));
        score.setDataSource("KEYWORD");

        // ===== 第 2 层：代码特征修正 =====
        String workDir = project.getWorkDir();
        if (workDir != null && !workDir.isEmpty() && new File(workDir).isDirectory()) {
            applyCodeFeatureBonus(score, workDir, lowerGoal);
        }

        // ===== 第 3 层：AI 深度分析叠加 =====
        applyAIAnalysisScore(score, projectId);

        // 计算综合分
        score.calculateOverall();

        // 生成痛点预测
        score.setPainPoints(predictPainPoints(lowerGoal, project, score));

        // 生成改进建议
        score.setImprovements(generateImprovements(lowerGoal, project, score));

        scoreCache.put(projectId, score);
        return score;
    }

    // ===== 第 2 层：代码特征扫描 =====

    /**
     * 扫描项目目录中的实际文件，用代码特征修正关键词基准分
     * 修正幅度：每个检测项 +5~+15 分，总修正不超过 +30 分/维度
     *
     * @param score 当前评分（会被就地修改）
     * @param workDir 项目工作目录
     * @param lowerGoal 项目目标（小写）
     */
    private void applyCodeFeatureBonus(FunScore score, String workDir, String lowerGoal) {
        File dir = new File(workDir);

        // 音效文件 → 奖励反馈 +12
        int audioCount = countFilesByExtensions(dir, AUDIO_EXTENSIONS, 4);
        if (audioCount > 0) {
            score.setRewardScore(clamp(score.getRewardScore() + Math.min(audioCount * 3, 12)));
            log.debug("代码特征: 发现 {} 个音效文件, 奖励反馈+{}", audioCount, Math.min(audioCount * 3, 12));
        }

        // 图片/精灵文件 → 核心循环 +10, 奖励反馈 +8
        int imageCount = countFilesByExtensions(dir, IMAGE_EXTENSIONS, 4);
        if (imageCount > 5) {
            score.setCoreLoopScore(clamp(score.getCoreLoopScore() + Math.min(imageCount / 3, 10)));
            score.setRewardScore(clamp(score.getRewardScore() + Math.min(imageCount / 4, 8)));
            log.debug("代码特征: 发现 {} 个图片文件, 核心循环+{}, 奖励反馈+{}",
                imageCount, Math.min(imageCount / 3, 10), Math.min(imageCount / 4, 8));
        }

        // 关卡/地图配置文件 → 挑战感 +15
        int levelCount = countFilesByKeywords(dir, LEVEL_KEYWORDS, 4);
        if (levelCount > 0) {
            score.setChallengeScore(clamp(score.getChallengeScore() + Math.min(levelCount * 5, 15)));
            log.debug("代码特征: 发现 {} 个关卡配置, 挑战感+{}", levelCount, Math.min(levelCount * 5, 15));
        }

        // 动画/特效文件 → 奖励反馈 +10
        int animCount = countFilesByKeywords(dir, ANIMATION_KEYWORDS, 4);
        if (animCount > 0) {
            score.setRewardScore(clamp(score.getRewardScore() + Math.min(animCount * 3, 10)));
            log.debug("代码特征: 发现 {} 个动画/特效文件, 奖励反馈+{}", animCount, Math.min(animCount * 3, 10));
        }

        // UI 相关文件 → 核心循环 +8
        int uiCount = countFilesByKeywords(dir, UI_KEYWORDS, 4);
        if (uiCount > 0) {
            score.setCoreLoopScore(clamp(score.getCoreLoopScore() + Math.min(uiCount * 2, 8)));
            log.debug("代码特征: 发现 {} 个UI文件, 核心循环+{}", uiCount, Math.min(uiCount * 2, 8));
        }

        // 源代码文件数量 → 核心循环 +5（有实际代码产出）
        int sourceCount = countSourceFiles(dir, 4);
        if (sourceCount > 3) {
            score.setCoreLoopScore(clamp(score.getCoreLoopScore() + 5));
            log.debug("代码特征: 发现 {} 个源代码文件, 核心循环+5", sourceCount);
        }

        // 目录结构深度 → 进度感 +10
        int maxDepth = getMaxDepth(dir, 0, 5);
        if (maxDepth >= 3) {
            score.setProgressionScore(clamp(score.getProgressionScore() + 10));
            log.debug("代码特征: 目录深度 {}, 进度感+10", maxDepth);
        } else if (maxDepth >= 2) {
            score.setProgressionScore(clamp(score.getProgressionScore() + 5));
            log.debug("代码特征: 目录深度 {}, 进度感+5", maxDepth);
        }

        // 配置文件多样性 → 新颖度 +8
        int configTypes = countConfigFileTypes(dir, 4);
        if (configTypes >= 3) {
            score.setNoveltyScore(clamp(score.getNoveltyScore() + 8));
            log.debug("代码特征: {} 种配置文件类型, 新颖度+8", configTypes);
        }
    }

    // ===== 第 3 层：AI 深度分析 =====

    /**
     * 从数据库加载最新的 AI 深度分析结果，叠加到体验评分上
     * AI 分数权重 40%，当前分数权重 60%
     *
     * @param score 当前评分（会被就地修改）
     * @param projectId 项目 ID
     */
    private void applyAIAnalysisScore(FunScore score, String projectId) {
        try {
            Optional<GameVerifyResult> latest = verifyResultRepository
                .findFirstByProjectIdAndVerifyTypeOrderByVerifiedAtDesc(projectId, "DEEP");

            if (latest.isEmpty()) {
                log.debug("无 AI 深度分析结果: project={}", projectId);
                return;
            }

            GameVerifyResult result = latest.get();

            // 检查是否在 24 小时内
            if (result.getVerifiedAt().plusHours(24).isBefore(LocalDateTime.now())) {
                log.debug("AI 深度分析结果已过期: project={}, verifiedAt={}", projectId, result.getVerifiedAt());
                return;
            }

            // 检查是否有有效分数
            if (result.getOverallScore() == null || result.getOverallScore() == 0) {
                log.debug("AI 深度分析结果无有效分数: project={}", projectId);
                return;
            }

            // AI 不能运行 → 直接大幅扣分
            if (result.getRunnableScore() != null && result.getRunnableScore() < 30) {
                score.setCoreLoopScore(clamp(Math.min(score.getCoreLoopScore(), 20)));
                score.setChallengeScore(clamp(Math.min(score.getChallengeScore(), 20)));
                score.setRewardScore(clamp(Math.min(score.getRewardScore(), 15)));
                score.setDataSource("AI_DEEP");
                log.info("AI 分析: 游戏不可运行(runnable={}), 大幅降分", result.getRunnableScore());
                return;
            }

            // 维度映射 + 加权融合 (AI 40%, 当前 60%)
            int aiCoreLoop = result.getPlayableScore() != null ? result.getPlayableScore() : 50;
            int aiChallenge = result.getCompletenessScore() != null ? result.getCompletenessScore() : 50;
            int aiReward = result.getUiuxScore() != null ? result.getUiuxScore() : 50;
            int aiProgression = result.getCompletenessScore() != null ? result.getCompletenessScore() : 50;
            int aiNovelty = result.getOverallScore() != null ? result.getOverallScore() : 50;

            score.setCoreLoopScore(clamp((int)(score.getCoreLoopScore() * 0.6 + aiCoreLoop * 0.4)));
            score.setChallengeScore(clamp((int)(score.getChallengeScore() * 0.6 + aiChallenge * 0.4)));
            score.setRewardScore(clamp((int)(score.getRewardScore() * 0.6 + aiReward * 0.4)));
            score.setProgressionScore(clamp((int)(score.getProgressionScore() * 0.6 + aiProgression * 0.4)));
            score.setNoveltyScore(clamp((int)(score.getNoveltyScore() * 0.6 + aiNovelty * 0.4)));
            score.setDataSource("AI_DEEP");

            log.info("AI 深度分析已融合: project={}, overall={}, playable={}, uiux={}",
                projectId, result.getOverallScore(), result.getPlayableScore(), result.getUiuxScore());

        } catch (Exception e) {
            log.debug("加载 AI 深度分析结果失败: {}", e.getMessage());
        }
    }

    // ===== 文件扫描工具方法 =====

    /**
     * 按扩展名统计文件数量
     */
    private int countFilesByExtensions(File dir, String[] extensions, int maxDepth) {
        if (maxDepth < 0 || dir == null) return 0;
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isDirectory() && !isExcludedDir(file.getName())) {
                count += countFilesByExtensions(file, extensions, maxDepth - 1);
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (name.endsWith(ext)) { count++; break; }
                }
            }
        }
        return count;
    }

    /**
     * 按文件名关键词统计文件数量
     */
    private int countFilesByKeywords(File dir, String[] keywords, int maxDepth) {
        if (maxDepth < 0 || dir == null) return 0;
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isDirectory() && !isExcludedDir(file.getName())) {
                count += countFilesByKeywords(file, keywords, maxDepth - 1);
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String kw : keywords) {
                    if (name.contains(kw)) { count++; break; }
                }
            }
        }
        return count;
    }

    /**
     * 统计源代码文件数量
     */
    private int countSourceFiles(File dir, int maxDepth) {
        String[] sourceExts = {".js", ".ts", ".py", ".java", ".html", ".css", ".vue",
            ".gd", ".cs", ".cpp", ".c", ".lua", ".dart", ".swift", ".kt"};
        return countFilesByExtensions(dir, sourceExts, maxDepth);
    }

    /**
     * 统计配置文件类型多样性
     */
    private int countConfigFileTypes(File dir, int maxDepth) {
        Set<String> types = new HashSet<>();
        collectConfigTypes(dir, 0, maxDepth, types);
        return types.size();
    }

    private void collectConfigTypes(File dir, int depth, int maxDepth, Set<String> types) {
        if (depth > maxDepth || dir == null) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory() && !isExcludedDir(file.getName())) {
                collectConfigTypes(file, depth + 1, maxDepth, types);
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".json")) types.add("json");
                else if (name.endsWith(".yaml") || name.endsWith(".yml")) types.add("yaml");
                else if (name.endsWith(".xml")) types.add("xml");
                else if (name.endsWith(".toml")) types.add("toml");
                else if (name.endsWith(".ini") || name.endsWith(".cfg")) types.add("ini");
                else if (name.endsWith(".conf") || name.endsWith(".config")) types.add("conf");
            }
        }
    }

    /**
     * 获取目录最大深度
     */
    private int getMaxDepth(File dir, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth || dir == null) return currentDepth;
        int deepest = currentDepth;
        File[] files = dir.listFiles();
        if (files == null) return currentDepth;
        for (File file : files) {
            if (file.isDirectory() && !isExcludedDir(file.getName())) {
                deepest = Math.max(deepest, getMaxDepth(file, currentDepth + 1, maxDepth));
            }
        }
        return deepest;
    }

    private boolean isExcludedDir(String name) {
        for (String excluded : EXCLUDED_DIRS) {
            if (name.equals(excluded)) return true;
        }
        return false;
    }

    private int clamp(int value) {
        return Math.min(100, Math.max(0, value));
    }

    // ===== 原有关键词评估方法 =====

    /**
     * 评估核心循环吸引力
     * 基于游戏类型的固有趣味性和目标描述的具体程度
     */
    private int evaluateCoreLoop(String lowerGoal, GameProject project) {
        int score = 50; // 基准分

        // 游戏类型固有趣味性
        if (containsAny(lowerGoal, "射击", "shooter", "动作", "action", "竞速", "racing")) {
            score += 20; // 动作类天然吸引力高
        } else if (containsAny(lowerGoal, "rpg", "角色扮演", "冒险", "adventure", "开放世界")) {
            score += 15; // RPG类有深度
        } else if (containsAny(lowerGoal, "策略", "strategy", "塔防", "tower defense", "模拟", "simulation")) {
            score += 10; // 策略类需要思考
        } else if (containsAny(lowerGoal, "益智", "puzzle", "三消", "消除")) {
            score += 5; // 益智类上手简单
        }

        // 目标描述越具体，核心循环越清晰
        if (lowerGoal.length() > 100) score += 10;
        if (containsAny(lowerGoal, "核心玩法", "游戏机制", "战斗系统", "操作手感")) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * 评估挑战感
     * 有难度系统、关卡设计的目标描述得分更高
     */
    private int evaluateChallenge(String lowerGoal, GameProject project) {
        int score = 45;

        if (containsAny(lowerGoal, "难度", "关卡", "level", "boss", "挑战")) {
            score += 15;
        }
        if (containsAny(lowerGoal, "渐进", "递增", "progressive", "曲线")) {
            score += 10;
        }
        // 里程碑中有明确的难度相关内容
        if (project.getMilestones() != null) {
            long challengeRelated = project.getMilestones().stream()
                .filter(m -> m.getTitle() != null && containsAny(m.getTitle().toLowerCase(), "难度", "关卡", "boss", "挑战"))
                .count();
            if (challengeRelated > 0) score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * 评估奖励反馈
     * 有成就系统、排行榜、奖励机制的目标得分更高
     */
    private int evaluateReward(String lowerGoal, GameProject project) {
        int score = 40;

        if (containsAny(lowerGoal, "奖励", "成就", "排行榜", "积分", "金币", "道具", "装备")) {
            score += 15;
        }
        if (containsAny(lowerGoal, "反馈", "特效", "动画", "音效", "打击感")) {
            score += 15;
        }
        if (containsAny(lowerGoal, "收集", "养成", "解锁", "皮肤")) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * 评估进度感
     * 有明确进度系统、成长体系的目标得分更高
     */
    private int evaluateProgression(String lowerGoal, GameProject project) {
        int score = 45;

        if (containsAny(lowerGoal, "等级", "经验", "升级", "成长", "进度")) {
            score += 15;
        }
        if (containsAny(lowerGoal, "章节", "剧情", "故事", "主线", "任务")) {
            score += 10;
        }
        if (containsAny(lowerGoal, "解锁", "开放", "新内容", "新功能")) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * 评估新颖度
     * 有创新元素、独特机制的目标得分更高
     */
    private int evaluateNovelty(String lowerGoal, GameProject project) {
        int score = 40;

        if (containsAny(lowerGoal, "创新", "独特", "原创", "新颖", "前所未有")) {
            score += 20;
        }
        if (containsAny(lowerGoal, "roguelike", "随机", "程序生成", "procedural", "沙盒", "sandbox")) {
            score += 15;
        }
        if (containsAny(lowerGoal, "多人", "联机", "社交", "竞技", "合作")) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * 预测玩家痛点
     * 基于评分找出可能导致玩家流失的问题
     */
    private List<String> predictPainPoints(String lowerGoal, GameProject project, FunScore score) {
        List<String> points = new ArrayList<>();

        if (score.getCoreLoopScore() < 50) {
            points.add("核心玩法不够吸引人 — 建议增加更具成瘾性的核心机制");
        }
        if (score.getChallengeScore() < 40) {
            points.add("缺少挑战感 — 玩家可能很快感到无聊，需要增加难度递增");
        }
        if (score.getRewardScore() < 40) {
            points.add("奖励反馈不足 — 玩家行为缺少正向反馈，容易失去动力");
        }
        if (score.getProgressionScore() < 40) {
            points.add("进度感弱 — 玩家感受不到成长，容易流失");
        }
        if (score.getNoveltyScore() < 35) {
            points.add("缺乏新意 — 与同类游戏差异不大，难以吸引玩家尝试");
        }

        // 特定类型痛点
        if (containsAny(lowerGoal, "roguelike", "随机") && !containsAny(lowerGoal, "存档", "永久")) {
            points.add("Roguelike类游戏需要考虑永久成长系统，否则挫败感过强");
        }

        return points;
    }

    /**
     * 生成改进建议
     * 按影响度排序（分数最低的维度优先改进）
     */
    private List<String> generateImprovements(String lowerGoal, GameProject project, FunScore score) {
        // 维度-分数映射
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        dimensions.put("核心循环", score.getCoreLoopScore());
        dimensions.put("挑战感", score.getChallengeScore());
        dimensions.put("奖励反馈", score.getRewardScore());
        dimensions.put("进度感", score.getProgressionScore());
        dimensions.put("新颖度", score.getNoveltyScore());

        // 按分数升序排序（最弱的维度优先改进）
        List<Map.Entry<String, Integer>> sorted = dimensions.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .toList();

        List<String> improvements = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            if (entry.getValue() < 60) {
                improvements.add(getImprovementSuggestion(entry.getKey(), entry.getValue(), lowerGoal));
            }
        }

        return improvements;
    }

    /**
     * 获取具体改进建议
     */
    private String getImprovementSuggestion(String dimension, int currentScore, String gameContext) {
        return switch (dimension) {
            case "核心循环" -> "核心循环改进: 增加核心玩法的深度和可重复性，让玩家每次游玩都有不同体验。可参考成功游戏的心流设计——操作简单但精通困难。";
            case "挑战感" -> "挑战感改进: 设计渐进式难度曲线，前期引导学习，中期增加复杂度，后期考验技巧。加入Boss战或特殊挑战关卡作为里程碑。";
            case "奖励反馈" -> "奖励反馈改进: 为玩家每个有意义的行为添加即时反馈（视觉特效、音效、数值变化）。设计多层次奖励：即时奖励（击杀）、短期奖励（关卡通关）、长期奖励（成就解锁）。";
            case "进度感" -> "进度感改进: 增加可视化的进度系统（经验条、成就墙、收集图鉴）。让玩家能清楚看到自己的成长轨迹，并预告即将到来的新内容。";
            case "新颖度" -> "新颖度改进: 在成熟玩法基础上加入独特元素（如Roguelike随机性、社交互动、创意机制）。" +
                "不必完全创新，但需要有1-2个让人眼前一亮的差异化特性。";
            default -> dimension + "需要改进: 当前评分 " + currentScore + "/100，建议参考同类成功游戏的做法。";
        };
    }

    /**
     * 获取竞品参考建议
     * 基于游戏类型推荐可参考的成功游戏设计模式
     *
     * @param gameType 游戏类型关键词
     * @return 竞品参考文本
     */
    public String getCompetitorReference(String gameType) {
        if (gameType == null) return "";

        String lower = gameType.toLowerCase();
        StringBuilder ref = new StringBuilder();

        if (containsAny(lower, "射击", "shooter")) {
            ref.append("### 射击类参考\n");
            ref.append("- 武器手感是核心，投入精力打磨射击反馈\n");
            ref.append("- 敌人AI要有层次感：炮灰型、精英型、Boss型\n");
            ref.append("- 弹药/资源管理增加策略深度\n\n");
        }
        if (containsAny(lower, "rpg", "角色扮演")) {
            ref.append("### RPG类参考\n");
            ref.append("- 角色成长曲线要平滑，避免前期无聊后期无挑战\n");
            ref.append("- 技能树设计要有多样性，支持不同玩法风格\n");
            ref.append("- 剧情和世界观是RPG的灵魂，投入足够篇幅\n\n");
        }
        if (containsAny(lower, "塔防", "tower defense", "策略", "strategy")) {
            ref.append("### 策略类参考\n");
            ref.append("- 单位/建筑多样性是策略深度的基础\n");
            ref.append("- 资源经济系统要平衡，不能太容易也不能太难\n");
            ref.append("- 提供多种胜利路径，避免单一最优解\n\n");
        }
        if (containsAny(lower, "益智", "puzzle", "消除")) {
            ref.append("### 益智类参考\n");
            ref.append("- 上手要极其简单，3秒内理解玩法\n");
            ref.append("- 难度递增要平滑，每5-10关引入新机制\n");
            ref.append("- 加入限时、限步等变体增加紧张感\n\n");
        }

        return ref.toString();
    }

    /**
     * 判断字符串是否包含任一关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
