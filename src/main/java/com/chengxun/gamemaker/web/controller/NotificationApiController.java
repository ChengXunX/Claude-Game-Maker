package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.NotificationRepository;
import com.chengxun.gamemaker.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知管理 API 控制器
 * 提供用户通知的 CRUD 接口
 *
 * 操作维度：用户级（每个用户只能看到自己的通知）
 * 权限要求：登录用户即可
 *
 * @author chengxun
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "用户通知相关接口")
@PreAuthorize("isAuthenticated()")
public class NotificationApiController {

    private static final Logger log = LoggerFactory.getLogger(NotificationApiController.class);

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationApiController(NotificationRepository notificationRepository,
                                     UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    /**
     * 获取当前用户的通知列表
     *
     * @param read 是否已读（可选，不传则返回全部）
     * @param page 页码
     * @param size 每页数量
     * @param authentication 认证信息
     * @return 通知列表
     */
    @GetMapping
    @Operation(summary = "获取通知列表")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> notifications;
        if (read != null && read) {
            notifications = notificationRepository.findByUserIdAndReadTrue(user.getId(), pageRequest);
        } else if (read != null && !read) {
            notifications = notificationRepository.findByUserIdAndReadFalse(user.getId(), pageRequest);
        } else {
            notifications = notificationRepository.findByUserId(user.getId(), pageRequest);
        }

        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取通知详情
     * 只能查看自己的通知
     *
     * @param id 通知ID
     * @param authentication 认证信息
     * @return 通知详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取通知详情")
    public ResponseEntity<?> detail(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        // 检查通知是否属于当前用户
        if (!notification.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权访问该通知"));
        }

        return ResponseEntity.ok(notification);
    }

    /**
     * 标记通知为已读
     * 只能标记自己的通知
     *
     * @param id 通知ID
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        // 检查通知是否属于当前用户
        if (!notification.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权操作该通知"));
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return ResponseEntity.ok().build();
    }

    /**
     * 标记所有通知为已读
     *
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        var unreadNotifications = notificationRepository.findByUserIdAndReadFalse(user.getId(), PageRequest.of(0, 1000));
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications.getContent());

        log.info("用户 {} 标记所有通知为已读", user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 删除通知
     * 只能删除自己的通知
     *
     * @param id 通知ID
     * @param authentication 认证信息
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        // 检查通知是否属于当前用户
        if (!notification.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权删除该通知"));
        }

        notificationRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取未读通知数量
     *
     * @param authentication 认证信息
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        long count = notificationRepository.countByUserIdAndReadFalse(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 清理无效通知
     * 删除标题或内容为空或为"-"的通知
     * 需要通知管理权限
     *
     * @return 删除数量
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasAuthority('PERM_notification:manage')")
    @Transactional
    @Operation(summary = "清理无效通知")
    public ResponseEntity<?> cleanup() {
        int deleted = notificationRepository.deleteInvalidNotifications();
        log.info("Cleaned up {} invalid notifications", deleted);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
