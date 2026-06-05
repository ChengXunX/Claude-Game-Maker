package com.chengxun.gamemaker.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

/**
 * 文件管理页面控制器
 */
@Controller
public class FilePageController {

    @GetMapping("/files")
    @PreAuthorize("hasAnyAuthority('PERM_agents:view', 'PERM_agents:manage', 'PERM_admin:manage')")
    public String filesPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "files/index";
    }
}
