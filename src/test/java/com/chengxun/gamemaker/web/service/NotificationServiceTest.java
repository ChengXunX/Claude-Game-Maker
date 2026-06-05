package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Notification;
import com.chengxun.gamemaker.web.entity.Notification.NotificationChannel;
import com.chengxun.gamemaker.web.entity.Notification.NotificationType;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.NotificationRepository;
import com.chengxun.gamemaker.web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知服务单元测试
 * 测试NotificationService的核心功能
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.chengxun.gamemaker.feishu.FeishuBotService feishuService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // 创建测试通知
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(1L);
        testNotification.setTitle("测试通知");
        testNotification.setContent("测试内容");
        testNotification.setType(NotificationType.SYSTEM);
        testNotification.setChannel(NotificationChannel.SYSTEM);
        testNotification.setRead(false);
    }

    @Test
    void testSendSystemNotification() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        assertDoesNotThrow(() -> {
            notificationService.sendSystemNotification(1L, "测试标题", "测试内容", NotificationType.SYSTEM);
        });

        // Then
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testGetUserNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(notifications));

        // When
        var result = notificationService.getUserNotifications(1L, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("测试通知", result.getContent().get(0).getTitle());
    }

    @Test
    void testGetUnreadCount() {
        // Given
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

        // When
        long count = notificationService.getUnreadCount(1L);

        // Then
        assertEquals(5L, count);
    }

    @Test
    void testGetUnreadNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(1L)).thenReturn(notifications);

        // When
        List<Notification> result = notificationService.getUnreadNotifications(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
    }

    @Test
    void testMarkAsRead_Success() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> {
            notificationService.markAsRead(1L, 1L);
        });

        // Then
        assertTrue(testNotification.isRead());
        assertNotNull(testNotification.getReadAt());
    }

    @Test
    void testMarkAsRead_WrongUser() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(1L, 2L);
        });
    }

    @Test
    void testMarkAsRead_NotFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(999L, 1L);
        });
    }

    @Test
    void testMarkAllAsRead() {
        // Given
        when(notificationRepository.markAllAsRead(1L)).thenReturn(5);

        // When
        int result = notificationService.markAllAsRead(1L);

        // Then
        assertEquals(5, result);
        verify(notificationRepository).markAllAsRead(1L);
    }

    @Test
    void testDeleteNotification_Success() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // When
        assertDoesNotThrow(() -> {
            notificationService.deleteNotification(1L, 1L);
        });

        // Then
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    void testDeleteNotification_WrongUser() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification(1L, 2L);
        });
    }

    @Test
    void testDeleteNotification_NotFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification(999L, 1L);
        });
    }
}
