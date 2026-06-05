package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.service.InstallService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安装重定向过滤器
 * 首次访问时如果系统未安装，自动跳转到安装页面
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
@Order(1)
public class InstallRedirectFilter implements Filter {

    private final InstallService installService;

    /** 不需要重定向的路径 */
    private static final String[] EXCLUDED_PATHS = {
        "/install",
        "/api/install",
        "/api/auth",
        "/api/v1/auth",
        "/login",
        "/css/",
        "/js/",
        "/images/",
        "/favicon"
    };

    public InstallRedirectFilter(InstallService installService) {
        this.installService = installService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        // 检查是否是排除的路径
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 如果系统未安装，重定向到安装页面
        if (!installService.isInstalled()) {
            httpResponse.sendRedirect("/install");
            return;
        }

        chain.doFilter(request, response);
    }
}
