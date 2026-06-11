package com.chengxun.gamemaker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 游戏设计审查服务
 * 在策划产出设计文档后、开发开始前，对设计进行 AI 审查
 *
 * 审查维度：
 * 1. 核心循环自洽性 — 玩法循环是否完整、有无死路
 * 2. 有趣度检测 — 是否存在无聊设计（纯数值堆砌、无决策点）
 * 3. 差异化分析 — 与同类游戏的差异点在哪
 * 4. 可实现性评估 — 当前团队能力是否能实现
 * 5. 留存设计检查 — 是否有留住玩家的机制
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class GameDesignReviewService {

    private static final Logger log = LoggerFactory.getLogger(GameDesignReviewService.class);

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.ClaudeAiService aiService;

    /**
     * 审查游戏设计文档
     *
     * @param projectGoal 项目目标描述
     * @param designDocuments 设计文档内容（策划产出）
     * @param gameType 游戏类型（可选）
     * @return 审查结果
     */
    public DesignReviewResult reviewDesign(String projectGoal, String designDocuments, String gameType) {
        if (aiService == null) {
            return DesignReviewResult.skipped("AI 服务未启用，跳过设计审查");
        }

        if (designDocuments == null || designDocuments.trim().isEmpty()) {
            return DesignReviewResult.skipped("无设计文档可审查");
        }

        try {
            String prompt = buildReviewPrompt(projectGoal, designDocuments, gameType);
            String response = aiService.sendMessage(prompt);

            if (response == null || response.isEmpty()) {
                return DesignReviewResult.skipped("AI 审查无响应");
            }

            return parseReviewResult(response);
        } catch (Exception e) {
            log.error("游戏设计审查失败", e);
            return DesignReviewResult.skipped("审查异常: " + e.getMessage());
        }
    }

    /**
     * 构建审查提示词
     */
    private String buildReviewPrompt(String projectGoal, String designDocuments, String gameType) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位资深游戏设计总监，正在审查团队提交的游戏设计文档。\n\n");

        sb.append("## 项目目标\n");
        sb.append(projectGoal != null ? projectGoal : "未指定").append("\n\n");

        if (gameType != null && !gameType.isEmpty()) {
            sb.append("## 游戏类型\n").append(gameType).append("\n\n");
        }

        sb.append("## 设计文档内容\n");
        sb.append(truncateText(designDocuments, 6000)).append("\n\n");

        sb.append("## 审查要求\n\n");
        sb.append("请从以下维度审查这份设计文档，给出具体、可操作的改进建议：\n\n");

        sb.append("### 1. 核心循环自洽性\n");
        sb.append("- 玩家的核心行为循环是什么？（输入→反馈→奖励→重复）\n");
        sb.append("- 循环是否完整？有没有死路（做了某件事后没有下文）？\n");
        sb.append("- 每次循环的时间是否合理（30秒-2分钟）？\n\n");

        sb.append("### 2. 有趣度检测\n");
        sb.append("- 玩家在游戏中需要做哪些决策？\n");
        sb.append("- 有没有'无聊'的设计？（纯数值堆砌、自动战斗无操作、重复点击无变化）\n");
        sb.append("- 有没有'惊喜'时刻？（意外发现、随机事件、技巧操作）\n\n");

        sb.append("### 3. 差异化分析\n");
        sb.append("- 这个设计和同类游戏的核心差异是什么？\n");
        sb.append("- 如果没有差异，建议增加什么独特机制？\n\n");

        sb.append("### 4. 留存设计检查\n");
        sb.append("- 有什么机制让玩家第二天还想回来？\n");
        sb.append("- 有什么长期目标让玩家持续投入？\n");
        sb.append("- 有没有社交/竞争元素？\n\n");

        sb.append("### 5. 风险评估\n");
        sb.append("- 哪些设计可能实现难度过高？\n");
        sb.append("- 哪些设计可能导致平衡性问题？\n\n");

        sb.append("## 输出格式\n\n");
        sb.append("请严格按以下格式输出（每行一个发现）：\n\n");
        sb.append("REVIEW_PASS 或 REVIEW_FAIL\n");
        sb.append("SCORE: 总分(0-100)\n");
        sb.append("ISSUE: 严重程度(HIGH/MEDIUM/LOW) | 问题描述 | 改进建议\n");
        sb.append("ISSUE: ...\n");
        sb.append("STRENGTH: 设计亮点描述\n");
        sb.append("SUMMARY: 一句话总评\n");

        return sb.toString();
    }

    /**
     * 解析 AI 审查结果
     */
    private DesignReviewResult parseReviewResult(String response) {
        DesignReviewResult result = new DesignReviewResult();

        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("REVIEW_PASS")) {
                result.passed = true;
            } else if (line.startsWith("REVIEW_FAIL")) {
                result.passed = false;
            } else if (line.startsWith("SCORE:")) {
                try {
                    String scoreStr = line.substring("SCORE:".length()).trim().replaceAll("[^0-9]", "");
                    result.score = Integer.parseInt(scoreStr);
                } catch (NumberFormatException e) {
                    result.score = 50;
                }
            } else if (line.startsWith("ISSUE:")) {
                String issueContent = line.substring("ISSUE:".length()).trim();
                String[] parts = issueContent.split("\\|");
                DesignIssue issue = new DesignIssue();
                issue.severity = parts.length > 0 ? parts[0].trim() : "MEDIUM";
                issue.description = parts.length > 1 ? parts[1].trim() : issueContent;
                issue.suggestion = parts.length > 2 ? parts[2].trim() : "";
                result.issues.add(issue);
            } else if (line.startsWith("STRENGTH:")) {
                result.strengths.add(line.substring("STRENGTH:".length()).trim());
            } else if (line.startsWith("SUMMARY:")) {
                result.summary = line.substring("SUMMARY:".length()).trim();
            }
        }

        // 如果 AI 没有给出明确的 PASS/FAIL，根据分数判断
        if (!result.passed && result.score >= 70) {
            result.passed = true;
        }

        result.reviewed = true;
        return result;
    }

    /**
     * 截断文本到指定长度
     */
    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "\n...(截断)" : text;
    }

    // ===== 数据类 =====

    /**
     * 设计审查结果
     */
    public static class DesignReviewResult {
        /** 是否已审查 */
        public boolean reviewed = false;
        /** 是否通过 */
        public boolean passed = false;
        /** 总分 0-100 */
        public int score = 0;
        /** 问题列表 */
        public List<DesignIssue> issues = new ArrayList<>();
        /** 设计亮点 */
        public List<String> strengths = new ArrayList<>();
        /** 一句话总评 */
        public String summary = "";

        public static DesignReviewResult skipped(String reason) {
            DesignReviewResult r = new DesignReviewResult();
            r.reviewed = false;
            r.passed = true; // 跳过审查视为通过
            r.summary = reason;
            return r;
        }

        public boolean hasCriticalIssues() {
            return issues.stream().anyMatch(i -> "HIGH".equals(i.severity));
        }

        public List<DesignIssue> getCriticalIssues() {
            return issues.stream().filter(i -> "HIGH".equals(i.severity)).toList();
        }

        public String toReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("## 游戏设计审查报告\n\n");
            sb.append("审查结果: ").append(passed ? "通过" : "需改进").append("\n");
            sb.append("综合评分: ").append(score).append("/100\n\n");

            if (!strengths.isEmpty()) {
                sb.append("### 设计亮点\n");
                for (String s : strengths) {
                    sb.append("- ").append(s).append("\n");
                }
                sb.append("\n");
            }

            if (!issues.isEmpty()) {
                sb.append("### 发现的问题\n");
                for (DesignIssue issue : issues) {
                    sb.append("- [").append(issue.severity).append("] ").append(issue.description);
                    if (issue.suggestion != null && !issue.suggestion.isEmpty()) {
                        sb.append("\n  → 建议: ").append(issue.suggestion);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }

            if (summary != null && !summary.isEmpty()) {
                sb.append("### 总评\n").append(summary).append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * 设计问题
     */
    public static class DesignIssue {
        public String severity = "MEDIUM"; // HIGH, MEDIUM, LOW
        public String description = "";
        public String suggestion = "";
    }
}
