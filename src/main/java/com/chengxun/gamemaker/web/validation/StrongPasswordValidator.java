package com.chengxun.gamemaker.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 强密码验证器
 * 验证密码是否符合强度要求
 *
 * @author chengxun
 * @since 1.0.0
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /** 密码强度正则表达式：至少8位，包含大小写字母、数字和特殊字符 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // 初始化无需操作
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // 空值由 @NotBlank 验证
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
