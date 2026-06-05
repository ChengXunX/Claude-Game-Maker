package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.web.util.FileOperationUtil;
import com.chengxun.gamemaker.web.util.GitCommandUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码浏览控制器
 * 处理代码文件的浏览、查看和搜索功能
 *
 * 主要功能：
 * - 项目代码目录浏览
 * - 文件内容查看
 * - 代码搜索
 * - 文件信息展示
 *
 * @author chengxun
 * @since 1.0.0
 */
@Tag(name = "代码浏览", description = "代码文件浏览和查看API")
@Controller
@RequestMapping("/code")
public class CodeWebController {

    private final ProjectManager projectManager;

    /**
     * 构造函数，注入依赖
     *
     * @param projectManager 项目管理器
     */
    public CodeWebController(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * 代码首页，显示项目列表
     *
     * @param model 模型对象
     * @param authentication 当前认证用户
     * @return 代码首页视图名
     */
    @Operation(summary = "代码首页", description = "显示所有项目的代码浏览入口")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String codeHome(Model model, Authentication authentication) {
        List<GameProject> projects = projectManager.getAllProjects();
        model.addAttribute("projects", projects);
        model.addAttribute("username", authentication.getName());
        return "code/index";
    }

    /**
     * 浏览项目目录
     *
     * @param projectId 项目ID
     * @param path 相对路径，为空时浏览根目录
     * @param model 模型对象
     * @param authentication 当前认证用户
     * @return 目录浏览页面视图名
     */
    @Operation(summary = "浏览项目目录", description = "浏览项目代码目录结构")
    @GetMapping("/browse/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String browseProject(@PathVariable String projectId,
                                @RequestParam(required = false, defaultValue = "") String path,
                                Model model, Authentication authentication) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/code";
        }

        String workDir = project.getWorkDir();
        Path targetDir = FileOperationUtil.safeResolve(Path.of(workDir), path);

        // 安全检查：不能跳出项目目录
        if (targetDir == null) {
            return "redirect:/code/browse/" + projectId;
        }

        List<FileOperationUtil.FileEntry> files = FileOperationUtil.listDirectory(targetDir);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("currentPath", path);
        model.addAttribute("files", files);
        model.addAttribute("parentPath", path.isEmpty() ? "" : Path.of(path).getParent() != null ? Path.of(path).getParent().toString() : "");
        model.addAttribute("username", authentication.getName());
        return "code/browse";
    }

    /**
     * 查看文件内容
     *
     * @param projectId 项目ID
     * @param path 文件相对路径
     * @param search 搜索关键词（可选）
     * @param model 模型对象
     * @param authentication 当前认证用户
     * @return 文件查看页面视图名
     */
    @Operation(summary = "查看文件", description = "查看代码文件内容")
    @GetMapping("/view/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String viewFile(@PathVariable String projectId,
                           @RequestParam String path,
                           @RequestParam(required = false) String search,
                           Model model, Authentication authentication) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/code";
        }

        String workDir = project.getWorkDir();
        Path filePath = FileOperationUtil.safeResolve(Path.of(workDir), path);

        // 安全检查
        if (filePath == null) {
            return "redirect:/code/browse/" + projectId;
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return "redirect:/code/browse/" + projectId + "?path=" + path;
        }

        try {
            String content = FileOperationUtil.readFile(filePath);
            long fileSize = FileOperationUtil.getFileSize(filePath);
            String fileName = filePath.getFileName().toString();

            // 搜索高亮
            List<Integer> matchLines = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].toLowerCase().contains(search.toLowerCase())) {
                        matchLines.add(i + 1);
                    }
                }
            }

            model.addAttribute("project", project);
            model.addAttribute("projectId", projectId);
            model.addAttribute("filePath", path);
            model.addAttribute("fileName", fileName);
            model.addAttribute("content", content);
            model.addAttribute("fileSize", fileSize);
            model.addAttribute("search", search);
            model.addAttribute("matchLines", matchLines);
            model.addAttribute("lineCount", content.split("\n").length);
            model.addAttribute("gitAvailable", GitCommandUtil.isGitRepo(workDir));
        } catch (IOException e) {
            model.addAttribute("error", "无法读取文件: " + e.getMessage());
        }

        model.addAttribute("username", authentication.getName());
        return "code/viewer";
    }

    /**
     * 代码搜索
     *
     * @param projectId 项目ID
     * @param keyword 搜索关键词
     * @param model 模型对象
     * @return 搜索结果页面视图名
     */
    @Operation(summary = "代码搜索", description = "在项目中搜索代码")
    @GetMapping("/search/{projectId}")
    @PreAuthorize("hasAuthority('PERM_projects:view')")
    public String searchCode(@PathVariable String projectId,
                             @RequestParam String keyword,
                             Model model) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/code";
        }

        List<FileOperationUtil.SearchResult> results = FileOperationUtil.searchInDirectory(
            Path.of(project.getWorkDir()), keyword);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("results", results);
        model.addAttribute("resultCount", results.size());
        return "code/search";
    }
}
