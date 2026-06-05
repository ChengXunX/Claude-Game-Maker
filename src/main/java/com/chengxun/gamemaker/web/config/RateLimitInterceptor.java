package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 限流拦截器
 * 基于滑动窗口计数器的内存限流
 *
 * 配置项（通过 SystemConfigService 动态读取）：
 * - rate-limit.global: 全局 API 每分钟请求限制
 * - rate-limit.auth: 认证接口每分钟请求限制
 * - rate-limit.write: 写操作每分钟请求限制
 *
 * @author chengxun
 * @since 1.0.0
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** 默认全局限流（配置不存在时使用） */
    private static final int DEFAULT_GLOBAL_LIMIT = 120;

    /** 默认认证限流（配置不存在时使用） */
    private static final int DEFAULT_AUTH_LIMIT = 10;

    /** 默认写操作限流（配置不存在时使用） */
    private static final int DEFAULT_WRITE_LIMIT = 60;

    /** 窗口大小（毫秒）：1 分钟 */
    private static final long WINDOW_MS = 60_000;

    /** 限流计数器：key = "ip:path_prefix" 或 "user:path_prefix" */
    private final ConcurrentHashMap<String, RateWindow> counters = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemConfigService configService;

    /**
     * 获取全局限流配置
     */
    private int getGlobalLimit() {
        return configService.getInt(SystemConstants.RATE_LIMIT_GLOBAL, DEFAULT_GLOBAL_LIMIT);
    }

    /**
     * 获取认证限流配置
     */
    private int getAuthLimit() {
        return configService.getInt(SystemConstants.RATE_LIMIT_AUTH, DEFAULT_AUTH_LIMIT);
    }

    /**
     * 获取写操作限流配置
     */
    private int getWriteLimit() {
        return configService.getInt(SystemConstants.RATE_LIMIT_WRITE, DEFAULT_WRITE_LIMIT);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. 认证 API 限流（登录/注册）
        if (path.startsWith("/api/v1/auth/") || path.equals("/login") || path.equals("/register")) {
            if (!checkRate("auth:" + clientIp, getAuthLimit())) {
                return reject(response, "登录/注册请求过于频繁，请稍后再试");
            }
        }

        // 2. 全局 API 限流
        if (!checkRate("global:" + clientIp, getGlobalLimit())) {
            return reject(response, "请求过于频繁，请稍后再试");
        }

        // 3. 写操作限流
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            String userId = request.getRemoteUser();
            String key = userId != null ? "write:" + userId : "write:" + clientIp;
            if (!checkRate(key, getWriteLimit())) {
                return reject(response, "操作过于频繁，请稍后再试");
            }
        }

        return true;
    }

    /**
     * 检查是否超过限流
     */
    private boolean checkRate(String key, int limit) {
        long now = Instant.now().toEpochMilli();
        RateWindow window = counters.computeIfAbsent(key, k -> new RateWindow(now));

        synchronized (window) {
            // 如果窗口已过期，重置
            if (now - window.startTime > WINDOW_MS) {
                window.startTime = now;
                window.count.set(0);
            }

            if (window.count.incrementAndGet() > limit) {
                log.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", key, window.count.get(), limit);
                return false;
            }
            return true;
        }
    }

    /**
     * 返回 429 响应
     */
    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
            "status", 429,
            "error", "TOO_MANY_REQUESTS",
            "message", message
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 取第一个 IP（经过多级代理时）
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 限流窗口
     */
    private static class RateWindow {
        long startTime;
        final AtomicInteger count;

        RateWindow(long startTime) {
            this.startTime = startTime;
            this.count = new AtomicInteger(0);
        }
    }
}
