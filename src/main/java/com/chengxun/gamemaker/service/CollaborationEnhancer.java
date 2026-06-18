package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.GameProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作增强服务
 * 提升 Agent 间协作开发游戏的智能水平
 *
 * 六大增强能力：
 * 1. 任务交接上下文传递 — 分任务时注入设计意图、技术约束、验收标准
 * 2. 游戏开发知识注入 — 将游戏类型、目标用户、核心玩法注入 Agent prompt
 * 3. 质量反馈循环 — 任务产出后自动验证，不通过则返工
 * 4. 跨 Agent 经验共享 — 一个 Agent 学到的教训自动同步给同项目其他 Agent
 * 5. 迭代精炼机制 — 产出→审查→修改→再审查的循环
 * 6. 任务依赖传递 — A 完成的产出，B 可以看到具体内容
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class CollaborationEnhancer {

    private static final Logger log = LoggerFactory.getLogger(CollaborationEnhancer.class);

    @Autowired
    private ProjectBoard projectBoard;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectManager projectManager;

    // ===== 1. 任务交接上下文传递 =====

    /**
     * 构建任务交接上下文
     * 当制作人分配任务时，自动附加设计意图、技术约束、验收标准
     *
     * @param projectId 项目 ID
     * @param taskTitle 任务标题
     * @param taskDescription 任务描述
     * @param assignedRole 被分配的角色
     * @return 丰富的任务上下文文本
     */
    public String buildTaskHandoffContext(String projectId, String taskTitle, String taskDescription, String assignedRole) {
        if (projectId == null) return "";

        StringBuilder ctx = new StringBuilder();
        ctx.append("## 任务交接上下文\n\n");

        // 1. 项目游戏信息
        GameProject project = projectManager.getProject(projectId);
        if (project != null) {
            ctx.append("### 项目信息\n");
            ctx.append(String.format("- 项目名称: %s\n", project.getName()));
            if (project.getGoal() != null) {
                ctx.append(String.format("- 项目目标: %s\n", project.getGoal()));
            }
            if (project.getDescription() != null) {
                ctx.append(String.format("- 项目描述: %s\n", project.getDescription()));
            }
            ctx.append(String.format("- 当前版本: %s\n", project.getVersion() != null ? project.getVersion() : "v1"));
            ctx.append("\n");
        }

        // 2. 从 ProjectBoard 获取相关上下文
        ProjectBoard.BoardData board = projectBoard.getBoard(projectId);
        if (board != null) {
            // 当前版本目标
            String currentGoal = (String) board.sharedContext.get("current_goal");
            if (currentGoal != null) {
                ctx.append("### 当前版本目标\n").append(currentGoal).append("\n\n");
            }

            // 验收标准
            String criteria = (String) board.sharedContext.get("verification_criteria");
            if (criteria != null) {
                ctx.append("### 验收标准\n").append(criteria).append("\n\n");
            }

            // 最近完成的任务（提供上下文）
            List<ProjectBoard.TaskCard> recentDone = board.taskCards.stream()
                .filter(t -> "DONE".equals(t.status))
                .sorted((a, b) -> {
                    if (a.completedAt == null) return 1;
                    if (b.completedAt == null) return -1;
                    return b.completedAt.compareTo(a.completedAt);
                })
                .limit(3)
                .toList();

            if (!recentDone.isEmpty()) {
                ctx.append("### 最近完成的相关任务\n");
                for (ProjectBoard.TaskCard card : recentDone) {
                    ctx.append(String.format("- **%s** (%s)\n", card.title, card.assignedRole));
                    if (card.result != null && !card.result.isEmpty()) {
                        String resultPreview = card.result.length() > 150 ?
                            card.result.substring(0, 150) + "..." : card.result;
                        ctx.append(String.format("  产出: %s\n", resultPreview));
                    }
                }
                ctx.append("\n");
            }

            // 活跃阻塞项
            List<ProjectBoard.Blocker> blockers = board.blockers.stream()
                .filter(b -> !b.resolved)
                .toList();
            if (!blockers.isEmpty()) {
                ctx.append("### ⚠️ 当前阻塞项\n");
                for (ProjectBoard.Blocker b : blockers) {
                    ctx.append(String.format("- [%s] %s\n", b.severity, b.description));
                }
                ctx.append("请在开发时注意规避这些阻塞项。\n\n");
            }
        }

        // 3. 同角色 Agent 的经验教训
        Map<String, String> experiences = memoryManager.getCategoryMemory(project, projectId + ":" + assignedRole, "experiences");
        if (experiences != null && !experiences.isEmpty()) {
            ctx.append("### 历史经验教训\n");
            int count = 0;
            for (Map.Entry<String, String> entry : experiences.entrySet()) {
                if (count >= 3) break;
                if (entry.getKey().startsWith("compact_")) continue; // 跳过压缩记录
                String preview = entry.getValue().length() > 100 ?
                    entry.getValue().substring(0, 100) + "..." : entry.getValue();
                ctx.append(String.format("- %s: %s\n", entry.getKey(), preview));
                count++;
            }
            ctx.append("\n");
        }

        ctx.append("请基于以上上下文理解任务的背景和意图，而不仅仅是字面描述。\n");

        return ctx.toString();
    }

    // ===== 2. 游戏开发知识注入 =====

    /**
     * 构建游戏开发知识上下文
     * 根据项目的游戏类型、目标用户、核心玩法，注入针对性的游戏开发指导
     *
     * @param projectId 项目 ID
     * @param agentRole Agent 角色
     * @return 游戏开发知识 prompt
     */
    public String buildGameKnowledgeContext(String projectId, String agentRole) {
        if (projectId == null) return "";

        GameProject project = projectManager.getProject(projectId);
        if (project == null) return "";

        StringBuilder ctx = new StringBuilder();
        ctx.append("## 游戏开发指导\n\n");

        // 从项目元数据获取游戏信息
        Map<String, String> metadata = project.getMetadata();
        String genre = metadata.getOrDefault("genre", "");
        String coreGameplay = metadata.getOrDefault("coreGameplay", "");
        String targetAudience = metadata.getOrDefault("targetAudience", "");
        String artStyle = metadata.getOrDefault("artStyle", "");
        String gameplayStyle = metadata.getOrDefault("gameplayStyle", "");

        if (!genre.isEmpty() || !coreGameplay.isEmpty()) {
            ctx.append("### 游戏定位\n");
            if (!genre.isEmpty()) ctx.append(String.format("- 游戏类型: %s\n", genre));
            if (!coreGameplay.isEmpty()) ctx.append(String.format("- 核心玩法: %s\n", coreGameplay));
            if (!targetAudience.isEmpty()) ctx.append(String.format("- 目标用户: %s\n", targetAudience));
            if (!artStyle.isEmpty()) ctx.append(String.format("- 美术风格: %s\n", artStyle));
            if (!gameplayStyle.isEmpty()) ctx.append(String.format("- 玩法风格: %s\n", gameplayStyle));
            ctx.append("\n");
        }

        // 根据角色注入针对性指导
        ctx.append("### 角色专项指导\n");
        switch (agentRole) {
            case "server-dev" -> {
                ctx.append("- 优先保证核心玩法循环的服务端逻辑完整\n");
                ctx.append("- 数据模型设计要考虑游戏类型的特殊需求\n");
                ctx.append("- API 设计要方便客户端实现游戏交互\n");
                if (!genre.isEmpty()) {
                    ctx.append(String.format("- %s 类型游戏的服务端重点: %s\n", genre, getServerFocus(genre)));
                }
            }
            case "client-dev", "ui-dev" -> {
                ctx.append("- UI/UX 要符合目标用户的使用习惯\n");
                ctx.append("- 交互设计要考虑核心玩法的操作流程\n");
                ctx.append("- 视觉风格要与游戏类型匹配\n");
                if (!genre.isEmpty()) {
                    ctx.append(String.format("- %s 类型游戏的客户端重点: %s\n", genre, getClientFocus(genre)));
                }
            }
            case "system-planner", "numerical-planner" -> {
                ctx.append("- 策划案要围绕核心玩法展开\n");
                ctx.append("- 数值设计要考虑目标用户的付费意愿和游戏习惯\n");
                ctx.append("- 系统设计要考虑留存和活跃\n");
                if (!genre.isEmpty()) {
                    ctx.append(String.format("- %s 类型游戏的策划重点: %s\n", genre, getPlannerFocus(genre)));
                }
            }
            case "verification" -> {
                ctx.append("- 验证要覆盖核心玩法的完整性\n");
                ctx.append("- 性能测试要模拟目标用户的使用场景\n");
                ctx.append("- 兼容性测试要考虑目标平台\n");
            }
            default -> {
                ctx.append("- 所有工作都要围绕核心玩法展开\n");
                ctx.append("- 考虑目标用户的体验和需求\n");
            }
        }
        ctx.append("\n");

        return ctx.toString();
    }

    // ===== 3. 质量反馈循环 =====

    /**
     * 构建质量验证 prompt
     * 当 Agent 完成任务时，自动生成验证指令
     *
     * @param projectId 项目 ID
     * @param taskTitle 任务标题
     * @param taskOutput 任务产出内容
     * @param assignedRole 完成任务的角色
     * @return 质量验证 prompt
     */
    public String buildQualityCheckPrompt(String projectId, String taskTitle, String taskOutput, String assignedRole) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 质量验证\n\n");
        prompt.append(String.format("任务「%s」已完成，产出内容如下：\n\n", taskTitle));

        // 截断产出内容
        String output = taskOutput != null ? taskOutput : "无产出";
        if (output.length() > 2000) {
            output = output.substring(0, 2000) + "\n...(已截断)";
        }
        prompt.append(output).append("\n\n");

        prompt.append("请从以下维度验证质量：\n\n");

        switch (assignedRole) {
            case "server-dev", "client-dev", "ui-dev" -> {
                prompt.append("### 代码质量\n");
                prompt.append("1. 功能完整性：是否实现了需求中的所有功能点？\n");
                prompt.append("2. 代码规范：是否有明显的代码质量问题？\n");
                prompt.append("3. 错误处理：是否考虑了异常情况？\n");
                prompt.append("4. 可维护性：代码结构是否清晰？\n\n");
            }
            case "system-planner", "numerical-planner" -> {
                prompt.append("### 策划质量\n");
                prompt.append("1. 完整性：策划案是否覆盖所有需求？\n");
                prompt.append("2. 可行性：技术上是否可实现？\n");
                prompt.append("3. 合理性：数值/系统设计是否合理？\n");
                prompt.append("4. 创新性：是否有亮点设计？\n\n");
            }
            default -> {
                prompt.append("### 通用质量\n");
                prompt.append("1. 完整性：是否完成了所有要求？\n");
                prompt.append("2. 准确性：产出是否正确？\n");
                prompt.append("3. 质量：是否达到可交付标准？\n\n");
            }
        }

        prompt.append("请输出验证结果：\n");
        prompt.append("- **PASS**：质量达标，可以交付\n");
        prompt.append("- **FAIL**：质量不达标，需要返工（请列出具体问题和改进建议）\n");

        return prompt.toString();
    }

    /**
     * 检查质量验证结果是否通过
     *
     * @param verificationResult 验证结果文本
     * @return 是否通过
     */
    public boolean isQualityPassed(String verificationResult) {
        if (verificationResult == null) return false;
        String upper = verificationResult.toUpperCase();
        // 明确的 PASS 且不包含 FAIL
        return upper.contains("PASS") && !upper.contains("FAIL");
    }

    // ===== 4. 跨 Agent 经验共享 =====

    /**
     * 共享 Agent 学到的经验给同项目其他 Agent
     * 当一个 Agent 保存经验时调用
     *
     * @param projectId 项目 ID
     * @param sourceAgentId 源 Agent ID
     * @param key 经验键
     * @param value 经验值
     */
    public void shareExperience(String projectId, String sourceAgentId, String key, String value) {
        if (projectId == null) return;

        // 将经验写入项目的共享知识区
        String sharedKey = "shared_exp_" + sourceAgentId + "_" + key;
        projectBoard.setSharedContext(projectId, sharedKey, value);

        // 也保存到每个同项目 Agent 的记忆中
        List<String> agentIds = projectManager.getProject(projectId) != null ?
            projectManager.getProject(projectId).getAgentIds() : null;
        if (agentIds != null) {
            for (String agentId : agentIds) {
                if (!agentId.equals(sourceAgentId)) {
                    try {
                        memoryManager.saveMemory(
                            projectManager.getProject(projectId),
                            agentId, "shared_experiences", key,
                            String.format("[来自 %s] %s", sourceAgentId, value)
                        );
                    } catch (Exception e) {
                        log.debug("Failed to share experience to agent {}: {}", agentId, e.getMessage());
                    }
                }
            }
        }

        log.info("Shared experience '{}' from {} to project {}", key, sourceAgentId, projectId);
    }

    // ===== 5. 迭代精炼机制 =====

    /**
     * 构建迭代精炼 prompt
     * 当任务产出需要返工时，生成精炼指令
     *
     * @param taskTitle 任务标题
     * @param originalOutput 原始产出
     * @param reviewFeedback 审查反馈
     * @param iterationCount 当前迭代次数
     * @return 精炼 prompt
     */
    public String buildRefinementPrompt(String taskTitle, String originalOutput, String reviewFeedback, int iterationCount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 迭代精炼\n\n");
        prompt.append(String.format("任务「%s」需要返工（第 %d 次迭代）\n\n", taskTitle, iterationCount));

        prompt.append("### 审查反馈\n");
        prompt.append(reviewFeedback).append("\n\n");

        prompt.append("### 原始产出\n");
        String output = originalOutput != null ? originalOutput : "无";
        if (output.length() > 1500) {
            output = output.substring(0, 1500) + "\n...(已截断)";
        }
        prompt.append(output).append("\n\n");

        prompt.append("请根据审查反馈修改产出。要求：\n");
        prompt.append("1. 逐条回应审查反馈中的问题\n");
        prompt.append("2. 保留原始产出中正确的部分\n");
        prompt.append("3. 只修改有问题的部分，不要全盘重写\n");
        if (iterationCount >= 3) {
            prompt.append("4. **已经迭代 3 次了，请特别注意质量，避免再次返工**\n");
        }

        return prompt.toString();
    }

    // ===== 6. 任务依赖传递 =====

    /**
     * 记录任务产出到 ProjectBoard
     * 当 Agent 完成任务时调用，让其他 Agent 可以看到产出
     *
     * @param projectId 项目 ID
     * @param agentId 完成任务的 Agent ID
     * @param taskTitle 任务标题
     * @param taskOutput 任务产出
     */
    public void recordTaskOutput(String projectId, String agentId, String taskTitle, String taskOutput) {
        if (projectId == null) return;

        // 写入 ProjectBoard 的共享上下文
        String outputKey = "task_output_" + taskTitle.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("agentId", agentId);
        outputData.put("taskTitle", taskTitle);
        outputData.put("output", taskOutput != null ? taskOutput : "");
        outputData.put("completedAt", LocalDateTime.now().toString());
        projectBoard.setSharedContext(projectId, outputKey, outputData);

        log.info("Recorded task output for '{}' by agent {} in project {}", taskTitle, agentId, projectId);
    }

    /**
     * 获取任务产出上下文
     * 当 Agent 需要参考其他 Agent 的产出时调用
     *
     * @param projectId 项目 ID
     * @param currentAgentId 当前 Agent ID
     * @return 相关任务产出的 prompt 片段
     */
    public String buildTaskOutputContext(String projectId, String currentAgentId) {
        if (projectId == null) return "";

        ProjectBoard.BoardData board = projectBoard.getBoard(projectId);
        if (board == null) return "";

        StringBuilder ctx = new StringBuilder();
        boolean hasOutput = false;

        // 查找其他 Agent 最近完成的任务产出
        List<ProjectBoard.TaskCard> recentDone = board.taskCards.stream()
            .filter(t -> "DONE".equals(t.status))
            .filter(t -> !currentAgentId.equals(t.assignedAgentId))
            .filter(t -> t.result != null && !t.result.isEmpty())
            .sorted((a, b) -> {
                if (a.completedAt == null) return 1;
                if (b.completedAt == null) return -1;
                return b.completedAt.compareTo(a.completedAt);
            })
            .limit(3)
            .toList();

        if (!recentDone.isEmpty()) {
            ctx.append("## 团队成员的最新产出\n\n");
            ctx.append("以下是其他成员最近完成的工作，请参考以保持一致性：\n\n");
            for (ProjectBoard.TaskCard card : recentDone) {
                ctx.append(String.format("### %s (%s)\n", card.title, card.assignedRole));
                String result = card.result;
                if (result != null && result.length() > 300) {
                    result = result.substring(0, 300) + "...";
                }
                ctx.append(result != null ? result : "无详细产出").append("\n\n");
                hasOutput = true;
            }
        }

        // 也检查共享上下文中的任务产出
        for (Map.Entry<String, Object> entry : board.sharedContext.entrySet()) {
            if (entry.getKey().startsWith("task_output_") && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> outputData = (Map<String, Object>) entry.getValue();
                String agentId = (String) outputData.get("agentId");
                if (!currentAgentId.equals(agentId)) {
                    if (!hasOutput) {
                        ctx.append("## 团队成员的最新产出\n\n");
                        hasOutput = true;
                    }
                    ctx.append(String.format("- **%s** (by %s): %s\n",
                        outputData.get("taskTitle"), agentId,
                        truncate((String) outputData.get("output"), 100)));
                }
            }
        }

        return hasOutput ? ctx.toString() : "";
    }

    // ===== 辅助方法 =====

    private String getServerFocus(String genre) {
        return switch (genre.toLowerCase()) {
            case "rpg", "mmorpg" -> "角色数据模型、装备系统、战斗计算、副本逻辑";
            case "slg", "strategy" -> "地图格子系统、资源计算、战斗回合、联盟逻辑";
            case "moba", "竞技" -> "实时同步、战斗帧同步、匹配系统、排行榜";
            case "casual", "休闲" -> "简洁的数据模型、快速响应、关卡配置";
            case "simulation", "模拟" -> "资源循环系统、时间系统、NPC 行为";
            default -> "核心玩法循环的服务端支持、数据持久化、状态同步";
        };
    }

    private String getClientFocus(String genre) {
        return switch (genre.toLowerCase()) {
            case "rpg", "mmorpg" -> "角色渲染、技能特效、UI 复杂度、背包系统";
            case "slg", "strategy" -> "地图渲染、拖拽操作、战报回放、建筑动画";
            case "moba", "竞技" -> "网络延迟优化、操作响应、技能指示器";
            case "casual", "休闲" -> "简洁 UI、流畅动画、快速反馈";
            case "simulation", "模拟" -> "数据可视化、操作面板、实时更新";
            default -> "核心交互体验、UI 布局、动画流畅度";
        };
    }

    private String getPlannerFocus(String genre) {
        return switch (genre.toLowerCase()) {
            case "rpg", "mmorpg" -> "角色成长曲线、装备数值、副本难度、经济系统";
            case "slg", "strategy" -> "资源产出消耗、科技树、兵种克制、地图设计";
            case "moba", "竞技" -> "英雄平衡、技能设计、地图机制、匹配算法";
            case "casual", "休闲" -> "关卡难度曲线、道具设计、社交分享";
            case "simulation", "模拟" -> "资源循环、目标系统、成就系统";
            default -> "核心循环设计、数值平衡、留存机制";
        };
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
