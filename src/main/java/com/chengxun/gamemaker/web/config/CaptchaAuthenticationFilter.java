package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.controller.DeviceVerifyController;
import com.chengxun.gamemaker.web.service.CaptchaService;
import com.chengxun.gamemaker.web.service.DeviceTrustService;
import com.chengxun.gamemaker.web.service.UserService;
import com.chengxun.gamemaker.web.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CaptchaAuthenticationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;
    private final DeviceTrustService deviceTrustService;
    private final UserService userService;

    public CaptchaAuthenticationFilter(CaptchaService captchaService,
                                       DeviceTrustService deviceTrustService,
                                       @Lazy UserService userService) {
        this.captchaService = captchaService;
        this.deviceTrustService = deviceTrustService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 处理设备验证完成后的登录
        if ("/login".equals(uri) && "GET".equals(request.getMethod())) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("device_verified_user") != null) {
                String username = (String) session.getAttribute("device_verified_user");
                session.removeAttribute("device_verified_user");
                request.setAttribute("device_verified_user", username);
            }
        }

        // 只拦截登录 POST 请求
        if ("/login".equals(uri) && "POST".equals(request.getMethod())) {
            // 如果是从设备验证页面跳转回来的，放行
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("device_verified_user") != null) {
                filterChain.doFilter(request, response);
                return;
            }

            String captchaId = request.getParameter("captchaId");
            String captchaCode = request.getParameter("captchaCode");

            if (!captchaService.verifyCaptcha(captchaId, captchaCode)) {
                // 验证码错误，重定向回登录页
                response.sendRedirect("/login?error=captcha");
                return;
            }

            // 验证码正确，检查设备信任
            if (deviceTrustService.isDeviceTrustEnabled()) {
                String username = request.getParameter("username");
                if (username != null && !username.isEmpty()) {
                    User user = userService.getUserByUsername(username);
                    if (user != null && user.isApproved() && user.getStatus() != User.UserStatus.DISABLED) {
                        // 检查设备是否已信任
                        if (!deviceTrustService.isDeviceTrusted(user.getId(), request)) {
                            // 设备未信任，需要设备验证
                            HttpSession newSession = request.getSession(true);
                            DeviceVerifyController.setPendingUser(newSession, username);
                            response.sendRedirect("/device/verify");
                            return;
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
