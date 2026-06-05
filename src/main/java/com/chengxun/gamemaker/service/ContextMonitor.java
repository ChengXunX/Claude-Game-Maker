package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.engine.ClaudeCliEngine;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文监控服务
 * 定时检查所有 Agent 的上下文有效性，自动恢复失效的上下文
 *
 * 上下文失效判定条件：
 * 1. CLI 进程死亡（ClaudeCliEngine 中进程不可用）
 * 2. 会话过期（sessionId 对应的会话已失效）
 * 3. 长时间无响应（超过指定时间没有收到 Claude 响应）
 * 4. 内存异常（AgentContext 损坏或丢失）
 * 5. 消息积压（pendingMessages 队列过大）
 *
 * 失效处理策略：
 * - 轻度失效（会话过期）: 自动注入恢复 prompt
 * - 中度失效（进程死亡）: 重建进程 + 恢复上下文
 * - 重度失效（多次恢复失败）: 停止 Agent + 通知管理员
 *
 * @author chengxun
 * @since 2.0.0
 */
@Service
public class ContextMonitor {

    private static final Logger log = LoggerFactory.getLogger(ContextMonitor.class);

    /** 最大消息积压数量 */
    private static final int MAX_MESSAGE_BACKLOG = 50;

    /** 最大无响应时间（分钟） */
    private static final int MAX_IDLE_MINUTES = 30;

    /** 最大恢复尝试次数 */
    private static final int MAX_RECOVERY_ATTEMPTS = 3;

    private final AgentManager agentManager;
    private final ClaudeCliEngine cliEngine;
    private final ContextManager contextManager;

    /**
     * Agent 健康状态缓存
     * key: agentId
     * value: 健康状态信息
     */
    private final ConcurrentHashMap<String, ContextHealthStatus> healthStatusMap = new ConcurrentHashMap<>();

    /**
     * 恢复尝试次数
     * key: agentId
     * value: 连续恢复失败次数
     */
    private final ConcurrentHashMap<String, Integer> recoveryAttempts = new ConcurrentHashMap<>();

    /**
     * 上次活动时间
     * key: agentId
     * value: 上次收到响应的时间
     */
    private final ConcurrentHashMap<String, LocalDateTime> lastActivityTime = new ConcurrentHashMap<>();

    public ContextMonitor(AgentManager agentManager, ClaudeCliEngine cliEngine, ContextManager contextManager) {
        this.agentManager = agentManager;
        this.cliEngine = cliEngine;
        this.contextManager = contextManager;
    }

    /**
     * 上下文健康状态
     */
    public static class ContextHealthStatus {
        private final String agentId;
        private boolean healthy;
        private String issue;
        private Severity severity;
        private LocalDateTime lastCheck;
        private LocalDateTime lastRecovery;

        public enum Severity {
            NORMAL,   // 正常
            WARNING,  // 警告（轻度失效）
            ERROR,    // 错误（中度失效）
            CRITICAL  // 严重（重度失效）
        }

        public ContextHealthStatus(String agentId) {
            this.agentId = agentId;
            this.healthy = true;
            this.severity = Severity.NORMAL;
            this.lastCheck = LocalDateTime.now();
        }

        public void markHealthy() {
            this.healthy = true;
            this.issue = null;
            this.severity = Severity.NORMAL;
            this.lastCheck = LocalDateTime.now();
        }

        public void markUnhealthy(String issue, Severity severity) {
            this.healthy = false;
            this.issue = issue;
            this.severity = severity;
            this.lastCheck = LocalDateTime.now();
        }

        public void markRecoveryAttempted() {
            this.lastRecovery = LocalDateTime.now();
        }

        // Getters
        public String getAgentId() { return agentId; }
        public boolean isHealthy() { return healthy; }
        public String getIssue() { return issue; }
        public Severity getSeverity() { return severity; }
        public LocalDateTime getLastCheck() { return lastCheck; }
        public LocalDateTime getLastRecovery() { return lastRecovery; }
    }

    // ===== 定时检查 =====

