package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.Intervention;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.exception.BusinessException;
import com.chengxun.gamemaker.web.repository.InterventionRepository;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 人工干预控制器
 * 提供用户对Agent进行干预的API接口
 *
 * 主要功能：
 * - 提交干预指令
 * - 查询干预历史
 * - 标记干预已处理
 * - 删除干预记录
 *
 * @author chengxun
 * @since 1.0.0
 * @deprecated 此控制器已废弃，请使用 {@link AgentInterventionController} 替代。
 *             AgentInterventionController 提供了更完整的干预功能。
 *             此控制器保留仅为向后兼容，将在未来版本中移除。
 */
@Deprecated
@RestController
@RequestMapping("/api/agents/{agentId}/interventions")
public class InterventionController {

    private static final Logger log = LoggerFactory.getLogger(InterventionController.class);

    private final InterventionRepository interventionRepository;
    private final UserService userService;
    private final OperationLogService logService;

    /**
     * 构造函数，注入依赖
     *
     * @param interventionRepository 干预记录仓库
     * @param userService 用户服务
     * @param logService 操作日志服务
     */
    public InterventionController(InterventionRepository interventionRepository,
                                  UserService userService,
                                  OperationLogService logService) {
        this.interventionRepository = interventionRepository;
        this.userService = userService;
        this.logService = logService;
    }

    /**
     * 提交干预指令
     *
     * @param agentId Agent ID
     * @param request 干预请求体，包含type、content、urgency、duration
     * @param authentication 当前认证用户
     * @return 提交结果，包含干预ID
     * @throws BusinessException 当用户不存在或参数无效时抛出
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> submitIntervention(
            @PathVariable String agentId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        // 获取当前用户
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }

        // 解析并验证请求参数
        String typeStr = request.get("type");
        String content = request.get("content");
        String urgencyStr = request.get("urgency");
        String durationStr = request.get("duration");

        if (typeStr == null || typeStr.isEmpty()) {
            throw BusinessException.badRequest("干预类型不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw BusinessException.badRequest("干预内容不能为空");
        }

        // 验证枚举值有效性
        Intervention.InterventionType interventionType;
        try {
            interventionType = Intervention.InterventionType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("无效的干预类型: " + typeStr);
        }

        Intervention.UrgencyLevel urgencyLevel;
        try {
            urgencyLevel = urgencyStr != null ? Intervention.UrgencyLevel.valueOf(urgencyStr) : Intervention.UrgencyLevel.NORMAL;
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("无效的紧急程度: " + urgencyStr);
        }

        Intervention.DurationType durationType;
        try {
            durationType = durationStr != null ? Intervention.DurationType.valueOf(durationStr) : Intervention.DurationType.ONCE;
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("无效的持续类型: " + durationStr);
        }

        // 创建干预记录
        Intervention intervention = new Intervention();
        intervention.setAgentId(agentId);
        intervention.setUserId(user.getId());
        intervention.setType(interventionType);
        intervention.setContent(content);
        intervention.setUrgency(urgencyLevel);
        intervention.setDuration(durationType);

        // 保存到数据库
        intervention = interventionRepository.save(intervention);

        // 记录操作日志
        logService.log(user.getId(), "INTERVENTION_SUBMITTED", agentId,
            String.format("提交干预: 类型=%s, 紧急程度=%s", typeStr, urgencyStr), null);

        log.info("Intervention submitted: agentId={}, interventionId={}", agentId, intervention.getId());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "干预已提交",
            "interventionId", intervention.getId()
        ));
    }

    /**
     * 获取Agent的干预历史
     *
     * @param agentId Agent ID
     * @return 干预记录列表，按创建时间倒序排列
     */
    @GetMapping
    public ResponseEntity<List<Intervention>> getInterventions(@PathVariable String agentId) {
        List<Intervention> interventions = interventionRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
        return ResponseEntity.ok(interventions);
    }

    /**
     * 获取Agent的未处理干预
     *
     * @param agentId Agent ID
     * @return 未处理的干预记录列表
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Intervention>> getPendingInterventions(@PathVariable String agentId) {
        List<Intervention> interventions = interventionRepository.findByAgentIdAndProcessedFalseOrderByCreatedAtDesc(agentId);
        return ResponseEntity.ok(interventions);
    }

    /**
     * 获取Agent的永久性干预
     *
     * @param agentId Agent ID
     * @return 永久性干预记录列表
     */
    @GetMapping("/permanent")
    public ResponseEntity<List<Intervention>> getPermanentInterventions(@PathVariable String agentId) {
        List<Intervention> interventions = interventionRepository.findPermanentInterventions(agentId);
        return ResponseEntity.ok(interventions);
    }

    /**
     * 标记干预为已处理
     *
     * @param agentId Agent ID
     * @param interventionId 干预ID
     * @param authentication 当前认证用户
     * @return 更新结果
     * @throws BusinessException 当干预记录不存在时抛出
     */
    @PutMapping("/{interventionId}/process")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> processIntervention(
            @PathVariable String agentId,
            @PathVariable Long interventionId,
            Authentication authentication) {
        Intervention intervention = interventionRepository.findById(interventionId)
            .orElseThrow(() -> BusinessException.notFound("干预记录不存在"));

        if (!intervention.getAgentId().equals(agentId)) {
            throw BusinessException.notFound("干预记录不存在");
        }

        intervention.setProcessed(true);
        intervention.setProcessedAt(LocalDateTime.now());
        interventionRepository.save(intervention);

        // 记录操作日志
        User user = userService.getUserByUsername(authentication.getName());
        if (user != null) {
            logService.log(user.getId(), "INTERVENTION_PROCESSED", agentId,
                String.format("标记干预已处理: ID=%d", interventionId), null);
        }

        log.info("Intervention processed: interventionId={}", interventionId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "干预已标记为已处理"
        ));
    }

    /**
     * 删除干预记录
     *
     * @param agentId Agent ID
     * @param interventionId 干预ID
     * @param authentication 当前认证用户
     * @return 删除结果
     * @throws BusinessException 当干预记录不存在时抛出
     */
    @DeleteMapping("/{interventionId}")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> deleteIntervention(
            @PathVariable String agentId,
            @PathVariable Long interventionId,
            Authentication authentication) {
        Intervention intervention = interventionRepository.findById(interventionId)
            .orElseThrow(() -> BusinessException.notFound("干预记录不存在"));

        if (!intervention.getAgentId().equals(agentId)) {
            throw BusinessException.notFound("干预记录不存在");
        }

        interventionRepository.delete(intervention);

        // 记录操作日志
        User user = userService.getUserByUsername(authentication.getName());
        if (user != null) {
            logService.log(user.getId(), "INTERVENTION_DELETED", agentId,
                String.format("删除干预: ID=%d", interventionId), null);
        }

        log.info("Intervention deleted: interventionId={}", interventionId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "干预已删除"
        ));
    }
}
