package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.PermissionDefinition;
import com.chengxun.gamemaker.web.entity.PermissionRequest;
import com.chengxun.gamemaker.web.entity.UserPermission;
import com.chengxun.gamemaker.web.repository.PermissionDefinitionRepository;
import com.chengxun.gamemaker.web.repository.PermissionRequestRepository;
import com.chengxun.gamemaker.web.repository.UserPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserPermissionRepository permissionRepository;

    @Mock
    private PermissionRequestRepository requestRepository;

    @Mock
    private PermissionDefinitionRepository definitionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private PermissionDefinition testDefinition;

    @BeforeEach
    void setUp() {
        testDefinition = new PermissionDefinition();
        testDefinition.setId(1L);
        testDefinition.setPermissionKey("PERM_test:manage");
        testDefinition.setName("测试管理");
        testDefinition.setEnabled(true);
        testDefinition.setSystem(false);
    }

    @Test
    void testGetAvailablePermissions() {
        when(definitionRepository.findByEnabledTrueOrderByCategoryAscSortOrderAsc())
            .thenReturn(List.of(testDefinition));

        var result = permissionService.getAvailablePermissions();

        assertNotNull(result);
        assertTrue(result.containsKey("PERM_test:manage"));
    }

    @Test
    void testSubmitRequest_ValidPermission() {
        when(definitionRepository.findByPermissionKey("PERM_test:manage"))
            .thenReturn(Optional.of(testDefinition));
        when(requestRepository.existsByUserIdAndPermissionAndStatus(1L, "PERM_test:manage",
            PermissionRequest.RequestStatus.PENDING)).thenReturn(false);
        when(permissionRepository.existsByUserIdAndPermission(1L, "PERM_test:manage")).thenReturn(false);
        when(requestRepository.save(any())).thenReturn(new PermissionRequest());

        PermissionRequest result = permissionService.submitRequest(1L, "testuser", "PERM_test:manage", "测试");

        assertNotNull(result);
        verify(requestRepository).save(any());
    }

    @Test
    void testSubmitRequest_InvalidPermission() {
        when(definitionRepository.findByPermissionKey("PERM_invalid"))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            permissionService.submitRequest(1L, "testuser", "PERM_invalid", "测试"));
    }

    @Test
    void testSubmitRequest_DisabledPermission() {
        testDefinition.setEnabled(false);
        when(definitionRepository.findByPermissionKey("PERM_test:manage"))
            .thenReturn(Optional.of(testDefinition));

        assertThrows(RuntimeException.class, () ->
            permissionService.submitRequest(1L, "testuser", "PERM_test:manage", "测试"));
    }

    @Test
    void testGrantPermission() {
        when(permissionRepository.findByUserIdAndPermission(1L, "PERM_test:manage"))
            .thenReturn(Optional.empty());
        when(permissionRepository.save(any())).thenReturn(new UserPermission());

        UserPermission result = permissionService.grantPermission(1L, "PERM_test:manage", "admin", "测试授予");

        assertNotNull(result);
        verify(permissionRepository).save(any());
    }

    @Test
    void testRevokePermission() {
        doNothing().when(permissionRepository).deleteByUserIdAndPermission(1L, "PERM_test:manage");

        permissionService.revokePermission(1L, "PERM_test:manage");

        verify(permissionRepository).deleteByUserIdAndPermission(1L, "PERM_test:manage");
    }

    @Test
    void testApproveRequest() {
        PermissionRequest request = new PermissionRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUsername("testuser");
        request.setPermission("PERM_test:manage");
        request.setStatus(PermissionRequest.RequestStatus.PENDING);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(definitionRepository.findByPermissionKey("PERM_test:manage"))
            .thenReturn(Optional.of(testDefinition));
        when(permissionRepository.findByUserIdAndPermission(1L, "PERM_test:manage"))
            .thenReturn(Optional.empty());
        when(permissionRepository.save(any())).thenReturn(new UserPermission());
        when(requestRepository.save(any())).thenReturn(request);

        PermissionRequest result = permissionService.approve(1L, 2L, "admin", "批准");

        assertNotNull(result);
        assertEquals(PermissionRequest.RequestStatus.APPROVED, result.getStatus());
    }

    @Test
    void testRejectRequest() {
        PermissionRequest request = new PermissionRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUsername("testuser");
        request.setPermission("PERM_test:manage");
        request.setStatus(PermissionRequest.RequestStatus.PENDING);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        PermissionRequest result = permissionService.reject(1L, 2L, "admin", "拒绝原因");

        assertNotNull(result);
        assertEquals(PermissionRequest.RequestStatus.REJECTED, result.getStatus());
    }
}
