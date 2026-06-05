package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.dingtalk.DingTalkService;
import com.chengxun.gamemaker.web.service.NotificationService;
import com.chengxun.gamemaker.web.entity.Notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 游戏评估服务
 * 自动评估生成游戏的质量，参考 OpenGame-Bench 的评估维度：
 * - 构建健康度（Build Health）：代码是否能成功编译运行
 * - 视觉可用性（Visual Usability）：UI 是否完整、交互是否流畅
 * - 意图对齐（Intent Alignment）：生成结果是否符合用户描述
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class GameEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(GameEvaluationService.class);

    private final NotificationService notificationService;
    private final DingTalkService dingTalkService;

    /** 评估结果缓存 */
    private final Map<String, EvaluationResult> evaluationCache = new ConcurrentHashMap<>();

    public GameEvaluationService(NotificationService notificationService, DingTalkService dingTalkService) {
        this.notificationService = notificationService;
        this.dingTalkService = dingTalkService;
    }

    /**
     * 评估游戏项目
     *
     * @param projectId 项目ID
     * @param projectPath 项目路径
     * @param userPrompt 用户原始描述
     * @return 评估结果
     */
    public EvaluationResult evaluate(String projectId, String projectPath, String userPrompt) {
        log.info("开始评估游戏项目: projectId={}", projectId);

        EvaluationResult result = new EvaluationResult();
        result.setProjectId(projectId);
        result.setEvaluatedAt(LocalDateTime.now());

        // 1. 构建健康度评估
        BuildHealth buildHealth = evaluateBuildHealth(projectPath);
        result.setBuildHealth(buildHealth);

        // 2. 视觉可用性评估
        VisualUsability visualUsability = evaluateVisualUsability(projectPath);
        result.setVisualUsability(visualUsability);

        // 3. 意图对齐评估
        IntentAlignment intentAlignment = evaluateIntentAlignment(projectPath, userPrompt);
        result.setIntentAlignment(intentAlignment);

        // 计算总分
        double totalScore = calculateTotalScore(buildHealth, visualUsability, intentAlignment);
        result.setTotalScore(totalScore);
        result.setGrade(calculateGrade(totalScore));

        // 缓存结果
        evaluationCache.put(projectId, result);

        log.info("游戏评估完成: projectId={}, score={}, grade={}",
            projectId, totalScore, result.getGrade());

        // 发送通知
        try {
            String title = "游戏评估完成";
            String content = String.format("项目 %s 评估完成，评分: %.1f，等级: %s",
                projectId, totalScore, result.getGrade());
            notificationService.sendSystemNotification(null, title, content, NotificationType.INFO);

            // 钉钉通知
            dingTalkService.sendMarkdown(title,
                "## 游戏评估完成\n\n- **项目**: " + projectId +
                "\n- **评分**: " + String.format("%.1f", totalScore) +
                "\n- **等级**: " + result.getGrade());
        } catch (Exception e) {
            log.warn("发送评估通知失败", e);
        }

        return result;
    }

    /**
     * 评估构建健康度
     * 检查项目是否能成功编译运行
     */
    private BuildHealth evaluateBuildHealth(String projectPath) {
        BuildHealth health = new BuildHealth();
        int score = 0;
        List<String> issues = new ArrayList<>();

        // 检查必要文件是否存在
        Path indexPath = Path.of(projectPath, "index.html");
        Path packagePath = Path.of(projectPath, "package.json");

        if (Files.exists(indexPath)) {
            score += 20;
        } else {
            issues.add("缺少 index.html 文件");
        }

        if (Files.exists(packagePath)) {
            score += 10;
        }

        // 检查 JavaScript 语法错误
        try {
            List<Path> jsFiles = findFiles(projectPath, "*.js");
            for (Path jsFile : jsFiles) {
                String content = Files.readString(jsFile);
                if (hasSyntaxErrors(content)) {
                    issues.add("语法错误: " + jsFile.getFileName());
                } else {
                    score += 2;
                }
            }
        } catch (IOException e) {
            issues.add("无法读取项目文件");
        }

        // 检查 HTML 结构
        try {
            if (Files.exists(indexPath)) {
                String html = Files.readString(indexPath);
                if (html.contains("<canvas") || html.contains("<div id=\"app\"")) {
                    score += 15;
                }
                if (html.contains("<script")) {
                    score += 10;
                }
                if (html.contains("</html>")) {
                    score += 5;
                }
            }
        } catch (IOException e) {
            // 忽略
        }

        health.setScore(Math.min(100, score));
        health.setIssues(issues);
        health.setStatus(score >= 60 ? "PASS" : "FAIL");

        return health;
    }

    /**
     * 评估视觉可用性
     * 检查 UI 元素是否完整
     */
    private VisualUsability evaluateVisualUsability(String projectPath) {
        VisualUsability usability = new VisualUsability();
        int score = 0;
        List<String> features = new ArrayList<>();

        try {
            List<Path> jsFiles = findFiles(projectPath, "*.js");
            String allContent = readAllFiles(jsFiles);

            // 检查 Canvas/WebGL 使用
            if (allContent.contains("getContext") || allContent.contains("Phaser")) {
                score += 20;
                features.add("图形渲染");
            }

            // 检查用户交互
            if (allContent.contains("addEventListener") || allContent.contains("onkeydown") || allContent.contains("createCursorKeys")) {
                score += 15;
                features.add("用户输入");
            }

            // 检查动画
            if (allContent.contains("requestAnimationFrame") || allContent.contains("tween") || allContent.contains("animate")) {
                score += 10;
                features.add("动画系统");
            }

            // 检查音效
            if (allContent.contains("Audio") || allContent.contains("sound") || allContent.contains("play()")) {
                score += 10;
                features.add("音效系统");
            }

            // 检查 UI 元素
            if (allContent.contains("score") || allContent.contains("health") || allContent.contains("HUD")) {
                score += 10;
                features.add("HUD界面");
            }

            // 检查菜单系统
            if (allContent.contains("menu") || allContent.contains("Menu") || allContent.contains("start")) {
                score += 10;
                features.add("菜单系统");
            }

            // 检查碰撞检测
            if (allContent.contains("collision") || allContent.contains("overlap") || allContent.contains("collide")) {
                score += 10;
                features.add("碰撞检测");
            }

            // 检查游戏状态管理
            if (allContent.contains("gameOver") || allContent.contains("win") || allContent.contains("lose")) {
                score += 10;
                features.add("胜负判定");
            }

            // 检查资源加载
            if (allContent.contains("preload") || allContent.contains("load.image") || allContent.contains("load.audio")) {
                score += 10;
                features.add("资源加载");
            }

        } catch (IOException e) {
            // 忽略
        }

        usability.setScore(Math.min(100, score));
        usability.setFeatures(features);
        usability.setStatus(score >= 50 ? "PASS" : "FAIL");

        return usability;
    }

    /**
     * 评估意图对齐
     * 检查生成结果是否符合用户描述
     */
    private IntentAlignment evaluateIntentAlignment(String projectPath, String userPrompt) {
        IntentAlignment alignment = new IntentAlignment();
        int score = 0;
        List<String> matches = new ArrayList<>();

        if (userPrompt == null || userPrompt.isEmpty()) {
            alignment.setScore(50);
            alignment.setStatus("UNKNOWN");
            return alignment;
        }

        String lowerPrompt = userPrompt.toLowerCase();

        try {
            List<Path> allFiles = findFiles(projectPath, "*");
            String allContent = readAllFiles(allFiles).toLowerCase();

            // 检查游戏类型匹配
            String[] gameTypes = {"platformer", "tower defense", "rpg", "puzzle", "shooter", "racing", "strategy"};
            for (String type : gameTypes) {
                if (lowerPrompt.contains(type) && allContent.contains(type.replace(" ", ""))) {
                    score += 15;
                    matches.add("游戏类型匹配: " + type);
                }
            }

            // 检查关键特性
            String[] features = {"player", "enemy", "score", "level", "health", "weapon", "map", "quest"};
            for (String feature : features) {
                if (lowerPrompt.contains(feature) && allContent.contains(feature)) {
                    score += 5;
                    matches.add("特性匹配: " + feature);
                }
            }

            // 检查技术要求
            String[] techs = {"phaser", "three.js", "canvas", "websocket", "multiplayer"};
            for (String tech : techs) {
                if (lowerPrompt.contains(tech) && allContent.contains(tech)) {
                    score += 10;
                    matches.add("技术匹配: " + tech);
                }
            }

        } catch (IOException e) {
            // 忽略
        }

        alignment.setScore(Math.min(100, Math.max(20, score)));
        alignment.setMatches(matches);
        alignment.setStatus(score >= 40 ? "PASS" : "PARTIAL");

        return alignment;
    }

    /**
     * 计算总分
     */
    private double calculateTotalScore(BuildHealth buildHealth, VisualUsability visualUsability, IntentAlignment intentAlignment) {
        // 权重：构建健康度 40%，视觉可用性 30%，意图对齐 30%
        return buildHealth.getScore() * 0.4 +
               visualUsability.getScore() * 0.3 +
               intentAlignment.getScore() * 0.3;
    }

    /**
     * 计算等级
     */
    private String calculateGrade(double score) {
        if (score >= 90) return "S";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "F";
    }

    /**
     * 获取评估结果
     */
    public EvaluationResult getResult(String projectId) {
        return evaluationCache.get(projectId);
    }

    /**
     * 查找文件
     */
    private List<Path> findFiles(String dir, String glob) throws IOException {
        List<Path> files = new ArrayList<>();
        Path dirPath = Path.of(dir);
        if (Files.exists(dirPath)) {
            try (var stream = Files.walk(dirPath, 10)) {
                stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        if (glob.equals("*")) return true;
                        return p.toString().endsWith(glob.replace("*", ""));
                    })
                    .forEach(files::add);
            }
        }
        return files;
    }

    /**
     * 读取所有文件内容
     */
    private String readAllFiles(List<Path> files) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Path file : files) {
            try {
                sb.append(Files.readString(file)).append("\n");
            } catch (IOException e) {
                // 忽略无法读取的文件
            }
        }
        return sb.toString();
    }

    /**
     * 检查 JavaScript 语法错误（简单检查）
     */
    private boolean hasSyntaxErrors(String content) {
        // 简单检查：括号匹配
        int braces = 0;
        int brackets = 0;
        int parens = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (inString) {
                if (c == stringChar && content.charAt(i - 1) != '\\') {
                    inString = false;
                }
                continue;
            }

            if (c == '"' || c == '\'' || c == '`') {
                inString = true;
                stringChar = c;
                continue;
            }

            if (c == '{') braces++;
            if (c == '}') braces--;
            if (c == '[') brackets++;
            if (c == ']') brackets--;
            if (c == '(') parens++;
            if (c == ')') parens--;
        }

        return braces != 0 || brackets != 0 || parens != 0;
    }

    // ===== 内部类 =====

    public static class EvaluationResult {
        private String projectId;
        private LocalDateTime evaluatedAt;
        private BuildHealth buildHealth;
        private VisualUsability visualUsability;
        private IntentAlignment intentAlignment;
        private double totalScore;
        private String grade;

        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
        public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
        public BuildHealth getBuildHealth() { return buildHealth; }
        public void setBuildHealth(BuildHealth buildHealth) { this.buildHealth = buildHealth; }
        public VisualUsability getVisualUsability() { return visualUsability; }
        public void setVisualUsability(VisualUsability visualUsability) { this.visualUsability = visualUsability; }
        public IntentAlignment getIntentAlignment() { return intentAlignment; }
        public void setIntentAlignment(IntentAlignment intentAlignment) { this.intentAlignment = intentAlignment; }
        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
    }

    public static class BuildHealth {
        private int score;
        private String status;
        private List<String> issues = new ArrayList<>();

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
    }

    public static class VisualUsability {
        private int score;
        private String status;
        private List<String> features = new ArrayList<>();

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }
    }

    public static class IntentAlignment {
        private int score;
        private String status;
        private List<String> matches = new ArrayList<>();

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getMatches() { return matches; }
        public void setMatches(List<String> matches) { this.matches = matches; }
    }
}
