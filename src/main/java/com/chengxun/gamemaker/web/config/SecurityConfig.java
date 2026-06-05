package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.chengxun.gamemaker.web.config.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final CaptchaAuthenticationFilter captchaAuthenticationFilter;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Lazy UserService userService, CaptchaAuthenticationFilter captchaAuthenticationFilter,
                         ForcePasswordChangeFilter forcePasswordChangeFilter,
                         JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userService = userService;
        this.captchaAuthenticationFilter = captchaAuthenticationFilter;
        this.forcePasswordChangeFilter = forcePasswordChangeFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * API 过滤链
     * - 使用 JWT Token 进行认证
     * - 飞书 webhook 因为是外部调用，禁用 CSRF
     * - 钉钉回调接口，禁用 CSRF
     * - 其他 API 需要认证
     * - 公开 API（如健康检查）允许匿名访问
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**", "/feishu/**", "/dingtalk/**", "/ws/**", "/projects/api/**",
                    "/search/**", "/resources/**", "/performance/**", "/performance-management/**",
                    "/tokens/**", "/reviews/**", "/git/**", "/agents/**", "/health/**",
                    "/interventions/**", "/recruitment/**", "/notifications/**",
                    "/notification-templates/**", "/notification-preferences/**",
                    "/game-templates/**", "/capabilities/**",
                    "/knowledge/**", "/knowledge-base/**", "/knowledge-evolution/**",
                    "/skills/**", "/alerts/**", "/configs/**", "/agent-scheduler/**",
                    "/diagnostic/**", "/workflow/**", "/pipelines/**", "/monitor/**",
                    "/code-quality/**", "/code-browser/**", "/chat/**",
                    "/mcp/**", "/admin/**", "/permissions/**", "/constants/**",
                    "/devices/**", "/roles/**", "/email/**", "/captcha/**")
            .csrf(csrf -> csrf.disable()) // API使用JWT认证，禁用CSRF
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)  // 添加 JWT 过滤器
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll() // 公开API允许匿名访问
                .requestMatchers("/api/v1/auth/**").permitAll() // 认证相关API（登录、注册、设备验证等）允许匿名访问
                .requestMatchers("/api/auth/**").permitAll() // 认证相关API允许匿名访问
                .requestMatchers("/api/install/**").permitAll() // 安装向导允许匿名访问
                .requestMatchers("/feishu/**").permitAll() // 飞书webhook允许匿名访问
                .requestMatchers("/dingtalk/**").permitAll() // 钉钉配置允许匿名访问
                .requestMatchers("/api/dingtalk/**").permitAll() // 钉钉配置允许匿名访问
                .requestMatchers("/api/dingtalk-bot/**").permitAll() // 钉钉机器人回调允许匿名访问
                .requestMatchers("/ws/**").permitAll() // WebSocket允许匿名访问
                .anyRequest().authenticated() // 其他API需要认证
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 禁用 Session，使用 JWT
            );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(captchaAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(forcePasswordChangeFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/install", "/captcha/**", "/email/**",
                    "/device/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger UI 允许匿名访问
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/roles/**").hasAuthority("PERM_roles:manage")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("GAME_MAKER_SESSION") // 登出时删除会话Cookie
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)  // 每个用户最多1个并发会话
                .expiredUrl("/login?expired=true")  // 会话过期跳转
                .maxSessionsPreventsLogin(false)  // 新登录踢掉旧会话
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403")
            );
        return http.build();
    }
}