    /**
     * 每 2 分钟检查所有 Agent 的上下文健康状态
     */
    @Scheduled(fixedRate = 120000)
    public void checkAllAgentsContext() {
        List<Agent> agents = agentManager.getAllAgents();
        if (agents.isEmpty()) return;

        int healthyCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        for (Agent agent : agents) {
            if (!agent.isAlive()) continue;

            ContextHealthStatus status = checkAgentContext(agent);
            healthStatusMap.put(agent.getId(), status);

            if (status.isHealthy()) {
                healthyCount++;
            } else {
                switch (status.getSeverity()) {
                    case WARNING -> warningCount++;
                    case ERROR, CRITICAL -> errorCount++;
                    default -> {}
                }
                handleContextFailure(agent, status);
            }
        }

        if (warningCount > 0 || errorCount > 0) {
            log.info("Context health check: {} healthy, {} warnings, {} errors (total {} agents)",
                healthyCount, warningCount, errorCount, agents.size());
        }
    }

    /**
     * 检查单个 Agent 的上下文健康状态
     */
    public ContextHealthStatus checkAgentContext(Agent agent) {
        ContextHealthStatus status = new ContextHealthStatus(agent.getId());

        // 1. 检查 CLI 进程（CLI是按需创建的，没有进程是正常的空闲状态）
        if (!checkCLIProcess(agent.getId())) {
            // 检查是否有待处理的消息需要CLI处理
            if (!agent.getPendingMessages().isEmpty()) {
                status.markUnhealthy("CLI 进程不可用且有待处理消息", ContextHealthStatus.Severity.WARNING);
                return status;
            }
            // 没有待处理消息，Agent处于空闲状态，这是正常的
            status.markHealthy();
            return status;
        }

        // 2. 检查消息积压
        if (agent.getPendingMessages().size() > MAX_MESSAGE_BACKLOG) {
            status.markUnhealthy("消息积压: " + agent.getPendingMessages().size() + " 条",
                ContextHealthStatus.Severity.WARNING);
            return status;
        }

        // 3. 检查长时间无响应
        LocalDateTime lastActivity = lastActivityTime.get(agent.getId());
        if (lastActivity != null && lastActivity.plusMinutes(MAX_IDLE_MINUTES).isBefore(LocalDateTime.now())) {
            status.markUnhealthy("长时间无响应: " + MAX_IDLE_MINUTES + " 分钟",
                ContextHealthStatus.Severity.WARNING);
            return status;
        }

        // 4. 检查 AgentContext 完整性
        if (agent instanceof BaseAgent ba) {
            if (ba.getAgentContext() == null) {
                status.markUnhealthy("AgentContext 为空", ContextHealthStatus.Severity.ERROR);
                return status;
            }
        }

        status.markHealthy();
        return status;
    }

    /**
     * 检查 CLI 进程是否可用
     */
    private boolean checkCLIProcess(String agentId) {
        try {
            return cliEngine.isProcessAlive(agentId);
        } catch (Exception e) {
            log.warn("Failed to check CLI process for agent {}: {}", agentId, e.getMessage());
            return false;
        }
    }

    // ===== 失效处理 =====

    /**
     * 处理上下文失效
     */
    private void handleContextFailure(Agent agent, ContextHealthStatus status) {
        String agentId = agent.getId();
        int attempts = recoveryAttempts.getOrDefault(agentId, 0);

        log.warn("Context failure detected for agent {}: {} (severity={}, attempts={})",
            agentId, status.getIssue(), status.getSeverity(), attempts);

        // 检查是否超过最大恢复次数
        if (attempts >= MAX_RECOVERY_ATTEMPTS) {
            log.error("Agent {} exceeded max recovery attempts ({}), stopping agent",
                agentId, MAX_RECOVERY_ATTEMPTS);
            handleCriticalFailure(agent, status);
            return;
        }

        // 根据严重程度选择恢复策略
        switch (status.getSeverity()) {
            case WARNING -> {
                // 轻度失效：注入恢复 prompt
                injectContextRecovery(agent, status);
                recoveryAttempts.put(agentId, attempts + 1);
            }
            case ERROR -> {
                // 中度失效：重建进程 + 恢复上下文
                rebuildAndRecover(agent, status);
                recoveryAttempts.put(agentId, attempts + 1);
            }
            case CRITICAL -> {
                // 重度失效：停止 Agent + 通知管理员
                handleCriticalFailure(agent, status);
            }
            default -> {}
        }

        status.markRecoveryAttempted();
    }

