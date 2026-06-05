package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.entity.ProjectMember;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.ProjectPermissionService;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.util.GitCommandUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Git操作控制器
 *
 * 权限模型：
 * - 读操作：需要项目 VIEWER 权限
 * - 写操作（commit/push/pull/branch）：需要项目 DEVELOPER 权限
 * - 所有写操作记录审计日志
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "Git操作", description = "Git仓库操作API")
@Controller
@RequestMapping("/code/git")
public class GitOperationsController {

    private static final Logger log = LoggerFactory.getLogger(GitOperationsController.class);

    private final ProjectManager projectManager;
    private final ProjectPermissionService permissionService;
    private final UserService userService;
    private final OperationLogService logService;

    public GitOperationsController(ProjectManager projectManager, ProjectPermissionService permissionService,
                                   UserService userService, OperationLogService logService) {
        this.projectManager = projectManager;
        this.permissionService = permissionService;
        this.userService = userService;
        this.logService = logService;
    }

    /**
     * Git操作页面
     */
    @Operation(summary = "Git操作页面", description = "显示Git仓库状态和操作界面")
    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String gitPage(@PathVariable String projectId,
                          @RequestParam(required = false, defaultValue = "status") String tab,
                          Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.VIEWER)) {
            return "redirect:/code";
        }

        GameProject project = projectManager.getProject(projectId);
        if (project == null) return "redirect:/code";

        String workDir = project.getWorkDir();
        boolean isGitRepo = GitCommandUtil.isGitRepo(workDir);

        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("isGitRepo", isGitRepo);
        model.addAttribute("activeTab", tab);

        if (isGitRepo) {
            model.addAttribute("gitStatus", GitCommandUtil.getStatus(workDir));
            model.addAttribute("currentBranch", GitCommandUtil.getCurrentBranch(workDir));
            model.addAttribute("gitLog", GitCommandUtil.getRecentCommits(workDir, 20));
            model.addAttribute("gitRemote", GitCommandUtil.getRemoteInfo(workDir));
            model.addAttribute("gitBranches", GitCommandUtil.getBranches(workDir));
            model.addAttribute("gitDiff", GitCommandUtil.getDiff(workDir));
            model.addAttribute("gitStash", GitCommandUtil.getStashList(workDir));
        }

        model.addAttribute("username", authentication.getName());
        return "code/git";
    }

    /**
     * Git初始化
     */
    @Operation(summary = "Git初始化", description = "初始化Git仓库")
    @PostMapping("/{projectId}/init")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitInit(@PathVariable String projectId,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && !GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result = GitCommandUtil.initRepo(project.getWorkDir());
            redirectAttributes.addFlashAttribute("success", "Git仓库初始化成功");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_INIT",
                project.getName(), "Initialized git repo", projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=status";
    }

    /**
     * Git添加文件
     */
    @Operation(summary = "Git添加", description = "添加文件到暂存区")
    @PostMapping("/{projectId}/add")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitAdd(@PathVariable String projectId,
                         @RequestParam(defaultValue = ".") String path,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result = GitCommandUtil.addFiles(project.getWorkDir(), path);
            redirectAttributes.addFlashAttribute("success", "文件已添加到暂存区");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_ADD",
                project.getName(), "Added: " + path, projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=status";
    }

    /**
     * Git提交
     */
    @Operation(summary = "Git提交", description = "提交暂存区的文件")
    @PostMapping("/{projectId}/commit")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitCommit(@PathVariable String projectId,
                            @RequestParam String message,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            if (message == null || message.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "提交信息不能为空");
                return "redirect:/code/git/" + projectId + "?tab=status";
            }
            String result = GitCommandUtil.commit(project.getWorkDir(), message);
            if (result.contains("nothing to commit")) {
                redirectAttributes.addFlashAttribute("info", "没有需要提交的更改");
            } else {
                redirectAttributes.addFlashAttribute("success", "提交成功");

                logService.log(user != null ? user.getId() : null, "GIT_COMMIT",
                    project.getName(), "Committed: " + message, projectId);
            }
            redirectAttributes.addFlashAttribute("gitResult", result);
        }
        return "redirect:/code/git/" + projectId + "?tab=status";
    }

    /**
     * Git推送
     */
    @Operation(summary = "Git推送", description = "推送本地提交到远程仓库")
    @PostMapping("/{projectId}/push")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitPush(@PathVariable String projectId,
                          @RequestParam(required = false, defaultValue = "false") boolean force,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result = GitCommandUtil.push(project.getWorkDir(), force);
            redirectAttributes.addFlashAttribute("success", "Push 完成");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_PUSH",
                project.getName(), "Pushed (force=" + force + ")", projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=status";
    }

    /**
     * Git拉取
     */
    @Operation(summary = "Git拉取", description = "从远程仓库拉取更新")
    @PostMapping("/{projectId}/pull")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitPull(@PathVariable String projectId,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result = GitCommandUtil.pull(project.getWorkDir());
            redirectAttributes.addFlashAttribute("success", "Pull 完成");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_PULL",
                project.getName(), "Pulled from remote", projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=status";
    }

    /**
     * Git远程仓库配置
     */
    @Operation(summary = "Git远程仓库", description = "配置远程仓库")
    @PostMapping("/{projectId}/remote")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitRemote(@PathVariable String projectId,
                            @RequestParam String action,
                            @RequestParam String remoteName,
                            @RequestParam(required = false) String remoteUrl,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result;
            if ("set-url".equals(action) && remoteUrl != null) {
                result = GitCommandUtil.setRemoteUrl(project.getWorkDir(), remoteUrl);
            } else {
                result = "操作暂不支持: " + action;
            }
            redirectAttributes.addFlashAttribute("success", "远程仓库操作完成");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_REMOTE",
                project.getName(), action + " remote: " + remoteName, projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=remote";
    }

    /**
     * 创建分支
     */
    @Operation(summary = "创建分支", description = "创建新分支")
    @PostMapping("/{projectId}/branch/create")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String createBranch(@PathVariable String projectId,
                               @RequestParam String branchName,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result = GitCommandUtil.createBranch(project.getWorkDir(), branchName);
            redirectAttributes.addFlashAttribute("success", "分支创建成功");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_BRANCH_CREATE",
                project.getName(), "Created branch: " + branchName, projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=branches";
    }

    /**
     * 切换分支
     */
    @Operation(summary = "切换分支", description = "切换到指定分支")
    @PostMapping("/{projectId}/branch/checkout")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String checkoutBranch(@PathVariable String projectId,
                                 @RequestParam String branchName,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result = GitCommandUtil.checkoutBranch(project.getWorkDir(), branchName);
            redirectAttributes.addFlashAttribute("success", "已切换到分支: " + branchName);
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_CHECKOUT",
                project.getName(), "Checked out: " + branchName, projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=branches";
    }

    /**
     * Git stash 操作
     */
    @Operation(summary = "Git stash", description = "暂存工作区更改")
    @PostMapping("/{projectId}/stash")
    @PreAuthorize("hasAuthority('PERM_projects:manage')")
    public String gitStash(@PathVariable String projectId,
                           @RequestParam String action,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = userService.getUserByUsername(authentication.getName());
        if (!permissionService.hasProjectAccess(user, projectId, ProjectMember.ProjectRole.DEVELOPER)) {
            redirectAttributes.addFlashAttribute("error", "无权限操作该项目");
            return "redirect:/code/git/" + projectId;
        }

        GameProject project = projectManager.getProject(projectId);
        if (project != null && GitCommandUtil.isGitRepo(project.getWorkDir())) {
            String result;
            switch (action) {
                case "save" -> result = GitCommandUtil.stashSave(project.getWorkDir(), "Stash at " + java.time.LocalDateTime.now());
                case "pop" -> result = GitCommandUtil.stashPop(project.getWorkDir());
                case "drop" -> result = GitCommandUtil.stashDrop(project.getWorkDir());
                default -> result = "Unknown action: " + action;
            }
            redirectAttributes.addFlashAttribute("success", "Stash 操作完成");
            redirectAttributes.addFlashAttribute("gitResult", result);

            logService.log(user != null ? user.getId() : null, "GIT_STASH",
                project.getName(), "Stash " + action, projectId);
        }
        return "redirect:/code/git/" + projectId + "?tab=status";
    }
}
