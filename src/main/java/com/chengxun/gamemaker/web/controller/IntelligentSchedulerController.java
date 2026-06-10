package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.CollaborationCoordinator;
import com.chengxun.gamemaker.service.IntelligentScheduler;
import com.chengxun.gamemaker.service.QualityGateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能调度和协作管理 API
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/scheduler")
@PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_admin:manage')")
public class IntelligentSchedulerController {

    private static final Logger log = LoggerFactory.getLogger(IntelligentSchedulerController.class);

    private final IntelligentScheduler intelligentScheduler;
    private final CollaborationCoordinator collaborationCoordinator;
    private final QualityGateService qualityGateService;

    public IntelligentSchedulerController(IntelligentScheduler intelligentScheduler,
                                           CollaborationCoordinator collaborationCoordinator,
                                           QualityGateService qualityGateService) {
        this.intelligentScheduler = intelligentScheduler;
        this.collaborationCoordinator = collaborationCoordinator;
        this.qualityGateService = qualityGateService;
    }

    // ===== 智能调度 =====

    /**
     * 获取调度统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getScheduleStats() {
        return ResponseEntity.ok(intelligentScheduler.getScheduleStats());
    }

    // ===== 协作管理 =====

    /**
     * 获取协作统计信息
     */
    @GetMapping("/collaboration/stats")
    public ResponseEntity<Map<String, Object>> getCollaborationStats() {
        return ResponseEntity.ok(collaborationCoordinator.getCollaborationStats());
    }

    /**
     * 获取项目的协作会话列表
     */
    @GetMapping("/collaboration/project/{projectId}")
    public ResponseEntity<List<CollaborationCoordinator.CollaborationSession>> getProjectCollaborations(
            @PathVariable String projectId) {
        return ResponseEntity.ok(collaborationCoordinator.getProjectSessions(projectId));
    }

    /**
     * 获取协作会话详情
     */
    @GetMapping("/collaboration/session/{sessionId}")
    public ResponseEntity<CollaborationCoordinator.CollaborationSession> getCollaborationSession(
            @PathVariable String sessionId) {
        CollaborationCoordinator.CollaborationSession session = collaborationCoordinator.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    // ===== Agent 综合评估 =====

    /**
     * 获取项目下所有 Agent 的综合评估
     * 用于调度页面展示 Agent 多维度评分和解雇风险
     */
    @GetMapping("/evaluations/{projectId}")
    public ResponseEntity<List<Map<String, Object>>> getAgentEvaluations(@PathVariable String projectId) {
        return ResponseEntity.ok(intelligentScheduler.getAgentEvaluations(projectId));
    }

    // ===== 质量门禁 =====

    /**
     * 获取质量门禁配置
     */
    @GetMapping("/quality-gates")
    public ResponseEntity<Map<String, QualityGateService.QualityGateConfig>> getQualityGateConfigs() {
        return ResponseEntity.ok(qualityGateService.getGateConfigs());
    }

    /**
     * 执行项目质量评估
     */
    @PostMapping("/quality-gates/assess/{projectId}")
    public ResponseEntity<QualityGateService.QualityAssessment> assessProjectQuality(
            @PathVariable String projectId,
            @RequestBody Map<String, String> request) {
        String projectDir = request.get("projectDir");
        String projectName = request.get("projectName");
        String projectGoal = request.get("projectGoal");

        if (projectDir == null || projectDir.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        QualityGateService.QualityAssessment assessment =
            qualityGateService.assessQuality(projectId, projectDir, projectName, projectGoal);

        return ResponseEntity.ok(assessment);
    }

    /**
     * 更新质量门禁配置
     */
    @PutMapping("/quality-gates/{gateId}")
    public ResponseEntity<Map<String, Object>> updateQualityGateConfig(
            @PathVariable String gateId,
            @RequestBody Map<String, Object> request) {
        int minScore = (int) request.getOrDefault("minScore", 60);
        boolean blocking = (boolean) request.getOrDefault("blocking", false);

        qualityGateService.updateGateConfig(gateId, minScore, blocking);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "质量门禁配置已更新"
        ));
    }

    /**
     * 获取项目质量评估历史
     */
    @GetMapping("/quality-gates/history/{projectId}")
    public ResponseEntity<List<com.chengxun.gamemaker.web.entity.QualityGateAssessment>> getAssessmentHistory(
            @PathVariable String projectId) {
        return ResponseEntity.ok(qualityGateService.getAssessmentHistory(projectId));
    }

    /**
     * 获取项目最新质量评估
     */
    @GetMapping("/quality-gates/latest/{projectId}")
    public ResponseEntity<com.chengxun.gamemaker.web.entity.QualityGateAssessment> getLatestAssessment(
            @PathVariable String projectId) {
        com.chengxun.gamemaker.web.entity.QualityGateAssessment assessment =
            qualityGateService.getLatestAssessment(projectId);
        if (assessment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(assessment);
    }
}
