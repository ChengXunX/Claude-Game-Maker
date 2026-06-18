package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.engine.ResourceGenerationEngine;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 资源资产管理服务
 * 管理 AI 生成的资源文件（音频、图片、UI 素材等）
 *
 * 资源元数据存储在 project.metadata["assets"] 中（JSON 字符串格式）
 *
 * @author chengxun
 * @since 4.0.0
 */
@Service
public class ResourceAssetService {

    private static final Logger log = LoggerFactory.getLogger(ResourceAssetService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ResourceGenerationEngine resourceEngine;

    /**
     * 生成并保存资源
     */
    public ResourceGenerationEngine.ResourceResult generateAndSave(String projectId,
                                                                     ResourceGenerationEngine.ResourceRequest request) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            return ResourceGenerationEngine.ResourceResult.failure("项目不存在: " + projectId);
        }

        String assetType = getAssetSubDir(request.resourceType);
        String outputDir = project.getWorkDir() + "/assets/" + assetType;
        request.outputDir = outputDir;

        ResourceGenerationEngine.ResourceResult result = resourceEngine.generateResource(request);

        if (result.success) {
            saveAssetMetadata(project, request.resourceType, result);
            log.info("资源已生成并保存: project={}, type={}, file={}", projectId, request.resourceType, result.filePath);
        }

        return result;
    }

    /**
     * 保存资源元数据到项目 metadata["assets"]（JSON 字符串）
     */
    private void saveAssetMetadata(GameProject project, ResourceGenerationEngine.ResourceType type,
                                    ResourceGenerationEngine.ResourceResult result) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("type", type.name());
        asset.put("filePath", result.filePath);
        asset.put("fileUrl", result.fileUrl);
        asset.put("mimeType", result.mimeType);
        asset.put("createdAt", LocalDateTime.now().toString());
        asset.putAll(result.metadata);

        List<Map<String, Object>> assets = getAssetsFromProject(project);
        assets.add(asset);

        try {
            project.getMetadata().put("assets", mapper.writeValueAsString(assets));
            project.getMetadata().put("assets_count", String.valueOf(assets.size()));
        } catch (Exception e) {
            log.warn("保存资源元数据失败: {}", e.getMessage());
        }

        projectManager.saveProjectConfig(project);
    }

    /**
     * 获取项目的资源清单
     */
    public List<Map<String, Object>> getAssets(String projectId) {
        GameProject project = projectManager.getProject(projectId);
        if (project == null) return List.of();
        return getAssetsFromProject(project);
    }

    /**
     * 按类型获取项目资源
     */
    public List<Map<String, Object>> getAssetsByType(String projectId, ResourceGenerationEngine.ResourceType type) {
        return getAssets(projectId).stream()
            .filter(a -> type.name().equals(a.get("type")))
            .toList();
    }

    /**
     * 获取资源清单文本（供 Agent 上下文使用）
     */
    public String getAssetManifest(String projectId) {
        List<Map<String, Object>> assets = getAssets(projectId);
        if (assets.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("## 项目资源清单\n\n");
        Map<String, List<Map<String, Object>>> byType = new LinkedHashMap<>();
        for (Map<String, Object> asset : assets) {
            String type = (String) asset.getOrDefault("type", "OTHER");
            byType.computeIfAbsent(type, k -> new ArrayList<>()).add(asset);
        }

        for (var entry : byType.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n");
            for (var asset : entry.getValue()) {
                String filePath = (String) asset.getOrDefault("filePath", "未知");
                String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1) : filePath;
                sb.append("- ").append(fileName);
                if (asset.containsKey("mimeType")) {
                    sb.append(" (").append(asset.get("mimeType")).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查项目是否有指定类型的资源
     */
    public boolean hasAsset(String projectId, ResourceGenerationEngine.ResourceType type) {
        return getAssetsByType(projectId, type).size() > 0;
    }

    private String getAssetSubDir(ResourceGenerationEngine.ResourceType type) {
        return switch (type) {
            case AUDIO_MUSIC -> "audio/music";
            case AUDIO_SFX -> "audio/sfx";
            case IMAGE_SPRITE -> "images/sprites";
            case IMAGE_UI -> "images/ui";
            case SHADER_CODE -> "shaders";
            default -> "other";
        };
    }

    /**
     * 从项目 metadata["assets"] 中解析资源列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAssetsFromProject(GameProject project) {
        String assetsJson = project.getMetadata().get("assets");
        if (assetsJson != null && !assetsJson.isEmpty()) {
            try {
                return mapper.readValue(assetsJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                log.warn("解析资源元数据失败: {}", e.getMessage());
            }
        }
        return new ArrayList<>();
    }
}
