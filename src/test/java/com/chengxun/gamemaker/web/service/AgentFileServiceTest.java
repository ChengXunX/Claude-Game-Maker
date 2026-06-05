package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.AgentFile;
import com.chengxun.gamemaker.web.repository.AgentFileRepository;
import com.chengxun.gamemaker.web.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AgentFileService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AgentFileServiceTest {

    @Mock
    private AgentFileRepository fileRepository;

    @Mock
    private NotificationWebSocketHandler wsHandler;

    @InjectMocks
    private AgentFileService fileService;

    private AgentFile testFile;

    @BeforeEach
    void setUp() {
        testFile = new AgentFile();
        testFile.setId(1L);
        testFile.setAgentId("proj:server-dev");
        testFile.setProjectId("proj");
        testFile.setFileName("test.txt");
        testFile.setFilePath("proj/server-dev/2026/06/01/test.txt");
        testFile.setFileSize(1024);
        testFile.setDeleted(false);
    }

    @Test
    void testGetAgentFiles() {
        Page<AgentFile> page = new PageImpl<>(List.of(testFile));
        when(fileRepository.findByAgentIdAndDeletedFalseOrderByCreatedAtDesc(
            "proj:server-dev", PageRequest.of(0, 20))).thenReturn(page);

        Page<AgentFile> result = fileService.getAgentFiles("proj:server-dev", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test.txt", result.getContent().get(0).getFileName());
    }

    @Test
    void testGetProjectFiles() {
        Page<AgentFile> page = new PageImpl<>(List.of(testFile));
        when(fileRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(
            "proj", PageRequest.of(0, 20))).thenReturn(page);

        Page<AgentFile> result = fileService.getProjectFiles("proj", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testDeleteFile() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        when(fileRepository.save(any())).thenReturn(testFile);

        fileService.deleteFile(1L, "admin");

        assertTrue(testFile.isDeleted());
        verify(fileRepository).save(testFile);
    }

    @Test
    void testDeleteFileNotFound() {
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.deleteFile(999L, "admin"));
    }

    @Test
    void testGetStorageUsage() {
        when(fileRepository.sumFileSizeByAgentId("proj:server-dev")).thenReturn(5 * 1024 * 1024L);
        when(fileRepository.countByAgentIdAndDeletedFalse("proj:server-dev")).thenReturn(10L);

        Map<String, Object> usage = fileService.getStorageUsage("proj:server-dev");

        assertNotNull(usage);
        assertEquals(5L, usage.get("usedMB"));
        assertEquals(10L, usage.get("fileCount"));
    }

    @Test
    void testGetFile() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));

        AgentFile result = fileService.getFile(1L);

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
    }

    @Test
    void testSearchFiles() {
        Page<AgentFile> page = new PageImpl<>(List.of(testFile));
        when(fileRepository.searchByFileName("proj:server-dev", "test", PageRequest.of(0, 20)))
            .thenReturn(page);

        Page<AgentFile> result = fileService.searchFiles("proj:server-dev", "test", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetFileVersions() {
        when(fileRepository.findByAgentIdAndFileNameAndDeletedFalseOrderByVersionDesc(
            "proj:server-dev", "test.txt")).thenReturn(List.of(testFile));

        List<AgentFile> versions = fileService.getFileVersions("proj:server-dev", "test.txt");

        assertNotNull(versions);
        assertEquals(1, versions.size());
    }
}
