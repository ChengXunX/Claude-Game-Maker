package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.CaptchaService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final OperationLogService logService;
    private final CaptchaService captchaService;

    public ProfileController(UserService userService, OperationLogService logService, CaptchaService captchaService) {
        this.userService = userService;
        this.logService = logService;
        this.captchaService = captchaService;
    }

    @GetMapping
    public String profile(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/";
        }
        model.addAttribute("user", user);
        model.addAttribute("username", authentication.getName());

        // 验证码（用于修改密码）
        String captchaId = captchaService.generateCaptcha();
        String captchaImage = captchaService.generateCaptchaImageBase64(captchaId);
        model.addAttribute("captchaId", captchaId);
        model.addAttribute("captchaImage", captchaImage);

        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String nickname,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String avatar,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/";
            }
            userService.updateProfile(user.getId(), nickname, email, avatar);
            logService.log(user.getId(), "UPDATE_PROFILE", user.getUsername(), "Updated profile", null);
            redirectAttributes.addFlashAttribute("success", "个人信息已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    /**
     * 强制修改密码页面（首次登录或管理员重置后）
     */
    @GetMapping("/change-password")
    public String changePasswordPage(Model model, Authentication authentication,
                                     @RequestParam(required = false) String forced) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/";
        }

        model.addAttribute("user", user);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("forced", "true".equals(forced) || user.isMustChangePassword());

        // 验证码
        String captchaId = captchaService.generateCaptcha();
        String captchaImage = captchaService.generateCaptchaImageBase64(captchaId);
        model.addAttribute("captchaId", captchaId);
        model.addAttribute("captchaImage", captchaImage);

        return "change-password";
    }

    /**
     * 处理修改密码请求
     */
    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @RequestParam String captchaId,
                                 @RequestParam String captchaCode,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        // 验证验证码
        if (!captchaService.verifyCaptcha(captchaId, captchaCode)) {
            redirectAttributes.addFlashAttribute("error", "验证码错误或已过期");
            return "redirect:/profile/change-password";
        }

        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "两次输入的新密码不一致");
                return "redirect:/profile/change-password";
            }

            // 密码强度验证
            String passwordError = validatePasswordStrength(newPassword);
            if (passwordError != null) {
                redirectAttributes.addFlashAttribute("error", passwordError);
                return "redirect:/profile/change-password";
            }

            userService.changePassword(user.getId(), currentPassword, newPassword);

            // 记录是否为强制修改密码（在清除标志之前检查）
            boolean wasForced = user.isMustChangePassword();

            // 清除强制修改密码标志
            if (wasForced) {
                user.setMustChangePassword(false);
                userService.saveUser(user);
            }

            // 刷新认证上下文，确保 mustChangePassword 标志更新生效
            refreshAuthentication(user, authentication);

            logService.log(user.getId(), "CHANGE_PASSWORD", user.getUsername(), "Password changed", null);
            redirectAttributes.addFlashAttribute("success", "密码已修改成功");

            // 如果是强制修改密码，跳转到首页
            if (wasForced) {
                return "redirect:/";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile/change-password";
    }

    /**
     * 验证密码强度
     * 要求：至少8位，包含大小写字母和数字（与注册规则保持一致）
     */
    private String validatePasswordStrength(String password) {
        if (password.length() < 8) {
            return "密码长度不能少于8位";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含至少一个大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含至少一个小写字母";
        }
        if (!password.matches(".*\\d.*")) {
            return "密码必须包含至少一个数字";
        }
        return null;
    }

    /**
     * 刷新认证上下文
     * 在用户信息变更后，更新 SecurityContext 中的认证信息
     *
     * @param user 更新后的用户对象
     * @param authentication 当前认证对象
     */
    private void refreshAuthentication(User user, Authentication authentication) {
        // 构建新的权限列表
        java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
            if (user.getRole().getPermissions() != null) {
                for (String perm : user.getRole().getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                }
            }
        }

        // 创建新的认证对象
        UsernamePasswordAuthenticationToken newAuth =
            new UsernamePasswordAuthenticationToken(
                authentication.getPrincipal(),
                authentication.getCredentials(),
                authorities
            );
        newAuth.setDetails(authentication.getDetails());

        // 更新 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
}
