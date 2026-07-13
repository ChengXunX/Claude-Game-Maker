package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.SystemConstantService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公开 API 控制器
 * 提供无需登录即可访问的公开接口（如ICP备案号）
 *
 * @author chengxun
 */
@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    private final SystemConstantService constantService;

    public PublicApiController(SystemConstantService constantService) {
        this.constantService = constantService;
    }

    /**
     * 获取指定系统常量的值
     * 用于网站首页等无需登录即可访问的场景
     *
     * @param key 常量键（如 site.icp-filing-number）
     * @return 常量键和值
     */
    @GetMapping("/constants/{key}")
    public Map<String, String> getConstant(@PathVariable String key) {
        String value = constantService.getString(key, "");
        return Map.of("key", key, "value", value);
    }

    /**
     * 获取网站公开信息（系统名称、ICP备案号等）
     * 用于登录页、页脚等无需登录即可访问的场景
     *
     * @return 网站信息 Map
     */
    @GetMapping("/site-info")
    public Map<String, String> getSiteInfo() {
        String systemName = constantService.getString("site.name", "ChengXun Game Maker");
        String icpFilingNumber = constantService.getString("site.icp-filing-number", "");
        return Map.of("systemName", systemName, "icpFilingNumber", icpFilingNumber);
    }
}
