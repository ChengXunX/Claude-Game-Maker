package com.chengxun.gamemaker.web.constants;

/**
 * 权限常量定义
 * 统一管理系统中所有的权限标识
 *
 * 权限命名规范：
 * - 模块:操作 格式
 * - 模块：user, role, agent, project, token, performance, system
 * - 操作：view, create, edit, delete, manage
 *
 * @author chengxun
 * @since 1.0.0
 */
public final class PermissionConstants {

    private PermissionConstants() {
        // 私有构造函数，防止实例化
    }

    // ===== 用户管理权限 =====

    /** 查看用户列表 */
    public static final String USER_VIEW = "user:view";

    /** 创建用户 */
    public static final String USER_CREATE = "user:create";

    /** 编辑用户 */
    public static final String USER_EDIT = "user:edit";

    /** 删除用户 */
    public static final String USER_DELETE = "user:delete";

    /** 管理用户（包含所有用户操作） */
    public static final String USER_MANAGE = "user:manage";

    // ===== 角色管理权限 =====

    /** 查看角色 */
    public static final String ROLE_VIEW = "role:view";

    /** 创建角色 */
    public static final String ROLE_CREATE = "role:create";

    /** 编辑角色 */
    public static final String ROLE_EDIT = "role:edit";

    /** 删除角色 */
    public static final String ROLE_DELETE = "role:delete";

    /** 管理角色 */
    public static final String ROLE_MANAGE = "role:manage";

    /** 管理角色（复数形式，兼容） */
    public static final String ROLES_MANAGE = "roles:manage";

    // ===== Agent管理权限 =====

    /** 查看Agent */
    public static final String AGENT_VIEW = "agent:view";

    /** 创建Agent */
    public static final String AGENT_CREATE = "agent:create";

    /** 编辑Agent */
    public static final String AGENT_EDIT = "agent:edit";

    /** 删除Agent */
    public static final String AGENT_DELETE = "agent:delete";

    /** 管理Agent */
    public static final String AGENT_MANAGE = "agent:manage";

    // ===== 项目管理权限 =====

    /** 查看项目 */
    public static final String PROJECT_VIEW = "project:view";

    /** 创建项目 */
    public static final String PROJECT_CREATE = "project:create";

    /** 编辑项目 */
    public static final String PROJECT_EDIT = "project:edit";

    /** 删除项目 */
    public static final String PROJECT_DELETE = "project:delete";

    /** 管理项目 */
    public static final String PROJECT_MANAGE = "project:manage";

    // ===== Token管理权限 =====

    /** 查看Token */
    public static final String TOKEN_VIEW = "token:view";

    /** 创建Token */
    public static final String TOKEN_CREATE = "token:create";

    /** 编辑Token */
    public static final String TOKEN_EDIT = "token:edit";

    /** 删除Token */
    public static final String TOKEN_DELETE = "token:delete";

    /** 管理Token */
    public static final String TOKEN_MANAGE = "token:manage";

    // ===== 性能评估权限 =====

    /** 查看性能数据 */
    public static final String PERFORMANCE_VIEW = "performance:view";

    /** 管理性能数据（记录、重置、删除） */
    public static final String PERFORMANCE_MANAGE = "performance:manage";

    // ===== 技能管理权限 =====

    /** 查看技能 */
    public static final String SKILL_VIEW = "skill:view";

    /** 创建技能 */
    public static final String SKILL_CREATE = "skill:create";

    /** 编辑技能 */
    public static final String SKILL_EDIT = "skill:edit";

    /** 删除技能 */
    public static final String SKILL_DELETE = "skill:delete";

    /** 管理技能 */
    public static final String SKILL_MANAGE = "skill:manage";

    // ===== 系统管理权限 =====

    /** 查看系统配置 */
    public static final String SYSTEM_VIEW = "system:view";

    /** 修改系统配置 */
    public static final String SYSTEM_EDIT = "system:edit";

    /** 管理系统（包含所有系统操作） */
    public static final String SYSTEM_MANAGE = "system:manage";

