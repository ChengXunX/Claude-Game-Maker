package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ProjectDiscussion;
import com.chengxun.gamemaker.web.entity.ProjectDiscussionMessage;
import com.chengxun.gamemaker.web.repository.ProjectDiscussionMessageRepository;
import com.chengxun.gamemaker.web.repository.ProjectDiscussionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目讨论服务
 * 管理项目讨论会话、消息、会议纪要生成和同步
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
@Transactional
public class ProjectDiscussionService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDiscussionService.class);

    @Autowired
    private ProjectDiscussionRepository discussionRepository;

    @Autowired
    private ProjectDiscussionMessageRepository messageRepository;

    @Autowired
    private ClaudeCliEngine cliEngine;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ProjectManager projectManager;

    /** 时间格式化 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== 会话管理 =====

    /**
     * 创建项目讨论会话
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @param username  用户名
     * @param title     会话标题（可选）
     * @return 创建的讨论会话
     */
    public ProjectDiscussion createDiscussion(String projectId, Long userId, String username, String title) {
        ProjectDiscussion discussion = new ProjectDiscussion();
        discussion.setProjectId(projectId);
        discussion.setUserId(userId);
        discussion.setUsername(username);
        discussion.setTitle(title != null ? title : "项目讨论 - " + LocalDateTime.now().format(TIME_FMT));
        discussion.setStatus("ACTIVE");

        ProjectDiscussion saved = discussionRepository.save(discussion);
        log.info("创建项目讨论: projectId={}, userId={}, id={}", projectId, userId, saved.getId());
        return saved;
    }

    /**
     * 获取项目的讨论列表
     */
    public List<ProjectDiscussion> getDiscussionsByProject(String projectId) {
        return discussionRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
    }

    /**
     * 获取讨论详情（含消息）
     */
    public ProjectDiscussion getDiscussion(Long discussionId) {
        return discussionRepository.findById(discussionId).orElse(null);
    }

    /**
     * 获取讨论的消息列表
     */
    public List<ProjectDiscussionMessage> getMessages(Long discussionId) {
        return messageRepository.findByDiscussionIdOrderByCreatedAtAsc(discussionId);
    }

    /**
     * 删除讨论会话
     */
    public void deleteDiscussion(Long discussionId) {
        discussionRepository.deleteById(discussionId);
        log.info("删除项目讨论: id={}", discussionId);
    }

    /**
     * 结束讨论会话
     * 将状态从 ACTIVE 改为 CLOSED，结束后只读
     */
    public void closeDiscussion(Long discussionId) {
        ProjectDiscussion discussion = discussionRepository.findById(discussionId)
            .orElseThrow(() -> new RuntimeException("讨论会话不存在"));
        discussion.setStatus("CLOSED");
        discussionRepository.save(discussion);
        log.info("结束项目讨论: id={}", discussionId);
    }

    // ===== 消息管理 =====

    /**
     * 添加用户消息
     */
    public ProjectDiscussionMessage addUserMessage(Long discussionId, String content, String sender) {
        ProjectDiscussionMessage msg = new ProjectDiscussionMessage();
        msg.setDiscussionId(discussionId);
        msg.setRole("user");
        msg.setContent(content);
        msg.setSender(sender);
        return messageRepository.save(msg);
    }

    /**
     * 添加AI回复消息
     */
    public ProjectDiscussionMessage addAssistantMessage(Long discussionId, String content) {
        ProjectDiscussionMessage msg = new ProjectDiscussionMessage();
        msg.setDiscussionId(discussionId);
        msg.setRole("assistant");
        msg.setContent(content);
        return messageRepository.save(msg);
    }

    /**
     * 添加系统消息（如会议纪要生成通知）
     */
    public ProjectDiscussionMessage addSystemMessage(Long discussionId, String content) {
        ProjectDiscussionMessage msg = new ProjectDiscussionMessage();
        msg.setDiscussionId(discussionId);
        msg.setRole("system");
        msg.setContent(content);
        return messageRepository.save(msg);
    }

    // ===== 会议纪要 =====

    /**
     * 生成会议纪要
     * 将对话历史发送给AI，生成结构化的会议纪要
     *
     * @param discussionId 讨论会话ID
     * @return 生成的会议纪要文本
     */
    public String generateMeetingMinutes(Long discussionId) {
        ProjectDiscussion discussion = discussionRepository.findById(discussionId)
            .orElseThrow(() -> new RuntimeException("讨论会话不存在"));

        if ("MINUTES_GENERATED".equals(discussion.getStatus())) {
            throw new RuntimeException("该讨论已生成过会议纪要");
        }

        // 获取所有对话消息
        List<ProjectDiscussionMessage> messages = messageRepository.findByDiscussionIdOrderByCreatedAtAsc(discussionId);
        if (messages.isEmpty()) {
            throw new RuntimeException("对话内容为空，无法生成会议纪要");
        }

        // 【强化】构建项目上下文信息
        String projectContext = buildProjectContext(discussion.getProjectId());

        // 构建对话历史文本
        StringBuilder dialogue = new StringBuilder();
        dialogue.append("# 项目讨论对话记录\n\n");
        dialogue.append("**项目**: ").append(discussion.getProjectId()).append("\n");
        dialogue.append("**时间**: ").append(discussion.getCreatedAt().format(TIME_FMT))
            .append(" ~ ").append(LocalDateTime.now().format(TIME_FMT)).append("\n");
        dialogue.append("**参与人**: ").append(discussion.getUsername()).append("\n\n");
        dialogue.append("## 对话内容\n\n");

        for (ProjectDiscussionMessage msg : messages) {
            if ("user".equals(msg.getRole())) {
                dialogue.append("**").append(msg.getSender() != null ? msg.getSender() : "用户").append("**: ");
                dialogue.append(msg.getContent()).append("\n\n");
            } else if ("assistant".equals(msg.getRole())) {
                dialogue.append("**AI助手**: ");
                dialogue.append(msg.getContent()).append("\n\n");
            }
        }

        // 【强化】调用AI生成会议纪要，添加项目上下文和角色定位
        String prompt = "你是这个游戏项目的专业分析员。你对项目的目标、进度、代码结构、玩法设计都有深入了解。\n\n" +
            "## 项目背景\n\n" + projectContext + "\n\n---\n\n" +
            dialogue.toString() + "\n\n---\n\n" +
            "请根据以上项目背景和对话内容，生成一份结构化的会议纪要。\n" +
            "你需要站在项目分析员的角度，结合项目实际情况，给出专业、具体、可执行的建议。\n\n" +
            "# 会议纪要\n\n" +
            "## 讨论主题\n" +
            "简要说明本次讨论的核心议题，结合项目当前阶段分析\n\n" +
            "## 关键决策\n" +
            "列出讨论中达成的共识和决策（编号列表）\n\n" +
            "## 项目方向调整\n" +
            "如果有方向性调整，明确列出需要调整的内容，并说明对项目目标的影响\n\n" +
            "## 待办事项\n" +
            "列出需要制作人执行的具体行动项（编号列表），每项要具体、可执行\n\n" +
            "## 风险与注意事项\n" +
            "列出讨论中提到的风险点，以及基于项目现状的潜在风险\n\n" +
            "## 分析员建议\n" +
            "基于项目当前进度和讨论内容，给出你作为项目分析员的专业建议\n\n" +
            "请直接输出纪要内容，不要有多余的解释。";

        try {
            String minutes = cliEngine.sendMessage(
                "meeting-minutes-" + discussionId,
                null,
                prompt,
                null,
                appConfig.getApiKey(),
                appConfig.getApiUrl(),
                appConfig.getModel()
            );

            // 保存纪要
            discussion.setMeetingMinutes(minutes);
            discussion.setStatus("MINUTES_GENERATED");
            discussionRepository.save(discussion);

            // 添加系统消息
            addSystemMessage(discussionId, "会议纪要已生成，将自动同步给项目制作人。");

            log.info("会议纪要已生成: discussionId={}", discussionId);
            return minutes;
        } catch (Exception e) {
            log.error("生成会议纪要失败: discussionId={}", discussionId, e);
            throw new RuntimeException("生成会议纪要失败: " + e.getMessage());
        }
    }

    /**
     * 构建项目上下文信息
     * 包含项目目标、里程碑进度、团队成员等，让 AI 了解项目全貌
     *
     * @param projectId 项目ID
     * @return 项目上下文文本
     */
    private String buildProjectContext(String projectId) {
        StringBuilder ctx = new StringBuilder();
        try {
            GameProject project = projectManager.getProject(projectId);
            if (project == null) {
                ctx.append("- 项目ID: ").append(projectId).append("\n");
                return ctx.toString();
            }

            // 项目基本信息
            ctx.append("- 项目名称: ").append(project.getName()).append("\n");
            ctx.append("- 项目描述: ").append(project.getDescription() != null ? project.getDescription() : "未设置").append("\n");
            ctx.append("- 当前版本: ").append(project.getVersion()).append("\n");
            ctx.append("- 运行状态: ").append(project.isRunning() ? "运行中" : "未运行").append("\n");

            // 项目目标
            if (project.hasGoal()) {
                ctx.append("\n### 项目目标\n");
                ctx.append("- 目标: ").append(project.getGoal()).append("\n");
                ctx.append("- 目标状态: ").append(project.getGoalStatus()).append("\n");
                ctx.append("- 目标进度: ").append(project.getGoalProgress()).append("%\n");

                // 里程碑进度
                if (project.getMilestones() != null && !project.getMilestones().isEmpty()) {
                    ctx.append("\n### 里程碑进度\n");
                    int completed = 0;
                    int total = project.getMilestones().size();
                    for (var m : project.getMilestones()) {
                        String status = switch (m.getStatus()) {
                            case COMPLETED -> "✅";
                            case IN_PROGRESS -> "🔄";
                            case BLOCKED -> "🚫";
                            default -> "⏳";
                        };
                        ctx.append("- ").append(status).append(" ").append(m.getTitle());
                        if (m.getAssignedAgentRole() != null) {
                            ctx.append(" (").append(m.getAssignedAgentRole()).append(")");
                        }
                        ctx.append("\n");
                        if (m.getStatus() == GameProject.MilestoneStatus.COMPLETED) {
                            completed++;
                        }
                    }
                    ctx.append("- 完成率: ").append(completed).append("/").append(total);
                    ctx.append(" (").append(total > 0 ? completed * 100 / total : 0).append("%)\n");
                }
            }

            // 工作目录
            ctx.append("\n### 工作目录\n");
            ctx.append("- ").append(project.getWorkDir()).append("\n");

        } catch (Exception e) {
            log.debug("获取项目上下文失败: {}", e.getMessage());
            ctx.append("- 项目ID: ").append(projectId).append("\n");
        }
        return ctx.toString();
    }

    /**
     * 获取待同步的会议纪要（供制作人轮询）
     * 返回 projectId → 会议纪要 的映射
     */
    public Map<String, String> getPendingMinutesForProducer(String projectId) {
        return discussionRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
            .filter(d -> "MINUTES_GENERATED".equals(d.getStatus()) && !d.isSyncedToProducer())
            .filter(d -> d.getMeetingMinutes() != null && !d.getMeetingMinutes().isEmpty())
            .collect(Collectors.toMap(
                d -> String.valueOf(d.getId()),
                d -> d.getMeetingMinutes(),
                (a, b) -> a + "\n\n---\n\n" + b
            ));
    }

    /**
     * 获取项目所有已生成的会议纪要（含未同步和已同步）
     */
    public List<ProjectDiscussion> getMinutesByProject(String projectId) {
        return discussionRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
            .filter(d -> d.getMeetingMinutes() != null && !d.getMeetingMinutes().isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * 标记纪要已同步给制作人
     */
    public void markSynced(Long discussionId) {
        ProjectDiscussion discussion = discussionRepository.findById(discussionId).orElse(null);
        if (discussion != null) {
            discussion.setSyncedToProducer(true);
            discussion.setSyncedAt(LocalDateTime.now());
            discussionRepository.save(discussion);
            log.info("纪要已同步: discussionId={}", discussionId);
        }
    }

    /**
     * 获取项目最新的未同步会议纪要（供制作人读取）
     */
    public String getLatestUnsyncedMinutes(String projectId) {
        return discussionRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
            .filter(d -> "MINUTES_GENERATED".equals(d.getStatus()) && !d.isSyncedToProducer())
            .filter(d -> d.getMeetingMinutes() != null)
            .findFirst()
            .map(d -> {
                // 标记为已同步
                markSynced(d.getId());
                return d.getMeetingMinutes();
            })
            .orElse(null);
    }
}
