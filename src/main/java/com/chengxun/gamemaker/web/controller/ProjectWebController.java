package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects")
public class ProjectWebController {

    private final ProjectManager projectManager;

    public ProjectWebController(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    @GetMapping
    public String listProjects(Model model, Authentication authentication) {
        List<GameProject> projects = projectManager.getAllProjects();
        model.addAttribute("projects", projects);
        model.addAttribute("username", authentication.getName());
        return "projects";
    }

    @GetMapping("/{projectId}")
    public String projectDetail(@PathVariable String projectId, Model model, Authentication authentication) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return "redirect:/projects";
        }

        model.addAttribute("project", project);
        model.addAttribute("rules", projectManager.loadProjectRules(projectId));
        model.addAttribute("username", authentication.getName());
        return "project-detail";
    }

    @PostMapping("/create")
    public String createProject(@RequestParam String name,
                               @RequestParam String workDir,
                               @RequestParam(required = false) String description,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            GameProject project = projectManager.createProject(name, description, workDir);
            redirectAttributes.addFlashAttribute("success", "项目创建成功: " + project.getName());
            return "redirect:/projects/" + project.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "项目创建失败: " + e.getMessage());
            return "redirect:/projects";
        }
    }

    @PostMapping("/{projectId}/rules")
    public String updateRules(@PathVariable String projectId,
                             @RequestParam String rules,
                             RedirectAttributes redirectAttributes) {
        projectManager.saveProjectRules(projectId, rules);
        redirectAttributes.addFlashAttribute("success", "项目规范已更新");
        return "redirect:/projects/" + projectId;
    }
}
