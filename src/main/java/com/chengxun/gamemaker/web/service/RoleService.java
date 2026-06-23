package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.repository.RoleRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
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

    // CICD流水线权限
    public static final String PERM_PIPELINE_VIEW = "pipeline:view";
    public static final String PERM_PIPELINE_CREATE = "pipeline:create";
    public static final String PERM_PIPELINE_MANAGE = "pipeline:manage";
    public static final String PERM_PIPELINE_EXECUTE = "pipeline:execute";
    public static final String PERM_PIPELINE_APPROVE = "pipeline:approve";
    public static final String PERM_PIPELINE_INTERVENE = "pipeline:intervene";

    // 监控权限
    public static final String PERM_MONITOR_VIEW = "system:monitor";
    public static final String PERM_MONITOR_MANAGE = "system:monitor:manage";

    // 工作流权限
    public static final String PERM_WORKFLOW_VIEW = "workflow:view";
    public static final String PERM_WORKFLOW_MANAGE = "workflow:manage";

    // 代码审查权限
    public static final String PERM_CODE_REVIEW = "code:review";

    // 通知权限
    public static final String PERM_NOTIFICATION_VIEW = "notification:view";
    public static final String PERM_NOTIFICATION_MANAGE = "notification:manage";

    // Token 权限
    public static final String PERM_TOKENS_VIEW = "tokens:view";

    // 审批权限
    public static final String PERM_APPROVAL_VIEW = "approval:view";

    // 系统查看权限
    public static final String PERM_SYSTEM_VIEW = "system:view";

    // AI 助手权限
    public static final String PERM_AI_USE = "ai:use";

    // 终端权限
    public static final String PERM_TERMINAL_USE = "terminal:use";

    // 游戏验证权限
    public static final String PERM_GAME_VERIFY = "game:verify";
    public static final String PERM_GAME_PREVIEW = "game:preview";
    public static final String PERM_GAME_VERIFY_VIEW = "game:verify:view";

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
        PERM_LOGS_VIEW,
        // CICD流水线权限
        PERM_PIPELINE_VIEW,
        PERM_PIPELINE_CREATE,
        PERM_PIPELINE_MANAGE,
        PERM_PIPELINE_EXECUTE,
        PERM_PIPELINE_APPROVE,
        PERM_PIPELINE_INTERVENE,
        // 监控权限
        PERM_MONITOR_VIEW,
        PERM_MONITOR_MANAGE,
        // 工作流权限
        PERM_WORKFLOW_VIEW,
        PERM_WORKFLOW_MANAGE,
        // 代码审查权限
        PERM_CODE_REVIEW,
        // 通知权限
        PERM_NOTIFICATION_VIEW,
        PERM_NOTIFICATION_MANAGE,
        // Token 权限
        PERM_TOKENS_VIEW,
        // 审批权限
        PERM_APPROVAL_VIEW,
        // 系统查看权限
        PERM_SYSTEM_VIEW,
        // AI 助手权限
        PERM_AI_USE,
        // 终端权限
        PERM_TERMINAL_USE,
        // 游戏验证权限
        PERM_GAME_VERIFY,
        PERM_GAME_PREVIEW,
        PERM_GAME_VERIFY_VIEW
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
            // CICD流水线权限
            case PERM_PIPELINE_VIEW -> "查看流水线";
            case PERM_PIPELINE_CREATE -> "创建流水线";
            case PERM_PIPELINE_MANAGE -> "管理流水线";
            case PERM_PIPELINE_EXECUTE -> "执行流水线";
            case PERM_PIPELINE_APPROVE -> "审批流水线";
            case PERM_PIPELINE_INTERVENE -> "干预流水线";
            // 监控权限
            case PERM_MONITOR_VIEW -> "查看监控";
            case PERM_MONITOR_MANAGE -> "管理监控";
            // 工作流权限
            case PERM_WORKFLOW_VIEW -> "查看工作流";
            case PERM_WORKFLOW_MANAGE -> "管理工作流";
            // 代码审查权限
            case PERM_CODE_REVIEW -> "代码审查";
            // 通知管理权限
            case PERM_NOTIFICATION_VIEW -> "查看通知";
            case PERM_NOTIFICATION_MANAGE -> "管理通知模板";
            // Token 权限
            case PERM_TOKENS_VIEW -> "查看 Token";
            // 审批权限
            case PERM_APPROVAL_VIEW -> "查看审批";
            // 系统查看权限
            case PERM_SYSTEM_VIEW -> "查看系统信息";
            // AI 助手权限
            case PERM_AI_USE -> "使用 AI 助手";
            // 终端权限
            case PERM_TERMINAL_USE -> "使用系统终端";
            // 游戏验证权限
            case PERM_GAME_VERIFY -> "触发游戏验证";
            case PERM_GAME_PREVIEW -> "启动游戏预览";
            case PERM_GAME_VERIFY_VIEW -> "查看验证结果";
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

        // 项目经理 - 管理项目、Agent和流水线
        createRoleIfNotExists("PROJECT_MANAGER", "项目经理", "负责项目管理和 Agent 调度", false,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_AGENTS_MANAGE,
                PERM_AGENTS_TASK,
                PERM_PROJECTS_VIEW,
                PERM_PROJECTS_MANAGE,
                PERM_PROJECTS_EDIT,
                PERM_SKILLS_VIEW,
                // CICD流水线权限
                PERM_PIPELINE_VIEW,
                PERM_PIPELINE_CREATE,
                PERM_PIPELINE_MANAGE,
                PERM_PIPELINE_EXECUTE,
                PERM_PIPELINE_APPROVE,
                PERM_PIPELINE_INTERVENE,
                // 监控权限
                PERM_MONITOR_VIEW,
                // 工作流权限
                PERM_WORKFLOW_VIEW,
                PERM_WORKFLOW_MANAGE,
                // 代码审查权限
                PERM_CODE_REVIEW,
                // 通知管理权限
                PERM_NOTIFICATION_MANAGE,
                // AI 助手权限
                PERM_AI_USE
            )));

        // 开发者 - 查看和使用 Agent，执行流水线
        createRoleIfNotExists("DEVELOPER", "开发者", "使用 Agent 进行开发工作", false,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_AGENTS_TASK,
                PERM_PROJECTS_VIEW,
                PERM_SKILLS_VIEW,
                // CICD流水线权限
                PERM_PIPELINE_VIEW,
                PERM_PIPELINE_EXECUTE,
                // 代码审查权限
                PERM_CODE_REVIEW,
                // AI 助手权限
                PERM_AI_USE,
                // 游戏验证权限
                PERM_GAME_VERIFY,
                PERM_GAME_VERIFY_VIEW
            )));

        // 运维工程师 - 管理流水线和监控
        createRoleIfNotExists("OPS_ENGINEER", "运维工程师", "负责系统运维和部署", false,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_PROJECTS_VIEW,
                // CICD流水线权限
                PERM_PIPELINE_VIEW,
                PERM_PIPELINE_CREATE,
                PERM_PIPELINE_MANAGE,
                PERM_PIPELINE_EXECUTE,
                PERM_PIPELINE_APPROVE,
                PERM_PIPELINE_INTERVENE,
                // 监控权限
                PERM_MONITOR_VIEW,
                PERM_MONITOR_MANAGE,
                // 工作流权限
                PERM_WORKFLOW_VIEW,
                PERM_WORKFLOW_MANAGE
            )));

        // 观察者 - 只读权限
        createRoleIfNotExists("OBSERVER", "观察者", "只读权限，查看系统状态", false,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_PROJECTS_VIEW,
                PERM_SKILLS_VIEW,
                // CICD流水线权限（只读）
                PERM_PIPELINE_VIEW,
                // 监控权限（只读）
                PERM_MONITOR_VIEW,
                // AI 助手权限
                PERM_AI_USE
            )));

        // 普通用户 - 基础权限
        createRoleIfNotExists("USER", "普通用户", "普通用户，基础权限", false,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_PROJECTS_VIEW
            )));

        // 只读访客 - 所有模块只读，不可修改
        createRoleIfNotExists("READONLY", "只读访客", "只读权限，可查看所有模块但不能修改，供外部人员了解系统特性", false,
            new HashSet<>(Arrays.asList(
                PERM_DASHBOARD_VIEW,
                PERM_AGENTS_VIEW,
                PERM_AI_USE,
                PERM_PROJECTS_VIEW,
                PERM_SKILLS_VIEW,
                PERM_TOKENS_VIEW,
                PERM_NOTIFICATION_VIEW,
                PERM_CODE_REVIEW,
                PERM_PIPELINE_VIEW,
                PERM_WORKFLOW_VIEW,
                PERM_APPROVAL_VIEW,
                PERM_USERS_VIEW,
                PERM_LOGS_VIEW,
                PERM_SYSTEM_VIEW,
                PERM_MONITOR_VIEW
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

    /**
     * 根据角色名获取角色
     * 使用缓存提高查询效率
     *
     * @param name 角色名称
     * @return 角色信息，不存在返回null
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#name")
    public Role getRoleByName(String name) {
        Role role = roleRepository.findByName(name).orElse(null);
        if (role != null) {
            // 强制初始化懒加载的permissions集合，确保缓存序列化时不会出错
            Hibernate.initialize(role.getPermissions());
            // 创建新Set避免代理对象问题
            role.setPermissions(new HashSet<>(role.getPermissions()));
        }
        return role;
    }

    /**
     * 获取所有角色
     * 使用缓存提高查询效率
     *
     * @return 角色列表
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "'all'")
    public List<Role> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        // 强制初始化每个角色的permissions集合，确保缓存序列化时不会出错
        for (Role role : roles) {
            Hibernate.initialize(role.getPermissions());
            role.setPermissions(new HashSet<>(role.getPermissions()));
        }
        return roles;
    }

    /**
     * 根据ID获取角色
     * 使用缓存提高查询效率
     *
     * @param id 角色ID
     * @return 角色信息，不存在返回null
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#id")
    public Role getRoleById(Long id) {
        Role role = roleRepository.findById(id).orElse(null);
        if (role != null) {
            // 强制初始化懒加载的permissions集合，确保缓存序列化时不会出错
            Hibernate.initialize(role.getPermissions());
            role.setPermissions(new HashSet<>(role.getPermissions()));
        }
        return role;
    }

    /**
     * 创建新角色
     * 创建成功后清除角色缓存
     *
     * @param name 角色名称
     * @param displayName 显示名称
     * @param description 角色描述
     * @param permissions 权限集合
     * @return 创建的角色
     * @throws RuntimeException 当角色名已存在时抛出
     */
    @CacheEvict(value = "roles", allEntries = true)
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

    /**
     * 更新角色信息
     * 更新成功后清除角色缓存
     *
     * @param id 角色ID
     * @param displayName 显示名称
     * @param description 角色描述
     * @param permissions 权限集合
     * @return 更新后的角色
     * @throws RuntimeException 当角色不存在或为系统内置角色时抛出
     */
    @CacheEvict(value = "roles", allEntries = true)
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

    /**
     * 保存角色（用于更新权重等字段）
     *
     * @param role 角色实体
     * @return 保存后的角色
     */
    @CacheEvict(value = "roles", allEntries = true)
    public Role saveRole(Role role) {
        Role saved = roleRepository.save(role);
        log.info("Role saved: {}", role.getName());
        return saved;
    }

    /**
     * 删除角色
     * 删除成功后清除角色缓存
     *
     * @param id 角色ID
     * @throws RuntimeException 当角色不存在或为系统内置角色时抛出
     */
    @CacheEvict(value = "roles", allEntries = true)
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));

        if (role.isSystem()) {
            throw new RuntimeException("系统内置角色不可删除");
        }

        roleRepository.delete(role);
        log.info("Role deleted: {}", role.getName());
    }

    /**
     * 获取所有角色的所有权限集合（从数据库读取）
     * 用于通配符权限 * 的展开，确保管理员拥有所有已定义的权限
     *
     * @return 所有权限的集合
     */
    public Set<String> getAllPermissionsFromDatabase() {
        Set<String> allPermissions = new HashSet<>();
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            if (role.getPermissions() != null) {
                allPermissions.addAll(role.getPermissions());
            }
        }
        // 移除通配符 *，避免循环
        allPermissions.remove("*");
        log.debug("从数据库读取到 {} 个权限定义", allPermissions.size());
        return allPermissions;
    }
}