    /** 查看日志 */
    public static final String LOG_VIEW = "log:view";

    /** 导出日志 */
    public static final String LOG_EXPORT = "log:export";

    // ===== 通知权限 =====

    /** 查看通知 */
    public static final String NOTIFICATION_VIEW = "notification:view";

    /** 发送通知 */
    public static final String NOTIFICATION_SEND = "notification:send";

    /** 管理通知 */
    public static final String NOTIFICATION_MANAGE = "notification:manage";

    // ===== 设备管理权限 =====

    /** 查看设备 */
    public static final String DEVICE_VIEW = "device:view";

    /** 管理设备 */
    public static final String DEVICE_MANAGE = "device:manage";

    // ===== 代码管理权限 =====

    /** 查看代码 */
    public static final String CODE_VIEW = "code:view";

    /** 浏览代码仓库 */
    public static final String CODE_BROWSE = "code:browse";

    /** 搜索代码 */
    public static final String CODE_SEARCH = "code:search";

    /** Git操作 */
    public static final String CODE_GIT = "code:git";

    /** 代码审查 */
    public static final String CODE_REVIEW = "code:review";

    // ===== CICD流水线权限 =====

    /** 查看流水线 */
    public static final String PIPELINE_VIEW = "pipeline:view";

    /** 创建流水线 */
    public static final String PIPELINE_CREATE = "pipeline:create";

    /** 管理流水线（编辑、删除） */
    public static final String PIPELINE_MANAGE = "pipeline:manage";

    /** 执行流水线（触发执行） */
    public static final String PIPELINE_EXECUTE = "pipeline:execute";

    /** 审批流水线 */
    public static final String PIPELINE_APPROVE = "pipeline:approve";

    /** 干预流水线（暂停、恢复、取消） */
    public static final String PIPELINE_INTERVENE = "pipeline:intervene";

    // ===== 监控权限 =====

    /** 查看监控数据 */
    public static final String MONITOR_VIEW = "system:monitor";

    /** 管理监控（配置告警规则等） */
    public static final String MONITOR_MANAGE = "system:monitor:manage";

    // ===== 工作流权限 =====

    /** 查看工作流 */
    public static final String WORKFLOW_VIEW = "workflow:view";

    /** 管理工作流 */
    public static final String WORKFLOW_MANAGE = "workflow:manage";

    // ===== AI助手权限 =====

    /** 使用AI助手 */
    public static final String AI_USE = "ai:use";

    /** 管理AI助手 */
    public static final String AI_ADMIN = "ai:admin";

    // ===== 告警权限 =====

    /** 查看告警 */
    public static final String ALERT_VIEW = "alert:view";

    /** 管理告警 */
    public static final String ALERT_MANAGE = "alert:manage";

    // ===== Git权限 =====

    /** 查看Git */
    public static final String GIT_VIEW = "git:view";

    /** 管理Git */
    public static final String GIT_MANAGE = "git:manage";

    // ===== 搜索权限 =====

    /** 搜索 */
    public static final String SEARCH_VIEW = "search:view";

    // ===== 资源权限 =====

    /** 查看资源 */
    public static final String RESOURCE_VIEW = "resource:view";

    // ===== 代码审查权限 =====

    /** 创建代码审查 */
    public static final String REVIEW_CREATE = "review:create";

    /** 查看代码审查 */
    public static final String REVIEW_VIEW = "review:view";

    /** 管理代码审查 */
    public static final String REVIEW_MANAGE = "review:manage";

    // ===== Agents权限（复数形式） =====

    /** 管理Agents */
    public static final String AGENTS_MANAGE = "agents:manage";

    /** Agent任务 */
    public static final String AGENTS_TASK = "agents:task";

    // ===== 管理员权限 =====

    /** 管理员管理 */
    public static final String ADMIN_MANAGE = "admin:manage";

    // ===== 系统配置权限 =====

    /** 系统配置 */
    public static final String SYSTEM_CONFIG = "system:config";

