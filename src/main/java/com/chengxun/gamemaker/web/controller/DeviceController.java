package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.DeviceTrust;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.DeviceTrustService;
import com.chengxun.gamemaker.web.service.OperationLogService;
import com.chengxun.gamemaker.web.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceTrustService deviceTrustService;
    private final UserService userService;
    private final OperationLogService logService;

    public DeviceController(DeviceTrustService deviceTrustService, UserService userService,
                           OperationLogService logService) {
        this.deviceTrustService = deviceTrustService;
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    public String listDevices(Model model, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/";
        }

        List<DeviceTrust> devices = deviceTrustService.getTrustedDevices(user.getId());
        model.addAttribute("devices", devices);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("trustDays", deviceTrustService.getTrustDays());
        model.addAttribute("deviceTrustEnabled", deviceTrustService.isDeviceTrustEnabled());
        return "devices";
    }

    @PostMapping("/{deviceId}/remove")
    public String removeDevice(@PathVariable Long deviceId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/";
            }

            deviceTrustService.removeDevice(user.getId(), deviceId);
            logService.log(user.getId(), "REMOVE_DEVICE", "Device " + deviceId, "移除设备信任", null);
            redirectAttributes.addFlashAttribute("success", "设备信任已移除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/devices";
    }

    @PostMapping("/remove-all")
    public String removeAllDevices(Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/";
            }

            deviceTrustService.removeAllDevices(user.getId());
            logService.log(user.getId(), "REMOVE_ALL_DEVICES", "All devices", "移除所有设备信任", null);
            redirectAttributes.addFlashAttribute("success", "所有设备信任已移除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/devices";
    }
}
