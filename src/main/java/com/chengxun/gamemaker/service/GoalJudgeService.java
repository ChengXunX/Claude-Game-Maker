package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 目标裁判服务
 * 用独立的 AI 模型评估目标是否真正达成，防止"乐观停止"
 *
 * 工作原理：
 * 1. Agent（如制作人）认为任务完成时，调用此服务
 * 2. 裁判模型独立阅读对话历史 + 验收标准
 * 3. 裁判模型返回 通过/未通过/不可能
 * 4. 只有裁判通过，才真正标记完成
 *
 * 灵感来源：目标评估与裁判机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class GoalJudgeService {

    private static final Logger log = LoggerFactory.getLogger(GoalJudgeService.class);

    @Autowired
    private ClaudeCliEngine cliEngine;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private SystemConfigService configService;

    /**
     * 评估目标是否达成
     *
     * @param project 项目
     * @param goalDescription 目标描述
     * @param verificationCriteria 验收标准
     * @param conversationHistory 对话历史摘要
     * @return 评估结果
     */
    public Verdict evaluateGoal(GameProject project, String goalDescription,
                                 String verificationCriteria, String conversationHistory) {
        if (!isEnabled()) {
            return Verdict.passed("裁判验证已禁用，自动通过");
        }

        log.info("裁判评估开始: project={}, goal={}", project.getId(), goalDescription);

        try {
            // 构建裁判提示词
            String judgePrompt = buildJudgePrompt(goalDescription, verificationCriteria, conversationHistory);

            // 使用独立的 AI 调用评估
            String response = cliEngine.sendMessage(
                "goal-judge-" + project.getId(),
                null, // 不复用会话
                judgePrompt,
                project.getWorkDir(),
                appConfig.getApiKey(),
                appConfig.getApiUrl(),
                appConfig.getModel()
            );

            // 解析裁判结果
            Verdict verdict = parseVerdict(response);

            log.info("裁判评估完成: project={}, ok={}, impossible={}, reason={}",
                project.getId(), verdict.ok(), verdict.impossible(), verdict.reason());

            return verdict;
        } catch (Exception e) {
            log.error("裁判评估失败: project={}", project.getId(), e);
            return Verdict.failed("裁判评估异常: " + e.getMessage());
        }
    }

    /**
     * 评估里程碑是否达成
     *
     * @param project 项目
     * @param milestoneTitle 里程碑标题
     * @param milestoneDescription 里程碑描述
     * @param verificationCriteria 验收标准
     * @param taskResults 任务结果列表
     * @return 评估结果
     */
    public Verdict evaluateMilestone(GameProject project, String milestoneTitle,
                                      String milestoneDescription, String verificationCriteria,
                                      List<String> taskResults) {
        StringBuilder history = new StringBuilder();
        history.append("## 里程碑信息\n");
        history.append("- 标题: ").append(milestoneTitle).append("\n");
        history.append("- 描述: ").append(milestoneDescription).append("\n\n");

        history.append("## 任务结果\n");
        for (int i = 0; i < taskResults.size(); i++) {
            history.append("### 任务 ").append(i + 1).append("\n");
            history.append(taskResults.get(i)).append("\n\n");
        }

        return evaluateGoal(project, milestoneTitle, verificationCriteria, history.toString());
    }

    /**
     * 构建裁判提示词
     */
    private String buildJudgePrompt(String goalDescription, String verificationCriteria, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个严格的目标评估裁判。你的任务是独立评估一个目标是否真正达成。\n\n");
        prompt.append("## 评估原则\n");
        prompt.append("1. 只看证据，不听承诺\n");
        prompt.append("2. 如果没有明确证据证明目标已达成，判定为未通过\n");
        prompt.append("3. 只有在目标确实不可能达成时，才判定为不可能\n");
        prompt.append("4. 引用具体的对话内容作为证据\n\n");

        prompt.append("## 目标\n").append(goalDescription).append("\n\n");

        if (verificationCriteria != null && !verificationCriteria.isEmpty()) {
            prompt.append("## 验收标准\n").append(verificationCriteria).append("\n\n");
        }

        prompt.append("## 对话历史\n").append(conversationHistory).append("\n\n");

        prompt.append("## 输出格式\n");
        prompt.append("请以 JSON 格式返回评估结果：\n");
        prompt.append("```json\n");
        prompt.append("{\"ok\": true/false, \"impossible\": true/false, \"reason\": \"评估理由（引用具体证据）\"}\n");
        prompt.append("```\n\n");
        prompt.append("- ok: 目标是否已达成\n");
        prompt.append("- impossible: 目标是否不可能达成\n");
        prompt.append("- reason: 评估理由，必须引用具体证据\n");

        return prompt.toString();
    }

    /**
     * 解析裁判结果
     */
    private Verdict parseVerdict(String response) {
        if (response == null || response.isEmpty()) {
            return Verdict.failed("裁判未返回结果");
        }

        try {
            // 尝试提取 JSON
            String json = extractJson(response);
            if (json != null) {
                // 简单解析
                boolean ok = json.contains("\"ok\": true") || json.contains("\"ok\":true");
                boolean impossible = json.contains("\"impossible\": true") || json.contains("\"impossible\":true");
                String reason = extractField(json, "reason");

                return new Verdict(ok, impossible, reason, null);
            }
        } catch (Exception e) {
            log.warn("解析裁判结果失败: {}", e.getMessage());
        }

        // 回退：检查关键词
        String lower = response.toLowerCase();
        if (lower.contains("通过") || lower.contains("满足") || lower.contains("ok")) {
            return Verdict.passed("裁判判定通过");
        }
        if (lower.contains("不可能") || lower.contains("impossible")) {
            return Verdict.impossible("裁判判定目标不可能达成");
        }

        return Verdict.failed("裁判判定未通过");
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";

        idx += pattern.length();
        // 跳过空格
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;

        if (idx < json.length() && json.charAt(idx) == '"') {
            int endQuote = json.indexOf('"', idx + 1);
            if (endQuote > idx) {
                return json.substring(idx + 1, endQuote);
            }
        }
        return "";
    }

    private boolean isEnabled() {
        return configService.getBoolean("goal.judge.enabled", true);
    }

    /**
     * 裁判评估结果
     */
    public record Verdict(boolean ok, boolean impossible, String reason, String error) {
        static Verdict passed(String reason) {
            return new Verdict(true, false, reason, null);
        }

        static Verdict failed(String reason) {
            return new Verdict(false, false, reason, null);
        }

        static Verdict impossible(String reason) {
            return new Verdict(false, true, reason, null);
        }
    }
}
