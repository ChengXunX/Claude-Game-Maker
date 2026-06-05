package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.InterventionActionRequest;
import com.chengxun.gamemaker.web.dto.InterventionRequest;
import com.chengxun.gamemaker.web.dto.PageRequest;
import com.chengxun.gamemaker.web.dto.UserInfo;
import com.chengxun.gamemaker.web.entity.AgentIntervention;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.exception.BusinessException;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.util.SecurityUtil;
import com.chengxun.gamemaker.service.AgentInterventionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Agent干预控制器
 * 提供人工干预Agent决策和方向的REST API
 *
 * 主要功能：
 * - 发送各种类型的干预指令（指令、决策覆盖、方向调整等）
 * - 暂停/恢复Agent工作
 * - 发送紧急指令
 * - Agent确认、执行、拒绝、取消干预
 * - 查询干预记录和统计（支持分页）
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "Agent干预", description = "人工干预Agent决策和方向的API")
@RestController
@RequestMapping("/api/interventions")
public class AgentInterventionController {

    private static final Logger log = LoggerFactory.getLogger(AgentInterventionController.class);

    private final AgentInterventionService interventionService;
    private final UserService userService;

    /**
     * 构造函数，注入依赖
     *
     * @param interventionService 干预服务
     * @param userService 用户服务
     */
    public AgentInterventionController(AgentInterventionService interventionService, UserService userService) {
        this.interventionService = interventionService;
        this.userService = userService;
    }

    // ===== 内部辅助方法 =====

    /**
     * 获取当前登录用户信息
     *
     * @param authentication Spring Security认证对象
     * @return 用户信息DTO
     */
    private UserInfo getCurrentUserInfo(Authentication authentication) {
        User currentUser = SecurityUtil.getCurrentUser(userService);
        Long userId = currentUser.getId();
        String username = authentication.getName();
        String userRole = currentUser.getRole() != null ? currentUser.getRole().getName() : "USER";
        return new UserInfo(userId, username, userRole);
    }

    // ===== 干预指令 =====

