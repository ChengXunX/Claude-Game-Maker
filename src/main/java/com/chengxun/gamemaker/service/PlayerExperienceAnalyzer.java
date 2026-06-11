package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家体验分析器
 * 从玩家视角评估游戏设计，提供趣味度评分和改进建议
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
        public String toReadableText() {
            StringBuilder sb = new StringBuilder();
            sb.append("## 玩家体验评分\n\n");
            sb.append(String.format("- 综合分: **%d/100**\n", overallScore));
            sb.append(String.format("- 核心循环: %d/100\n", coreLoopScore));
            sb.append(String.format("- 挑战感: %d/100\n", challengeScore));
            sb.append(String.format("- 奖励反馈: %d/100\n", rewardScore));
            sb.append(String.format("- 进度感: %d/100\n", progressionScore));
            sb.append(String.format("- 新颖度: %d/100\n\n", noveltyScore));

            if (!painPoints.isEmpty()) {
                sb.append("### 预测痛点\n");
                for (String point : painPoints) {
                    sb.append("- ").append(point).append("\n");
                }
                sb.append("\n");
            }

            if (!improvements.isEmpty()) {
                sb.append("### 改进建议（按影响度排序）\n");
                for (int i = 0; i < improvements.size(); i++) {
                    sb.append(String.format("%d. %s\n", i + 1, improvements.get(i)));
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * 分析项目的玩家体验
     * 基于项目目标、游戏类型、当前进度进行评估
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

        // 1. 核心循环评分 — 基于游戏类型和目标描述判断
        score.setCoreLoopScore(evaluateCoreLoop(lowerGoal, project));

        // 2. 挑战感评分
        score.setChallengeScore(evaluateChallenge(lowerGoal, project));

        // 3. 奖励反馈评分
        score.setRewardScore(evaluateReward(lowerGoal, project));

        // 4. 进度感评分
        score.setProgressionScore(evaluateProgression(lowerGoal, project));

        // 5. 新颖度评分
        score.setNoveltyScore(evaluateNovelty(lowerGoal, project));

        // 计算综合分
        score.calculateOverall();

        // 生成痛点预测
        score.setPainPoints(predictPainPoints(lowerGoal, project, score));

        // 生成改进建议
        score.setImprovements(generateImprovements(lowerGoal, project, score));

        scoreCache.put(projectId, score);
        return score;
    }

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
