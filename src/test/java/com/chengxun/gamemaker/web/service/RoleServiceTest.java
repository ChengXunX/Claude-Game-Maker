package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Captor
    private ArgumentCaptor<Role> roleCaptor;

    // ---- Helper methods ----

    private Role createRole(Long id, String name, String displayName, boolean system, Set<String> perms) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDisplayName(displayName);
        role.setSystem(system);
        role.setPermissions(perms != null ? perms : new HashSet<>());
        return role;
    }

    // ---- initDefaultRoles() ----

    @Nested
    @DisplayName("initDefaultRoles()")
    class InitDefaultRolesTests {

        @Test
        @DisplayName("should create all 6 default roles when none exist")
        void shouldCreateAllDefaultRolesWhenNoneExist() {
            when(roleRepository.existsByName(anyString())).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            roleService.initDefaultRoles();

            verify(roleRepository, times(7)).save(any(Role.class));

            // Verify each default role was checked
            verify(roleRepository).existsByName("ADMIN");
            verify(roleRepository).existsByName("PROJECT_MANAGER");
            verify(roleRepository).existsByName("DEVELOPER");
            verify(roleRepository).existsByName("OPS_ENGINEER");
            verify(roleRepository).existsByName("OBSERVER");
            verify(roleRepository).existsByName("USER");
        }

        @Test
        @DisplayName("should not create roles that already exist")
        void shouldNotCreateRolesThatAlreadyExist() {
            when(roleRepository.existsByName(anyString())).thenReturn(true);

            roleService.initDefaultRoles();

            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("should create ADMIN role with wildcard permission")
        void shouldCreateAdminWithWildcardPermission() {
            // Order matters: generic matcher first, specific matcher last to override
            when(roleRepository.existsByName(anyString())).thenReturn(true);
            when(roleRepository.existsByName("ADMIN")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            roleService.initDefaultRoles();

            verify(roleRepository).save(roleCaptor.capture());
            Role adminRole = roleCaptor.getValue();

            assertEquals("ADMIN", adminRole.getName());
            assertTrue(adminRole.getPermissions().contains(RoleService.PERM_ALL));
            assertTrue(adminRole.isSystem());
        }

        @Test
        @DisplayName("should create DEVELOPER role with expected permissions")
        void shouldCreateDeveloperWithExpectedPermissions() {
            // Let DEVELOPER through, block all others
            when(roleRepository.existsByName("DEVELOPER")).thenReturn(false);
            when(roleRepository.existsByName("ADMIN")).thenReturn(true);
            when(roleRepository.existsByName("PROJECT_MANAGER")).thenReturn(true);
            when(roleRepository.existsByName("OPS_ENGINEER")).thenReturn(true);
            when(roleRepository.existsByName("OBSERVER")).thenReturn(true);
            when(roleRepository.existsByName("USER")).thenReturn(true);
            when(roleRepository.existsByName("READONLY")).thenReturn(true);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            roleService.initDefaultRoles();

            verify(roleRepository).save(roleCaptor.capture());
            Role devRole = roleCaptor.getValue();

            assertEquals("DEVELOPER", devRole.getName());
            Set<String> perms = devRole.getPermissions();
            assertTrue(perms.contains(RoleService.PERM_DASHBOARD_VIEW));
            assertTrue(perms.contains(RoleService.PERM_AGENTS_VIEW));
            assertTrue(perms.contains(RoleService.PERM_AGENTS_TASK));
            assertTrue(perms.contains(RoleService.PERM_PROJECTS_VIEW));
            // CICD流水线权限
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_VIEW));
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_EXECUTE));
            assertTrue(perms.contains(RoleService.PERM_SKILLS_VIEW));
            // 游戏验证权限
            assertTrue(perms.contains(RoleService.PERM_GAME_VERIFY));
            assertTrue(perms.contains(RoleService.PERM_GAME_VERIFY_VIEW));
            assertFalse(perms.contains(RoleService.PERM_USERS_MANAGE));
        }
    }

    // ---- getRoleByName() ----

    @Nested
    @DisplayName("getRoleByName()")
    class GetRoleByNameTests {

        @Test
        @DisplayName("should return role when found")
        void shouldReturnRoleWhenFound() {
            Role role = createRole(1L, "ADMIN", "Admin", true, null);
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

            Role result = roleService.getRoleByName("ADMIN");

            assertNotNull(result);
            assertEquals("ADMIN", result.getName());
        }

        @Test
        @DisplayName("should return null when role not found")
        void shouldReturnNullWhenNotFound() {
            when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

            Role result = roleService.getRoleByName("NONEXISTENT");

            assertNull(result);
        }
    }

    // ---- getAllRoles() ----

    @Nested
    @DisplayName("getAllRoles()")
    class GetAllRolesTests {

        @Test
        @DisplayName("should return all roles from repository")
        void shouldReturnAllRoles() {
            List<Role> roles = Arrays.asList(
                createRole(1L, "ADMIN", "Admin", true, null),
                createRole(2L, "USER", "User", true, null)
            );
            when(roleRepository.findAll()).thenReturn(roles);

            List<Role> result = roleService.getAllRoles();

            assertEquals(2, result.size());
            verify(roleRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no roles exist")
        void shouldReturnEmptyListWhenNoRoles() {
            when(roleRepository.findAll()).thenReturn(List.of());

            List<Role> result = roleService.getAllRoles();

            assertTrue(result.isEmpty());
        }
    }

    // ---- getRoleById() ----

    @Nested
    @DisplayName("getRoleById()")
    class GetRoleByIdTests {

        @Test
        @DisplayName("should return role when found by ID")
        void shouldReturnRoleWhenFoundById() {
            Role role = createRole(1L, "ADMIN", "Admin", true, null);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            Role result = roleService.getRoleById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("should return null when ID not found")
        void shouldReturnNullWhenIdNotFound() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            Role result = roleService.getRoleById(999L);

            assertNull(result);
        }
    }

    // ---- createRole() ----

    @Nested
    @DisplayName("createRole()")
    class CreateRoleTests {

        @Test
        @DisplayName("should create a new role with given parameters")
        void shouldCreateNewRole() {
            Set<String> permissions = new HashSet<>(Arrays.asList("dashboard:view", "agents:view"));
            when(roleRepository.existsByName("CUSTOM")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            Role result = roleService.createRole("CUSTOM", "Custom Role", "A custom role", permissions);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            verify(roleRepository).save(roleCaptor.capture());
            Role saved = roleCaptor.getValue();

            assertEquals("CUSTOM", saved.getName());
            assertEquals("Custom Role", saved.getDisplayName());
            assertEquals("A custom role", saved.getDescription());
            assertFalse(saved.isSystem());
            assertEquals(permissions, saved.getPermissions());
        }

        @Test
        @DisplayName("should throw RuntimeException when role name already exists")
        void shouldThrowWhenNameExists() {
            when(roleRepository.existsByName("ADMIN")).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                roleService.createRole("ADMIN", "Admin", "desc", new HashSet<>()));

            assertTrue(ex.getMessage().contains("ADMIN"));
            verify(roleRepository, never()).save(any());
        }
    }

    // ---- updateRole() ----

    @Nested
    @DisplayName("updateRole()")
    class UpdateRoleTests {

        @Test
        @DisplayName("should update a non-system role successfully")
        void shouldUpdateNonSystemRole() {
            Role existing = createRole(1L, "CUSTOM", "Old Name", false, new HashSet<>());
            Set<String> newPerms = new HashSet<>(Arrays.asList("dashboard:view"));

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = roleService.updateRole(1L, "New Name", "New Desc", newPerms);

            assertEquals("New Name", result.getDisplayName());
            assertEquals("New Desc", result.getDescription());
            assertEquals(newPerms, result.getPermissions());
            verify(roleRepository).save(existing);
        }

        @Test
        @DisplayName("should throw RuntimeException when role not found")
        void shouldThrowWhenRoleNotFound() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                roleService.updateRole(999L, "Name", "Desc", new HashSet<>()));

            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("should throw RuntimeException when trying to update a system role")
        void shouldThrowWhenUpdatingSystemRole() {
            Role systemRole = createRole(1L, "ADMIN", "Admin", true, new HashSet<>());
            when(roleRepository.findById(1L)).thenReturn(Optional.of(systemRole));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                roleService.updateRole(1L, "New Name", "Desc", new HashSet<>()));

            assertTrue(ex.getMessage().contains("不可修改"));
            verify(roleRepository, never()).save(any());
        }
    }

    // ---- deleteRole() ----

    @Nested
    @DisplayName("deleteRole()")
    class DeleteRoleTests {

        @Test
        @DisplayName("should delete a non-system role")
        void shouldDeleteNonSystemRole() {
            Role customRole = createRole(1L, "CUSTOM", "Custom", false, new HashSet<>());
            when(roleRepository.findById(1L)).thenReturn(Optional.of(customRole));

            roleService.deleteRole(1L);

            verify(roleRepository).delete(customRole);
        }

        @Test
        @DisplayName("should throw RuntimeException when role not found")
        void shouldThrowWhenRoleNotFound() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                roleService.deleteRole(999L));

            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("should throw RuntimeException when trying to delete a system role")
        void shouldThrowWhenDeletingSystemRole() {
            Role systemRole = createRole(1L, "ADMIN", "Admin", true, new HashSet<>());
            when(roleRepository.findById(1L)).thenReturn(Optional.of(systemRole));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                roleService.deleteRole(1L));

            assertTrue(ex.getMessage().contains("不可删除"));
            verify(roleRepository, never()).delete(any());
        }
    }

    // ---- getPermissionDescription() ----

    @Nested
    @DisplayName("getPermissionDescription()")
    class GetPermissionDescriptionTests {

        @Test
        @DisplayName("should return Chinese description for known permissions")
        void shouldReturnDescriptionForKnownPermissions() {
            assertEquals("查看仪表盘", RoleService.getPermissionDescription(RoleService.PERM_DASHBOARD_VIEW));
            assertEquals("查看 Agent 列表", RoleService.getPermissionDescription(RoleService.PERM_AGENTS_VIEW));
            assertEquals("管理 Agent", RoleService.getPermissionDescription(RoleService.PERM_AGENTS_MANAGE));
            assertEquals("发送任务给 Agent", RoleService.getPermissionDescription(RoleService.PERM_AGENTS_TASK));
            assertEquals("查看项目列表", RoleService.getPermissionDescription(RoleService.PERM_PROJECTS_VIEW));
            assertEquals("管理项目", RoleService.getPermissionDescription(RoleService.PERM_PROJECTS_MANAGE));
            assertEquals("编辑项目配置", RoleService.getPermissionDescription(RoleService.PERM_PROJECTS_EDIT));
            assertEquals("查看技能列表", RoleService.getPermissionDescription(RoleService.PERM_SKILLS_VIEW));
            assertEquals("管理技能", RoleService.getPermissionDescription(RoleService.PERM_SKILLS_MANAGE));
            assertEquals("查看用户列表", RoleService.getPermissionDescription(RoleService.PERM_USERS_VIEW));
            assertEquals("管理用户", RoleService.getPermissionDescription(RoleService.PERM_USERS_MANAGE));
            assertEquals("管理角色", RoleService.getPermissionDescription(RoleService.PERM_ROLES_MANAGE));
            assertEquals("查看操作日志", RoleService.getPermissionDescription(RoleService.PERM_LOGS_VIEW));
            assertEquals("所有权限", RoleService.getPermissionDescription(RoleService.PERM_ALL));
        }

        @Test
        @DisplayName("should return the permission string itself for unknown permissions")
        void shouldReturnRawStringForUnknownPermission() {
            assertEquals("unknown:perm", RoleService.getPermissionDescription("unknown:perm"));
        }
    }

    // ---- ALL_PERMISSIONS constant ----

    @Nested
    @DisplayName("ALL_PERMISSIONS constant")
    class AllPermissionsTests {

        @Test
        @DisplayName("should contain exactly 34 permissions")
        void shouldContain27Permissions() {
            assertEquals(34, RoleService.ALL_PERMISSIONS.size());
        }

        @Test
        @DisplayName("should not contain the wildcard permission")
        void shouldNotContainWildcard() {
            assertFalse(RoleService.ALL_PERMISSIONS.contains(RoleService.PERM_ALL));
        }

        @Test
        @DisplayName("should contain all expected permission keys")
        void shouldContainAllExpectedKeys() {
            List<String> perms = RoleService.ALL_PERMISSIONS;
            assertTrue(perms.contains(RoleService.PERM_DASHBOARD_VIEW));
            assertTrue(perms.contains(RoleService.PERM_AGENTS_VIEW));
            assertTrue(perms.contains(RoleService.PERM_AGENTS_MANAGE));
            assertTrue(perms.contains(RoleService.PERM_AGENTS_TASK));
            assertTrue(perms.contains(RoleService.PERM_PROJECTS_VIEW));
            assertTrue(perms.contains(RoleService.PERM_PROJECTS_MANAGE));
            assertTrue(perms.contains(RoleService.PERM_PROJECTS_EDIT));
            assertTrue(perms.contains(RoleService.PERM_SKILLS_VIEW));
            assertTrue(perms.contains(RoleService.PERM_SKILLS_MANAGE));
            assertTrue(perms.contains(RoleService.PERM_USERS_VIEW));
            assertTrue(perms.contains(RoleService.PERM_USERS_MANAGE));
            assertTrue(perms.contains(RoleService.PERM_ROLES_MANAGE));
            assertTrue(perms.contains(RoleService.PERM_LOGS_VIEW));
            // CICD流水线权限
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_VIEW));
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_CREATE));
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_MANAGE));
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_EXECUTE));
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_APPROVE));
            assertTrue(perms.contains(RoleService.PERM_PIPELINE_INTERVENE));
            // 监控权限
            assertTrue(perms.contains(RoleService.PERM_MONITOR_VIEW));
            assertTrue(perms.contains(RoleService.PERM_MONITOR_MANAGE));
            // 工作流权限
            assertTrue(perms.contains(RoleService.PERM_WORKFLOW_VIEW));
            assertTrue(perms.contains(RoleService.PERM_WORKFLOW_MANAGE));
            // 代码审查权限
            assertTrue(perms.contains(RoleService.PERM_CODE_REVIEW));
            // 通知管理权限
            assertTrue(perms.contains(RoleService.PERM_NOTIFICATION_MANAGE));
            // AI 助手权限
            assertTrue(perms.contains(RoleService.PERM_AI_USE));
        }
    }
}
