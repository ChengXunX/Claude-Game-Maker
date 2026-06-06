package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器
 * 负责拦截请求，验证 JWT Token，并设置认证信息到 SecurityContext
 *
 * 主要功能：
 * - 从请求头中提取 JWT Token
 * - 验证 Token 的有效性
 * - 解析 Token 获取用户信息
 * - 设置认证信息到 SecurityContext
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final RoleService roleService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, @Lazy RoleService roleService, @Lazy UserService userService) {
        this.jwtUtils = jwtUtils;
        this.roleService = roleService;
        this.userService = userService;
    }

    /**
     * 执行过滤逻辑
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 内部 API 路径跳过 JWT 认证
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(JwtUtils.HEADER_STRING);

        // 如果没有 Authorization 头，尝试从 URL 参数获取 token（支持 SSE）
        if (authHeader == null || !authHeader.startsWith(JwtUtils.TOKEN_PREFIX)) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isEmpty()) {
                authHeader = JwtUtils.TOKEN_PREFIX + tokenParam;
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        }

        try {
            // 提取 Token
            String token = jwtUtils.extractToken(authHeader);

            if (token == null) {
                log.warn("JWT Token 格式错误: {}", authHeader);
                filterChain.doFilter(request, response);
                return;
            }

            // 验证 Token
            if (!jwtUtils.validateToken(token)) {
                log.warn("JWT Token 验证失败");
                filterChain.doFilter(request, response);
                return;
            }

            // 从 Token 中获取用户信息
            String username = jwtUtils.getUsernameFromToken(token);
            Long userId = jwtUtils.getUserIdFromToken(token);
            String role = jwtUtils.getRoleFromToken(token);

            if (username == null || userId == null) {
                log.warn("JWT Token 解析失败: 无法获取用户信息");
                filterChain.doFilter(request, response);
                return;
            }

            // 检查用户状态是否为已禁用
            try {
                User user = userService.getUserByUsername(username);
                if (user == null || user.getStatus() == User.UserStatus.DISABLED) {
                    log.warn("用户已被禁用或不存在: {}", username);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                log.warn("检查用户状态失败: {}", e.getMessage());
            }

            // 构建权限列表
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (role != null && !role.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                // 从数据库加载用户的实际权限（PERM_xxx）
                try {
                    Role roleEntity = roleService.getRoleByName(role);
                    if (roleEntity != null && roleEntity.getPermissions() != null) {
                        Set<String> permissions = roleEntity.getPermissions();
                        for (String perm : permissions) {
                            authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                        }
                        // 如果有通配符权限 *，添加所有具体权限
                        if (permissions.contains("*")) {
                            addAllPermissions(authorities);
                        }
                    }
                } catch (Exception e) {
                    log.warn("加载用户权限失败: {}", e.getMessage());
                }
            }

            // 创建认证对象
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            // 设置用户 ID 到 details 中，方便后续使用
            authentication.setDetails(userId);

            // 设置认证信息到 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 认证成功: username={}, userId={}, role={}", username, userId, role);

        } catch (Exception e) {
            log.error("JWT 认证异常: {}", e.getMessage());
            // 认证失败，清除 SecurityContext
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 添加所有具体权限
     * 当用户拥有通配符权限 * 时，需要添加所有具体权限以支持 hasAuthority 检查
     *
     * @param authorities 权限列表
     */
    private void addAllPermissions(List<SimpleGrantedAuthority> authorities) {
        String[] allPermissions = {
            "PERM_agents:manage", "PERM_agents:view", "PERM_agents:task",
            "PERM_skills:view", "PERM_skills:manage",
            "PERM_tokens:view", "PERM_tokens:manage",
            "PERM_projects:view", "PERM_projects:manage",
            "PERM_system:monitor", "PERM_system:manage", "PERM_system:view",
            "PERM_system:config", "PERM_system:config:manage", "PERM_system:monitor:manage",
            "PERM_ai:use", "PERM_ai:admin",
            "PERM_approval:view", "PERM_approval:manage",
            "PERM_notification:view", "PERM_notification:manage",
            "PERM_admin:manage",
            "PERM_roles:manage",
            "PERM_users:manage",
            "PERM_logs:view", "PERM_log:view",
            "PERM_code:review",
            "PERM_pipeline:view", "PERM_pipeline:manage", "PERM_pipeline:execute",
            "PERM_pipeline:approve", "PERM_pipeline:intervene",
            "PERM_workflow:view", "PERM_workflow:manage",
            "PERM_dashboard:view",
            "PERM_terminal:use",
            "PERM_knowledge:manage",
            "PERM_agent:view", "PERM_agent:manage"
        };
        for (String perm : allPermissions) {
            if (!authorities.contains(new SimpleGrantedAuthority(perm))) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }
    }
}
