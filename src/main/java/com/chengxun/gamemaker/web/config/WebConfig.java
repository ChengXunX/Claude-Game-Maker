package com.chengxun.gamemaker.web.config;

import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class WebConfig {

    private final UserService userService;

    public WebConfig(UserService userService) {
        this.userService = userService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // 创建默认管理员账号
        userService.initAdminUser();
    }
}
