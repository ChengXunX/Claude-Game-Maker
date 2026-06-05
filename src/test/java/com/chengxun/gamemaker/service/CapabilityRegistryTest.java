package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.AgentCapability;
import com.chengxun.gamemaker.web.repository.AgentCapabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CapabilityRegistry 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CapabilityRegistryTest {

    @Mock
    private AgentCapabilityRepository capabilityRepository;

    @InjectMocks
    private CapabilityRegistry capabilityRegistry;

    private AgentCapability testCapability;

    @BeforeEach
    void setUp() {
        testCapability = new AgentCapability();
        testCapability.setId(1L);
        testCapability.setAgentRole("producer");
        testCapability.setCapabilityName("createAgent");
        testCapability.setDisplayName("创建 Agent");
        testCapability.setEnabled(true);
        testCapability.setPriority(1);
    }

    @Test
    void testGetCapabilities() {
        // 设置全局能力mock
        when(capabilityRepository.findAllByOrderByAgentRoleAscPriorityAsc())
            .thenReturn(List.of(testCapability));

        // 重新加载缓存
        capabilityRegistry.reloadAll();

        List<AgentCapability> result = capabilityRegistry.getCapabilities("producer");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("createAgent", result.get(0).getCapabilityName());
    }

    @Test
    void testGetCapability_Found() {
        when(capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("createAgent", "producer"))
            .thenReturn(Optional.of(testCapability));

        AgentCapability result = capabilityRegistry.getCapability("producer", "createAgent");

        assertNotNull(result);
        assertEquals("createAgent", result.getCapabilityName());
    }

    @Test
    void testGetCapability_NotFound() {
        when(capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("unknown", "producer"))
            .thenReturn(Optional.empty());

        AgentCapability result = capabilityRegistry.getCapability("producer", "unknown");

        assertNull(result);
    }

    @Test
    void testGetCapability_Disabled() {
        testCapability.setEnabled(false);
        when(capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("createAgent", "producer"))
            .thenReturn(Optional.of(testCapability));

        AgentCapability result = capabilityRegistry.getCapability("producer", "createAgent");

        assertNull(result);
    }

    @Test
    void testIsCapabilityEnabled() {
        when(capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("createAgent", "producer"))
            .thenReturn(Optional.of(testCapability));

        assertTrue(capabilityRegistry.isCapabilityEnabled("producer", "createAgent"));
    }

    @Test
    void testValidateParams_Valid() {
        testCapability.setParamSchema("{\"name\":\"string|required\",\"role\":\"string\"}");
        when(capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("createAgent", "producer"))
            .thenReturn(Optional.of(testCapability));

        var result = capabilityRegistry.validateParams("producer", "createAgent",
            java.util.Map.of("name", "test", "role", "server-dev"));

        assertTrue(result.isValid());
    }

    @Test
    void testValidateParams_MissingRequired() {
        testCapability.setParamSchema("{\"name\":\"string|required\"}");
        when(capabilityRepository.findByCapabilityNameAndAgentRoleAndProjectIdIsNull("createAgent", "producer"))
            .thenReturn(Optional.of(testCapability));

        var result = capabilityRegistry.validateParams("producer", "createAgent",
            java.util.Map.of("role", "server-dev"));

        assertFalse(result.isValid());
    }

    @Test
    void testToggleEnabled() {
        when(capabilityRepository.findById(1L)).thenReturn(Optional.of(testCapability));
        when(capabilityRepository.save(any())).thenReturn(testCapability);

        AgentCapability result = capabilityRegistry.toggleEnabled(1L);

        assertNotNull(result);
        verify(capabilityRepository).save(any());
    }
}
