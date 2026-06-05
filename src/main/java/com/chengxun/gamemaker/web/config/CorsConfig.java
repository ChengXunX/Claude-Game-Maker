package com.chengxun.gamemaker.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS跨域配置
 * 允许Vue前端跨域访问API
 *
 * 主要功能：
 * - 配置允许的源（Origin）
 * - 配置允许的HTTP方法
 * - 配置允许的请求头
 * - 配置凭证支持
 *
 * @author chengxun
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    /**
     * CORS过滤器
     * 处理跨域请求
     *
     * @return CorsFilter实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源 - 支持本地开发和生产环境
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",       // 本地开发
            "http://127.0.0.1:*",      // 本地开发
            "http://101.96.193.145:*",  // 生产环境公网IP
            "http://*.local:*",         // 本地网络
            "https://*.local:*",        // 本地网络 HTTPS
            "*"  // 其他环境
        ));

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList("*"));

        // 暴露的响应头
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // 允许凭证（Cookie）
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/actuator/**", config);

        return new CorsFilter(source);
    }
}
