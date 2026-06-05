package com.chengxun.gamemaker.web.dto;

import com.chengxun.gamemaker.web.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求 DTO
 * 用于用户修改密码时的参数验证
 *
 * @author chengxun
 * @since 1.0.0
 */
public class ChangePasswordRequest {

    /** 当前密码 */
    @NotBlank(message = "请输入当前密码")
    private String currentPassword;

    /** 新密码 */
    @NotBlank(message = "请输入新密码")
    @StrongPassword
    private String newPassword;

    /** 确认新密码 */
    @NotBlank(message = "请确认新密码")
    private String confirmPassword;

    // Getters and Setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
