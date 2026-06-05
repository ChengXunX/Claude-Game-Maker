package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.AgentMcpBinding;
import com.chengxun.gamemaker.web.entity.McpServer;
import com.chengxun.gamemaker.web.entity.McpTool;
import com.chengxun.gamemaker.web.repository.AgentMcpBindingRepository;
import com.chengxun.gamemaker.web.repository.McpServerRepository;
import com.chengxun.gamemaker.web.repository.McpToolRepository;
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
 * McpService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class McpServiceTest {

    @Mock
    private McpServerRepository serverRepository;

    @Mock
    private McpToolRepository toolRepository;

    @Mock
    private AgentMcpBindingRepository bindingRepository;

    @InjectMocks
    private McpService mcpService;

    private McpServer testServer;

    @BeforeEach
    void setUp() {
        testServer = new McpServer();
        testServer.setId(1L);
        testServer.setName("GitHub");
        testServer.setTransportType(McpServer.TransportType.STDIO);
        testServer.setCommand("npx");
        testServer.setArgs("[\"-y\", \"@modelcontextprotocol/server-github\"]");
        testServer.setEnabled(true);
        testServer.setTemplate(false);
    }

    @Test
    void testGetAllServers() {
        when(serverRepository.findAllByOrderByNameAsc()).thenReturn(List.of(testServer));

        List<McpServer> result = mcpService.getAllServers();

        assertEquals(1, result.size());
        assertEquals("GitHub", result.get(0).getName());
    }

    @Test
    void testGetTemplates() {
        testServer.setTemplate(true);
        when(serverRepository.findByTemplateTrueOrderByNameAsc()).thenReturn(List.of(testServer));

        List<McpServer> result = mcpService.getTemplates();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isTemplate());
    }

    @Test
    void testSaveServer() {
        when(serverRepository.save(any())).thenReturn(testServer);

        McpServer result = mcpService.saveServer(testServer);

        assertNotNull(result);
        assertEquals("GitHub", result.getName());
        verify(serverRepository).save(testServer);
    }

    @Test
    void testDeleteServer() {
        doNothing().when(serverRepository).deleteById(1L);
        doNothing().when(bindingRepository).deleteByServerId(1L);

        mcpService.deleteServer(1L);

        verify(serverRepository).deleteById(1L);
        verify(bindingRepository).deleteByServerId(1L);
    }

    @Test
    void testToggleServer() {
        when(serverRepository.findById(1L)).thenReturn(Optional.of(testServer));
        when(serverRepository.save(any())).thenReturn(testServer);

        McpServer result = mcpService.toggleServer(1L);

        assertFalse(result.isEnabled());
        verify(serverRepository).save(any());
    }

    @Test
    void testToggleServerNotFound() {
        when(serverRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> mcpService.toggleServer(999L));
    }

    @Test
    void testBindTool() {
        when(bindingRepository.findByAgentRoleAndProjectIdAndServerIdAndToolId(
            "server-dev", "proj", 1L, 2L)).thenReturn(Optional.empty());
        when(bindingRepository.save(any())).thenReturn(new AgentMcpBinding());

        AgentMcpBinding result = mcpService.bindTool("server-dev", "proj", 1L, 2L);

        assertNotNull(result);
        verify(bindingRepository).save(any());
    }

    @Test
    void testBindToolAlreadyBound() {
        AgentMcpBinding existing = new AgentMcpBinding();
        existing.setEnabled(true);
        when(bindingRepository.findByAgentRoleAndProjectIdAndServerIdAndToolId(
            "server-dev", "proj", 1L, 2L)).thenReturn(Optional.of(existing));

        AgentMcpBinding result = mcpService.bindTool("server-dev", "proj", 1L, 2L);

        assertNotNull(result);
        assertTrue(result.isEnabled());
    }

    @Test
    void testUnbindTool() {
        doNothing().when(bindingRepository).deleteById(1L);

        mcpService.unbindTool(1L);

        verify(bindingRepository).deleteById(1L);
    }

    @Test
    void testGetBindings() {
        when(bindingRepository.findByAgentRoleAndProjectIdAndEnabledTrueOrderByPriorityAsc(
            "server-dev", "proj")).thenReturn(List.of(new AgentMcpBinding()));

        List<AgentMcpBinding> result = mcpService.getBindings("server-dev", "proj");

        assertEquals(1, result.size());
    }

    @Test
    void testGenerateMcpConfigNoBindings() {
        when(bindingRepository.findByAgentRoleAndProjectIdAndEnabledTrueOrderByPriorityAsc(
            "server-dev", "proj")).thenReturn(List.of());

        String config = mcpService.generateMcpConfig("server-dev", "proj");

        assertNull(config);
    }

    @Test
    void testToggleTool() {
        McpTool tool = new McpTool();
        tool.setId(1L);
        tool.setEnabled(true);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any())).thenReturn(tool);

        McpTool result = mcpService.toggleTool(1L);

        assertFalse(result.isEnabled());
    }

    @Test
    void testSetToolApproval() {
        McpTool tool = new McpTool();
        tool.setId(1L);
        tool.setRequiresApproval(false);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any())).thenReturn(tool);

        McpTool result = mcpService.setToolApproval(1L, true);

        assertTrue(result.isRequiresApproval());
    }
}