    // ===== 日志权限（复数形式） =====

    /** 查看日志（复数） */
    public static final String LOGS_VIEW = "logs:view";

    // ===== 项目权限（复数形式） =====

    /** 查看项目（复数） */
    public static final String PROJECTS_VIEW = "projects:view";

    /** 管理项目（复数） */
    public static final String PROJECTS_MANAGE = "projects:manage";

    // ===== 技能权限（复数形式） =====

    /** 管理技能（复数） */
    public static final String SKILLS_MANAGE = "skills:manage";

    // ===== 检查点权限 =====

    /** 查看检查点 */
    public static final String CHECKPOINT_VIEW = "checkpoint:view";

    /** 管理检查点（创建、恢复、删除） */
    public static final String CHECKPOINT_MANAGE = "checkpoint:manage";

    // ===== 目标/裁判权限 =====

    /** 查看目标 */
    public static final String GOAL_VIEW = "goal:view";

    /** 管理目标（设置、评估、完成） */
    public static final String GOAL_MANAGE = "goal:manage";

    // ===== 知识提取权限 =====

    /** 执行 Dream 知识提取 */
    public static final String DREAM_EXECUTE = "dream:execute";

    // ===== 子代理权限 =====

    /** 查看子代理 */
    public static final String SUBAGENT_VIEW = "subagent:view";

    /** 创建子代理 */
    public static final String SUBAGENT_CREATE = "subagent:create";

    /** 管理子代理（终止、清理） */
    public static final String SUBAGENT_MANAGE = "subagent:manage";

    // ===== Skill 发现权限 =====

    /** 发现文件系统 Skill */
    public static final String SKILL_DISCOVER = "skill:discover";

    // ===== Distill 工作流发现权限 =====

    /** 执行 Distill 工作流发现 */
    public static final String DISTILL_EXECUTE = "distill:execute";

    // ===== 快照回滚权限 =====

    /** 查看快照 */
    public static final String SNAPSHOT_VIEW = "snapshot:view";

    /** 管理快照（创建、恢复、删除、undo） */
    public static final String SNAPSHOT_MANAGE = "snapshot:manage";

    // ===== 会话分叉权限 =====

    /** 查看会话分叉 */
    public static final String FORK_VIEW = "fork:view";

    /** 创建会话分叉 */
    public static final String FORK_CREATE = "fork:create";

    /** 管理会话分叉（合并、丢弃） */
    public static final String FORK_MANAGE = "fork:manage";

    // ===== 工具权限管理 =====

    /** 管理 Agent 工具权限 */
    public static final String TOOL_PERMISSION_MANAGE = "tool:permission:manage";

    // ===== 多轮推理权限 =====

    /** 查看多轮推理记录 */
    public static final String REASONING_VIEW = "reasoning:view";

    /** 管理多轮推理（触发、配置） */
    public static final String REASONING_MANAGE = "reasoning:manage";

    // ===== 质量预测权限 =====

    /** 查看质量预测 */
    public static final String QUALITY_VIEW = "quality:view";

    /** 执行质量预测 */
    public static final String QUALITY_PREDICT = "quality:predict";

    // ===== 迭代适应权限 =====

    /** 应用迭代策略建议 */
    public static final String ITERATION_ADAPT = "iteration:adapt";

    // ===== 知识图谱权限 =====

    /** 查看和构建知识图谱 */
    public static final String KNOWLEDGE_GRAPH = "knowledge:graph";

    // ===== 游戏验证相关权限 =====

    /** 触发游戏验证 */
    public static final String GAME_VERIFY = "game:verify";

    /** 启动游戏预览 */
    public static final String GAME_PREVIEW = "game:preview";

    /** 查看验证结果 */
    public static final String GAME_VERIFY_VIEW = "game:verify:view";

    /** G8 新增：查看游戏视觉验证（截图、视觉分析） */
    public static final String GAME_VISUAL_VIEW = "game:visual:view";

