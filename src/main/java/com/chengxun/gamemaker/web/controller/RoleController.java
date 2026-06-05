package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping({"/roles", "/api/roles"})
@PreAuthorize("hasAuthority('PERM_roles:manage')")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;
    private final OperationLogService logService;

    public RoleController(RoleService roleService, UserService userService, OperationLogService logService) {
        this.roleService = roleService;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    public String listRoles(Model model, Authentication authentication) {
        List<Role> roles = roleService.getAllRoles();
        model.addAttribute("roles", roles);
        model.addAttribute("username", authentication.getName());
        return "admin/roles";
    }

    @GetMapping("/create")
    public String createRolePage(Model model, Authentication authentication) {
        model.addAttribute("allPermissions", RoleService.ALL_PERMISSIONS);
        model.addAttribute("username", authentication.getName());
        return "admin/role-form";
    }

    @PostMapping("/create")
    public String createRole(@RequestParam String name,
                            @RequestParam String displayName,
                            @RequestParam String description,
                            @RequestParam(required = false) String[] permissions,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Set<String> permSet = permissions != null ? new HashSet<>(Arrays.asList(permissions)) : new HashSet<>();
            roleService.createRole(name, displayName, description, permSet);
            logService.log(getUserId(authentication), "CREATE_ROLE", name, "Created role: " + displayName, null);
            redirectAttributes.addFlashAttribute("success", "角色 " + displayName + " 创建成功");
            return "redirect:/roles";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/roles/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editRolePage(@PathVariable Long id, Model model, Authentication authentication) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return "redirect:/roles";
        }
        model.addAttribute("role", role);
        model.addAttribute("allPermissions", RoleService.ALL_PERMISSIONS);
        model.addAttribute("username", authentication.getName());
        return "admin/role-form";
    }

    @PostMapping("/{id}/edit")
    public String updateRole(@PathVariable Long id,
                            @RequestParam String displayName,
                            @RequestParam String description,
                            @RequestParam(required = false) String[] permissions,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Set<String> permSet = permissions != null ? new HashSet<>(Arrays.asList(permissions)) : new HashSet<>();
            roleService.updateRole(id, displayName, description, permSet);
            logService.log(getUserId(authentication), "UPDATE_ROLE", displayName, "Updated role", null);
            redirectAttributes.addFlashAttribute("success", "角色已更新");
            return "redirect:/roles";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/roles/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRole(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Role role = roleService.getRoleById(id);
            String roleName = role != null ? role.getDisplayName() : "Unknown";
            roleService.deleteRole(id);
            logService.log(getUserId(authentication), "DELETE_ROLE", roleName, "Deleted role", null);
            redirectAttributes.addFlashAttribute("success", "角色已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }

    private Long getUserId(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return user != null ? user.getId() : null;
    }
}
