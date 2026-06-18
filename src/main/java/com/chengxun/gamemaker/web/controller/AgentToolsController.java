package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.service.*;
import com.chengxun.gamemaker.service.SessionForkService.ForkInfo;
import com.chengxun.gamemaker.service.SessionForkService.MergeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Agent 工具控制器
 * 提供检查点、记忆搜索、知识提取、目标评估、子代理、Skill 发现等工作流和会话管理 API
 *
 * @author chengxun
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/agent-tools")
public class AgentToolsController {

    private static final Logger log = LoggerFactory.getLogger(AgentToolsController.class);

    @Autowired
    private CheckpointService checkpointService;

    @Autowired
    private MemorySearchService memorySearchService;

    @Autowired
    private DreamService dreamService;

    @Autowired
    private GoalJudgeService goalJudgeService;

    @Autowired
    private SubAgentService subAgentService;

    @Autowired
    private SkillDiscoveryService skillDiscoveryService;

    @Autowired
    private DistillService distillService;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private TaskGateService taskGateService;

    @Autowired
    private BudgetedContextInjector budgetedContextInjector;

    @Autowired
    private AgentToolPermissionService agentToolPermissionService;

    @Autowired
    private SessionForkService sessionForkService;

    @Autowired
    private ProjectManager projectManager;

    // ===== 检查点 API =====

