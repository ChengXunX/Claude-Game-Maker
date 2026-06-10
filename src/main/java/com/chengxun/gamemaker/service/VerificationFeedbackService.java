package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.model.TaskAssignment;
import com.chengxun.gamemaker.manager.ProjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 验证反馈服务
 * 将游戏验证结果转化为 Agent 可执行的修复任务
 *
 * 核心闭环：验证 → 发现问题 → 派任务 → Agent 修复 → 再验证
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class VerificationFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(VerificationFeedbackService.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired(required = false)
    private EventBus eventBus;

    @Autowired(required = false)
    private ProjectBoard projectBoard;

    @Autowired(required = false)
    private KnowledgeEvolutionService knowledgeEvolutionService;

    /**
     * 处理验证结果，生成修复任务并分配给 Agent
     *
     * @param projectId 项目 ID
     * @param analysisResult AI 分析结果
     */
    public void processVerificationResult(String projectId,
                                           GameRuntimeVerifier.QualityAnalysisResult analysisResult) {
        if (analysisResult == null || !analysisResult.isSuccess()) return;

        GameProject project = projectManager.getProject(projectId);
        if (project == null) return;

        // 1. 发布验证结果事件
        publishVerifyEvent(projectId, analysisResult);

        // 2. 根据问题生成修复任务
        List<FixTask> fixTasks = generateFixTasks(projectId, analysisResult);

        // 3. 分配任务给对应 Agent
        assignFixTasks(projectId, fixTasks);

        // 4. 将验证经验存入知识库
        saveVerificationKnowledge(projectId, analysisResult);

        log.info("验证反馈处理完成: 项目={}, 生成{}个修复任务", projectId, fixTasks.size());
    }

    /**
     * 发布验证结果事件
     */
    private void publishVerifyEvent(String projectId, GameRuntimeVerifier.QualityAnalysisResult result) {
        if (eventBus == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("overallScore", result.getOverallScore());
        data.put("runnableScore", result.getRunnableScore());
        data.put("playableScore", result.getPlayableScore());
        data.put("completenessScore", result.getCompletenessScore());
        data.put("uiuxScore", result.getUiuxScore());
        data.put("codeQualityScore", result.getCodeQualityScore());
        data.put("summary", result.getSummary());
        data.put("issues", result.getIssues());
        data.put("suggestions", result.getSuggestions());

        eventBus.publish(projectId, EventBus.VERIFY_RESULT, "system", data);
    }

    /**
     * 根据分析结果生成修复任务
     * 按问题类型分配给对应角色的 Agent
     */
    private List<FixTask> generateFixTasks(String projectId,
                                            GameRuntimeVerifier.QualityAnalysisResult result) {
        List<FixTask> tasks = new ArrayList<>();

        // 可运行性问题 → 服务端开发修复
        if (result.getRunnableScore() < 60) {
            tasks.add(new FixTask(
                "修复运行时错误",
                String.format("游戏可运行性评分 %d/100，需要修复以下问题:\n%s",
                    result.getRunnableScore(),
                    String.join("\n", filterIssuesByType(result.getIssues(), "runnable"))),
                "server-dev",
                "HIGH"
            ));
        }

        // 可玩性问题 → 系统策划设计改进
        if (result.getPlayableScore() < 60) {
            tasks.add(new FixTask(
                "改进核心玩法",
                String.format("游戏可玩性评分 %d/100，需要改进:\n%s",
                    result.getPlayableScore(),
                    String.join("\n", filterIssuesByType(result.getIssues(), "playable"))),
                "system-planner",
                "HIGH"
            ));
        }

        // 完整性问题 → 系统策划补充设计
        if (result.getCompletenessScore() < 60) {
            tasks.add(new FixTask(
                "补充缺失功能",
                String.format("玩法完整性评分 %d/100，以下功能缺失:\n%s",
                    result.getCompletenessScore(),
                    String.join("\n", filterIssuesByType(result.getIssues(), "completeness"))),
                "system-planner",
                "MEDIUM"
            ));
        }

        // UI/UX 问题 → 前端开发改进
        if (result.getUiuxScore() < 60) {
            tasks.add(new FixTask(
                "改进界面和交互",
                String.format("UI/UX 评分 %d/100，需要改进:\n%s",
                    result.getUiuxScore(),
                    String.join("\n", filterIssuesByType(result.getIssues(), "uiux"))),
                "server-dev",
                "MEDIUM"
            ));
        }

        // 代码质量问题 → 服务端开发重构
        if (result.getCodeQualityScore() < 60) {
            tasks.add(new FixTask(
                "修复代码质量问题",
                String.format("代码质量评分 %d/100，需要修复:\n%s",
                    result.getCodeQualityScore(),
                    String.join("\n", filterIssuesByType(result.getIssues(), "code"))),
                "server-dev",
                "LOW"
            ));
        }

        // 通用建议 → 制作人统筹
        if (!result.getSuggestions().isEmpty() && result.getOverallScore() < 70) {
            tasks.add(new FixTask(
                "版本改进计划",
                String.format("综合评分 %d/100，AI 建议:\n%s",
                    result.getOverallScore(),
                    String.join("\n", result.getSuggestions())),
                "producer",
                "MEDIUM"
            ));
        }

        return tasks;
    }

    /**
     * 根据问题类型过滤
     */
    private List<String> filterIssuesByType(List<String> issues, String type) {
        if (issues.isEmpty()) return List.of("（AI 未列出具体问题，请参考总评）");
        // 返回所有问题，因为 AI 可能不会按类型分类
        return issues.stream().limit(5).toList();
    }

    /**
     * 分配修复任务给 Agent
     */
    private void assignFixTasks(String projectId, List<FixTask> tasks) {
        for (FixTask task : tasks) {
            // 查找对应角色的 Agent
            Agent agent = findAgentByRole(projectId, task.targetRole);
            if (agent == null) {
                log.warn("未找到角色 {} 的 Agent，跳过任务: {}", task.targetRole, task.title);
                continue;
            }

            // 通过 Agent 的任务队列派发
            String taskContent = String.format(
                "【验证反馈修复任务】\n\n标题: %s\n优先级: %s\n\n%s\n\n" +
                "请在工作目录中修复以上问题，完成后报告修复结果。",
                task.title, task.priority, task.description
            );

            try {
                TaskAssignment fixTask = TaskAssignment.builder()
                    .id("verify-fix-" + UUID.randomUUID().toString())
                    .assignerId("system")
                    .assigneeId(agent.getId())
                    .title(task.title)
                    .description(taskContent)
                    .priority(TaskAssignment.TaskPriority.valueOf(task.priority))
                    .status(TaskAssignment.TaskStatus.PENDING)
                    .build();

                agent.assignTask(fixTask);
                log.info("已分配修复任务给 {}: {}", agent.getName(), task.title);

                // 更新看板
                if (projectBoard != null) {
                    projectBoard.addTaskCard(projectId,
                        new ProjectBoard.TaskCard(fixTask.getId(), task.title, task.targetRole));
                }
            } catch (Exception e) {
                log.error("分配修复任务失败: {} -> {}", agent.getName(), task.title, e);
            }
        }
    }

    /**
     * 查找项目下指定角色的 Agent
     */
    private Agent findAgentByRole(String projectId, String role) {
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        return agents.stream()
            .filter(a -> role.equals(a.getRole()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 将验证经验存入知识库
     * 通过 learnFromGameGeneration 方法记录验证经验
     */
    private void saveVerificationKnowledge(String projectId,
                                            GameRuntimeVerifier.QualityAnalysisResult result) {
        if (knowledgeEvolutionService == null) return;

        try {
            String gameDescription = String.format(
                "项目验证结果 - 综合评分: %d/100\n" +
                "可运行性: %d, 可玩性: %d, 完整性: %d, UI/UX: %d, 代码质量: %d\n" +
                "问题: %s\n建议: %s",
                result.getOverallScore(),
                result.getRunnableScore(), result.getPlayableScore(),
                result.getCompletenessScore(), result.getUiuxScore(), result.getCodeQualityScore(),
                String.join("; ", result.getIssues()),
                String.join("; ", result.getSuggestions())
            );

            // 记录验证经验到知识库
            knowledgeEvolutionService.learnFromGameGeneration(
                gameDescription, null, projectId,
                result.getOverallScore() >= 70
            );
            log.debug("验证经验已存入知识库: 项目={}", projectId);
        } catch (Exception e) {
            log.error("保存验证经验到知识库失败", e);
        }
    }

    /**
     * 修复任务
     */
    private static class FixTask {
        String title;
        String description;
        String targetRole;
        String priority;

        FixTask(String title, String description, String targetRole, String priority) {
            this.title = title;
            this.description = description;
            this.targetRole = targetRole;
            this.priority = priority;
        }
    }
}
