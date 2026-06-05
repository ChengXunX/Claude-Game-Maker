package com.chengxun.gamemaker.web.dto;

import com.chengxun.gamemaker.web.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO
 * 用于用户注册时的参数验证
 *
 * @author chengxun
 * @since 1.0.0
 */
public class RegisterRequest {

    /** 用户名 */
    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 密码 */
    @NotBlank(message = "请输入密码")
    @StrongPassword
    private String password;

    /** 确认密码 */
    @NotBlank(message = "请确认密码")
    private String confirmPassword;

    /** 邮箱 */
    @Email(message = "请输入有效的邮箱地址")
    private String email;

    /** 昵称 */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
