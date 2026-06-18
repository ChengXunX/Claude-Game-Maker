package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.ContextManager;
import com.chengxun.gamemaker.model.AgentContext;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 检查点服务
 * 负责会话状态的持久化和恢复
 *
 * 主要功能：
 * - 创建检查点：保存当前会话状态到文件
 * - 加载检查点：从文件恢复会话状态
 * - 自动创建：对话消息数超过阈值时自动触发
 * - 清理旧检查点：保留最近 N 个检查点
 *
 * 灵感来源：会话检查点与恢复机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private SystemConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建检查点
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param reason 创建原因
     * @return 检查点 ID（时间戳）
     */
    public String createCheckpoint(String agentId, GameProject project, String reason) {
        if (project == null) {
            log.warn("无法创建检查点：项目为空");
            return null;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        Path checkpointPath = getCheckpointPath(agentId, project, timestamp);

        try {
            Files.createDirectories(checkpointPath.getParent());

            // 加载当前上下文
            AgentContext context = contextManager.loadContext(agentId, project);

            // 加载最近对话
            List<ContextManager.ConversationMessage> recentMessages =
                contextManager.getRecentMessages(agentId, project, 20);

            // 构建检查点数据
            Map<String, Object> checkpoint = new LinkedHashMap<>();
            checkpoint.put("timestamp", timestamp);
            checkpoint.put("agentId", agentId);
            checkpoint.put("projectId", project.getId());
            checkpoint.put("reason", reason);
            checkpoint.put("createdAt", LocalDateTime.now().toString());
            checkpoint.put("context", context);
            checkpoint.put("recentMessages", recentMessages);
            checkpoint.put("messageCount", recentMessages.size());

            // 保存检查点
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(checkpointPath.toFile(), checkpoint);

            log.info("检查点已创建: agent={}, project={}, timestamp={}, reason={}",
                agentId, project.getId(), timestamp, reason);

            // 清理旧检查点
            cleanupOldCheckpoints(agentId, project);

            return timestamp;
        } catch (IOException e) {
            log.error("创建检查点失败: agent={}, project={}", agentId, project.getId(), e);
            return null;
        }
    }

    /**
     * 加载最新检查点
     *
     * @param agentId Agent ID
     * @param project 项目
     * @return 检查点数据，不存在返回 null
     */
    public Map<String, Object> loadLatestCheckpoint(String agentId, GameProject project) {
        if (project == null) return null;

        Path checkpointDir = getCheckpointDir(agentId, project);
        if (!Files.exists(checkpointDir)) {
            return null;
        }

        try {
            Optional<Path> latest = Files.list(checkpointDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .findFirst();

            if (latest.isPresent()) {
                return objectMapper.readValue(latest.get().toFile(), Map.class);
            }
        } catch (IOException e) {
            log.error("加载检查点失败: agent={}, project={}", agentId, project.getId(), e);
        }

        return null;
    }

    /**
     * 加载指定检查点
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param timestamp 检查点时间戳
     * @return 检查点数据
     */
    public Map<String, Object> loadCheckpoint(String agentId, GameProject project, String timestamp) {
        if (project == null || timestamp == null) return null;

        Path checkpointPath = getCheckpointPath(agentId, project, timestamp);
        if (!Files.exists(checkpointPath)) {
            return null;
        }

        try {
            return objectMapper.readValue(checkpointPath.toFile(), Map.class);
        } catch (IOException e) {
            log.error("加载检查点失败: agent={}, project={}, timestamp={}", agentId, project.getId(), timestamp, e);
            return null;
        }
    }

    /**
     * 列出所有检查点
     *
     * @param agentId Agent ID
     * @param project 项目
     * @return 检查点时间戳列表（最新在前）
     */
    public List<String> listCheckpoints(String agentId, GameProject project) {
        if (project == null) return Collections.emptyList();

        Path checkpointDir = getCheckpointDir(agentId, project);
        if (!Files.exists(checkpointDir)) {
            return Collections.emptyList();
        }

        try {
            return Files.list(checkpointDir)
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .sorted(Comparator.reverseOrder())
                .toList();
        } catch (IOException e) {
            log.error("列出检查点失败: agent={}, project={}", agentId, project.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 删除检查点
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param timestamp 检查点时间戳
     * @return 是否删除成功
     */
    public boolean deleteCheckpoint(String agentId, GameProject project, String timestamp) {
        if (project == null || timestamp == null) return false;

        Path checkpointPath = getCheckpointPath(agentId, project, timestamp);
        try {
            return Files.deleteIfExists(checkpointPath);
        } catch (IOException e) {
            log.error("删除检查点失败: agent={}, project={}, timestamp={}", agentId, project.getId(), timestamp, e);
            return false;
        }
    }

    /**
     * 检查是否需要自动创建检查点
     *
     * @param agentId Agent ID
     * @param project 项目
     * @param currentMessageCount 当前消息数
     * @return 是否需要创建
     */
    public boolean shouldAutoCreate(String agentId, GameProject project, int currentMessageCount) {
        int threshold = configService.getInt(SystemConstants.CHECKPOINT_AUTO_CREATE_THRESHOLD, 30);
        if (currentMessageCount % threshold != 0 || currentMessageCount == 0) {
            return false;
        }

        // 检查最近的检查点，避免重复创建
        List<String> checkpoints = listCheckpoints(agentId, project);
        if (!checkpoints.isEmpty()) {
            String latest = checkpoints.get(0);
            // 如果最近的检查点是 5 分钟内创建的，跳过
            try {
                LocalDateTime latestTime = LocalDateTime.parse(latest, TIMESTAMP_FORMAT);
                if (LocalDateTime.now().minusMinutes(5).isBefore(latestTime)) {
                    return false;
                }
            } catch (Exception e) {
                // 解析失败，允许创建
            }
        }

        return true;
    }

    /**
     * 从检查点重建上下文提示
     *
     * @param checkpoint 检查点数据
     * @return 上下文恢复提示
     */
    public String buildRecoveryPrompt(Map<String, Object> checkpoint) {
        if (checkpoint == null) {
            return "## 上下文恢复\n\n没有找到检查点，无法恢复上下文。";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("## 上下文恢复\n\n");
        prompt.append("从检查点恢复工作上下文...\n\n");

        // 基本信息
        prompt.append("### 检查点信息\n");
        prompt.append("- 时间: ").append(checkpoint.get("createdAt")).append("\n");
        prompt.append("- 原因: ").append(checkpoint.get("reason")).append("\n");
        prompt.append("- 消息数: ").append(checkpoint.get("messageCount")).append("\n\n");

        // 上下文信息
        Object contextObj = checkpoint.get("context");
        if (contextObj instanceof Map<?, ?> context) {
            prompt.append("### 工作状态\n");
            Object currentTask = context.get("currentTaskId");
            if (currentTask != null) {
                prompt.append("- 当前任务: ").append(currentTask).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("请基于以上信息恢复工作上下文，并确认你已准备好继续工作。");

        // 截断到最大长度
        int maxLength = configService.getInt(SystemConstants.CONTEXT_RECOVERY_MAX_LENGTH, 3000);
        String result = prompt.toString();
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength) + "\n...(已截断)";
        }

        return result;
    }

    /**
     * 清理旧检查点
     */
    private void cleanupOldCheckpoints(String agentId, GameProject project) {
        int keepCount = configService.getInt(SystemConstants.CHECKPOINT_MAX_KEEP_COUNT, 10);
        Path checkpointDir = getCheckpointDir(agentId, project);

        if (!Files.exists(checkpointDir)) return;

        try {
            List<Path> checkpoints = Files.list(checkpointDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .toList();

            for (int i = keepCount; i < checkpoints.size(); i++) {
                Files.deleteIfExists(checkpoints.get(i));
            }

            if (checkpoints.size() > keepCount) {
                log.info("清理了 {} 个旧检查点: agent={}, project={}",
                    checkpoints.size() - keepCount, agentId, project.getId());
            }
        } catch (IOException e) {
            log.error("清理检查点失败: agent={}, project={}", agentId, project.getId(), e);
        }
    }

    // ===== 路径方法 =====

    private Path getCheckpointDir(String agentId, GameProject project) {
        return Path.of(project.getWorkDir(), ".game-maker", "checkpoints", agentId);
    }

    private Path getCheckpointPath(String agentId, GameProject project, String timestamp) {
        return getCheckpointDir(agentId, project).resolve(timestamp + ".json");
    }
}
