package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 子代理服务
 * 支持按需创建临时子 Agent 处理具体任务
 *
 * 主要功能：
 * - 创建子 Agent：继承父 Agent 的项目上下文
 * - 终止子 Agent：任务完成后清理
 * - 列出子 Agent：查看活跃的子 Agent
 * - 生命周期管理：超时自动清理
 *
 * 灵感来源：子代理协作系统
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class SubAgentService {

    private static final Logger log = LoggerFactory.getLogger(SubAgentService.class);

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private SystemConfigService configService;

    /** 子代理注册表：key=subAgentId, value=子代理信息 */
    private final ConcurrentHashMap<String, SubAgentInfo> subAgents = new ConcurrentHashMap<>();

    /**
     * 创建子代理
     *
     * @param parentAgentId 父代理 ID
     * @param projectId 项目 ID
     * @param workDir 工作目录
     * @param taskDescription 任务描述
     * @param role 子代理角色（可选，默认使用通用角色）
     * @return 子代理信息
     */
    public SubAgentInfo spawnSubAgent(String parentAgentId, String projectId, String workDir,
                                       String taskDescription, String role) {
        // 检查并发限制
        int maxConcurrent = configService.getInt("subagent.max-concurrent", 5);
        long activeCount = subAgents.values().stream()
            .filter(s -> s.parentAgentId().equals(parentAgentId) && s.status() == SubAgentStatus.RUNNING)
            .count();

        if (activeCount >= maxConcurrent) {
            log.warn("父代理 {} 的子代理数量已达上限: {}", parentAgentId, maxConcurrent);
            return null;
        }

        // 生成子代理 ID
        String subAgentId = parentAgentId + "-sub-" + System.currentTimeMillis();

        // 确定角色
        if (role == null || role.isEmpty()) {
            role = "general"; // 通用角色
        }

        // 创建子代理定义
        AgentDefinition definition = AgentDefinition.builder()
            .id(subAgentId)
            .name("子代理-" + subAgentId.substring(subAgentId.length() - 6))
            .role(role)
            .description("临时子代理: " + taskDescription)
            .workDir(workDir)
            .projectId(projectId)
            .parentId(parentAgentId)
            .build();

        try {
            // 创建子代理
            Agent agent = agentManager.createAgent(definition);

            // 注册子代理
            SubAgentInfo info = new SubAgentInfo(
                subAgentId,
                parentAgentId,
                projectId,
                role,
                taskDescription,
                SubAgentStatus.RUNNING,
                LocalDateTime.now(),
                null,
                agent
            );
            subAgents.put(subAgentId, info);

            log.info("子代理已创建: id={}, parent={}, task={}", subAgentId, parentAgentId, taskDescription);

            // 启动子代理
            if (agent instanceof BaseAgent baseAgent) {
                baseAgent.start();
                // 发送任务
                baseAgent.sendMessage(taskDescription);
            }

            return info;
        } catch (Exception e) {
            log.error("创建子代理失败: parent={}", parentAgentId, e);
            return null;
        }
    }

    /**
     * 终止子代理
     *
     * @param subAgentId 子代理 ID
     * @return 是否成功终止
     */
    public boolean terminateSubAgent(String subAgentId) {
        SubAgentInfo info = subAgents.get(subAgentId);
        if (info == null) {
            return false;
        }

        try {
            // 停止代理
            if (info.agent() instanceof BaseAgent baseAgent) {
                baseAgent.stop();
            }

            // 更新状态
            SubAgentInfo updated = new SubAgentInfo(
                info.subAgentId(),
                info.parentAgentId(),
                info.projectId(),
                info.role(),
                info.taskDescription(),
                SubAgentStatus.TERMINATED,
                info.createdAt(),
                LocalDateTime.now(),
                info.agent()
            );
            subAgents.put(subAgentId, updated);

            log.info("子代理已终止: id={}", subAgentId);
            return true;
        } catch (Exception e) {
            log.error("终止子代理失败: id={}", subAgentId, e);
            return false;
        }
    }

    /**
     * 获取子代理信息
     *
     * @param subAgentId 子代理 ID
     * @return 子代理信息
     */
    public SubAgentInfo getSubAgent(String subAgentId) {
        return subAgents.get(subAgentId);
    }

    /**
     * 列出父代理的所有子代理
     *
     * @param parentAgentId 父代理 ID
     * @return 子代理列表
     */
    public List<SubAgentInfo> listSubAgents(String parentAgentId) {
        return subAgents.values().stream()
            .filter(s -> s.parentAgentId().equals(parentAgentId))
            .sorted(Comparator.comparing(SubAgentInfo::createdAt).reversed())
            .toList();
    }

    /**
     * 列出项目的所有子代理
     *
     * @param projectId 项目 ID
     * @return 子代理列表
     */
    public List<SubAgentInfo> listByProject(String projectId) {
        return subAgents.values().stream()
            .filter(s -> s.projectId().equals(projectId))
            .sorted(Comparator.comparing(SubAgentInfo::createdAt).reversed())
            .toList();
    }

    /**
     * 清理过期子代理
     * 由定时任务调用
     */
    public void cleanupExpired() {
        int timeoutMinutes = configService.getInt("subagent.timeout-minutes", 30);
        boolean autoCleanup = configService.getBoolean("subagent.auto-cleanup", true);

        if (!autoCleanup) return;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        subAgents.entrySet().removeIf(entry -> {
            SubAgentInfo info = entry.getValue();
            if (info.status() == SubAgentStatus.RUNNING && info.createdAt().isBefore(cutoff)) {
                log.warn("子代理超时，自动终止: id={}, createdAt={}", info.subAgentId(), info.createdAt());
                terminateSubAgent(info.subAgentId());
                return true;
            }
            if (info.status() == SubAgentStatus.TERMINATED && info.terminatedAt() != null
                && info.terminatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                return true; // 清理已终止超过 1 小时的记录
            }
            return false;
        });
    }

    /**
     * 获取子代理统计
     */
    public Map<String, Object> getStats() {
        long running = subAgents.values().stream()
            .filter(s -> s.status() == SubAgentStatus.RUNNING)
            .count();
        long terminated = subAgents.values().stream()
            .filter(s -> s.status() == SubAgentStatus.TERMINATED)
            .count();

        return Map.of(
            "total", subAgents.size(),
            "running", running,
            "terminated", terminated
        );
    }

    // ===== 内部类 =====

    /**
     * 子代理信息
     */
    public record SubAgentInfo(
        String subAgentId,
        String parentAgentId,
        String projectId,
        String role,
        String taskDescription,
        SubAgentStatus status,
        LocalDateTime createdAt,
        LocalDateTime terminatedAt,
        Agent agent
    ) {}

    /**
     * 子代理状态
     */
    public enum SubAgentStatus {
        RUNNING,
        TERMINATED,
        FAILED
    }
}
