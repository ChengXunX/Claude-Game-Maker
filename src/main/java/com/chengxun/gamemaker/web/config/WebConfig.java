package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.service.RoleService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class WebConfig {

    private final UserService userService;
    private final RoleService roleService;

    public WebConfig(UserService userService, RoleService roleService) {
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
}
