package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    // 权限常量
    public static final String PERM_DASHBOARD_VIEW = "dashboard:view";
    public static final String PERM_AGENTS_VIEW = "agents:view";
    public static final String PERM_AGENTS_MANAGE = "agents:manage";
    public static final String PERM_AGENTS_TASK = "agents:task";
    public static final String PERM_PROJECTS_VIEW = "projects:view";
    public static final String PERM_PROJECTS_MANAGE = "projects:manage";
    public static final String PERM_PROJECTS_EDIT = "projects:edit";
    public static final String PERM_SKILLS_VIEW = "skills:view";
    public static final String PERM_SKILLS_MANAGE = "skills:manage";
    public static final String PERM_USERS_VIEW = "users:view";
    public static final String PERM_USERS_MANAGE = "users:manage";
    public static final String PERM_ROLES_MANAGE = "roles:manage";
    public static final String PERM_LOGS_VIEW = "logs:view";
    public static final String PERM_ALL = "*";

    // 所有可用权限
    public static final List<String> ALL_PERMISSIONS = Arrays.asList(
        PERM_DASHBOARD_VIEW,
        PERM_AGENTS_VIEW,
        PERM_AGENTS_MANAGE,
        PERM_AGENTS_TASK,
        PERM_PROJECTS_VIEW,
        PERM_PROJECTS_MANAGE,
        PERM_PROJECTS_EDIT,
        PERM_SKILLS_VIEW,
        PERM_SKILLS_MANAGE,
        PERM_USERS_VIEW,
        PERM_USERS_MANAGE,
        PERM_ROLES_MANAGE,
        PERM_LOGS_VIEW
    );

    // 权限描述
    public static final String getPermissionDescription(String permission) {
        return switch (permission) {
            case PERM_DASHBOARD_VIEW -> "查看仪表盘";
            case PERM_AGENTS_VIEW -> "查看 Agent 列表";
            case PERM_AGENTS_MANAGE -> "管理 Agent";
            case PERM_AGENTS_TASK -> "发送任务给 Agent";
            case PERM_PROJECTS_VIEW -> "查看项目列表";
            case PERM_PROJECTS_MANAGE -> "管理项目";
            case PERM_PROJECTS_EDIT -> "编辑项目配置";
            case PERM_SKILLS_VIEW -> "查看技能列表";
            case PERM_SKILLS_MANAGE -> "管理技能";
            case PERM_USERS_VIEW -> "查看用户列表";
            case PERM_USERS_MANAGE -> "管理用户";
            case PERM_ROLES_MANAGE -> "管理角色";
            case PERM_LOGS_VIEW -> "查看操作日志";
            case PERM_ALL -> "所有权限";
            default -> permission;
        };
    }

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void initDefaultRoles() {
        // 超级管理员 - 拥有所有权限
        createRoleIfNotExists("ADMIN", "超级管理员", "系统超级管理员，拥有所有权限", true,
            new HashSet<>(Arrays.asList(PERM_ALL)));

        // 项目经理 - 管理项目和 Agent
        createRoleIfNotExists("PROJECT_MANAGER", "项目经理", "负责项目管理和 Agent 调度", true,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_AGENTS_MANAGE,
                PERM_AGENTS_TASK,
                PERM_PROJECTS_VIEW,
                PERM_PROJECTS_MANAGE,
                PERM_PROJECTS_EDIT,
                PERM_SKILLS_VIEW
            )));

        // 开发者 - 查看和使用 Agent
        createRoleIfNotExists("DEVELOPER", "开发者", "使用 Agent 进行开发工作", true,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_AGENTS_TASK,
                PERM_PROJECTS_VIEW,
                PERM_SKILLS_VIEW
            )));

        // 观察者 - 只读权限
        createRoleIfNotExists("OBSERVER", "观察者", "只读权限，查看系统状态", true,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_PROJECTS_VIEW,
                PERM_SKILLS_VIEW
            )));

        // 普通用户 - 基础权限
        createRoleIfNotExists("USER", "普通用户", "普通用户，基础权限", true,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_PROJECTS_VIEW
            )));

        log.info("Default roles initialized");
    }

    private void createRoleIfNotExists(String name, String displayName, String description,
                                       boolean system, Set<String> permissions) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role();
            role.setName(name);
            role.setDisplayName(displayName);
            role.setDescription(description);
            role.setSystem(system);
            role.setPermissions(permissions);
            roleRepository.save(role);
            log.info("Role created: {}", name);
        }
    }

    public Role getRoleByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    public Role createRole(String name, String displayName, String description, Set<String> permissions) {
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("角色名已存在: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDisplayName(displayName);
        role.setDescription(description);
        role.setSystem(false);
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        log.info("Role created: {}", name);
        return saved;
    }

    public Role updateRole(Long id, String displayName, String description, Set<String> permissions) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));

        if (role.isSystem()) {
            throw new RuntimeException("系统内置角色不可修改");
        }

        role.setDisplayName(displayName);
        role.setDescription(description);
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        log.info("Role updated: {}", role.getName());
        return saved;
    }

    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));

        if (role.isSystem()) {
            throw new RuntimeException("系统内置角色不可删除");
        }

        roleRepository.delete(role);
        log.info("Role deleted: {}", role.getName());
    }
}
