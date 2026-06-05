package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentMessage;
import com.chengxun.gamemaker.web.dto.PageRequest;
import com.chengxun.gamemaker.web.entity.AgentIntervention;
import com.chengxun.gamemaker.web.entity.AgentLog;
import com.chengxun.gamemaker.web.repository.AgentInterventionRepository;
import com.chengxun.gamemaker.web.repository.AgentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent干预服务
 * 负责人工对Agent决策和方向的干预
 *
 * 主要功能：
 * - 发送干预指令
 * - 覆盖Agent决策
 * - 调整工作方向
 * - 暂停/恢复Agent
 * - 取消干预
 * - 记录干预历史
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AgentInterventionService {

    private static final Logger log = LoggerFactory.getLogger(AgentInterventionService.class);

    private final AgentInterventionRepository interventionRepository;
    private final AgentLogRepository agentLogRepository;
    private final AgentManager agentManager;

    /**
     * 构造函数注入依赖
     *
     * @param interventionRepository 干预记录仓库
     * @param agentLogRepository Agent日志仓库
     * @param agentManager Agent管理器
     */
    public AgentInterventionService(AgentInterventionRepository interventionRepository,
                                     AgentLogRepository agentLogRepository,
                                     AgentManager agentManager) {
        this.interventionRepository = interventionRepository;
        this.agentLogRepository = agentLogRepository;
        this.agentManager = agentManager;
    }

    // ===== 干预指令 =====

    /**
     * 发送干预指令
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param userRole 用户角色
     * @param agentId Agent ID
     * @param interventionType 干预类型
     * @param instruction 指令内容
     * @param reason 干预原因
     * @param taskId 相关任务ID（可选）
     * @return 干预记录
     */
    @Transactional
    public AgentIntervention sendIntervention(Long userId, String username, String userRole,
                                               String agentId, AgentIntervention.InterventionType interventionType,
                                               String instruction, String reason, String taskId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            throw new RuntimeException("Agent不存在: " + agentId);
        }

        // 生成干预编号（带唯一性检查）
        String interventionNo = generateUniqueInterventionNo();

        // 创建干预记录
        AgentIntervention intervention = new AgentIntervention();
        intervention.setInterventionNo(interventionNo);
        intervention.setAgentId(agentId);
        intervention.setAgentName(agent.getName() != null ? agent.getName() : agentId);
        intervention.setAgentRole(agent.getRole() != null ? agent.getRole() : "unknown");
        intervention.setUserId(userId);
        intervention.setUsername(username);
        intervention.setUserRole(userRole);
        intervention.setInterventionType(interventionType);
        intervention.setInstruction(instruction);
        intervention.setReason(reason);
        intervention.setTaskId(taskId);
        intervention.setStatus(AgentIntervention.Status.PENDING);

        AgentIntervention saved = interventionRepository.save(intervention);

        // 发送消息给Agent
        sendInterventionToAgent(agent, saved);

        // 记录日志
        logIntervention(saved);

        log.info("Intervention sent to agent {}: {} - {}", agentId, interventionType, instruction);

        return saved;
    }

    /**
     * 发送指令干预
     */
    @Transactional
    public AgentIntervention sendInstruction(Long userId, String username, String userRole,
                                              String agentId, String instruction, String reason) {
        return sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.INSTRUCTION, instruction, reason, null);
    }

    /**
     * 覆盖Agent决策
     */
    @Transactional
    public AgentIntervention overrideDecision(Long userId, String username, String userRole,
                                               String agentId, String originalDecision,
                                               String newDecision, String reason) {
        AgentIntervention intervention = sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.DECISION_OVERRIDE, newDecision, reason, null);

        intervention.setOriginalDecision(originalDecision);
        intervention.setNewDecision(newDecision);
        interventionRepository.save(intervention);

        return intervention;
    }

    /**
     * 调整工作方向
     */
    @Transactional
    public AgentIntervention changeDirection(Long userId, String username, String userRole,
                                              String agentId, String newDirection, String reason) {
        return sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.DIRECTION_CHANGE, newDirection, reason, null);
    }

    /**
     * 暂停Agent工作
     * 先创建干预记录，再暂停Agent；如果Agent不存在则抛出异常
     * 这样可以确保干预记录和Agent状态的一致性
     */
    @Transactional
    public AgentIntervention pauseAgent(Long userId, String username, String userRole,
                                         String agentId, String reason) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            throw new RuntimeException("Agent不存在: " + agentId);
        }

        // 先创建干预记录
        AgentIntervention intervention = sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.PAUSE, "暂停工作", reason, null);

        // 再暂停Agent（如果失败，干预记录会保留，但Agent状态可能不一致）
        try {
            agent.stop();
        } catch (Exception e) {
            log.error("Failed to pause agent {} after creating intervention {}", agentId, intervention.getInterventionNo(), e);
            // 标记干预为执行失败
            intervention.setExecutionResult("暂停失败: " + e.getMessage());
            intervention.reject("暂停Agent失败: " + e.getMessage());
            interventionRepository.save(intervention);
            throw new RuntimeException("暂停Agent失败: " + e.getMessage(), e);
        }

        return intervention;
    }

    /**
     * 恢复Agent工作
     * 先创建干预记录，再恢复Agent；如果Agent不存在则抛出异常
     * 这样可以确保干预记录和Agent状态的一致性
     */
    @Transactional
    public AgentIntervention resumeAgent(Long userId, String username, String userRole,
                                          String agentId, String reason) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            throw new RuntimeException("Agent不存在: " + agentId);
        }

        // 先创建干预记录
        AgentIntervention intervention = sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.RESUME, "恢复工作", reason, null);

        // 再恢复Agent（如果失败，干预记录会保留，但Agent状态可能不一致）
        try {
            agent.start();
        } catch (Exception e) {
            log.error("Failed to resume agent {} after creating intervention {}", agentId, intervention.getInterventionNo(), e);
            // 标记干预为执行失败
            intervention.setExecutionResult("恢复失败: " + e.getMessage());
            intervention.reject("恢复Agent失败: " + e.getMessage());
            interventionRepository.save(intervention);
            throw new RuntimeException("恢复Agent失败: " + e.getMessage(), e);
        }

        return intervention;
    }

    /**
     * 取消任务
     */
    @Transactional
    public AgentIntervention cancelTask(Long userId, String username, String userRole,
                                         String agentId, String taskId, String reason) {
        return sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.TASK_CANCEL, "取消任务: " + taskId, reason, taskId);
    }

    /**
     * 发送紧急指令
     */
    @Transactional
    public AgentIntervention sendUrgentInstruction(Long userId, String username, String userRole,
                                                    String agentId, String instruction, String reason) {
        return sendIntervention(userId, username, userRole, agentId,
            AgentIntervention.InterventionType.URGENT_INSTRUCTION, instruction, reason, null);
    }

    // ===== Agent处理干预 =====

    /**
     * Agent确认干预
     */
    @Transactional
    public AgentIntervention acknowledgeIntervention(Long interventionId, String acknowledgement) {
        try {
            AgentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new RuntimeException("干预记录不存在"));

            intervention.acknowledge(acknowledgement);
            AgentIntervention saved = interventionRepository.save(intervention);

            log.info("Intervention {} acknowledged by agent {}", intervention.getInterventionNo(), intervention.getAgentId());

            return saved;
        } catch (OptimisticLockingFailureException e) {
            log.warn("并发冲突：干预 {} 正在被其他用户处理", interventionId);
            throw new RuntimeException("该干预正在被其他用户处理，请刷新后重试", e);
        }
    }

    /**
     * Agent执行干预
     * 状态转换：PENDING/ACKNOWLEDGED -> EXECUTING -> COMPLETED
     */
    @Transactional
    public AgentIntervention executeIntervention(Long interventionId, String result) {
        try {
            AgentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new RuntimeException("干预记录不存在"));

            intervention.startExecution();
            // flush中间状态，使EXECUTING状态持久化
            interventionRepository.flush();
            intervention.complete(result);
            AgentIntervention saved = interventionRepository.save(intervention);

            log.info("Intervention {} executed by agent {}", intervention.getInterventionNo(), intervention.getAgentId());

            return saved;
        } catch (OptimisticLockingFailureException e) {
            log.warn("并发冲突：干预 {} 正在被其他用户处理", interventionId);
            throw new RuntimeException("该干预正在被其他用户处理，请刷新后重试", e);
        } catch (IllegalStateException e) {
            log.warn("干预 {} 状态转换失败: {}", interventionId, e.getMessage());
            throw new RuntimeException("干预状态不允许此操作: " + e.getMessage(), e);
        }
    }

    /**
     * Agent拒绝干预
     */
    @Transactional
    public AgentIntervention rejectIntervention(Long interventionId, String reason) {
        try {
            AgentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new RuntimeException("干预记录不存在"));

            intervention.reject(reason);
            AgentIntervention saved = interventionRepository.save(intervention);

            log.info("Intervention {} rejected by agent {}: {}", intervention.getInterventionNo(), intervention.getAgentId(), reason);

            return saved;
        } catch (OptimisticLockingFailureException e) {
            log.warn("并发冲突：干预 {} 正在被其他用户处理", interventionId);
            throw new RuntimeException("该干预正在被其他用户处理，请刷新后重试", e);
        }
    }

    /**
     * 取消干预
     * 只有待处理状态的干预才能取消
     *
     * @param interventionId 干预ID
     * @param reason 取消原因
     * @return 取消后的干预记录
     */
    @Transactional
    public AgentIntervention cancelIntervention(Long interventionId, String reason) {
        try {
            AgentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new RuntimeException("干预记录不存在"));

            intervention.cancel();
            if (reason != null && !reason.isEmpty()) {
                intervention.setExecutionResult("取消原因: " + reason);
            }
            AgentIntervention saved = interventionRepository.save(intervention);

            log.info("Intervention {} cancelled: {}", intervention.getInterventionNo(), reason);

            return saved;
        } catch (OptimisticLockingFailureException e) {
            log.warn("并发冲突：干预 {} 正在被其他用户处理", interventionId);
            throw new RuntimeException("该干预正在被其他用户处理，请刷新后重试", e);
        }
    }

    // ===== 查询方法 =====

    /**
     * 获取干预记录
     */
    @Transactional(readOnly = true)
    public AgentIntervention getIntervention(Long interventionId) {
        return interventionRepository.findById(interventionId).orElse(null);
    }

    /**
     * 获取Agent的干预历史
     */
    @Transactional(readOnly = true)
    public List<AgentIntervention> getAgentInterventions(String agentId) {
        return interventionRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    /**
     * 获取Agent待处理的干预
     */
    @Transactional(readOnly = true)
    public List<AgentIntervention> getPendingInterventions(String agentId) {
        return interventionRepository.findByAgentIdAndStatusOrderByCreatedAtDesc(agentId, AgentIntervention.Status.PENDING);
    }

    /**
     * 获取所有待处理的干预
     */
    @Transactional(readOnly = true)
    public List<AgentIntervention> getAllPendingInterventions() {
        return interventionRepository.findByStatusOrderByCreatedAtDesc(AgentIntervention.Status.PENDING);
    }

    /**
     * 获取所有干预记录（分页）
     *
     * @param pageRequest 分页参数
     * @return 分页后的干预记录
     */
    @Transactional(readOnly = true)
    public Page<AgentIntervention> getAllInterventions(PageRequest pageRequest) {
        return interventionRepository.findAll(pageRequest.toPageRequest());
    }

    /**
     * 获取用户的干预记录
     */
    @Transactional(readOnly = true)
    public List<AgentIntervention> getUserInterventions(Long userId) {
        return interventionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取干预统计
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getInterventionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Long> statusMap = new HashMap<>();

        try {
            // 各状态统计（enum类型转为字符串）
            List<Object[]> statusCounts = interventionRepository.countByStatus();
            for (Object[] row : statusCounts) {
                try {
                    String key = row[0] instanceof AgentIntervention.Status ? ((AgentIntervention.Status) row[0]).name() : row[0].toString();
                    Long count = row[1] instanceof Long ? (Long) row[1] : Long.valueOf(row[1].toString());
                    statusMap.put(key, count);
                } catch (Exception e) {
                    log.warn("Failed to parse status count row: {}", Arrays.toString(row), e);
                }
            }
            stats.put("statusCounts", statusMap);

            // 各类型统计（enum类型转为字符串）
            List<Object[]> typeCounts = interventionRepository.countByType();
            Map<String, Long> typeMap = new HashMap<>();
            for (Object[] row : typeCounts) {
                try {
                    String key = row[0] instanceof AgentIntervention.InterventionType ? ((AgentIntervention.InterventionType) row[0]).name() : row[0].toString();
                    Long count = row[1] instanceof Long ? (Long) row[1] : Long.valueOf(row[1].toString());
                    typeMap.put(key, count);
                } catch (Exception e) {
                    log.warn("Failed to parse type count row: {}", Arrays.toString(row), e);
                }
            }
            stats.put("typeCounts", typeMap);

            // 各Agent统计
            List<Object[]> agentCounts = interventionRepository.countByAgent();
            stats.put("agentCounts", agentCounts);
        } catch (Exception e) {
            log.error("Failed to get intervention statistics", e);
            // 返回空统计而不是抛出异常
            stats.put("statusCounts", new HashMap<>());
            stats.put("typeCounts", new HashMap<>());
            stats.put("agentCounts", new ArrayList<>());
        }

        // 待处理数量
        stats.put("pendingCount", statusMap.getOrDefault("PENDING", 0L));

        return stats;
    }

    // ===== 内部方法 =====

    /**
     * 发送干预消息给Agent
     */
    private void sendInterventionToAgent(Agent agent, AgentIntervention intervention) {
        if (agent instanceof BaseAgent baseAgent) {
            // 构建干预消息
            String message = buildInterventionMessage(intervention);

            // 发送消息给Agent
            AgentMessage agentMessage = AgentMessage.builder()
                .fromAgentId("system")
                .toAgentId(intervention.getAgentId())
                .type(AgentMessage.MessageType.SYSTEM)
                .content(message)
                .build();

            baseAgent.receiveMessage(agentMessage);

            log.debug("Intervention message sent to agent {}", intervention.getAgentId());
        }
    }

    /**
     * 构建干预消息
     */
    private String buildInterventionMessage(AgentIntervention intervention) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 人工干预通知\n\n");
        sb.append("**干预编号**: ").append(intervention.getInterventionNo()).append("\n");
        sb.append("**干预类型**: ").append(intervention.getInterventionTypeDescription()).append("\n");
        sb.append("**干预人**: ").append(intervention.getUsername()).append(" (").append(intervention.getUserRole()).append(")\n\n");

        if (intervention.getInstruction() != null) {
            sb.append("**指令内容**:\n").append(intervention.getInstruction()).append("\n\n");
        }

        if (intervention.getReason() != null) {
            sb.append("**干预原因**:\n").append(intervention.getReason()).append("\n\n");
        }

        if (intervention.getOriginalDecision() != null) {
            sb.append("**原决策**:\n").append(intervention.getOriginalDecision()).append("\n\n");
            sb.append("**新决策**:\n").append(intervention.getNewDecision()).append("\n\n");
        }

        sb.append("请确认并执行此干预指令。");

        return sb.toString();
    }

    /**
     * 记录干预日志
     */
    private void logIntervention(AgentIntervention intervention) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentId(intervention.getAgentId());
        logEntry.setAgentName(intervention.getAgentName());
        logEntry.setAction("INTERVENTION_SENT");
        logEntry.setLevel("WARN");
        logEntry.setSummary("对 " + intervention.getAgentName() + " 发送干预指令（操作人: " + intervention.getUsername() + "）");
        logEntry.setDetail(String.format(
            "干预编号: %s\n干预类型: %s\n指令内容: %s\n干预原因: %s\n操作人: %s (%s)",
            intervention.getInterventionNo(),
            intervention.getInterventionTypeDescription(),
            intervention.getInstruction(),
            intervention.getReason(),
            intervention.getUsername(),
            intervention.getUserRole()
        ));
        logEntry.setProjectId(intervention.getProjectId());
        agentLogRepository.save(logEntry);
    }

    /**
     * 生成唯一的干预编号
     * 格式: INT-YYYYMMDD-XXXXXX，如果冲突则重新生成
     */
    private String generateUniqueInterventionNo() {
        for (int i = 0; i < 5; i++) {
            String date = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
            String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String no = "INT-" + date + "-" + random;
            if (!interventionRepository.findByInterventionNo(no).isPresent()) {
                return no;
            }
        }
        // 极端情况：使用更长的随机串
        String date = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return "INT-" + date + "-" + random;
    }
}
