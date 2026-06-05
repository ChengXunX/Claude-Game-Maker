package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.SystemConstant;
import com.chengxun.gamemaker.web.repository.SystemConstantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SystemConstantService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SystemConstantServiceTest {

    @Mock
    private SystemConstantRepository constantRepository;

    @InjectMocks
    private SystemConstantService constantService;

    private SystemConstant testConstant;

    @BeforeEach
    void setUp() {
        testConstant = new SystemConstant();
        testConstant.setId(1L);
        testConstant.setConstantKey("agent.max-idle-minutes");
        testConstant.setDisplayName("最大空闲时间");
        testConstant.setDescription("Agent 最大无响应时间");
        testConstant.setValue("30");
        testConstant.setDefaultValue("30");
        testConstant.setValueType("int");
        testConstant.setGroupName("agent");
        testConstant.setUnit("分钟");
        testConstant.setMinValue(1L);
        testConstant.setMaxValue(1440L);
    }

    @Test
    void testGetAll() {
        when(constantRepository.findAllByOrderByGroupNameAscDisplayNameAsc())
            .thenReturn(List.of(testConstant));

        List<SystemConstant> result = constantService.getAll();

        assertEquals(1, result.size());
    }

    @Test
    void testGetByGroup() {
        when(constantRepository.findByGroupNameOrderByDisplayNameAsc("agent"))
            .thenReturn(List.of(testConstant));

        List<SystemConstant> result = constantService.getByGroup("agent");

        assertEquals(1, result.size());
        assertEquals("agent", result.get(0).getGroupName());
    }

    @Test
    void testUpdate() {
        when(constantRepository.findByConstantKey("agent.max-idle-minutes"))
            .thenReturn(Optional.of(testConstant));
        when(constantRepository.save(any())).thenReturn(testConstant);

        SystemConstant result = constantService.update("agent.max-idle-minutes", "60");

        assertNotNull(result);
        assertEquals("60", result.getValue());
    }

    @Test
    void testUpdateNotFound() {
        when(constantRepository.findByConstantKey("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> constantService.update("nonexistent", "10"));
    }

    @Test
    void testUpdateOutOfRange() {
        when(constantRepository.findByConstantKey("agent.max-idle-minutes"))
            .thenReturn(Optional.of(testConstant));

        assertThrows(RuntimeException.class, () -> constantService.update("agent.max-idle-minutes", "99999"));
    }

    @Test
    void testResetToDefault() {
        testConstant.setValue("60");
        when(constantRepository.findByConstantKey("agent.max-idle-minutes"))
            .thenReturn(Optional.of(testConstant));
        when(constantRepository.save(any())).thenReturn(testConstant);

        SystemConstant result = constantService.resetToDefault("agent.max-idle-minutes");

        assertEquals("30", result.getValue());
    }

    @Test
    void testBatchUpdate() {
        when(constantRepository.findByConstantKey(any())).thenReturn(Optional.of(testConstant));
        when(constantRepository.save(any())).thenReturn(testConstant);

        Map<String, String> updates = Map.of("agent.max-idle-minutes", "45");
        List<SystemConstant> results = constantService.batchUpdate(updates);

        assertEquals(1, results.size());
    }

    @Test
    void testGetGroups() {
        testConstant.setGroupName("agent");
        SystemConstant secConstant = new SystemConstant();
        secConstant.setGroupName("security");

        when(constantRepository.findAllByOrderByGroupNameAscDisplayNameAsc())
            .thenReturn(List.of(testConstant, secConstant));

        Set<String> groups = constantService.getGroups();

        assertTrue(groups.contains("agent"));
        assertTrue(groups.contains("security"));
    }

    @Test
    void testResetAllToDefault() {
        testConstant.setValue("60");
        when(constantRepository.findAll()).thenReturn(List.of(testConstant));
        when(constantRepository.saveAll(any())).thenReturn(List.of(testConstant));

        constantService.resetAllToDefault();

        verify(constantRepository).saveAll(any());
    }
}
