package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.CaptchaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/generate")
    @ResponseBody
    public Map<String, String> generateCaptcha() {
        String captchaId = captchaService.generateCaptcha();
        String imageBase64 = captchaService.generateCaptchaImageBase64(captchaId);

        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("image", imageBase64);
        return result;
    }
}