    /**
     * 注入上下文恢复 prompt（轻度失效）
     */
    private void injectContextRecovery(Agent agent, ContextHealthStatus status) {
        log.info("Injecting context recovery prompt for agent: {}", agent.getId());

        String recoveryPrompt = String.format(
            "## 上下文恢复通知\n\n" +
            "检测到你的工作上下文出现异常: %s\n\n" +
            "请执行以下恢复步骤:\n" +
            "1. 重新确认你的身份和角色\n" +
            "2. 重新加载项目信息\n" +
            "3. 检查当前任务状态\n" +
            "4. 如有未完成的任务，请继续执行\n\n" +
            "恢复后请输出确认信息。",
            status.getIssue()
        );

        try {
            agent.sendMessage(recoveryPrompt);
        } catch (Exception e) {
            log.error("Failed to inject recovery prompt for agent {}", agent.getId(), e);
        }
    }

    /**
     * 重建进程并恢复上下文（中度失效）
     */
    private void rebuildAndRecover(Agent agent, ContextHealthStatus status) {
        log.info("Rebuilding context for agent: {}", agent.getId());

        try {
            // 如果是 BaseAgent，调用其恢复方法
            if (agent instanceof BaseAgent ba) {
                String recoveryResult = ba.recoverContext();
                log.info("Context recovery result for agent {}: {}", agent.getId(),
                    recoveryResult.length() > 200 ? recoveryResult.substring(0, 200) + "..." : recoveryResult);

                // 恢复成功，重置尝试次数
                recoveryAttempts.remove(agent.getId());
            }
        } catch (Exception e) {
            log.error("Failed to rebuild context for agent {}", agent.getId(), e);
        }
    }

    /**
     * 处理重度失效（停止 Agent + 通知管理员）
     */
    private void handleCriticalFailure(Agent agent, ContextHealthStatus status) {
        log.error("Critical context failure for agent {}: {}", agent.getId(), status.getIssue());

        // 停止 Agent
        try {
            agent.stop();
            log.info("Agent {} stopped due to critical context failure", agent.getId());
        } catch (Exception e) {
            log.error("Failed to stop agent {}", agent.getId(), e);
        }

        // 重置恢复尝试次数
        recoveryAttempts.remove(agent.getId());

        // TODO: 通知管理员（通过飞书、邮件等渠道）
        log.error("ADMIN ALERT: Agent {} stopped due to critical context failure: {}",
            agent.getId(), status.getIssue());
    }

    // ===== 活动时间跟踪 =====

    /**
     * 更新 Agent 的活动时间（由 Agent 在收到 Claude 响应时调用）
     */
    public void updateActivityTime(String agentId) {
        lastActivityTime.put(agentId, LocalDateTime.now());
        // 活动正常，重置恢复尝试次数
        recoveryAttempts.remove(agentId);
    }

    // ===== 状态查询 =====

    /**
     * 获取所有 Agent 的健康状态
     */
    public Map<String, ContextHealthStatus> getAllHealthStatus() {
        return Collections.unmodifiableMap(healthStatusMap);
    }

    /**
     * 获取指定 Agent 的健康状态
     */
    public ContextHealthStatus getHealthStatus(String agentId) {
        return healthStatusMap.get(agentId);
    }

    /**
     * 获取健康统计
     */
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();
        int healthy = 0, warning = 0, error = 0, critical = 0;

        for (ContextHealthStatus status : healthStatusMap.values()) {
            if (status.isHealthy()) {
                healthy++;
            } else {
                switch (status.getSeverity()) {
                    case WARNING -> warning++;
                    case ERROR -> error++;
                    case CRITICAL -> critical++;
                    default -> {}
                }
            }
        }

        summary.put("total", healthStatusMap.size());
        summary.put("healthy", healthy);
        summary.put("warning", warning);
        summary.put("error", error);
        summary.put("critical", critical);
        summary.put("lastCheck", LocalDateTime.now());
        return summary;
    }

    /**
     * 手动触发恢复
     */
    public boolean manualRecover(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            log.warn("Agent not found for manual recovery: {}", agentId);
            return false;
        }

        log.info("Manual recovery triggered for agent: {}", agentId);
        recoveryAttempts.remove(agentId);

        if (agent instanceof BaseAgent ba) {
            try {
                ba.recoverContext();
                return true;
            } catch (Exception e) {
                log.error("Manual recovery failed for agent {}", agentId, e);
                return false;
            }
        }
        return false;
    }
}
