package com.chengxun.gamemaker.agent;

import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.engine.MessageBus;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.manager.MemoryManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.manager.SkillManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.model.Skill;
import com.chengxun.gamemaker.model.TaskAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * UI/美术开发Agent
 * 负责游戏界面设计、CSS样式、响应式布局等
 *
 * 特殊能力：
 * - 如果API支持图片生成，可以生成图标和图片资源
 * - 如果API不支持图片生成，专注于HTML/CSS/SVG实现
 * - 支持预制资源管理和引用
 *
 * @author chengxun
 * @since 1.0.0
 */
public class UiDevAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(UiDevAgent.class);

    /** 是否支持图片生成 */
    private final boolean supportsImageGeneration;

    public UiDevAgent(AgentDefinition definition,
                      ClaudeCliEngine cliEngine,
                      MessageBus messageBus,
                      ContextManager contextManager,
                      MemoryManager memoryManager,
                      SkillManager skillManager,
                      ProjectManager projectManager) {
        super(definition, cliEngine, messageBus, contextManager, memoryManager, skillManager, projectManager);
        this.supportsImageGeneration = definition.isSupportsImageGeneration();
    }

    @Override
    protected void doWork() {
        // UI Agent的主要工作循环
        if (shouldCompactContext()) {
            compactContext();
        }
    }

    @Override
    protected void handleMessage(AgentMessage message) {
        switch (message.getType()) {
            case TASK -> handleTaskMessage(message);
            case REVIEW -> handleReviewMessage(message);
            case QUERY -> handleQueryMessage(message);
            default -> log.debug("UI Agent received message: {}", message.getType());
        }
    }

    /**
     * 处理任务消息
     */
    private void handleTaskMessage(AgentMessage message) {
        String content = message.getContent();
        log.info("UI Agent processing task: {}", content.length() > 50 ? content.substring(0, 50) + "..." : content);

        // 根据是否支持图片生成，调整任务处理方式
        if (!supportsImageGeneration && requiresImageGeneration(content)) {
            // 不支持图片生成时，提供替代方案
            String alternativePrompt = buildAlternativePrompt(content);
            String response = sendMessage(alternativePrompt);
            sendResponse(message.getFromAgentId(), response);
        } else {
            // 正常处理
            String response = sendMessage(content);
            sendResponse(message.getFromAgentId(), response);
        }
    }

    /**
     * 检查任务是否需要图片生成
     */
    private boolean requiresImageGeneration(String content) {
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("生成图片") ||
               lowerContent.contains("生成图标") ||
               lowerContent.contains("生成logo") ||
               lowerContent.contains("generate image") ||
               lowerContent.contains("generate icon") ||
               lowerContent.contains("create graphic");
    }

    /**
     * 构建替代方案提示
     */
    private String buildAlternativePrompt(String originalPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 任务说明\n\n");
        sb.append(originalPrompt);
        sb.append("\n\n## 重要提示\n\n");
        sb.append("当前API不支持图片生成，请使用以下替代方案：\n");
        sb.append("1. 使用SVG矢量图形代替位图\n");
        sb.append("2. 使用CSS渐变和阴影效果\n");
        sb.append("3. 引用预制的图片资源（如有）\n");
        sb.append("4. 使用图标字体（如Font Awesome）\n");
        sb.append("5. 提供详细的图片规格说明，供美术人员手动制作\n\n");
        sb.append("请基于以上约束，提供最佳的UI实现方案。");
        return sb.toString();
    }

    /**
     * 处理审查消息
     */
    private void handleReviewMessage(AgentMessage message) {
        log.info("UI Agent handling review request");
        // UI相关的审查重点关注：
        // 1. 响应式设计
        // 2. 浏览器兼容性
        // 3. 可访问性
        // 4. 性能优化
        String reviewPrompt = buildUiReviewPrompt(message.getContent());
        String response = sendMessage(reviewPrompt);
        sendResponse(message.getFromAgentId(), response);
    }

    /**
     * 构建UI审查提示
     */
    private String buildUiReviewPrompt(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("## UI代码审查\n\n");
        sb.append("请从以下角度审查UI代码：\n\n");
        sb.append("### 1. 响应式设计\n");
        sb.append("- 是否支持移动端\n");
        sb.append("- 断点设置是否合理\n");
        sb.append("- 布局是否灵活\n\n");
        sb.append("### 2. 浏览器兼容性\n");
        sb.append("- CSS属性兼容性\n");
        sb.append("- JavaScript API兼容性\n");
        sb.append("- 降级方案\n\n");
        sb.append("### 3. 可访问性\n");
        sb.append("- 语义化HTML\n");
        sb.append("- ARIA标签\n");
        sb.append("- 键盘导航\n\n");
        sb.append("### 4. 性能\n");
        sb.append("- 图片优化\n");
        sb.append("- CSS/JS压缩\n");
        sb.append("- 懒加载\n\n");
        sb.append("### 审查内容\n\n");
        sb.append(content);
        return sb.toString();
    }

    /**
     * 处理查询消息
     */
    private void handleQueryMessage(AgentMessage message) {
        log.info("UI Agent handling query");
        String response = sendMessage(message.getContent());
        sendResponse(message.getFromAgentId(), response);
    }

    /**
     * 发送响应消息
     */
    private void sendResponse(String targetAgentId, String content) {
        AgentMessage response = AgentMessage.builder()
            .fromAgentId(getId())
            .toAgentId(targetAgentId)
            .type(AgentMessage.MessageType.RESPONSE)
            .content(content)
            .build();
        sendMessage(response);
    }

    @Override
    public void assignTask(TaskAssignment task) {
        super.assignTask(task);

        // 根据任务类型设置合适的推理深度
        String taskTitle = task.getTitle().toLowerCase();
        if (taskTitle.contains("设计") || taskTitle.contains("design")) {
            // 设计任务需要更深入的思考
            definition.setReasoningDepth(4);
        } else if (taskTitle.contains("修改") || taskTitle.contains("fix")) {
            // 修改任务可以快速处理
            definition.setReasoningDepth(2);
        }
    }

    /**
     * 获取UI Agent的能力描述
     */
    public String getCapabilityDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("## UI Agent 能力\n\n");
        sb.append("### 支持的功能\n");
        sb.append("- HTML/CSS界面开发\n");
        sb.append("- 响应式布局设计\n");
        sb.append("- CSS动画效果\n");
        sb.append("- SVG矢量图形\n");

        if (supportsImageGeneration) {
            sb.append("- 图片资源生成\n");
            sb.append("- 图标设计\n");
        }

        sb.append("\n### 不支持的功能\n");
        if (!supportsImageGeneration) {
            sb.append("- 位图图片生成（使用SVG或预制资源替代）\n");
        }
        sb.append("- 3D建模\n");
        sb.append("- 视频编辑\n");

        sb.append("\n### 支持的文件类型\n");
        for (String type : definition.getSupportedFileTypes()) {
            sb.append("- ").append(type).append("\n");
        }

        return sb.toString();
    }
}
