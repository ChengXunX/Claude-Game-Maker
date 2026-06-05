package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.web.dto.ErrorResponse;
import com.chengxun.gamemaker.web.entity.AgentFile;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.service.AgentFileService;
import com.chengxun.gamemaker.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Agent 文件管理控制器
 * 提供文件上传下载、列表查询、版本管理、配额查询
 *
 * @author chengxun
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/files")
@PreAuthorize("isAuthenticated()")
public class AgentFileController {

    private static final Logger log = LoggerFactory.getLogger(AgentFileController.class);

    private final AgentFileService fileService;
    private final UserService userService;

    public AgentFileController(AgentFileService fileService, UserService userService) {
        this.fileService = fileService;
        this.userService = userService;
    }

    /**
     * 上传文件到 Agent
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('PERM_agents:manage', 'PERM_agents:task', 'PERM_admin:manage')")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam String agentId,
                                         @RequestParam String projectId,
                                         @RequestParam(required = false) String remark,
                                         Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            AgentFile saved = fileService.uploadFile(agentId, projectId, file,
                user != null ? user.getUsername() : "unknown", remark);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id, Authentication authentication) {
        try {
            AgentFile file = fileService.getFile(id);
            Path filePath = fileService.getFilePath(id);
            Resource resource = new FileSystemResource(filePath.toFile());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                    file.getMimeType() != null ? file.getMimeType() : "application/octet-stream"))
                .contentLength(file.getFileSize())
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取 Agent 的文件列表
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<Page<AgentFile>> getAgentFiles(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(fileService.getAgentFiles(agentId, PageRequest.of(page, size)));
    }

    /**
     * 获取项目的文件列表
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<AgentFile>> getProjectFiles(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(fileService.getProjectFiles(projectId, PageRequest.of(page, size)));
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search/{agentId}")
    public ResponseEntity<Page<AgentFile>> searchFiles(
            @PathVariable String agentId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(fileService.searchFiles(agentId, keyword, PageRequest.of(page, size)));
    }

    /**
     * 获取文件版本历史
     */
    @GetMapping("/{agentId}/versions")
    public ResponseEntity<List<AgentFile>> getFileVersions(@PathVariable String agentId,
                                                            @RequestParam String fileName) {
        return ResponseEntity.ok(fileService.getFileVersions(agentId, fileName));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            fileService.deleteFile(id, user != null ? user.getUsername() : "unknown");
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取 Agent 存储使用情况
     */
    @GetMapping("/usage/{agentId}")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@PathVariable String agentId) {
        return ResponseEntity.ok(fileService.getStorageUsage(agentId));
    }
}
