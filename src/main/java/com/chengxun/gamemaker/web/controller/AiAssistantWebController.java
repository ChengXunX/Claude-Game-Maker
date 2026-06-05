package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.AiAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * AI助手Web控制器
 * 处理AI助手页面路由
 *
 * @author chengxun
 * @since 1.0.0
 */
@Controller
public class AiAssistantWebController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantWebController.class);

    @Autowired
    private AiAssistantService aiAssistantService;

    /**
     * AI助手页面
     */
    @GetMapping("/ai-assistant")
    @PreAuthorize("hasAuthority('PERM_ai:use')")
    public String aiAssistantPage(Model model) {
        log.info("Loading AI assistant page");

        // 初始化知识库（如果尚未初始化）
        aiAssistantService.initSystemKnowledge();

        // 获取知识库统计
        model.addAttribute("knowledgeStats", aiAssistantService.getKnowledgeStats());

        return "ai-assistant";
    }
}
