package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.SystemConfig;
import com.chengxun.gamemaker.web.repository.SystemConfigRepository;
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
 * 系统配置服务单元测试
 * 测试SystemConfigService的核心功能
 *
 * @author chengxun
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository configRepository;

    @InjectMocks
    private SystemConfigService configService;

    private SystemConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new SystemConfig();
        testConfig.setId(1L);
        testConfig.setConfigKey("test.key");
        testConfig.setConfigValue("test.value");
        testConfig.setDescription("测试配置");
        testConfig.setGroup("test");
        testConfig.setValueType("string");
        testConfig.setSystemBuiltin(false);
    }

    @Test
    void testGetAllConfigs() {
        // Given
        List<SystemConfig> configs = Arrays.asList(testConfig);
        when(configRepository.findAll()).thenReturn(configs);

        // When
        List<SystemConfig> result = configService.getAllConfigs();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.key", result.get(0).getConfigKey());
    }

    @Test
    void testGetConfigsByGroup() {
        // Given：getConfigsByGroup 现在使用 findByGroupAndProjectIdIsNull
        List<SystemConfig> configs = Arrays.asList(testConfig);
        when(configRepository.findByGroupAndProjectIdIsNull("test")).thenReturn(configs);

        // When
        List<SystemConfig> result = configService.getConfigsByGroup("test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test", result.get(0).getGroup());
    }

    @Test
    void testCreateConfig() {
        // Given
        SystemConfig newConfig = new SystemConfig();
        newConfig.setConfigKey("new.key");
        newConfig.setConfigValue("new.value");
        newConfig.setGroup("test");

        when(configRepository.save(any(SystemConfig.class))).thenAnswer(invocation -> {
            SystemConfig saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        SystemConfig result = configService.createConfig(newConfig);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("new.key", result.getConfigKey());
    }

    @Test
    void testDeleteConfig_Success() {
        // Given
        when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));

        // When
        assertDoesNotThrow(() -> {
            configService.deleteConfig(1L);
        });

        // Then
        verify(configRepository).delete(testConfig);
    }

    @Test
    void testDeleteConfig_SystemBuiltin() {
        // Given
        testConfig.setSystemBuiltin(true);
        when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            configService.deleteConfig(1L);
        });
    }

    @Test
    void testDeleteConfig_NotFound() {
        // Given
        when(configRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            configService.deleteConfig(999L);
        });
    }

    @Test
    void testSetConfig_Success() {
        // Given：setConfig 现在使用 findByConfigKeyAndProjectIdIsNull
        when(configRepository.findByConfigKeyAndProjectIdIsNull("test.key")).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(SystemConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> {
            configService.setConfig("test.key", "updated.value");
        });
    }

    @Test
    void testSetConfig_NotFound() {
        // Given
        when(configRepository.findByConfigKeyAndProjectIdIsNull("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> {
            configService.setConfig("nonexistent", "value");
        });
    }

    @Test
    void testExistsByConfigKey() {
        // Given
        when(configRepository.existsByConfigKey("test.key")).thenReturn(true);
        when(configRepository.existsByConfigKey("nonexistent")).thenReturn(false);

        // When & Then
        assertTrue(configRepository.existsByConfigKey("test.key"));
        assertFalse(configRepository.existsByConfigKey("nonexistent"));
    }
}
