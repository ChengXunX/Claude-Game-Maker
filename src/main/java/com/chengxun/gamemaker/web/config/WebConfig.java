package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class WebConfig {

    private final UserService userService;
    private final RoleService roleService;

    public WebConfig(@Lazy UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // 初始化默认角色
        roleService.initDefaultRoles();

        // 创建默认管理员账号
        userService.initAdminUser();
    }

    /**
     * 启用 ForwardedHeaderFilter
     * 当应用在反向代理（如 Nginx）后面时，Spring Boot 需要读取 X-Forwarded-* 头
     * 来确定原始请求的协议、主机和端口
     *
     * 解决 HTTPS 代理后面的 Mixed Content 问题：
     * - 代理传递 X-Forwarded-Proto: https
     * - Spring Boot 据此构造正确的 HTTPS URL
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}
