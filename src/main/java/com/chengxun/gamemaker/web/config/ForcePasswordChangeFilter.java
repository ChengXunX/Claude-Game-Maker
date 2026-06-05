package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 强制修改密码过滤器
 * 当用户的mustChangePassword标志为true时，强制跳转到修改密码页面
 * 只允许访问修改密码相关的接口
 */
@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    /** 不需要强制修改密码就能访问的精确路径 */
    private static final Set<String> ALLOWED_EXACT_PATHS = Set.of(
        "/profile/password",
        "/profile/change-password",
        "/logout"
    );

    /** 不需要强制修改密码就能访问的路径前缀（必须以/结尾） */
    private static final Set<String> ALLOWED_PATH_PREFIXES = Set.of(
        "/css/",
        "/js/",
        "/images/"
    );

    private final UserService userService;

    public ForcePasswordChangeFilter(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            String username = authentication.getName();
            User user = userService.getUserByUsername(username);

            if (user != null && user.isMustChangePassword()) {
                String path = request.getRequestURI();

                // 检查是否是允许的路径（精确匹配或前缀匹配）
                boolean isAllowed = ALLOWED_EXACT_PATHS.contains(path) ||
                    ALLOWED_PATH_PREFIXES.stream().anyMatch(path::startsWith);

                if (!isAllowed) {
                    // 强制跳转到修改密码页面
                    response.sendRedirect("/profile/change-password?forced=true");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
