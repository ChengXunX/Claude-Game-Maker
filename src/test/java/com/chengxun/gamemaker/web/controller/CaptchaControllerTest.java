package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CaptchaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private CaptchaController captchaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(captchaController).build();
    }

    @Test
    void generateCaptcha_happyPath_returnsCaptchaIdAndImage() throws Exception {
        String captchaId = "abc123def456";
        String imageBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==";

        when(captchaService.generateCaptcha()).thenReturn(captchaId);
        when(captchaService.generateCaptchaImageBase64(captchaId)).thenReturn(imageBase64);

        mockMvc.perform(get("/captcha/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").value(captchaId))
                .andExpect(jsonPath("$.image").value(imageBase64));
    }

    @Test
    void generateCaptcha_whenImageGenerationFails_returnsNullImage() throws Exception {
        String captchaId = "abc123def456";

        when(captchaService.generateCaptcha()).thenReturn(captchaId);
        when(captchaService.generateCaptchaImageBase64(captchaId)).thenReturn(null);

        mockMvc.perform(get("/captcha/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").value(captchaId))
                .andExpect(jsonPath("$.image").isEmpty());
    }

    @Test
    void generateCaptcha_returnsJsonContentType() throws Exception {
        String captchaId = "test123";
        String imageBase64 = "data:image/png;base64,abc";

        when(captchaService.generateCaptcha()).thenReturn(captchaId);
        when(captchaService.generateCaptchaImageBase64(captchaId)).thenReturn(imageBase64);

        mockMvc.perform(get("/captcha/generate"))
                .andExpect(content().contentType("application/json"));
    }
}
