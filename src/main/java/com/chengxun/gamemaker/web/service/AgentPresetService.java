package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import com.chengxun.gamemaker.model.AgentDefinition;
import com.chengxun.gamemaker.web.entity.AgentPreset;
import com.chengxun.gamemaker.web.repository.AgentPresetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Agent 预设服务
 * 管理全局 Agent 预设模板，支持从运行中的 Agent 导出能力
 *
 * 主要功能：
 * - 预设的 CRUD 操作
 * - 从运行中的 Agent 导出为全局预设
 * - 从预设创建 AgentDefinition
 * - 系统内置预设管理
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AgentPresetService {

    private static final Logger log = LoggerFactory.getLogger(AgentPresetService.class);

    private final AgentPresetRepository presetRepository;
    private final ObjectMapper objectMapper;

    public AgentPresetService(AgentPresetRepository presetRepository, ObjectMapper objectMapper) {
        this.presetRepository = presetRepository;
        this.objectMapper = objectMapper;
    }

    // ===== CRUD 操作 =====

    public List<AgentPreset> getAllPresets() {
        return presetRepository.findAll();
    }

    public Optional<AgentPreset> getPresetById(Long id) {
        return presetRepository.findById(id);
    }

    public List<AgentPreset> getPresetsByRole(String role) {
        return presetRepository.findByRole(role);
    }

    public List<AgentPreset> getSystemPresets() {
        return presetRepository.findBySystemTrue();
    }

    public List<AgentPreset> searchPresets(String name) {
        return presetRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public AgentPreset createPreset(AgentPreset preset) {
        preset.setSystem(false);
        return presetRepository.save(preset);
    }

    @Transactional
    public AgentPreset updatePreset(Long id, AgentPreset updated) {
        AgentPreset existing = presetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("预设不存在: " + id));

        if (existing.isSystem()) {
            throw new IllegalStateException("系统内置预设不可修改");
        }

        existing.setName(updated.getName());
        existing.setRole(updated.getRole());
        existing.setDescription(updated.getDescription());
        existing.setReasoningDepth(updated.getReasoningDepth());
        existing.setCapabilities(updated.getCapabilities());
        existing.setTags(updated.getTags());
        existing.setSupportedFileTypes(updated.getSupportedFileTypes());
        existing.setUnsupportedFeatures(updated.getUnsupportedFeatures());
        existing.setMaxContextSize(updated.getMaxContextSize());
        existing.setSupportsImageGeneration(updated.isSupportsImageGeneration());
        existing.setSupportsCodeExecution(updated.isSupportsCodeExecution());
        existing.setSupportsFileOperations(updated.isSupportsFileOperations());
        existing.setApiProvider(updated.getApiProvider());

        return presetRepository.save(existing);
    }

    @Transactional
    public void deletePreset(Long id) {
        AgentPreset preset = presetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("预设不存在: " + id));

        if (preset.isSystem()) {
            throw new IllegalStateException("系统内置预设不可删除");
        }

        presetRepository.deleteById(id);
    }

    // ===== 从 Agent 导出预设 =====

    /**
     * 从运行中的 Agent 导出为全局预设
     * 用于将优秀 Agent 的能力同步到全局预设库
     *
     * @param agent 运行中的 Agent
     * @param presetName 预设名称
     * @return 创建的预设
     */
    @Transactional
    public AgentPreset exportFromAgent(Agent agent, String presetName) {
        AgentDefinition def = agent.getDefinition();

        AgentPreset preset = new AgentPreset();
        preset.setName(presetName);
        preset.setRole(def.getRole());
        preset.setDescription(def.getDescription());
        preset.setReasoningDepth(def.getReasoningDepth());
        preset.setMaxContextSize(def.getMaxContextSize());
        preset.setSupportsImageGeneration(def.isSupportsImageGeneration());
        preset.setSupportsCodeExecution(def.isSupportsCodeExecution());
        preset.setSupportsFileOperations(def.isSupportsFileOperations());
        preset.setApiProvider(def.getApiProvider());
        preset.setSystem(false);

        // 记录来源
        preset.setSourceAgentId(agent.getId());
        if (agent instanceof BaseAgent baseAgent && baseAgent.getCurrentProject() != null) {
            preset.setSourceProjectId(baseAgent.getCurrentProject().getId());
        }

        // 序列化集合字段
        try {
            preset.setCapabilities(objectMapper.writeValueAsString(def.getCapabilities()));
            preset.setTags(objectMapper.writeValueAsString(def.getTags()));
            preset.setSupportedFileTypes(objectMapper.writeValueAsString(def.getSupportedFileTypes()));
            preset.setUnsupportedFeatures(objectMapper.writeValueAsString(def.getUnsupportedFeatures()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize agent attributes: {}", e.getMessage());
        }

        AgentPreset saved = presetRepository.save(preset);
        log.info("Exported agent {} to preset: {} (role={})", agent.getId(), saved.getId(), def.getRole());
        return saved;
    }

    // ===== 从预设创建 AgentDefinition =====

    /**
     * 从预设创建 AgentDefinition
     *
     * @param presetId 预设 ID
     * @param projectId 项目 ID
     * @param agentId Agent 原始 ID
     * @param agentName Agent 名称
     * @param workDir 工作目录
     * @return AgentDefinition
     */
    public AgentDefinition createDefinitionFromPreset(Long presetId, String projectId,
                                                       String agentId, String agentName, String workDir) {
        AgentPreset preset = presetRepository.findById(presetId)
            .orElseThrow(() -> new IllegalArgumentException("预设不存在: " + presetId));

        AgentDefinition.Builder builder = AgentDefinition.builder()
            .id(agentId)
            .name(agentName)
            .role(preset.getRole())
            .description(preset.getDescription())
            .workDir(workDir)
            .projectId(projectId)
            .reasoningDepth(preset.getReasoningDepth())
            .maxContextSize(preset.getMaxContextSize())
            .supportsImageGeneration(preset.isSupportsImageGeneration())
            .supportsCodeExecution(preset.isSupportsCodeExecution())
            .supportsFileOperations(preset.isSupportsFileOperations())
            .apiProvider(preset.getApiProvider());

        // 反序列化集合字段
        try {
            if (preset.getCapabilities() != null) {
                Set<String> caps = objectMapper.readValue(preset.getCapabilities(),
                    objectMapper.getTypeFactory().constructCollectionType(HashSet.class, String.class));
                builder.capabilities(caps);
            }
            if (preset.getSupportedFileTypes() != null) {
                Set<String> types = objectMapper.readValue(preset.getSupportedFileTypes(),
                    objectMapper.getTypeFactory().constructCollectionType(HashSet.class, String.class));
                for (String type : types) {
                    builder.supportedFileType(type);
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize preset attributes: {}", e.getMessage());
        }

        return builder.build();
    }

    /**
     * 初始化系统内置预设
     * 在应用启动时调用，确保基础预设存在
     */
    @Transactional
    public void initSystemPresets() {
        if (!presetRepository.findBySystemTrue().isEmpty()) {
            return; // 已初始化
        }

        log.info("Initializing system agent presets...");

        createSystemPreset("标准制作人", "producer", "项目制作人，负责协调团队、分配任务、审查工作", 3);
        createSystemPreset("标准服务端开发", "server-dev", "服务端开发 Agent，负责后端逻辑、API 接口、数据库设计", 3);
        createSystemPreset("标准客户端开发", "client-dev", "客户端开发 Agent，负责前端逻辑、交互实现、性能优化", 3);
        createSystemPreset("标准 UI 开发", "ui-dev", "UI/美术开发 Agent，负责界面设计、图标制作、视觉效果", 3);
        createSystemPreset("标准系统策划", "system-planner", "系统策划 Agent，负责游戏系统设计、玩法策划、文档编写", 3);
        createSystemPreset("标准数值策划", "numerical-planner", "数值策划 Agent，负责游戏数值设计、经济系统、平衡性", 3);
        createSystemPreset("标准 Git 专员", "git-commit", "Git 提交专员 Agent，负责代码提交、分支管理、版本控制", 2);
        createSystemPreset("标准测试员", "tester", "测试 Agent，负责功能测试、性能测试、Bug 报告", 2);

        log.info("System agent presets initialized");
    }

    private void createSystemPreset(String name, String role, String description, int depth) {
        AgentPreset preset = new AgentPreset();
        preset.setName(name);
        preset.setRole(role);
        preset.setDescription(description);
        preset.setReasoningDepth(depth);
        preset.setSystem(true);
        presetRepository.save(preset);
    }
}