    /**
     * 发送通用干预指令
     *
     * @param request 干预请求DTO
     * @param authentication 当前认证用户
     * @return 发送结果，包含干预ID和编号
     */
    @Operation(summary = "发送通用干预指令", description = "发送各种类型的干预指令到指定Agent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "发送成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> sendIntervention(@Valid @RequestBody InterventionRequest request,
                                                                 Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        // 验证干预类型是否有效
        if (request.getInterventionType() == null || request.getInterventionType().isEmpty()) {
            throw BusinessException.badRequest("干预类型不能为空");
        }
        AgentIntervention.InterventionType type;
        try {
            type = AgentIntervention.InterventionType.valueOf(request.getInterventionType());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("无效的干预类型: " + request.getInterventionType());
        }

        AgentIntervention intervention = interventionService.sendIntervention(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(),
            request.getAgentId(), type, request.getInstruction(), request.getReason(), request.getTaskId()
        );

        log.info("Intervention sent: agentId={}, interventionNo={}", request.getAgentId(), intervention.getInterventionNo());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "interventionNo", intervention.getInterventionNo(),
            "message", "干预指令已发送"
        ));
    }

    /**
     * 发送指令干预
     *
     * @param request 干预请求DTO
     * @param authentication 当前认证用户
     * @return 发送结果
     */
    @Operation(summary = "发送指令干预", description = "向Agent发送新的指令")
    @PostMapping("/instruction")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> sendInstruction(@Valid @RequestBody InterventionRequest request,
                                                                Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        if (request.getInstruction() == null || request.getInstruction().isEmpty()) {
            throw BusinessException.badRequest("指令内容不能为空");
        }

        AgentIntervention intervention = interventionService.sendInstruction(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(),
            request.getAgentId(), request.getInstruction(), request.getReason()
        );

        log.info("Instruction sent: agentId={}, interventionNo={}", request.getAgentId(), intervention.getInterventionNo());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "interventionNo", intervention.getInterventionNo(),
            "message", "指令已发送"
        ));
    }

    /**
     * 覆盖Agent决策
     *
     * @param request 干预请求DTO
     * @param authentication 当前认证用户
     * @return 覆盖结果
     */
    @Operation(summary = "覆盖Agent决策", description = "覆盖Agent的某个决策")
    @PostMapping("/override-decision")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> overrideDecision(@Valid @RequestBody InterventionRequest request,
                                                                 Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        if (request.getNewDecision() == null || request.getNewDecision().isEmpty()) {
            throw BusinessException.badRequest("新决策不能为空");
        }

        AgentIntervention intervention = interventionService.overrideDecision(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(),
            request.getAgentId(), request.getOriginalDecision(), request.getNewDecision(), request.getReason()
        );

        log.info("Decision overridden: agentId={}, interventionNo={}", request.getAgentId(), intervention.getInterventionNo());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "interventionNo", intervention.getInterventionNo(),
            "message", "决策已覆盖"
        ));
    }

    /**
     * 调整工作方向
     *
     * @param request 干预请求DTO
     * @param authentication 当前认证用户
     * @return 调整结果
     */
    @Operation(summary = "调整工作方向", description = "调整Agent的工作方向")
    @PostMapping("/change-direction")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> changeDirection(@Valid @RequestBody InterventionRequest request,
                                                                Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        if (request.getNewDirection() == null || request.getNewDirection().isEmpty()) {
            throw BusinessException.badRequest("新方向不能为空");
        }

        AgentIntervention intervention = interventionService.changeDirection(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(),
            request.getAgentId(), request.getNewDirection(), request.getReason()
        );

        log.info("Direction changed: agentId={}, interventionNo={}", request.getAgentId(), intervention.getInterventionNo());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "interventionNo", intervention.getInterventionNo(),
            "message", "工作方向已调整"
        ));
    }

    /**
     * 暂停Agent工作
     *
     * @param agentId Agent ID
     * @param reason 暂停原因
     * @param authentication 当前认证用户
     * @return 暂停结果
     */
    @Operation(summary = "暂停Agent", description = "暂停指定Agent的工作")
    @PostMapping("/pause/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> pauseAgent(@PathVariable String agentId,
                                                           @RequestParam String reason,
                                                           Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        AgentIntervention intervention = interventionService.pauseAgent(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(), agentId, reason
        );

        log.info("Agent paused: agentId={}", agentId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "message", "Agent已暂停"
        ));
    }

    /**
     * 恢复Agent工作
     *
     * @param agentId Agent ID
     * @param reason 恢复原因
     * @param authentication 当前认证用户
     * @return 恢复结果
     */
    @Operation(summary = "恢复Agent", description = "恢复指定Agent的工作")
    @PostMapping("/resume/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> resumeAgent(@PathVariable String agentId,
                                                            @RequestParam String reason,
                                                            Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        AgentIntervention intervention = interventionService.resumeAgent(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(), agentId, reason
        );

        log.info("Agent resumed: agentId={}", agentId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "message", "Agent已恢复"
        ));
    }

    /**
     * 发送紧急指令
     *
     * @param request 干预请求DTO
     * @param authentication 当前认证用户
     * @return 发送结果
     */
    @Operation(summary = "发送紧急指令", description = "向Agent发送紧急指令")
    @PostMapping("/urgent")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> sendUrgentInstruction(@Valid @RequestBody InterventionRequest request,
                                                                      Authentication authentication) {
        UserInfo userInfo = getCurrentUserInfo(authentication);

        if (request.getInstruction() == null || request.getInstruction().isEmpty()) {
            throw BusinessException.badRequest("指令内容不能为空");
        }

        AgentIntervention intervention = interventionService.sendUrgentInstruction(
            userInfo.getUserId(), userInfo.getUsername(), userInfo.getRoleName(),
            request.getAgentId(), request.getInstruction(), request.getReason()
        );

        log.info("Urgent instruction sent: agentId={}, interventionNo={}", request.getAgentId(), intervention.getInterventionNo());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionId", intervention.getId(),
            "interventionNo", intervention.getInterventionNo(),
            "message", "紧急指令已发送"
        ));
    }

    // ===== Agent处理干预 =====

    /**
     * Agent确认干预
     *
     * @param interventionId 干预ID
     * @param request 操作请求DTO
     * @return 确认结果
     */
    @Operation(summary = "确认干预", description = "Agent确认收到干预指令")
    @PostMapping("/{interventionId}/acknowledge")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> acknowledgeIntervention(
            @PathVariable Long interventionId,
            @Valid @RequestBody InterventionActionRequest request) {
        AgentIntervention intervention = interventionService.acknowledgeIntervention(interventionId, request.getComment());

        log.info("Intervention acknowledged: interventionId={}", interventionId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionNo", intervention.getInterventionNo(),
            "message", "干预已确认"
        ));
    }

    /**
     * Agent执行干预
     *
     * @param interventionId 干预ID
     * @param request 操作请求DTO
     * @return 执行结果
     */
    @Operation(summary = "执行干预", description = "Agent执行干预指令")
    @PostMapping("/{interventionId}/execute")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> executeIntervention(
            @PathVariable Long interventionId,
            @Valid @RequestBody InterventionActionRequest request) {
        AgentIntervention intervention = interventionService.executeIntervention(interventionId, request.getComment());

        log.info("Intervention executed: interventionId={}", interventionId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionNo", intervention.getInterventionNo(),
            "message", "干预已执行"
        ));
    }

    /**
     * Agent拒绝干预
     *
     * @param interventionId 干预ID
     * @param request 操作请求DTO
     * @return 拒绝结果
     */
    @Operation(summary = "拒绝干预", description = "Agent拒绝干预指令")
    @PostMapping("/{interventionId}/reject")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> rejectIntervention(
            @PathVariable Long interventionId,
            @Valid @RequestBody InterventionActionRequest request) {
        AgentIntervention intervention = interventionService.rejectIntervention(interventionId, request.getComment());

        log.info("Intervention rejected: interventionId={}", interventionId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionNo", intervention.getInterventionNo(),
            "message", "干预已拒绝"
        ));
    }

    /**
     * 取消干预
     *
     * @param interventionId 干预ID
     * @param request 操作请求DTO
     * @return 取消结果
     */
    @Operation(summary = "取消干预", description = "取消待处理的干预指令")
    @PostMapping("/{interventionId}/cancel")
    @PreAuthorize("hasAuthority('PERM_agents:manage')")
    public ResponseEntity<Map<String, Object>> cancelIntervention(
            @PathVariable Long interventionId,
            @Valid @RequestBody InterventionActionRequest request) {
        AgentIntervention intervention = interventionService.cancelIntervention(interventionId, request.getComment());

        log.info("Intervention cancelled: interventionId={}", interventionId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "interventionNo", intervention.getInterventionNo(),
            "message", "干预已取消"
        ));
    }

    // ===== 查询接口 =====

    /**
     * 获取干预记录详情
     *
     * @param interventionId 干预ID
     * @return 干预记录
     */
    @Operation(summary = "获取干预详情", description = "根据ID获取干预记录详情")
    @GetMapping("/{interventionId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<AgentIntervention> getIntervention(@PathVariable Long interventionId) {
        AgentIntervention intervention = interventionService.getIntervention(interventionId);
        if (intervention == null) {
            throw BusinessException.notFound("干预记录不存在");
        }
        return ResponseEntity.ok(intervention);
    }

    /**
     * 获取Agent的干预历史
     *
     * @param agentId Agent ID
     * @return 干预记录列表
     */
    @Operation(summary = "获取Agent干预历史", description = "获取指定Agent的所有干预记录")
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<AgentIntervention>> getAgentInterventions(@PathVariable String agentId) {
        return ResponseEntity.ok(interventionService.getAgentInterventions(agentId));
    }

    /**
     * 获取Agent待处理的干预
     *
     * @param agentId Agent ID
     * @return 待处理干预列表
     */
    @Operation(summary = "获取Agent待处理干预", description = "获取指定Agent的待处理干预")
    @GetMapping("/agent/{agentId}/pending")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<AgentIntervention>> getPendingInterventions(@PathVariable String agentId) {
        return ResponseEntity.ok(interventionService.getPendingInterventions(agentId));
    }

    /**
     * 获取所有待处理的干预
     *
     * @return 所有待处理干预列表
     */
    @Operation(summary = "获取所有待处理干预", description = "获取所有待处理的干预指令")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<List<AgentIntervention>> getAllPendingInterventions() {
        return ResponseEntity.ok(interventionService.getAllPendingInterventions());
    }

    /**
     * 获取所有干预记录（支持分页）
     *
     * @param pageRequest 分页参数
     * @return 分页后的干预记录
     */
    @Operation(summary = "获取所有干预记录", description = "分页获取所有干预记录")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Page<AgentIntervention>> getAllInterventions(PageRequest pageRequest) {
        return ResponseEntity.ok(interventionService.getAllInterventions(pageRequest));
    }

    /**
     * 获取干预统计
     *
     * @return 统计数据，包含总数、各状态数量等
     */
    @Operation(summary = "获取干预统计", description = "获取干预记录的统计数据")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_agents:view')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(interventionService.getInterventionStatistics());
    }
}
