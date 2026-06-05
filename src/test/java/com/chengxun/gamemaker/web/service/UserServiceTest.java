package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 测试UserService的核心功能
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // 创建测试角色
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("USER");
        testRole.setDisplayName("普通用户");
        testRole.setPermissions(Set.of("dashboard:view"));

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setEmail("test@example.com");
        testUser.setNickname("Test User");
        testUser.setRole(testRole);
        testUser.setStatus(User.UserStatus.APPROVED);
    }

    @Test
    void testGetUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetUserByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        User result = userService.getUserByUsername("nonexistent");

        // Then
        assertNull(result);
    }

    @Test
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        User result = userService.getUserById(999L);

        // Then
        assertNull(result);
    }

    @Test
    void testRegister_Success() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleService.getRoleByName("USER")).thenReturn(testRole);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        User result = userService.register("newuser", "Password123", "new@example.com", "New User");

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals(User.UserStatus.PENDING, result.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_UsernameExists() {
        // Given
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.register("existinguser", "Password123", "test@example.com", "Test");
        });
    }

    @Test
    void testApproveUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.approveUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(User.UserStatus.APPROVED, result.getStatus());
    }

    @Test
    void testRejectUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.rejectUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(User.UserStatus.REJECTED, result.getStatus());
    }

    @Test
    void testDisableUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.disableUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(User.UserStatus.DISABLED, result.getStatus());
    }

    @Test
    void testUpdateUserRole_Success() {
        // Given
        Role newRole = new Role();
        newRole.setId(2L);
        newRole.setName("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.getRoleById(2L)).thenReturn(newRole);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateUserRole(1L, 2L);

        // Then
        assertNotNull(result);
        assertEquals("ADMIN", result.getRole().getName());
    }

    @Test
    void testUpdateUserRole_RoleNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.getRoleById(999L)).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.updateUserRole(1L, 999L);
        });
    }

    @Test
    void testChangePassword_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpassword", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("new_encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> {
            userService.changePassword(1L, "oldpassword", "newpassword");
        });
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.changePassword(1L, "wrongpassword", "newpassword");
        });
    }

    @Test
    void testUpdateProfile_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updateProfile(1L, "New Nickname", "new@example.com", "avatar.png");

        // Then
        assertNotNull(result);
        assertEquals("New Nickname", result.getNickname());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("avatar.png", result.getAvatar());
    }
}