    // ===== 特殊权限 =====

    /** 超级管理员权限（包含所有权限） */
    public static final String ADMIN = "*";

    /**
     * 检查权限是否有效
     * @param permission 权限标识
     * @return 是否有效
     */
    public static boolean isValidPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }

        // 超级管理员权限
        if (ADMIN.equals(permission)) {
            return true;
        }

        // 检查是否是已定义的权限
        return permission.matches("^[a-z]+:[a-z]+$");
    }

    /**
     * 获取权限的显示名称
     * @param permission 权限标识
     * @return 显示名称
     */
    public static String getPermissionDisplayName(String permission) {
        if (permission == null) {
            return "";
        }

        return switch (permission) {
            case ADMIN -> "超级管理员";
            case USER_VIEW -> "查看用户";
            case USER_CREATE -> "创建用户";
            case USER_EDIT -> "编辑用户";
            case USER_DELETE -> "删除用户";
            case USER_MANAGE -> "管理用户";
            case ROLE_VIEW -> "查看角色";
            case ROLE_CREATE -> "创建角色";
            case ROLE_EDIT -> "编辑角色";
            case ROLE_DELETE -> "删除角色";
            case ROLE_MANAGE -> "管理角色";
            case AGENT_VIEW -> "查看Agent";
            case AGENT_CREATE -> "创建Agent";
            case AGENT_EDIT -> "编辑Agent";
            case AGENT_DELETE -> "删除Agent";
            case AGENT_MANAGE -> "管理Agent";
            case PROJECT_VIEW -> "查看项目";
            case PROJECT_CREATE -> "创建项目";
            case PROJECT_EDIT -> "编辑项目";
            case PROJECT_DELETE -> "删除项目";
            case PROJECT_MANAGE -> "管理项目";
            case TOKEN_VIEW -> "查看Token";
            case TOKEN_CREATE -> "创建Token";
            case TOKEN_EDIT -> "编辑Token";
            case TOKEN_DELETE -> "删除Token";
            case TOKEN_MANAGE -> "管理Token";
            case PERFORMANCE_VIEW -> "查看性能评估";
            case PERFORMANCE_MANAGE -> "管理性能评估";
            case SKILL_VIEW -> "查看技能";
            case SKILL_CREATE -> "创建技能";
            case SKILL_EDIT -> "编辑技能";
            case SKILL_DELETE -> "删除技能";
            case SKILL_MANAGE -> "管理技能";
            case SYSTEM_VIEW -> "查看系统配置";
            case SYSTEM_EDIT -> "修改系统配置";
            case SYSTEM_MANAGE -> "管理系统";
            case SYSTEM_CONFIG -> "系统配置";
            case LOG_VIEW -> "查看日志";
            case LOG_EXPORT -> "导出日志";
            case LOGS_VIEW -> "查看日志";
            case NOTIFICATION_VIEW -> "查看通知";
            case NOTIFICATION_SEND -> "发送通知";
            case NOTIFICATION_MANAGE -> "管理通知";
            case DEVICE_VIEW -> "查看设备";
            case DEVICE_MANAGE -> "管理设备";
            case CODE_VIEW -> "查看代码";
            case CODE_BROWSE -> "浏览代码";
            case CODE_SEARCH -> "搜索代码";
            case CODE_GIT -> "Git操作";
            case CODE_REVIEW -> "代码审查";
            case PIPELINE_VIEW -> "查看流水线";
            case PIPELINE_CREATE -> "创建流水线";
            case PIPELINE_MANAGE -> "管理流水线";
            case PIPELINE_EXECUTE -> "执行流水线";
            case PIPELINE_APPROVE -> "审批流水线";
            case PIPELINE_INTERVENE -> "干预流水线";
            case MONITOR_VIEW -> "查看监控";
            case MONITOR_MANAGE -> "管理监控";
            case WORKFLOW_VIEW -> "查看工作流";
            case WORKFLOW_MANAGE -> "管理工作流";
            case AI_USE -> "使用AI助手";
            case AI_ADMIN -> "管理AI助手";
            case ALERT_VIEW -> "查看告警";
            case ALERT_MANAGE -> "管理告警";
            case GIT_VIEW -> "查看Git";
            case GIT_MANAGE -> "管理Git";
            case SEARCH_VIEW -> "搜索";
            case RESOURCE_VIEW -> "查看资源";
            case REVIEW_CREATE -> "创建审查";
            case REVIEW_VIEW -> "查看审查";
            case REVIEW_MANAGE -> "管理审查";
            case AGENTS_MANAGE -> "管理Agents";
            case AGENTS_TASK -> "Agent任务";
            case ADMIN_MANAGE -> "管理员管理";
            case PROJECTS_VIEW -> "查看项目";
            case PROJECTS_MANAGE -> "管理项目";
            case SKILLS_MANAGE -> "管理技能";
            case ROLES_MANAGE -> "管理角色";
            case CHECKPOINT_VIEW -> "查看检查点";
            case CHECKPOINT_MANAGE -> "管理检查点";
            case GOAL_VIEW -> "查看目标";
            case GOAL_MANAGE -> "管理目标";
            case DREAM_EXECUTE -> "执行知识提取";
            case SUBAGENT_VIEW -> "查看子代理";
            case SUBAGENT_CREATE -> "创建子代理";
            case SUBAGENT_MANAGE -> "管理子代理";
            case SKILL_DISCOVER -> "发现Skill";
            case DISTILL_EXECUTE -> "执行工作流发现";
            case SNAPSHOT_VIEW -> "查看快照";
            case SNAPSHOT_MANAGE -> "管理快照";
            case FORK_VIEW -> "查看会话分叉";
            case FORK_CREATE -> "创建会话分叉";
            case FORK_MANAGE -> "管理会话分叉";
            case TOOL_PERMISSION_MANAGE -> "管理工具权限";
            case GAME_VERIFY -> "触发游戏验证";
            case GAME_PREVIEW -> "启动游戏预览";
            case GAME_VERIFY_VIEW -> "查看验证结果";
            case GAME_VISUAL_VIEW -> "查看游戏视觉验证";
            default -> permission;
        };
    }

    /**
     * 获取权限所属模块
     * @param permission 权限标识
     * @return 模块名称
     */
    public static String getPermissionModule(String permission) {
        if (permission == null || permission.isEmpty()) {
            return "";
        }

        int colonIndex = permission.indexOf(':');
        if (colonIndex > 0) {
            String module = permission.substring(0, colonIndex);
            return switch (module) {
                case "user" -> "用户管理";
                case "role" -> "角色管理";
                case "agent" -> "Agent管理";
                case "agents" -> "Agent管理";
                case "project" -> "项目管理";
                case "projects" -> "项目管理";
                case "token" -> "Token管理";
                case "performance" -> "性能评估";
                case "skill" -> "技能管理";
                case "skills" -> "技能管理";
                case "system" -> "系统管理";
                case "admin" -> "管理员";
                case "log" -> "日志管理";
                case "logs" -> "日志管理";
                case "notification" -> "通知管理";
                case "device" -> "设备管理";
                case "code" -> "代码管理";
                case "git" -> "Git管理";
                case "pipeline" -> "CICD流水线";
                case "workflow" -> "工作流";
                case "ai" -> "AI助手";
                case "alert" -> "告警管理";
                case "monitor" -> "监控";
                case "resource" -> "资源管理";
                case "review" -> "代码审查";
                case "search" -> "搜索";
                case "checkpoint" -> "检查点管理";
                case "goal" -> "目标管理";
                case "dream" -> "知识提取";
                case "subagent" -> "子代理管理";
                case "distill" -> "工作流发现";
                case "snapshot" -> "快照管理";
                case "fork" -> "会话分叉";
                case "tool" -> "工具权限";
                case "game" -> "游戏验证";
                default -> module;
            };
        }

        return "其他";
    }
}
