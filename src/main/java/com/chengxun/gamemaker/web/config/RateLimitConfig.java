package com.chengxun.gamemaker.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 限流配置
 * 基于滑动窗口的内存限流，无需外部依赖
 *
 * 限流策略：
 * - 全局 API：每 IP 每分钟 120 次
 * - 认证 API（登录/注册）：每 IP 每分钟 10 次
 * - 写操作 API：每用户每分钟 60 次
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
            .addPathPatterns("/api/**", "/login", "/register");
    }
}
