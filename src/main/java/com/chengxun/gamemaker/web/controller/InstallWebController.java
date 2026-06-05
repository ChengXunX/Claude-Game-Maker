package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.InstallService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

/**
 * 安装向导页面控制器
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class InstallWebController {

    private static final Logger log = LoggerFactory.getLogger(InstallWebController.class);

    private final InstallService installService;

    public InstallWebController(InstallService installService) {
        this.installService = installService;
    }

    /**
     * 安装向导页面
     * 已安装时返回 404
     */
    @GetMapping("/install")
    public String installPage(HttpServletResponse response) throws IOException {
        boolean installed = installService.isInstalled();
        log.info("Install page requested, isInstalled: {}", installed);
        if (installed) {
            log.info("System is installed, returning 404");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "系统已安装，无需重复安装");
            return null;
        }
        log.info("System is not installed, returning install view");
        return "install";
    }
}
