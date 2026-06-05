package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.model.AgentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent能力管理服务
 * 负责Agent的自定义标签、能力匹配和任务分配
 *
 * 主要功能：
 * - 管理Agent的自定义标签
 * - 根据能力匹配任务
 * - 上下文大小管理
 * - API能力检测
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class AgentCapabilityService {

    private static final Logger log = LoggerFactory.getLogger(AgentCapabilityService.class);

    @Autowired
    private AgentManager agentManager;

    /**
     * 获取Agent的标签
     */
    public Map<String, String> getAgentTags(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return Collections.emptyMap();
        }
        return agent.getDefinition().getTags();
    }

    /**
     * 设置Agent的标签
     */
    public void setAgentTag(String agentId, String key, String value) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            agent.getDefinition().setTag(key, value);
            log.info("Set tag for agent {}: {} = {}", agentId, key, value);
        }
    }

    /**
     * 删除Agent的标签
     */
    public void removeAgentTag(String agentId, String key) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            agent.getDefinition().removeTag(key);
            log.info("Removed tag from agent {}: {}", agentId, key);
        }
    }

    /**
     * 获取Agent的能力列表
     */
    public Set<String> getAgentCapabilities(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return Collections.emptySet();
        }
        return agent.getDefinition().getCapabilities();
    }

    /**
     * 添加Agent能力
     */
    public void addCapability(String agentId, String capability) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            agent.getDefinition().addCapability(capability);
            log.info("Added capability to agent {}: {}", agentId, capability);
        }
    }

    /**
     * 根据能力查找合适的Agent
     * @param requiredCapabilities 需要的能力
     * @param excludeAgentIds 排除的Agent ID
     * @return 匹配的Agent列表（按匹配度排序）
     */
    public List<Agent> findAgentsByCapabilities(Set<String> requiredCapabilities, Set<String> excludeAgentIds) {
        List<Agent> allAgents = agentManager.getAllAgents();

        return allAgents.stream()
            .filter(agent -> !excludeAgentIds.contains(agent.getId()))
            .filter(agent -> agent.isAlive())
            .map(agent -> {
                Set<String> agentCaps = agent.getDefinition().getCapabilities();
                long matchCount = requiredCapabilities.stream()
                    .filter(agentCaps::contains)
                    .count();
                return new AbstractMap.SimpleEntry<>(agent, matchCount);
            })
            .filter(entry -> entry.getValue() > 0)
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 根据标签查找Agent
     */
    public List<Agent> findAgentsByTag(String key, String value) {
        List<Agent> allAgents = agentManager.getAllAgents();

        return allAgents.stream()
            .filter(agent -> {
                String tagValue = agent.getDefinition().getTag(key);
                return value.equals(tagValue);
            })
            .collect(Collectors.toList());
    }

    /**
     * 根据部门查找Agent
     */
    public List<Agent> findAgentsByDepartment(String department) {
        return findAgentsByTag("department", department);
    }

    /**
     * 获取Agent的上下文使用情况
     */
    public Map<String, Object> getContextUsage(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return Collections.emptyMap();
        }

        AgentDefinition def = agent.getDefinition();
        Map<String, Object> usage = new HashMap<>();
        usage.put("agentId", agentId);
        usage.put("maxContextSize", def.getMaxContextSize());
        usage.put("currentUsage", def.getCurrentContextUsage());
        usage.put("usagePercent", def.getContextUsagePercent());
        usage.put("remainingSize", def.getRemainingContextSize());
        usage.put("nearLimit", def.isContextNearLimit());
        usage.put("warningThreshold", def.getContextWarningThreshold());

        return usage;
    }

    /**
     * 更新Agent的上下文使用量
     */
    public void updateContextUsage(String agentId, int usage) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent != null) {
            agent.getDefinition().setCurrentContextUsage(usage);

            // 检查是否需要警告
            if (agent.getDefinition().isContextNearLimit()) {
                log.warn("Agent {} context usage is near limit: {:.1f}%",
                    agentId, agent.getDefinition().getContextUsagePercent());
            }
        }
    }

    /**
     * 获取Agent的API能力
     */
    public Map<String, Object> getApiCapabilities(String agentId) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return Collections.emptyMap();
        }

        AgentDefinition def = agent.getDefinition();
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("agentId", agentId);
        capabilities.put("supportsImageGeneration", def.isSupportsImageGeneration());
        capabilities.put("supportsCodeExecution", def.isSupportsCodeExecution());
        capabilities.put("supportsFileOperations", def.isSupportsFileOperations());
        capabilities.put("apiProvider", def.getApiProvider());
        capabilities.put("unsupportedFeatures", def.getUnsupportedFeatures());

        return capabilities;
    }

    /**
     * 检查Agent是否支持特定功能
     */
    public boolean supportsFeature(String agentId, String feature) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return false;
        }
        return agent.getDefinition().supportsFeature(feature);
    }

    /**
     * 检查Agent是否支持特定文件类型
     */
    public boolean supportsFileType(String agentId, String fileType) {
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return false;
        }
        return agent.getDefinition().supportsFileType(fileType);
    }

    /**
     * 智能任务分配
     * 根据任务描述、所需能力和Agent负载进行分配
     *
     * @param taskDescription 任务描述
     * @param requiredCapabilities 需要的能力
     * @param requiredFileType 需要处理的文件类型
     * @param maxLoad 最大负载
     * @return 推荐的Agent列表
     */
    public List<Agent> recommendAgentsForTask(String taskDescription, Set<String> requiredCapabilities,
                                               String requiredFileType, int maxLoad) {
        List<Agent> candidates = new ArrayList<>();

        for (Agent agent : agentManager.getAllAgents()) {
            if (!agent.isAlive() || agent.isBusy()) {
                continue;
            }

            AgentDefinition def = agent.getDefinition();

            // 检查能力匹配
            if (requiredCapabilities != null && !requiredCapabilities.isEmpty()) {
                boolean hasCapability = requiredCapabilities.stream()
                    .anyMatch(def.getCapabilities()::contains);
                if (!hasCapability) {
                    continue;
                }
            }

            // 检查文件类型支持
            if (requiredFileType != null && !requiredFileType.isEmpty()) {
                if (!def.supportsFileType(requiredFileType)) {
                    continue;
                }
            }

            // 检查功能支持
            if (taskDescription != null) {
                if (taskDescription.contains("图片") && !def.isSupportsImageGeneration()) {
                    // 如果任务需要图片生成但Agent不支持，降低优先级但不排除
                    // 可以使用替代方案
                }
            }

            candidates.add(agent);
        }

        return candidates;
    }

    /**
     * 获取所有Agent的能力摘要
     */
    public List<Map<String, Object>> getAllAgentsCapabilities() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Agent agent : agentManager.getAllAgents()) {
            AgentDefinition def = agent.getDefinition();
            Map<String, Object> info = new HashMap<>();
            info.put("id", agent.getId());
            info.put("name", agent.getName());
            info.put("role", agent.getRole());
            info.put("tags", def.getTags());
            info.put("capabilities", def.getCapabilities());
            info.put("supportedFileTypes", def.getSupportedFileTypes());
            info.put("supportsImageGeneration", def.isSupportsImageGeneration());
            info.put("contextUsagePercent", def.getContextUsagePercent());
            info.put("apiProvider", def.getApiProvider());
            result.add(info);
        }

        return result;
    }

    /**
     * 获取能力统计
     */
    public Map<String, Object> getCapabilityStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Agent> allAgents = agentManager.getAllAgents();
        stats.put("totalAgents", allAgents.size());

        // 统计各能力的Agent数量
        Map<String, Long> capabilityCounts = new HashMap<>();
        Map<String, Long> departmentCounts = new HashMap<>();
        int imageCapableCount = 0;

        for (Agent agent : allAgents) {
            AgentDefinition def = agent.getDefinition();

            // 统计能力
            for (String cap : def.getCapabilities()) {
                capabilityCounts.merge(cap, 1L, Long::sum);
            }

            // 统计部门
            String dept = def.getTag("department");
            if (dept != null) {
                departmentCounts.merge(dept, 1L, Long::sum);
            }

            // 统计图片生成能力
            if (def.isSupportsImageGeneration()) {
                imageCapableCount++;
            }
        }

        stats.put("capabilityCounts", capabilityCounts);
        stats.put("departmentCounts", departmentCounts);
        stats.put("imageCapableAgents", imageCapableCount);
        stats.put("nonImageCapableAgents", allAgents.size() - imageCapableCount);

        return stats;
    }
}