    /**
     * 创建检查点
     */
    @PostMapping("/checkpoints")
    @PreAuthorize("hasAuthority('PERM_checkpoint:manage')")
    public ResponseEntity<Map<String, Object>> createCheckpoint(@RequestBody Map<String, String> request) {
        String agentId = request.get("agentId");
        String projectId = request.get("projectId");
        String reason = request.getOrDefault("reason", "手动创建");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        String checkpointId = checkpointService.createCheckpoint(agentId, project, reason);
        if (checkpointId != null) {
            return ResponseEntity.ok(Map.of("success", true, "checkpointId", checkpointId));
        }
        return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "创建检查点失败"));
    }

    /**
     * 列出检查点
     */
    @GetMapping("/checkpoints/{projectId}/{agentId}")
    @PreAuthorize("hasAuthority('PERM_checkpoint:view')")
    public ResponseEntity<Map<String, Object>> listCheckpoints(@PathVariable String projectId,
                                                                @PathVariable String agentId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        List<String> checkpoints = checkpointService.listCheckpoints(agentId, project);
        return ResponseEntity.ok(Map.of("success", true, "checkpoints", checkpoints));
    }

    /**
     * 加载检查点详情
     */
    @GetMapping("/checkpoints/{projectId}/{agentId}/{timestamp}")
    @PreAuthorize("hasAuthority('PERM_checkpoint:view')")
    public ResponseEntity<Map<String, Object>> getCheckpoint(@PathVariable String projectId,
                                                              @PathVariable String agentId,
                                                              @PathVariable String timestamp) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        Map<String, Object> checkpoint = checkpointService.loadCheckpoint(agentId, project, timestamp);
        if (checkpoint != null) {
            return ResponseEntity.ok(Map.of("success", true, "checkpoint", checkpoint));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 恢复检查点
     */
    @PostMapping("/checkpoints/{projectId}/{agentId}/{timestamp}/restore")
    @PreAuthorize("hasAuthority('PERM_checkpoint:manage')")
    public ResponseEntity<Map<String, Object>> restoreCheckpoint(@PathVariable String projectId,
                                                                  @PathVariable String agentId,
                                                                  @PathVariable String timestamp) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        Map<String, Object> checkpoint = checkpointService.loadCheckpoint(agentId, project, timestamp);
        if (checkpoint == null) {
            return ResponseEntity.notFound().build();
        }

        String recoveryPrompt = checkpointService.buildRecoveryPrompt(checkpoint);
        return ResponseEntity.ok(Map.of("success", true, "recoveryPrompt", recoveryPrompt));
    }

    /**
     * 删除检查点
     */
    @DeleteMapping("/checkpoints/{projectId}/{agentId}/{timestamp}")
    @PreAuthorize("hasAuthority('PERM_checkpoint:manage')")
    public ResponseEntity<Map<String, Object>> deleteCheckpoint(@PathVariable String projectId,
                                                                 @PathVariable String agentId,
                                                                 @PathVariable String timestamp) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        boolean deleted = checkpointService.deleteCheckpoint(agentId, project, timestamp);
        return ResponseEntity.ok(Map.of("success", deleted));
    }

    // ===== 记忆搜索 API =====

    /**
     * 搜索记忆
     */
    @GetMapping("/memory/search")
    @PreAuthorize("hasAuthority('PERM_skill:view')")
    public ResponseEntity<Map<String, Object>> searchMemory(@RequestParam String projectId,
                                                              @RequestParam String agentId,
                                                              @RequestParam String query) {
        List<MemorySearchService.MemorySearchResult> results = memorySearchService.search(projectId, agentId, query);
        return ResponseEntity.ok(Map.of("success", true, "results", results, "total", results.size()));
    }

    /**
     * 跨项目搜索记忆
     */
    @GetMapping("/memory/search/global")
    @PreAuthorize("hasAuthority('PERM_skill:view')")
    public ResponseEntity<Map<String, Object>> searchMemoryGlobal(@RequestParam String agentId,
                                                                    @RequestParam String query) {
        List<MemorySearchService.MemorySearchResult> results = memorySearchService.searchGlobal(agentId, query);
        return ResponseEntity.ok(Map.of("success", true, "results", results, "total", results.size()));
    }

    /**
     * 重建记忆索引
     */
    @PostMapping("/memory/rebuild-index")
    @PreAuthorize("hasAuthority('PERM_skill:manage')")
    public ResponseEntity<Map<String, Object>> rebuildIndex(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        String agentId = request.get("agentId");

        int count = memorySearchService.rebuildIndex(projectId, agentId);
        return ResponseEntity.ok(Map.of("success", true, "message", "索引已重建", "count", count));
    }

    // ===== Dream 知识提取 API =====

    /**
     * 触发 Dream 知识提取
     */
    @PostMapping("/dream/trigger")
    @PreAuthorize("hasAuthority('PERM_dream:execute')")
    public ResponseEntity<Map<String, Object>> triggerDream(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        String agentId = request.get("agentId");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        // 构造正确的 Agent ID：project-{projectId}:{role}
        if (agentId != null && !agentId.contains(":")) {
            agentId = projectId + ":" + agentId;
        }

        DreamService.DreamResult result = dreamService.dream(project, agentId);
        return ResponseEntity.ok(Map.of(
            "success", result.success(),
            "savedCount", result.savedCount(),
            "extractedCount", result.extractedCount()
        ));
    }

    // ===== 裁判评估 API =====

    /**
     * 评估目标
     */
    @PostMapping("/goal-judge/evaluate")
    @PreAuthorize("hasAuthority('PERM_goal:manage')")
    public ResponseEntity<Map<String, Object>> evaluateGoal(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        String goalDescription = request.get("goalDescription");
        String criteria = request.get("verificationCriteria");
        String history = request.get("conversationHistory");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        GoalJudgeService.Verdict verdict = goalJudgeService.evaluateGoal(project, goalDescription, criteria, history);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "ok", verdict.ok(),
            "impossible", verdict.impossible(),
            "reason", verdict.reason() != null ? verdict.reason() : ""
        ));
    }

    // ===== 子代理 API =====

    /**
     * 创建子代理
     */
    @PostMapping("/sub-agents/spawn")
    @PreAuthorize("hasAuthority('PERM_subagent:create')")
    public ResponseEntity<Map<String, Object>> spawnSubAgent(@RequestBody Map<String, String> request) {
        String parentAgentId = request.get("parentAgentId");
        String projectId = request.get("projectId");
        String task = request.get("task");
        String role = request.get("role");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        SubAgentService.SubAgentInfo info = subAgentService.spawnSubAgent(
            parentAgentId, projectId, project.getWorkDir(), task, role);

        if (info != null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "subAgentId", info.subAgentId(),
                "status", info.status().name()
            ));
        }
        return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "创建子代理失败"));
    }

    /**
     * 列出子代理
     */
    @GetMapping("/sub-agents/{parentAgentId}")
    @PreAuthorize("hasAuthority('PERM_subagent:view')")
    public ResponseEntity<Map<String, Object>> listSubAgents(@PathVariable String parentAgentId) {
        List<SubAgentService.SubAgentInfo> agents = subAgentService.listSubAgents(parentAgentId);
        return ResponseEntity.ok(Map.of("success", true, "subAgents", agents));
    }

    /**
     * 终止子代理
     */
    @PostMapping("/sub-agents/{subAgentId}/terminate")
    @PreAuthorize("hasAuthority('PERM_subagent:manage')")
    public ResponseEntity<Map<String, Object>> terminateSubAgent(@PathVariable String subAgentId) {
        boolean terminated = subAgentService.terminateSubAgent(subAgentId);
        return ResponseEntity.ok(Map.of("success", terminated));
    }

    /**
     * 子代理统计
     */
    @GetMapping("/sub-agents/stats")
    @PreAuthorize("hasAuthority('PERM_subagent:view')")
    public ResponseEntity<Map<String, Object>> getSubAgentStats() {
        return ResponseEntity.ok(Map.of("success", true, "stats", subAgentService.getStats()));
    }

    // ===== Skill 发现 API =====

    /**
     * 发现项目中的 Skill
     */
    @GetMapping("/skills/discover/{projectId}")
    @PreAuthorize("hasAuthority('PERM_skill:view')")
    public ResponseEntity<Map<String, Object>> discoverSkills(@PathVariable String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        List<SkillDiscoveryService.DiscoveredSkill> skills =
            skillDiscoveryService.discoverSkills(Path.of(project.getWorkDir()));

        return ResponseEntity.ok(Map.of("success", true, "skills", skills, "total", skills.size()));
    }

    // ===== Distill 工作流发现 API =====

    /**
     * 触发 Distill 工作流发现
     */
    @PostMapping("/distill/trigger")
    @PreAuthorize("hasAuthority('PERM_distill:execute')")
    public ResponseEntity<Map<String, Object>> triggerDistill(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        String agentId = request.get("agentId");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        DistillService.DistillResult result = distillService.distill(project, agentId);
        return ResponseEntity.ok(Map.of(
            "success", result.success(),
            "skillCount", result.skillCount(),
            "patternCount", result.patternCount()
        ));
    }

    // ===== 快照回滚 API =====

    /**
     * 创建快照
     */
    @PostMapping("/snapshots")
    @PreAuthorize("hasAuthority('PERM_snapshot:manage')")
    public ResponseEntity<Map<String, Object>> createSnapshot(@RequestBody Map<String, Object> request) {
        String agentId = (String) request.get("agentId");
        String projectId = (String) request.get("projectId");
        String description = (String) request.getOrDefault("description", "手动快照");
        @SuppressWarnings("unchecked")
        List<String> filePaths = (List<String>) request.get("filePaths");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        String snapshotId = snapshotService.createSnapshot(project, agentId, filePaths, description);
        if (snapshotId != null) {
            return ResponseEntity.ok(Map.of("success", true, "snapshotId", snapshotId));
        }
        return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "创建快照失败"));
    }

    /**
     * 列出快照
     */
    @GetMapping("/snapshots/{projectId}/{agentId}")
    @PreAuthorize("hasAuthority('PERM_snapshot:view')")
    public ResponseEntity<Map<String, Object>> listSnapshots(@PathVariable String projectId,
                                                               @PathVariable String agentId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        List<String> snapshots = snapshotService.listSnapshots(project, agentId);
        return ResponseEntity.ok(Map.of("success", true, "snapshots", snapshots, "total", snapshots.size()));
    }

    /**
     * 恢复快照
     */
    @PostMapping("/snapshots/{projectId}/{agentId}/{snapshotId}/restore")
    @PreAuthorize("hasAuthority('PERM_snapshot:manage')")
    public ResponseEntity<Map<String, Object>> restoreSnapshot(@PathVariable String projectId,
                                                                 @PathVariable String agentId,
                                                                 @PathVariable String snapshotId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        List<String> restoredFiles = snapshotService.restoreSnapshot(project, agentId, snapshotId);
        return ResponseEntity.ok(Map.of("success", true, "restoredFiles", restoredFiles));
    }

    /**
     * 撤销（undo）
     */
    @PostMapping("/snapshots/{projectId}/{agentId}/undo")
    @PreAuthorize("hasAuthority('PERM_snapshot:manage')")
    public ResponseEntity<Map<String, Object>> undo(@PathVariable String projectId,
                                                      @PathVariable String agentId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        List<String> restoredFiles = snapshotService.undo(project, agentId);
        if (!restoredFiles.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "restoredFiles", restoredFiles));
        }
        return ResponseEntity.ok(Map.of("success", false, "message", "没有可撤销的快照"));
    }

    /**
     * 删除快照
     */
    @DeleteMapping("/snapshots/{projectId}/{agentId}/{snapshotId}")
    @PreAuthorize("hasAuthority('PERM_snapshot:manage')")
    public ResponseEntity<Map<String, Object>> deleteSnapshot(@PathVariable String projectId,
                                                                @PathVariable String agentId,
                                                                @PathVariable String snapshotId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        boolean deleted = snapshotService.deleteSnapshot(project, agentId, snapshotId);
        return ResponseEntity.ok(Map.of("success", deleted));
    }

    // ===== 任务门禁 API =====

    /**
     * 检查任务门禁
     */
    @PostMapping("/task-gate/check")
    @PreAuthorize("hasAuthority('PERM_goal:view')")
    public ResponseEntity<Map<String, Object>> checkTaskGate(@RequestBody Map<String, Object> request) {
        String agentId = (String) request.get("agentId");
        String projectId = (String) request.get("projectId");
        boolean isSubAgent = Boolean.TRUE.equals(request.get("isSubAgent"));
        int currentGateCount = request.containsKey("currentGateCount") ?
            ((Number) request.get("currentGateCount")).intValue() : 0;

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        TaskGateService.GateResult result = taskGateService.checkGate(agentId, project, isSubAgent, currentGateCount);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "passed", result.passed(),
            "message", result.message(),
            "pendingItems", result.pendingItems()
        ));
    }

    // ===== 预算上下文注入 API =====

    /**
     * 获取预算注入的上下文
     */
    @PostMapping("/context/budgeted")
    @PreAuthorize("hasAuthority('PERM_checkpoint:view')")
    public ResponseEntity<Map<String, Object>> getBudgetedContext(@RequestBody Map<String, Object> request) {
        String agentId = (String) request.get("agentId");
        String projectId = (String) request.get("projectId");
        int totalBudget = request.containsKey("totalBudget") ?
            ((Number) request.get("totalBudget")).intValue() : 0;

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        String context = budgetedContextInjector.injectContext(agentId, project, totalBudget);
        return ResponseEntity.ok(Map.of("success", true, "context", context, "length", context.length()));
    }

    // ===== Agent 工具权限 API =====

    /**
     * 获取 Agent 工具权限
     */
    @GetMapping("/tool-permissions/{agentId}")
    @PreAuthorize("hasAuthority('PERM_tool:permission:manage')")
    public ResponseEntity<Map<String, Object>> getToolPermissions(@PathVariable String agentId) {
        var permissions = agentToolPermissionService.getPermissions(agentId);
        return ResponseEntity.ok(Map.of("success", true, "permissions", permissions));
    }

    /**
     * 设置 Agent 工具权限
     */
    @PostMapping("/tool-permissions/{agentId}")
    @PreAuthorize("hasAuthority('PERM_tool:permission:manage')")
    public ResponseEntity<Map<String, Object>> setToolPermissions(@PathVariable String agentId,
                                                                    @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> permList = (List<Map<String, String>>) request.get("permissions");
        if (permList == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "权限列表为空"));
        }

        List<AgentToolPermissionService.ToolPermission> permissions = permList.stream()
            .map(p -> new AgentToolPermissionService.ToolPermission(
                p.getOrDefault("tool", "*"),
                p.getOrDefault("pattern", "*"),
                p.getOrDefault("action", "ask")
            ))
            .toList();

        agentToolPermissionService.setPermissions(agentId, permissions);
        return ResponseEntity.ok(Map.of("success", true, "message", "权限已更新"));
    }

    // ===== 会话分叉 API =====

    /**
     * 创建会话分叉
     */
    @PostMapping("/forks")
    @PreAuthorize("hasAuthority('PERM_fork:create')")
    public ResponseEntity<Map<String, Object>> createFork(@RequestBody Map<String, String> request) {
        String agentId = request.get("agentId");
        String projectId = request.get("projectId");
        String description = request.get("description");

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "项目不存在"));
        }

        ForkInfo forkInfo = sessionForkService.fork(agentId, project, description);
        if (forkInfo != null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "forkId", forkInfo.forkId(),
                "status", forkInfo.status().name()
            ));
        }
        return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "创建分叉失败"));
    }

    /**
     * 列出分叉
     */
    @GetMapping("/forks/{parentAgentId}")
    @PreAuthorize("hasAuthority('PERM_fork:view')")
    public ResponseEntity<Map<String, Object>> listForks(@PathVariable String parentAgentId) {
        List<ForkInfo> forks = sessionForkService.listForks(parentAgentId);
        return ResponseEntity.ok(Map.of("success", true, "forks", forks, "total", forks.size()));
    }

    /**
     * 合并分叉
     */
    @PostMapping("/forks/{forkId}/merge")
    @PreAuthorize("hasAuthority('PERM_fork:manage')")
    public ResponseEntity<Map<String, Object>> mergeFork(@PathVariable String forkId,
                                                           @RequestParam(defaultValue = "merge") String strategy) {
        MergeStrategy mergeStrategy;
        try {
            mergeStrategy = MergeStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            mergeStrategy = MergeStrategy.MERGE;
        }

        boolean merged = sessionForkService.merge(forkId, mergeStrategy);
        if (merged) {
            return ResponseEntity.ok(Map.of("success", true, "message", "分叉已合并"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "合并失败"));
    }

    /**
     * 丢弃分叉
     */
    @PostMapping("/forks/{forkId}/discard")
    @PreAuthorize("hasAuthority('PERM_fork:manage')")
    public ResponseEntity<Map<String, Object>> discardFork(@PathVariable String forkId) {
        boolean discarded = sessionForkService.discard(forkId);
        return ResponseEntity.ok(Map.of("success", discarded));
    }
}
