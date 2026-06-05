package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailController emailController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(emailController).build();
    }

    @Test
    void sendVerificationCode_happyPath_returnsSuccess() throws Exception {
        String email = "user@example.com";
        String code = "123456";

        when(emailService.isEmailEnabled()).thenReturn(true);
        when(emailService.generateVerificationCode(email)).thenReturn(code);

        mockMvc.perform(post("/email/send-code")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("验证码已发送到 " + email));

        verify(emailService).sendVerificationEmail(email, code);
    }

    @Test
    void sendVerificationCode_nullEmail_returnsError() throws Exception {
        mockMvc.perform(post("/email/send-code")
                        .param("email", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请输入有效的邮箱地址"));

        verifyNoInteractions(emailService);
    }

    @Test
    void sendVerificationCode_blankEmail_returnsError() throws Exception {
        mockMvc.perform(post("/email/send-code")
                        .param("email", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请输入有效的邮箱地址"));

        verifyNoInteractions(emailService);
    }

    @Test
    void sendVerificationCode_invalidEmailNoAt_returnsError() throws Exception {
        mockMvc.perform(post("/email/send-code")
                        .param("email", "invalidemail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请输入有效的邮箱地址"));

        verifyNoInteractions(emailService);
    }

    @Test
    void sendVerificationCode_emailServiceDisabled_returnsError() throws Exception {
        String email = "user@example.com";

        when(emailService.isEmailEnabled()).thenReturn(false);

        mockMvc.perform(post("/email/send-code")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮件服务未启用，请联系管理员"));

        verify(emailService, never()).generateVerificationCode(anyString());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void sendVerificationCode_serviceThrowsException_returnsError() throws Exception {
        String email = "user@example.com";

        when(emailService.isEmailEnabled()).thenReturn(true);
        when(emailService.generateVerificationCode(email))
                .thenThrow(new RuntimeException("SMTP connection failed"));

        mockMvc.perform(post("/email/send-code")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("发送失败: SMTP connection failed"));

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
}
