package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.AppConfig;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
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

        if (!"ACTIVE".equals(discussion.getStatus())) {
            throw new RuntimeException("只能对进行中的讨论生成会议纪要");
        }

        // 获取所有对话消息
        List<ProjectDiscussionMessage> messages = messageRepository.findByDiscussionIdOrderByCreatedAtAsc(discussionId);
        if (messages.isEmpty()) {
            throw new RuntimeException("对话内容为空，无法生成会议纪要");
        }

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

        // 调用AI生成会议纪要
        String prompt = dialogue.toString() + "\n\n---\n\n" +
            "请根据以上对话内容，生成一份结构化的会议纪要。格式要求：\n\n" +
            "# 会议纪要\n\n" +
            "## 讨论主题\n" +
            "简要说明本次讨论的核心议题\n\n" +
            "## 关键决策\n" +
            "列出讨论中达成的共识和决策（编号列表）\n\n" +
            "## 项目方向调整\n" +
            "如果有方向性调整，明确列出需要调整的内容\n\n" +
            "## 待办事项\n" +
            "列出需要制作人执行的具体行动项（编号列表）\n\n" +
            "## 风险与注意事项\n" +
            "列出讨论中提到的风险点\n\n" +
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
