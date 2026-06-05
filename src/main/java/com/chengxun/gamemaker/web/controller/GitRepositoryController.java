package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.CodeReview;
import com.chengxun.gamemaker.web.entity.GitRepository;
import com.chengxun.gamemaker.web.service.CodeReviewService;
import com.chengxun.gamemaker.web.service.GitRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Git仓库管理控制器
 * 提供Git仓库的管理接口
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
@RequestMapping({"/git", "/api/git"})
public class GitRepositoryController {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryController.class);

    @Autowired
    private GitRepositoryService gitRepositoryService;

    @Autowired
    private CodeReviewService codeReviewService;

    // ===== 页面路由 =====

    /**
     * Git仓库管理页面
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String gitDashboard(@PathVariable String projectId, Model model) {
        log.info("Loading git repositories for project: {}", projectId);

        List<GitRepository> repositories = gitRepositoryService.getProjectRepositories(projectId);
        model.addAttribute("repositories", repositories);
        model.addAttribute("projectId", projectId);

        // 获取统计信息
        Map<String, Object> statistics = gitRepositoryService.getRepositoryStatistics(projectId);
        model.addAttribute("statistics", statistics);

        return "git/dashboard";
    }

    /**
     * Git仓库详情页
     */
    @GetMapping("/repository/{id}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String repositoryDetail(@PathVariable Long id, Model model) {
        GitRepository repository = gitRepositoryService.getRepository(id);
        model.addAttribute("repository", repository);

        // 获取关联的Agent
        List<String> assignedAgents = gitRepositoryService.getAssignedAgents(id);
        model.addAttribute("assignedAgents", assignedAgents);

        // 获取仓库的审查记录
        List<CodeReview> reviews = codeReviewService.getRepositoryReviews(id);
        model.addAttribute("reviews", reviews);

        return "git/detail";
    }

    /**
     * 创建/编辑仓库页面
     */
    @GetMapping("/repository/{projectId}/new")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String createRepository(@PathVariable String projectId, Model model) {
        model.addAttribute("projectId", projectId);
        model.addAttribute("repository", new GitRepository());
        return "git/form";
    }

    @GetMapping("/repository/{id}/edit")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String editRepository(@PathVariable Long id, Model model) {
        GitRepository repository = gitRepositoryService.getRepository(id);
        model.addAttribute("repository", repository);
        model.addAttribute("projectId", repository.getProjectId());
        return "git/form";
    }

    // ===== API接口 =====

    /**
     * 获取项目的所有Git仓库（JSON）
     */
    @GetMapping("/api/project/{projectId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<GitRepository>> getProjectRepositories(@PathVariable String projectId) {
        return ResponseEntity.ok(gitRepositoryService.getProjectRepositories(projectId));
    }

    /**
     * 获取Git仓库详情（JSON）
     */
    @GetMapping("/api/repository/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<GitRepository> getRepository(@PathVariable Long id) {
        GitRepository repository = gitRepositoryService.getRepository(id);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository);
    }

    /**
     * 创建Git仓库（JSON）
     */
    @PostMapping("/api/repository")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<GitRepository> createRepository(@RequestBody GitRepository repository) {
        GitRepository created = gitRepositoryService.createRepository(repository);
        return ResponseEntity.ok(created);
    }

    /**
     * 更新Git仓库（JSON）
     */
    @PutMapping("/api/repository/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<GitRepository> updateRepository(@PathVariable Long id, @RequestBody GitRepository repository) {
        repository.setId(id);
        GitRepository updated = gitRepositoryService.updateRepository(repository);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除Git仓库（JSON）
     */
    @DeleteMapping("/api/repository/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, String>> deleteRepository(@PathVariable Long id) {
        gitRepositoryService.deleteRepository(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Repository deleted"));
    }

    /**
     * 分配Agent到仓库（JSON）
     */
    @PostMapping("/api/repository/{id}/assign-agent")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, String>> assignAgent(@PathVariable Long id, @RequestParam String agentId) {
        gitRepositoryService.assignAgent(id, agentId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Agent assigned"));
    }

    /**
     * 取消分配Agent（JSON）
     */
    @PostMapping("/api/repository/{id}/unassign-agent")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, String>> unassignAgent(@PathVariable Long id, @RequestParam String agentId) {
        gitRepositoryService.unassignAgent(id, agentId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Agent unassigned"));
    }

    /**
     * 启用/禁用自动审查（JSON）
     */
    @PostMapping("/api/repository/{id}/toggle-auto-review")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, String>> toggleAutoReview(@PathVariable Long id, @RequestParam boolean enabled) {
        gitRepositoryService.toggleAutoReview(id, enabled);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Auto review " + (enabled ? "enabled" : "disabled")));
    }

    /**
     * 获取仓库的审查记录（JSON）
     */
    @GetMapping("/api/repository/{id}/reviews")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<CodeReview>> getRepositoryReviews(@PathVariable Long id) {
        return ResponseEntity.ok(codeReviewService.getRepositoryReviews(id));
    }

    /**
     * 获取仓库的待审查记录（JSON）
     */
    @GetMapping("/api/repository/{id}/reviews/pending")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<List<CodeReview>> getRepositoryPendingReviews(@PathVariable Long id) {
        return ResponseEntity.ok(codeReviewService.getRepositoryPendingReviews(id));
    }

    /**
     * 初始化项目的默认Git仓库结构（JSON）
     */
    @PostMapping("/api/project/{projectId}/initialize")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public ResponseEntity<Map<String, String>> initializeDefaultRepositories(
            @PathVariable String projectId,
            @RequestParam String projectWorkDir) {
        gitRepositoryService.initializeDefaultRepositories(projectId, projectWorkDir);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Default repositories initialized"));
    }

    /**
     * 获取项目Git仓库统计（JSON）
     */
    @GetMapping("/api/project/{projectId}/statistics")
    @ResponseBody
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public ResponseEntity<Map<String, Object>> getProjectStatistics(@PathVariable String projectId) {
        return ResponseEntity.ok(gitRepositoryService.getRepositoryStatistics(projectId));
    }
}
