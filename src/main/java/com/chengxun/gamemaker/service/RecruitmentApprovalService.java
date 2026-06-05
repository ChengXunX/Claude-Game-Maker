package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.RecruitmentRequest;
import com.chengxun.gamemaker.web.repository.RecruitmentRequestRepository;
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
 * 招聘审批服务
 * 管理招聘申请的完整审批流程
 *
 * 流程：
 * 1. 制作人发起招聘申请
 * 2. 系统通知管理员
 * 3. 管理员审批（同意/驳回）
 * 4. 同意后制作人执行招聘
 * 5. 驳回后制作人可重新申请或取消
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class RecruitmentApprovalService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentApprovalService.class);

    @Autowired
    private RecruitmentRequestRepository requestRepository;

    @Autowired
    private AgentRecruitmentService recruitmentService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 制作人发起招聘申请
     *
     * @param producerId 制作人Agent ID
     * @param roleName 角色名称
     * @param employeeName 员工姓名
     * @param description 描述
     * @param capabilities 能力列表
     * @param workDir 工作目录
     * @param reason 招聘原因
     * @return 创建的申请
     */
    public RecruitmentRequest submitRequest(String producerId, String roleName, String employeeName,
                                             String description, String capabilities, String workDir,
                                             String reason) {
        // 验证制作人身份
        if (!recruitmentService.isCoreAgent(producerId)) {
            throw new RuntimeException("只有制作人Agent可以发起招聘申请");
        }

        // 生成申请编号
        String requestNo = generateRequestNo();

        // 创建申请
        RecruitmentRequest request = new RecruitmentRequest();
        request.setRequestNo(requestNo);
        request.setProducerId(producerId);
        request.setProducerName("制作人"); // 可以从Agent获取
        request.setRole(roleName);
        request.setRoleName(getRoleDisplayName(roleName));
        request.setEmployeeName(employeeName);
        request.setDescription(description);
        request.setCapabilities(capabilities);
        request.setWorkDir(workDir);
        request.setReason(reason);
        request.setIsCustomRole(!isPresetRole(roleName));
        request.setStatus(RecruitmentRequest.Status.PENDING.name());

        RecruitmentRequest saved = requestRepository.save(request);

        // 通知管理员
        notifyAdmin(saved);

        log.info("Recruitment request submitted: {} by producer {}", requestNo, producerId);

        return saved;
    }

    /**
     * 完整招聘申请（包含更多信息）
     */
    public RecruitmentRequest submitFullRequest(String producerId, String roleName, String roleNameDisplay,
                                                  String employeeName, String description, String capabilities,
                                                  String supportedFileTypes, String workDir, String reason) {
        // 验证制作人身份
        if (!recruitmentService.isCoreAgent(producerId)) {
            throw new RuntimeException("只有制作人Agent可以发起招聘申请");
        }

        // 生成申请编号
        String requestNo = generateRequestNo();

        // 创建申请
        RecruitmentRequest request = new RecruitmentRequest();
        request.setRequestNo(requestNo);
        request.setProducerId(producerId);
        request.setProducerName("制作人");
        request.setRole(roleName);
        request.setRoleName(roleNameDisplay != null ? roleNameDisplay : getRoleDisplayName(roleName));
        request.setEmployeeName(employeeName);
        request.setDescription(description);
        request.setCapabilities(capabilities);
        request.setSupportedFileTypes(supportedFileTypes);
        request.setWorkDir(workDir);
        request.setReason(reason);
        request.setIsCustomRole(!isPresetRole(roleName));
        request.setStatus(RecruitmentRequest.Status.PENDING.name());

        RecruitmentRequest saved = requestRepository.save(request);

        // 通知管理员
        notifyAdmin(saved);

        log.info("Full recruitment request submitted: {} by producer {}", requestNo, producerId);

        return saved;
    }

    /**
     * 管理员批准申请
     *
     * @param requestId 申请ID
     * @param adminId 管理员用户ID
     * @param adminName 管理员名称
     * @param comment 审批意见
     * @return 更新的申请
     */
    public RecruitmentRequest approveRequest(Long requestId, Long adminId, String adminName, String comment) {
        RecruitmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("申请状态不正确，无法审批");
        }

        request.approve(adminId, adminName, comment);
        RecruitmentRequest saved = requestRepository.save(request);

        // 通知制作人已批准
        notifyProducerApproved(saved);

        log.info("Recruitment request {} approved by admin {}", request.getRequestNo(), adminId);

        return saved;
    }

    /**
     * 管理员驳回申请
     *
     * @param requestId 申请ID
     * @param adminId 管理员用户ID
     * @param adminName 管理员名称
     * @param reason 驳回原因
     * @return 更新的申请
     */
    public RecruitmentRequest rejectRequest(Long requestId, Long adminId, String adminName, String reason) {
        RecruitmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("申请状态不正确，无法审批");
        }

        request.reject(adminId, adminName, reason);
        RecruitmentRequest saved = requestRepository.save(request);

        // 通知制作人已驳回
        notifyProducerRejected(saved);

        log.info("Recruitment request {} rejected by admin {}: {}", request.getRequestNo(), adminId, reason);

        return saved;
    }

    /**
     * 制作人执行招聘（申请已批准后）
     *
     * @param requestId 申请ID
     * @param producerId 制作人Agent ID
     * @return 创建的Agent
     */
    public Agent executeRecruitment(Long requestId, String producerId) {
        RecruitmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        // 验证制作人身份
        if (!producerId.equals(request.getProducerId())) {
            throw new RuntimeException("只能执行自己的招聘申请");
        }

        // 验证状态
        if (!"APPROVED".equals(request.getStatus())) {
            throw new RuntimeException("申请未批准，无法执行招聘");
        }

        // 执行招聘
        Agent agent;
        if (request.getIsCustomRole()) {
            // 自定义角色招聘
            agent = recruitmentService.recruitAgent(producerId, request.getRole(), request.getEmployeeName(), request.getWorkDir());
        } else {
            // 预设角色招聘
            agent = recruitmentService.recruitAgent(producerId, request.getRole(), request.getEmployeeName(), request.getWorkDir());
        }

        // 更新申请状态
        request.execute(agent.getId());
        requestRepository.save(request);

        log.info("Recruitment executed: {} - Agent created: {}", request.getRequestNo(), agent.getId());

        return agent;
    }

    /**
     * 制作人取消申请
     *
     * @param requestId 申请ID
     * @param producerId 制作人Agent ID
     * @return 更新的申请
     */
    public RecruitmentRequest cancelRequest(Long requestId, String producerId) {
        RecruitmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        // 验证制作人身份
        if (!producerId.equals(request.getProducerId())) {
            throw new RuntimeException("只能取消自己的申请");
        }

        // 验证状态
        if (!"PENDING".equals(request.getStatus()) && !"REJECTED".equals(request.getStatus())) {
            throw new RuntimeException("申请状态不正确，无法取消");
        }

        request.cancel();
        RecruitmentRequest saved = requestRepository.save(request);

        log.info("Recruitment request {} cancelled by producer {}", request.getRequestNo(), producerId);

        return saved;
    }

    /**
     * 制作人重新申请（被驳回后）
     *
     * @param requestId 原申请ID
     * @param producerId 制作人Agent ID
     * @param newReason 新的招聘原因（根据驳回原因调整）
     * @return 新的申请
     */
    public RecruitmentRequest reviseRequest(Long requestId, String producerId, String newReason) {
        RecruitmentRequest originalRequest = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("原申请不存在"));

        // 验证制作人身份
        if (!producerId.equals(originalRequest.getProducerId())) {
            throw new RuntimeException("只能重新申请自己的招聘");
        }

        // 验证状态
        if (!"REJECTED".equals(originalRequest.getStatus())) {
            throw new RuntimeException("只有被驳回的申请才能重新申请");
        }

        // 创建新申请
        RecruitmentRequest newRequest = originalRequest.createRevision();
        newRequest.setReason(newReason);

        // 保存原申请（状态更新为REVISED）
        requestRepository.save(originalRequest);

        // 保存新申请
        RecruitmentRequest saved = requestRepository.save(newRequest);

        // 通知管理员有新的申请
        notifyAdmin(saved);

        log.info("Recruitment request revised: {} -> {}", originalRequest.getRequestNo(), saved.getRequestNo());

        return saved;
    }

    /**
     * 获取申请详情
     */
    public RecruitmentRequest getRequest(Long requestId) {
        return requestRepository.findById(requestId).orElse(null);
    }

    /**
     * 获取待审批的申请列表
     */
    public List<RecruitmentRequest> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * 获取制作人的申请列表
     */
    public List<RecruitmentRequest> getProducerRequests(String producerId) {
        return requestRepository.findByProducerIdOrderByCreatedAtDesc(producerId);
    }

    /**
     * 获取所有申请（分页）
     */
    public List<RecruitmentRequest> getAllRequests() {
        return requestRepository.findRecentRequests();
    }

    /**
     * 获取申请统计
     */
    public Map<String, Object> getRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态统计
        List<Object[]> statusCounts = requestRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        // 待审批数量
        stats.put("pendingCount", statusMap.getOrDefault("PENDING", 0L));

        // 制作人统计
        List<Object[]> producerCounts = requestRepository.countByProducer();
        stats.put("producerCounts", producerCounts.size());

        return stats;
    }

    /**
     * 通知管理员有新的招聘申请
     */
    private void notifyAdmin(RecruitmentRequest request) {
        String title = "新的招聘申请: " + request.getRequestNo();
        String content = String.format(
            "制作人 %s 发起了招聘申请\n\n" +
            "招聘角色: %s\n" +
            "员工姓名: %s\n" +
            "招聘原因: %s\n\n" +
            "请前往招聘审批页面进行审批。",
            request.getProducerName(),
            request.getRoleName(),
            request.getEmployeeName(),
            request.getReason()
        );

        // 发送系统通知给所有管理员
        // 这里简化处理，实际应该查询管理员列表
        notificationService.sendSystemNotification(
            null, // 广播给所有管理员
            title,
            content,
            Notification.NotificationType.SYSTEM
        );

        log.info("Admin notified about recruitment request: {}", request.getRequestNo());
    }

    /**
     * 通知制作人申请已批准
     */
    private void notifyProducerApproved(RecruitmentRequest request) {
        String title = "招聘申请已批准: " + request.getRequestNo();
        String content = String.format(
            "您的招聘申请已被管理员 %s 批准。\n\n" +
            "审批意见: %s\n\n" +
            "请前往招聘页面执行招聘。",
            request.getApproverName(),
            request.getApprovalComment() != null ? request.getApprovalComment() : "无"
        );

        // 这里需要根据制作人发送通知
        // 简化处理
        log.info("Producer notified about approved request: {}", request.getRequestNo());
    }

    /**
     * 通知制作人申请已驳回
     */
    private void notifyProducerRejected(RecruitmentRequest request) {
        String title = "招聘申请已驳回: " + request.getRequestNo();
        String content = String.format(
            "您的招聘申请已被管理员 %s 驳回。\n\n" +
            "驳回原因: %s\n\n" +
            "您可以：\n" +
            "1. 根据驳回原因修改后重新申请\n" +
            "2. 取消本次招聘",
            request.getApproverName(),
            request.getRejectionReason()
        );

        // 这里需要根据制作人发送通知
        // 简化处理
        log.info("Producer notified about rejected request: {}", request.getRequestNo());
    }

    /**
     * 生成申请编号
     */
    private String generateRequestNo() {
        String date = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "REC-" + date + "-" + random;
    }

    /**
     * 检查是否是预设角色
     */
    private boolean isPresetRole(String role) {
        Set<String> presetRoles = Set.of(
            "server-dev", "client-dev", "ui-dev",
            "system-planner", "numerical-planner",
            "tester", "git-commit"
        );
        return presetRoles.contains(role);
    }

    /**
     * 获取角色显示名称
     */
    private String getRoleDisplayName(String role) {
        return switch (role) {
            case "server-dev" -> "服务端开发";
            case "client-dev" -> "客户端开发";
            case "ui-dev" -> "UI设计";
            case "system-planner" -> "系统策划";
            case "numerical-planner" -> "数值策划";
            case "tester" -> "测试工程师";
            case "git-commit" -> "Git专员";
            default -> role;
        };
    }
}
