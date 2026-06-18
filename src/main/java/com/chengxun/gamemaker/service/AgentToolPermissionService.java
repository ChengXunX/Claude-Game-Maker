package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Agent 工具权限服务
 * 为每个 Agent 提供细粒度的工具 allow/deny/ask 权限控制
 *
 * 权限模型：
 * - allow：允许使用，无需确认
 * - deny：禁止使用
 * - ask：每次使用前需确认
 *
 * 支持 glob 模式匹配：
 * - bash:git push: ask
 * - bash:rm *: deny
 * - read:*: allow
 * - edit:*: allow
 *
 * 灵感来源：Agent 工具权限控制机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class AgentToolPermissionService {

    private static final Logger log = LoggerFactory.getLogger(AgentToolPermissionService.class);

    /** Agent 权限配置缓存：key=agentId, value=权限列表 */
    private final ConcurrentHashMap<String, List<ToolPermission>> permissionCache = new ConcurrentHashMap<>();

    /** 默认权限（未配置时使用） */
    private static final List<ToolPermission> DEFAULT_PERMISSIONS = List.of(
        new ToolPermission("bash", "*", "ask"),
        new ToolPermission("read", "*", "allow"),
        new ToolPermission("write", "*", "ask"),
        new ToolPermission("edit", "*", "allow"),
        new ToolPermission("grep", "*", "allow"),
        new ToolPermission("glob", "*", "allow"),
        new ToolPermission("search", "*", "allow"),
        new ToolPermission("deploy", "*", "ask"),
        new ToolPermission("test", "*", "allow"),
        new ToolPermission("build", "*", "allow"),
        new ToolPermission("commit", "*", "ask"),
        new ToolPermission("push", "*", "ask")
    );

    @Autowired
    private SystemConfigService configService;

    /**
     * 检查工具是否被允许
     *
     * @param agentId Agent ID
     * @param toolName 工具名称
     * @param toolArgs 工具参数（用于 bash 命令的细粒度匹配）
     * @return 权限决定
     */
    public PermissionDecision checkPermission(String agentId, String toolName, String toolArgs) {
        List<ToolPermission> permissions = getPermissions(agentId);

        // 从最高优先级开始检查（最后添加的优先级最高）
        for (int i = permissions.size() - 1; i >= 0; i--) {
            ToolPermission perm = permissions.get(i);
            if (matchesTool(perm, toolName, toolArgs)) {
                return switch (perm.action()) {
                    case "allow" -> PermissionDecision.allow(toolName);
                    case "deny" -> PermissionDecision.deny(toolName, "权限拒绝: " + perm.pattern());
                    case "ask" -> PermissionDecision.ask(toolName, "需要确认: " + perm.pattern());
                    default -> PermissionDecision.ask(toolName, "未知操作，默认询问");
                };
            }
        }

        // 默认询问
        return PermissionDecision.ask(toolName, "未配置权限，默认询问");
    }

    /**
     * 设置 Agent 的工具权限
     *
     * @param agentId Agent ID
     * @param permissions 权限列表
     */
    public void setPermissions(String agentId, List<ToolPermission> permissions) {
        permissionCache.put(agentId, new ArrayList<>(permissions));
        log.info("已更新 Agent 工具权限: agentId={}, count={}", agentId, permissions.size());
    }

    /**
     * 添加单条权限规则
     *
     * @param agentId Agent ID
     * @param toolName 工具名称
     * @param pattern 参数模式（glob）
     * @param action 操作（allow/deny/ask）
     */
    public void addPermission(String agentId, String toolName, String pattern, String action) {
        List<ToolPermission> permissions = getPermissions(agentId);
        permissions.add(new ToolPermission(toolName, pattern, action));
        permissionCache.put(agentId, permissions);
    }

    /**
     * 获取 Agent 的权限列表
     *
     * @param agentId Agent ID
     * @return 权限列表
     */
    public List<ToolPermission> getPermissions(String agentId) {
        return permissionCache.computeIfAbsent(agentId, k -> new ArrayList<>(DEFAULT_PERMISSIONS));
    }

    /**
     * 清除 Agent 的权限缓存
     *
     * @param agentId Agent ID
     */
    public void clearPermissions(String agentId) {
        permissionCache.remove(agentId);
    }

    /**
     * 检查工具是否匹配权限规则
     */
    private boolean matchesTool(ToolPermission perm, String toolName, String toolArgs) {
        // 检查工具名
        if (!"*".equals(perm.toolName()) && !perm.toolName().equalsIgnoreCase(toolName)) {
            return false;
        }

        // 检查参数模式（glob 匹配）
        if ("*".equals(perm.pattern())) {
            return true;
        }

        if (toolArgs == null || toolArgs.isEmpty()) {
            return "*".equals(perm.pattern());
        }

        // 简化 glob 匹配：* 匹配任意字符
        String regex = perm.pattern()
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return Pattern.matches(regex, toolArgs);
    }

    /**
     * 从 AgentDefinition 加载权限配置
     *
     * @param definition Agent 定义
     */
    public void loadFromDefinition(AgentDefinition definition) {
        if (definition == null || definition.getId() == null) return;

        // 从 AgentDefinition 的 tags 字段读取权限配置
        // 如果没有配置，使用默认权限
        Object permConfig = definition.getTags() != null ?
            definition.getTags().get("toolPermissions") : null;

        if (permConfig instanceof List<?> permList) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> typedList = (List<Map<String, String>>) (List<?>) permList;
            List<ToolPermission> permissions = new ArrayList<>();
            for (Map<String, String> map : typedList) {
                String tool = map.getOrDefault("tool", "*");
                String pattern = map.getOrDefault("pattern", "*");
                String action = map.getOrDefault("action", "ask");
                permissions.add(new ToolPermission(tool, pattern, action));
            }
            if (!permissions.isEmpty()) {
                setPermissions(definition.getId(), permissions);
            }
        }
    }

    // ===== 内部类 =====

    /**
     * 工具权限规则
     */
    public record ToolPermission(String toolName, String pattern, String action) {}

    /**
     * 权限决定
     */
    public record PermissionDecision(boolean allowed, boolean needsConfirmation,
                                      String toolName, String reason) {
        static PermissionDecision allow(String toolName) {
            return new PermissionDecision(true, false, toolName, "允许");
        }

        static PermissionDecision deny(String toolName, String reason) {
            return new PermissionDecision(false, false, toolName, reason);
        }

        static PermissionDecision ask(String toolName, String reason) {
            return new PermissionDecision(true, true, toolName, reason);
        }
    }
}
