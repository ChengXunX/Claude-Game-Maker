package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.web.entity.AgentLog;
import com.chengxun.gamemaker.web.entity.DismissalRequest;
import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.PerformanceReview;
import com.chengxun.gamemaker.web.entity.ProducerReplacement;
import com.chengxun.gamemaker.web.repository.AgentLogRepository;
import com.chengxun.gamemaker.web.repository.DismissalRequestRepository;
import com.chengxun.gamemaker.web.repository.PerformanceReviewRepository;
import com.chengxun.gamemaker.web.repository.ProducerReplacementRepository;
import com.chengxun.gamemaker.web.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 绩效管理服务
 * 负责绩效打分、警告、解雇申请的完整流程
 *
 * 流程：
 * 1. 制作人给团队成员打分
 * 2. 绩效过低时发出警告
 * 3. 多次警告后发起解雇申请
 * 4. 管理员审批解雇
 * 5. 执行解雇
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class PerformanceManagementService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceManagementService.class);

    /** 低分阈值 */
    private static final int LOW_SCORE_THRESHOLD = 60;

    /** 警告阈值 */
    private static final int WARNING_THRESHOLD = 50;

    /** 连续低分期数触发解雇 */
    private static final int CONSECUTIVE_LOW_PERIODS_FOR_DISMISSAL = 3;

    /** 最大警告次数 */
    private static final int MAX_WARNINGS_BEFORE_DISMISSAL = 3;

    @Autowired
    private PerformanceReviewRepository reviewRepository;

    @Autowired
    private DismissalRequestRepository dismissalRepository;

    @Autowired
    private ProducerReplacementRepository replacementRepository;

    @Autowired
    private AgentLogRepository agentLogRepository;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private NotificationService notificationService;

    // ===== 绩效打分 =====

    /**
     * 制作人给团队成员打分
     *
     * @param producerId 制作人Agent ID
     * @param agentId 被评审Agent ID
     * @param projectId 项目ID
     * @param reviewPeriod 评审周期
     * @param qualityScore 质量评分
     * @param efficiencyScore 效率评分
     * @param collaborationScore 协作评分
     * @param innovationScore 创新评分
     * @param strengths 优点
     * @param improvements 待改进
     * @param comments 评价
     * @param highlights 亮点
     * @return 绩效评审记录
     */
    public PerformanceReview submitReview(String producerId, String agentId, String projectId,
                                           String reviewPeriod, Integer qualityScore, Integer efficiencyScore,
                                           Integer collaborationScore, Integer innovationScore,
                                           String strengths, String improvements, String comments,
                                           String highlights) {
        // 验证制作人身份
        Agent producer = agentManager.getAgent(producerId);
        if (producer == null || !"producer".equals(producer.getRole())) {
            throw new RuntimeException("只有制作人可以进行绩效评审");
        }

        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            throw new RuntimeException("被评审Agent不存在");
        }

        // 检查是否已经评审过
        Optional<PerformanceReview> existing = reviewRepository.findByAgentIdAndReviewPeriod(agentId, reviewPeriod);
        if (existing.isPresent()) {
            throw new RuntimeException("该Agent在本评审周期已经评审过");
        }

        // 生成评审编号
        String reviewNo = generateReviewNo(reviewPeriod);

        // 创建评审记录
        PerformanceReview review = new PerformanceReview();
        review.setReviewNo(reviewNo);
        review.setAgentId(agentId);
        review.setAgentName(agent.getName());
        review.setAgentRole(agent.getRole());
        review.setProducerId(producerId);
        review.setProducerName(producer.getName());
        review.setProjectId(projectId);
        review.setProjectName("项目"); // 可以从项目管理获取
        review.setReviewPeriod(reviewPeriod);
        review.setQualityScore(qualityScore);
        review.setEfficiencyScore(efficiencyScore);
        review.setCollaborationScore(collaborationScore);
        review.setInnovationScore(innovationScore);
        review.setStrengths(strengths);
        review.setImprovements(improvements);
        review.setComments(comments);
        review.setHighlights(highlights);

        // 计算综合评分
        review.calculateOverallScore();

        // 检查是否需要警告
        if (review.needsWarning()) {
            review.setIsWarning(true);
            review.setWarningReason("综合评分低于警告阈值（" + WARNING_THRESHOLD + "分）");
        }

        PerformanceReview saved = reviewRepository.save(review);

        // 记录日志
        logPerformanceReview(saved);

        // 检查是否需要触发解雇流程
        checkAndTriggerDismissalIfNeeded(agentId, producerId, projectId);

        log.info("Performance review submitted: {} for agent {} by producer {}",
            reviewNo, agentId, producerId);

        return saved;
    }

    /**
     * 发出警告
     *
     * @param producerId 制作人Agent ID
     * @param agentId 被警告Agent ID
     * @param reason 警告原因
     */
    public void issueWarning(String producerId, String agentId, String reason) {
        Agent producer = agentManager.getAgent(producerId);
        if (producer == null || !"producer".equals(producer.getRole())) {
            throw new RuntimeException("只有制作人可以发出警告");
        }

        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            throw new RuntimeException("被警告Agent不存在");
        }

        // 记录警告日志
        AgentLog warningLog = new AgentLog();
        warningLog.setAgentId(producerId);
        warningLog.setAgentName(producer.getName());
        warningLog.setAction("WARNING_ISSUED");
        warningLog.setLevel("WARN");
        warningLog.setSummary("对 " + agent.getName() + " 发出警告");
        warningLog.setDetail("警告原因: " + reason);
        warningLog.setProjectId(null);
        agentLogRepository.save(warningLog);

        log.warn("Warning issued to agent {} by producer {}: {}", agentId, producerId, reason);
    }

    /**
     * 检查是否需要触发解雇流程
     */
    private void checkAndTriggerDismissalIfNeeded(String agentId, String producerId, String projectId) {
        // 获取最近的评审记录
        List<PerformanceReview> recentReviews = reviewRepository.findRecentByAgentId(agentId);

        // 计算连续低分期数
        int consecutiveLowPeriods = 0;
        for (PerformanceReview review : recentReviews) {
            if (review.isLowScore()) {
                consecutiveLowPeriods++;
            } else {
                break;
            }
        }

        // 统计警告次数
        Long warningCount = reviewRepository.countWarningsByAgentId(agentId);

        // 检查是否满足解雇条件
        if (consecutiveLowPeriods >= CONSECUTIVE_LOW_PERIODS_FOR_DISMISSAL ||
            warningCount >= MAX_WARNINGS_BEFORE_DISMISSAL) {

            Agent agent = agentManager.getAgent(agentId);
            Agent producer = agentManager.getAgent(producerId);

            if (agent != null && producer != null) {
                // 创建解雇申请
                DismissalRequest request = new DismissalRequest();
                request.setRequestNo(generateDismissalRequestNo());
                request.setAgentId(agentId);
                request.setAgentName(agent.getName());
                request.setAgentRole(agent.getRole());
                request.setIsSystemRole(isSystemRole(agent.getRole()));
                request.setProducerId(producerId);
                request.setProducerName(producer.getName());
                request.setProjectId(projectId);
                request.setProjectName("项目");
                request.setReasonType(DismissalRequest.ReasonType.LOW_PERFORMANCE.name());
                request.setReason("连续" + consecutiveLowPeriods + "期低分，警告" + warningCount + "次");
                request.setWarningCount(warningCount.intValue());
                request.setLastWarningAt(LocalDateTime.now());
                request.setConsecutiveLowScorePeriods(consecutiveLowPeriods);

                DismissalRequest saved = dismissalRepository.save(request);

                // 通知管理员
                notifyAdminForDismissal(saved);

                log.info("Dismissal request created for agent {} by producer {} - consecutive low periods: {}, warnings: {}",
                    agentId, producerId, consecutiveLowPeriods, warningCount);
            }
        }
    }

    /**
     * 制作人主动发起解雇申请
     *
     * @param producerId 制作人Agent ID
     * @param agentId 被解雇Agent ID
     * @param reasonType 原因类型
     * @param reason 原因详情
     * @return 解雇申请
     */
    public DismissalRequest submitDismissalRequest(String producerId, String agentId,
                                                     String reasonType, String reason) {
        Agent producer = agentManager.getAgent(producerId);
        if (producer == null || !"producer".equals(producer.getRole())) {
            throw new RuntimeException("只有制作人可以发起解雇申请");
        }

        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            throw new RuntimeException("被解雇Agent不存在");
        }

        // 检查是否有待审批的申请
        if (dismissalRepository.hasPendingDismissalRequest(agentId)) {
            throw new RuntimeException("该Agent已有待审批的解雇申请");
        }

        // 获取警告次数
        Long warningCount = reviewRepository.countWarningsByAgentId(agentId);

        DismissalRequest request = new DismissalRequest();
        request.setRequestNo(generateDismissalRequestNo());
        request.setAgentId(agentId);
        request.setAgentName(agent.getName());
        request.setAgentRole(agent.getRole());
        request.setIsSystemRole(isSystemRole(agent.getRole()));
        request.setProducerId(producerId);
        request.setProducerName(producer.getName());
        request.setReasonType(reasonType);
        request.setReason(reason);
        request.setWarningCount(warningCount.intValue());

        DismissalRequest saved = dismissalRepository.save(request);

        // 通知管理员
        notifyAdminForDismissal(saved);

        // 记录日志
        logDismissalRequest(saved);

        log.info("Dismissal request submitted: {} for agent {} by producer {}",
            request.getRequestNo(), agentId, producerId);

        return saved;
    }

    // ===== 管理员审批 =====

    /**
     * 管理员批准解雇
     */
    public DismissalRequest approveDismissal(Long requestId, Long adminId, String adminName, String comment) {
        DismissalRequest request = dismissalRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("申请状态不正确");
        }

        request.approve(adminId, adminName, comment);
        DismissalRequest saved = dismissalRepository.save(request);

        log.info("Dismissal request {} approved by admin {}", request.getRequestNo(), adminId);

        return saved;
    }

    /**
     * 管理员驳回解雇
     */
    public DismissalRequest rejectDismissal(Long requestId, Long adminId, String adminName, String reason) {
        DismissalRequest request = dismissalRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("申请状态不正确");
        }

        request.reject(adminId, adminName, reason);
        DismissalRequest saved = dismissalRepository.save(request);

        log.info("Dismissal request {} rejected by admin {}: {}", request.getRequestNo(), adminId, reason);

        return saved;
    }

    /**
     * 执行解雇（管理员批准后）
     */
    public void executeDismissal(Long requestId, String executedBy) {
        DismissalRequest request = dismissalRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!"APPROVED".equals(request.getStatus())) {
            throw new RuntimeException("申请未批准，无法执行");
        }

        // 执行解雇
        agentManager.removeAgent(request.getAgentId());

        request.execute(executedBy);
        dismissalRepository.save(request);

        // 记录日志
        logDismissalExecution(request);

        log.info("Dismissal executed: {} - Agent {} removed", request.getRequestNo(), request.getAgentId());
    }

    // ===== 查询方法 =====

    /**
     * 获取Agent的绩效评审历史
     */
    public List<PerformanceReview> getAgentReviews(String agentId) {
        return reviewRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    /**
     * 获取制作人的评审记录
     */
    public List<PerformanceReview> getProducerReviews(String producerId) {
        return reviewRepository.findByProducerIdOrderByCreatedAtDesc(producerId);
    }

    /**
     * 获取项目的评审记录
     */
    public List<PerformanceReview> getProjectReviews(String projectId) {
        return reviewRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * 获取待审批的解雇申请
     */
    public List<DismissalRequest> getPendingDismissalRequests() {
        return dismissalRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * 获取所有解雇申请
     */
    public List<DismissalRequest> getAllDismissalRequests() {
        return dismissalRepository.findRecentRequests();
    }

    /**
     * 获取解雇申请详情
     */
    public DismissalRequest getDismissalRequest(Long requestId) {
        return dismissalRepository.findById(requestId).orElse(null);
    }

    /**
     * 获取绩效统计
     */
    public Map<String, Object> getPerformanceStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各Agent平均评分
        List<Object[]> agentScores = reviewRepository.getAverageScoresByAgent();
        stats.put("agentScores", agentScores);

        // 各项目平均评分
        List<Object[]> projectScores = reviewRepository.getAverageScoresByProject();
        stats.put("projectScores", projectScores);

        // 解雇申请统计
        List<Object[]> dismissalStats = dismissalRepository.countByStatus();
        Map<String, Long> dismissalMap = new HashMap<>();
        for (Object[] row : dismissalStats) {
            dismissalMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("dismissalStats", dismissalMap);

        return stats;
    }

    /**
     * 获取Agent的警告次数
     */
    public Long getWarningCount(String agentId) {
        return reviewRepository.countWarningsByAgentId(agentId);
    }

    /**
     * 检查是否是系统预设角色
     */
    private boolean isSystemRole(String role) {
        Set<String> systemRoles = Set.of(
            "producer", "server-dev", "client-dev", "ui-dev",
            "system-planner", "numerical-planner", "tester", "git-commit"
        );
        return systemRoles.contains(role);
    }

    // ===== 日志记录 =====

    private void logPerformanceReview(PerformanceReview review) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentId(review.getProducerId());
        logEntry.setAgentName(review.getProducerName());
        logEntry.setAction("PERFORMANCE_REVIEW");
        logEntry.setLevel(review.getIsWarning() ? "WARN" : "INFO");
        logEntry.setSummary("对 " + review.getAgentName() + " 进行绩效评审");
        logEntry.setDetail(String.format(
            "评审周期: %s\n综合评分: %d\n质量: %d, 效率: %d, 协作: %d, 创新: %d\n等级: %s%s",
            review.getReviewPeriod(),
            review.getOverallScore(),
            review.getQualityScore(),
            review.getEfficiencyScore(),
            review.getCollaborationScore(),
            review.getInnovationScore(),
            review.getGrade(),
            review.getIsWarning() ? "\n[警告] 评分过低" : ""
        ));
        logEntry.setProjectId(review.getProjectId());
        agentLogRepository.save(logEntry);
    }

    private void logDismissalRequest(DismissalRequest request) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentId(request.getProducerId());
        logEntry.setAgentName(request.getProducerName());
        logEntry.setAction("DISMISSAL_REQUEST");
        logEntry.setLevel("WARN");
        logEntry.setSummary("发起对 " + request.getAgentName() + " 的解雇申请");
        logEntry.setDetail(String.format(
            "申请编号: %s\n被解雇Agent: %s (%s)\n原因类型: %s\n原因: %s\n警告次数: %d",
            request.getRequestNo(),
            request.getAgentName(),
            request.getAgentRole(),
            request.getReasonTypeDescription(),
            request.getReason(),
            request.getWarningCount()
        ));
        logEntry.setProjectId(request.getProjectId());
        agentLogRepository.save(logEntry);
    }

    private void logDismissalExecution(DismissalRequest request) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentId(request.getExecutedBy());
        logEntry.setAgentName(request.getExecutedBy());
        logEntry.setAction("DISMISSAL_EXECUTED");
        logEntry.setLevel("ERROR");
        logEntry.setSummary("执行解雇 " + request.getAgentName());
        logEntry.setDetail(String.format(
            "申请编号: %s\n被解雇Agent: %s (%s)\n执行人: %s\n审批人: %s",
            request.getRequestNo(),
            request.getAgentName(),
            request.getAgentRole(),
            request.getExecutedBy(),
            request.getApproverName()
        ));
        logEntry.setProjectId(request.getProjectId());
        agentLogRepository.save(logEntry);
    }

    // ===== 通知 =====

    private void notifyAdminForDismissal(DismissalRequest request) {
        String title = "解雇申请待审批: " + request.getRequestNo();
        String content = String.format(
            "制作人 %s 发起了解雇申请\n\n" +
            "被解雇Agent: %s (%s)\n" +
            "原因类型: %s\n" +
            "原因: %s\n" +
            "警告次数: %d\n\n" +
            "请前往解雇审批页面进行审批。",
            request.getProducerName(),
            request.getAgentName(),
            request.getAgentRole(),
            request.getReasonTypeDescription(),
            request.getReason(),
            request.getWarningCount()
        );

        notificationService.sendSystemNotification(
            null,
            title,
            content,
            Notification.NotificationType.SYSTEM
        );
    }

    // ===== 管理员解雇制作人（更换制作人） =====

    /**
     * 管理员解雇制作人（实际上是更换制作人）
     *
     * 说明：
     * - 解雇制作人不是删除制作人角色
     * - 而是"重置"或"替换"制作人
     * - 新制作人会继承项目，但可能有不同的规范
     * - 保留原制作人的历史记录
     *
     * @param adminId 管理员用户ID
     * @param adminName 管理员名称
     * @param producerId 被解雇的制作人Agent ID
     * @param projectId 项目ID
     * @param reasonType 原因类型
     * @param reason 原因详情
     * @param newGuidelines 新规范/文档说明
     * @return 更换记录
     */
    public ProducerReplacement replaceProducer(Long adminId, String adminName, String producerId,
                                                 String projectId, String reasonType, String reason,
                                                 String newGuidelines) {
        Agent producer = agentManager.getAgent(producerId);
        if (producer == null || !"producer".equals(producer.getRole())) {
            throw new RuntimeException("只能更换制作人角色的Agent");
        }

        // 保存原制作人的历史信息
        String oldHistory = buildProducerHistory(producerId);

        // 创建更换记录
        ProducerReplacement replacement = new ProducerReplacement();
        replacement.setReplacementNo(generateReplacementNo());
        replacement.setOldProducerId(producerId);
        replacement.setOldProducerName(producer.getName());
        replacement.setOldProducerCreatedAt(LocalDateTime.now().minusDays(30)); // 示例
        replacement.setOldProducerHistory(oldHistory);
        replacement.setProjectId(projectId);
        replacement.setProjectName("项目"); // 从项目管理获取
        replacement.setReasonType(reasonType);
        replacement.setReason(reason);
        replacement.setNewGuidelines(newGuidelines);
        replacement.setAdminId(adminId);
        replacement.setAdminName(adminName);
        replacement.setExecutedAt(LocalDateTime.now());

        ProducerReplacement saved = replacementRepository.save(replacement);

        // 记录日志
        logProducerReplacement(saved);

        // 创建新的制作人Agent
        Agent newProducer = createNewProducer(producer, projectId, newGuidelines);

        // 更新更换记录
        saved.setNewProducerId(newProducer.getId());
        saved.setNewProducerName(newProducer.getName());
        replacementRepository.save(saved);

        log.info("Producer replaced: {} -> {} by admin {}", producerId, newProducer.getId(), adminId);

        return saved;
    }

    /**
     * 创建新的制作人Agent
     */
    private Agent createNewProducer(Agent oldProducer, String projectId, String newGuidelines) {
        // 生成新的制作人ID
        String newId = "producer-" + UUID.randomUUID().toString().substring(0, 8);

        // 创建新的制作人定义
        com.chengxun.gamemaker.model.AgentDefinition definition = com.chengxun.gamemaker.model.AgentDefinition.builder()
            .id(newId)
            .name("制作人")
            .role("producer")
            .description("项目制作人，负责团队管理和项目协调")
            .workDir(oldProducer.getDefinition().getWorkDir())
            .tag("project_id", projectId)
            .tag("replaced_from", oldProducer.getId())
            .tag("replaced_at", String.valueOf(System.currentTimeMillis()))
            .build();

        // 添加新规范到知识库
        if (newGuidelines != null && !newGuidelines.isEmpty()) {
            definition.setTag("guidelines", newGuidelines);
        }

        Agent newProducer = agentManager.createAgent(definition);

        log.info("New producer created: {} for project {}", newId, projectId);

        return newProducer;
    }

    /**
     * 构建制作人历史摘要
     */
    private String buildProducerHistory(String producerId) {
        StringBuilder history = new StringBuilder();

        // 获取制作人的评审记录
        List<PerformanceReview> reviews = reviewRepository.findByProducerIdOrderByCreatedAtDesc(producerId);
        if (!reviews.isEmpty()) {
            history.append("评审记录:\n");
            for (PerformanceReview review : reviews) {
                history.append(String.format("- %s: %d分 (%s)\n",
                    review.getReviewPeriod(), review.getOverallScore(), review.getGrade()));
            }
        }

        // 获取制作人发起的解雇记录
        List<DismissalRequest> dismissals = dismissalRepository.findByProducerIdOrderByCreatedAtDesc(producerId);
        if (!dismissals.isEmpty()) {
            history.append("\n发起的解雇:\n");
            for (DismissalRequest dismissal : dismissals) {
                history.append(String.format("- %s: %s (%s)\n",
                    dismissal.getAgentName(), dismissal.getReasonTypeDescription(), dismissal.getStatus()));
            }
        }

        return history.toString();
    }

    /**
     * 获取制作人更换历史
     */
    public List<ProducerReplacement> getProducerReplacementHistory(String projectId) {
        return replacementRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * 获取所有制作人更换记录
     */
    public List<ProducerReplacement> getAllProducerReplacements() {
        return replacementRepository.findRecentReplacements();
    }

    /**
     * 记录制作人更换日志
     */
    private void logProducerReplacement(ProducerReplacement replacement) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentId(replacement.getAdminName());
        logEntry.setAgentName(replacement.getAdminName());
        logEntry.setAction("PRODUCER_REPLACED");
        logEntry.setLevel("ERROR");
        logEntry.setSummary("更换制作人: " + replacement.getOldProducerName());
        logEntry.setDetail(String.format(
            "更换编号: %s\n原制作人: %s (%s)\n项目: %s\n原因类型: %s\n原因: %s\n新规范: %s",
            replacement.getReplacementNo(),
            replacement.getOldProducerName(),
            replacement.getOldProducerId(),
            replacement.getProjectName(),
            replacement.getReasonTypeDescription(),
            replacement.getReason(),
            replacement.getNewGuidelines() != null ? replacement.getNewGuidelines() : "无"
        ));
        logEntry.setProjectId(replacement.getProjectId());
        agentLogRepository.save(logEntry);
    }

    // ===== 工具方法 =====

    private String generateReviewNo(String reviewPeriod) {
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PR-" + reviewPeriod + "-" + random;
    }

    private String generateDismissalRequestNo() {
        String date = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "DIS-" + date + "-" + random;
    }

    private String generateReplacementNo() {
        String date = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "REP-" + date + "-" + random;
    }
}
