package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.KnowledgeEvolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识进化 API 控制器
 * 提供知识库自进化功能的接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/knowledge-evolution")
@Tag(name = "知识进化", description = "知识库自进化接口")
public class KnowledgeEvolutionApiController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeEvolutionApiController.class);

    private final KnowledgeEvolutionService evolutionService;
    private final AgentManager agentManager;
    private final ProjectManager projectManager;

    public KnowledgeEvolutionApiController(KnowledgeEvolutionService evolutionService,
                                            AgentManager agentManager,
                                            ProjectManager projectManager) {
        this.evolutionService = evolutionService;
        this.agentManager = agentManager;
        this.projectManager = projectManager;
    }

    /**
     * 处理 Agent 文档
     */
    @PostMapping("/process-document")
    @Operation(summary = "处理 Agent 文档")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processDocument(@RequestBody Map<String, String> data) {
        try {
            String agentId = data.get("agentId");
            String documentPath = data.get("documentPath");
            String documentContent = data.get("documentContent");

            evolutionService.processAgentDocument(agentId, documentPath, documentContent);

            return ResponseEntity.ok(Map.of("message", "文档处理成功"));
        } catch (Exception e) {
            log.error("处理文档失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理 Agent 技能
     */
    @PostMapping("/process-skill")
    @Operation(summary = "处理 Agent 技能")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processSkill(@RequestBody Map<String, String> data) {
        try {
            String agentId = data.get("agentId");
            String skillId = data.get("skillId");
            String skillName = data.get("skillName");
            String skillDescription = data.get("skillDescription");

            // 简化实现：记录日志
            log.info("处理 Agent 技能: agent={}, skill={}", agentId, skillId);

            return ResponseEntity.ok(Map.of("message", "技能处理成功"));
        } catch (Exception e) {
            log.error("处理技能失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("处理失败: " + e.getMessage()));
        }
    }

    /**
     * 从游戏生成中学习
     */
    @PostMapping("/learn-from-game")
    @Operation(summary = "从游戏生成中学习")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> learnFromGame(@RequestBody Map<String, Object> data) {
        try {
            String gameDescription = (String) data.get("gameDescription");
            String templateId = (String) data.get("templateId");
            String generatedCode = (String) data.get("generatedCode");
            Boolean success = (Boolean) data.get("success");

            evolutionService.learnFromGameGeneration(gameDescription, templateId, generatedCode, success);

            return ResponseEntity.ok(Map.of("message", "学习完成"));
        } catch (Exception e) {
            log.error("学习失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("学习失败: " + e.getMessage()));
        }
    }

    /**
     * 从 Agent 记忆中提取知识
     */
    @PostMapping("/extract-from-memory")
    @Operation(summary = "从 Agent 记忆中提取知识")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> extractFromMemory(@RequestBody Map<String, String> data) {
        try {
            String agentId = data.get("agentId");
            String projectId = data.get("projectId");

            // 获取项目
            GameProject project = null;
            if (projectId != null && !projectId.isEmpty()) {
                project = projectManager.getProject(projectId);
            } else {
                // 尝试从 Agent 获取项目
                Agent agent = agentManager.getAgent(agentId);
                if (agent instanceof BaseAgent baseAgent) {
                    project = baseAgent.getCurrentProject();
                }
            }

            if (project == null) {
                return ResponseEntity.badRequest().body(com.chengxun.gamemaker.web.dto.ErrorResponse.badRequest("无法确定项目，请提供 projectId"));
            }

            evolutionService.extractKnowledgeFromMemory(agentId, project);

            return ResponseEntity.ok(Map.of("message", "知识提取完成"));
        } catch (Exception e) {
            log.error("知识提取失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("提取失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发知识整理
     */
    @PostMapping("/organize")
    @Operation(summary = "手动触发知识整理")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> organizeKnowledge() {
        try {
            evolutionService.organizeKnowledgeBase();
            return ResponseEntity.ok(Map.of("message", "知识整理完成"));
        } catch (Exception e) {
            log.error("知识整理失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("整理失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发自进化
     */
    @PostMapping("/evolve")
    @Operation(summary = "手动触发自进化")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerEvolution() {
        try {
            evolutionService.selfEvolve();
            return ResponseEntity.ok(Map.of("message", "自进化完成"));
        } catch (Exception e) {
            log.error("自进化失败", e);
            return ResponseEntity.internalServerError().body(com.chengxun.gamemaker.web.dto.ErrorResponse.internal("进化失败: " + e.getMessage()));
        }
    }

    /**
     * 获取知识进化统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取知识进化统计")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(evolutionService.getEvolutionStats());
    }

    /**
     * 获取已学习的模式列表
     */
    @GetMapping("/learned-patterns")
    @Operation(summary = "获取已学习的模式")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getLearnedPatterns() {
        return ResponseEntity.ok(evolutionService.getLearnedPatternsList());
    }

    /**
     * 获取已学习的技能列表
     */
    @GetMapping("/learned-skills")
    @Operation(summary = "获取已学习的技能")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getLearnedSkills() {
        return ResponseEntity.ok(evolutionService.getLearnedSkillsList());
    }
}
