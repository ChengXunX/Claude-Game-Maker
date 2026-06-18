package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Distill 工作流自动化发现服务
 * 扫描近期会话，发现重复出现的手动工作流，打包为可复用 Skill
 *
 * 与 Dream 的区别：
 * - Dream：提取知识（决策、方案、经验、待办）
 * - Distill：发现可自动化的工作流模式，生成 Skill
 *
 * 主要功能：
 * - 扫描近期会话中的工具调用序列
 * - 识别重复出现的操作模式
 * - 将高频模式打包为 Skill 定义
 * - 去重：与已有 Skill 比对
 *
 * 灵感来源：工作流模式发现与 Skill 生成机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class DistillService {

    private static final Logger log = LoggerFactory.getLogger(DistillService.class);

    /** 工具调用模式：匹配 [tool_name] 或 调用工具: tool_name 等格式 */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
        "\\[(bash|read|write|edit|grep|glob|search|deploy|test|build|commit|push)]",
        Pattern.CASE_INSENSITIVE
    );

    /** 动作模式：识别操作序列 */
    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "(?:执行|运行|运行|读取|写入|编辑|搜索|部署|测试|构建|提交|推送|创建|删除|修改|更新|检查|验证|安装|配置)",
        Pattern.CASE_INSENSITIVE
    );

    /** 工作流关键词：标识流程性操作 */
    private static final String[] WORKFLOW_KEYWORDS = {
        "首先", "然后", "接着", "最后", "步骤", "流程", "先", "再", "接下来",
        "first", "then", "next", "finally", "step", "workflow", "process",
        "1.", "2.", "3.", "4.", "5."
    };

    /** 最小重复次数：同一模式至少出现 N 次才认为可自动化 */
    private static final int MIN_REPEAT_COUNT = 2;

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private SkillManager skillManager;

    @Autowired
    private SystemConfigService configService;

    /**
     * 执行 Distill 工作流发现
     *
     * @param project 项目
     * @param agentId Agent ID
     * @return 发现结果
     */
    public DistillResult distill(GameProject project, String agentId) {
        if (project == null) {
            return DistillResult.failure("项目为空");
        }

        log.info("开始 Distill 工作流发现: agent={}, project={}", agentId, project.getId());

        // 1. 加载近期对话（比 Dream 扫描更多）
        List<ContextManager.ConversationMessage> recentMessages =
            contextManager.getRecentMessages(agentId, project, 200);

        if (recentMessages.size() < 10) {
            return DistillResult.failure("对话太少，无法发现模式");
        }

        // 2. 提取工具调用序列
        List<List<String>> toolSequences = extractToolSequences(recentMessages);

        // 3. 发现重复模式
        List<WorkflowPattern> patterns = findRepeatedPatterns(toolSequences);

        // 4. 过滤已有 Skill
        List<WorkflowPattern> newPatterns = filterExistingSkills(project, agentId, patterns);

        // 5. 生成 Skill 定义
        List<DistillableSkill> skills = generateSkills(newPatterns);

        // 6. 限制数量
        int maxItems = configService.getInt("distill.max-extract-items", 10);
        if (skills.size() > maxItems) {
            skills = skills.subList(0, maxItems);
        }

        DistillResult result = new DistillResult(true, skills.size(), patterns.size(), skills);
        log.info("Distill 完成: agent={}, project={}, 发现模式={}, 生成Skill={}",
            agentId, project.getId(), patterns.size(), skills.size());

        return result;
    }

    /**
     * 从对话中提取工具调用序列
     * 每条消息提取一个工具调用序列
     */
    private List<List<String>> extractToolSequences(List<ContextManager.ConversationMessage> messages) {
        List<List<String>> sequences = new ArrayList<>();

        for (ContextManager.ConversationMessage msg : messages) {
            String content = msg.getContent();
            if (content == null || content.length() < 10) continue;

            List<String> tools = new ArrayList<>();
            Matcher matcher = TOOL_CALL_PATTERN.matcher(content);
            while (matcher.find()) {
                tools.add(matcher.group(1).toLowerCase());
            }

            // 也提取动作模式
            Matcher actionMatcher = ACTION_PATTERN.matcher(content);
            while (actionMatcher.find()) {
                tools.add("action:" + actionMatcher.group());
            }

            if (!tools.isEmpty()) {
                sequences.add(tools);
            }
        }

        return sequences;
    }

    /**
     * 发现重复出现的工具调用模式
     * 使用滑动窗口 + 频繁项集挖掘
     */
    private List<WorkflowPattern> findRepeatedPatterns(List<List<String>> sequences) {
        // 统计 2-gram 和 3-gram 的频率
        Map<String, Integer> ngramCounts = new HashMap<>();
        Map<String, List<String>> ngramExamples = new HashMap<>();

        for (List<String> seq : sequences) {
            // 2-gram
            for (int i = 0; i < seq.size() - 1; i++) {
                String bigram = seq.get(i) + " -> " + seq.get(i + 1);
                ngramCounts.merge(bigram, 1, Integer::sum);
                ngramExamples.computeIfAbsent(bigram, k -> new ArrayList<>())
                    .add(seq.get(i) + " -> " + seq.get(i + 1));
            }

            // 3-gram
            for (int i = 0; i < seq.size() - 2; i++) {
                String trigram = seq.get(i) + " -> " + seq.get(i + 1) + " -> " + seq.get(i + 2);
                ngramCounts.merge(trigram, 1, Integer::sum);
                ngramExamples.computeIfAbsent(trigram, k -> new ArrayList<>())
                    .add(seq.get(i) + " -> " + seq.get(i + 1) + " -> " + seq.get(i + 2));
            }
        }

        // 过滤低频模式
        return ngramCounts.entrySet().stream()
            .filter(e -> e.getValue() >= MIN_REPEAT_COUNT)
            .map(e -> new WorkflowPattern(e.getKey(), e.getValue(),
                ngramExamples.getOrDefault(e.getKey(), List.of())))
            .sorted(Comparator.comparingInt(WorkflowPattern::count).reversed())
            .limit(20)
            .toList();
    }

    /**
     * 过滤已有 Skill 覆盖的模式
     */
    private List<WorkflowPattern> filterExistingSkills(GameProject project, String agentId,
                                                        List<WorkflowPattern> patterns) {
        List<Skill> existingSkills = skillManager.getAllSkills(project.getId());
        Set<String> existingNames = existingSkills.stream()
            .map(Skill::getName)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return patterns.stream()
            .filter(p -> {
                // 生成候选名称
                String candidateName = generateSkillName(p.pattern());
                return !existingNames.contains(candidateName.toLowerCase());
            })
            .toList();
    }

    /**
     * 将模式打包为可发布的 Skill
     */
    private List<DistillableSkill> generateSkills(List<WorkflowPattern> patterns) {
        List<DistillableSkill> skills = new ArrayList<>();

        for (WorkflowPattern pattern : patterns) {
            String name = generateSkillName(pattern.pattern());
            String description = generateSkillDescription(pattern);
            String content = generateSkillContent(pattern);

            skills.add(new DistillableSkill(name, description, content, pattern.count()));
        }

        return skills;
    }

    /**
     * 生成 Skill 名称
     */
    private String generateSkillName(String pattern) {
        // 将 "bash -> read -> edit" 转换为 "bash-read-edit"
        return pattern.toLowerCase()
            .replace(" -> ", "-")
            .replace("action:", "")
            .replaceAll("[^a-z0-9-]", "")
            .substring(0, Math.min(pattern.length(), 50));
    }

    /**
     * 生成 Skill 描述
     */
    private String generateSkillDescription(WorkflowPattern pattern) {
        String[] steps = pattern.pattern().split(" -> ");
        StringBuilder desc = new StringBuilder("自动化工作流: ");
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) desc.append(" -> ");
            desc.append(steps[i]);
        }
        desc.append(String.format(" (出现 %d 次)", pattern.count()));
        return desc.toString();
    }

    /**
     * 生成 Skill 内容（Markdown 格式）
     */
    private String generateSkillContent(WorkflowPattern pattern) {
        String[] steps = pattern.pattern().split(" -> ");
        StringBuilder content = new StringBuilder();
        content.append("## 自动化步骤\n\n");
        content.append("此 Skill 由 Distill 自动发现，基于重复出现的工作模式生成。\n\n");
        content.append("### 执行步骤\n\n");
        for (int i = 0; i < steps.length; i++) {
            content.append(String.format("%d. %s\n", i + 1, describeStep(steps[i])));
        }
        content.append("\n### 统计信息\n\n");
        content.append(String.format("- 模式出现次数: %d\n", pattern.count()));
        content.append(String.format("- 步骤数: %d\n", steps.length));
        return content.toString();
    }

    /**
     * 描述单个步骤
     */
    private String describeStep(String step) {
        if (step.startsWith("action:")) {
            return step.substring(7);
        }
        return switch (step.toLowerCase()) {
            case "bash" -> "执行命令";
            case "read" -> "读取文件";
            case "write" -> "写入文件";
            case "edit" -> "编辑文件";
            case "grep" -> "搜索内容";
            case "glob" -> "搜索文件";
            case "search" -> "搜索";
            case "deploy" -> "部署";
            case "test" -> "测试";
            case "build" -> "构建";
            case "commit" -> "提交";
            case "push" -> "推送";
            default -> step;
        };
    }

    /**
     * 将发现的 Skill 保存到知识库（不直接创建 Skill，只记录发现）
     */
    public int saveDistillResult(GameProject project, String agentId, List<DistillableSkill> skills) {
        int savedCount = 0;
        for (DistillableSkill skill : skills) {
            String key = "distill_" + skill.name();
            memoryManager.saveMemory(project, agentId, "knowledge", key,
                String.format("## %s\n\n%s\n\n重复次数: %d", skill.name(), skill.description(), skill.count()));
            savedCount++;
        }
        return savedCount;
    }

    // ===== 内部类 =====

    /**
     * 工作流模式
     */
    public record WorkflowPattern(String pattern, int count, List<String> examples) {}

    /**
     * 可发布的 Skill
     */
    public record DistillableSkill(String name, String description, String content, int count) {}

    /**
     * Distill 结果
     */
    public record DistillResult(boolean success, int skillCount, int patternCount,
                                 List<DistillableSkill> skills) {
        static DistillResult failure(String reason) {
            return new DistillResult(false, 0, 0, Collections.emptyList());
        }
    }
}
