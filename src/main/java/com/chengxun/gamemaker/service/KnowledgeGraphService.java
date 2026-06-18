package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.web.entity.KnowledgeGraphEdge;
import com.chengxun.gamemaker.web.entity.KnowledgeGraphNode;
import com.chengxun.gamemaker.web.repository.KnowledgeGraphEdgeRepository;
import com.chengxun.gamemaker.web.repository.KnowledgeGraphNodeRepository;
import com.chengxun.gamemaker.manager.AgentManager;
import com.chengxun.gamemaker.manager.ProjectManager;
import com.chengxun.gamemaker.model.GameProject;
import com.chengxun.gamemaker.agent.Agent;
import com.chengxun.gamemaker.agent.BaseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱服务
 * 构建和管理项目级知识图谱，将 Agent、技能、文档、里程碑等实体关联起来
 *
 * 核心能力：
 * - 从项目数据自动构建知识图谱
 * - 添加/查询节点和边
 * - 关联查询（邻居、路径）
 * - 搜索节点
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
@Transactional
public class KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphService.class);

    @Autowired
    private KnowledgeGraphNodeRepository nodeRepository;

    @Autowired
    private KnowledgeGraphEdgeRepository edgeRepository;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired(required = false)
    private com.chengxun.gamemaker.web.service.AgentHealthService agentHealthService;

    /**
     * 添加节点
     */
    public KnowledgeGraphNode addNode(String projectId, KnowledgeGraphNode.NodeType type,
                                       String refId, String displayName, String properties) {
        Optional<KnowledgeGraphNode> existing = nodeRepository.findByProjectIdAndNodeRefId(projectId, refId);
        if (existing.isPresent()) {
            KnowledgeGraphNode node = existing.get();
            node.setDisplayName(displayName);
            if (properties != null) node.setProperties(properties);
            return nodeRepository.save(node);
        }
        KnowledgeGraphNode node = new KnowledgeGraphNode();
        node.setProjectId(projectId);
        node.setNodeType(type);
        node.setNodeRefId(refId);
        node.setDisplayName(displayName);
        node.setProperties(properties);
        return nodeRepository.save(node);
    }

    /**
     * 添加边
     */
    public KnowledgeGraphEdge addEdge(String projectId, Long fromNodeId, Long toNodeId,
                                       KnowledgeGraphEdge.RelationType relationType, String properties) {
        KnowledgeGraphEdge edge = new KnowledgeGraphEdge();
        edge.setProjectId(projectId);
        edge.setFromNodeId(fromNodeId);
        edge.setToNodeId(toNodeId);
        edge.setRelationType(relationType);
        edge.setProperties(properties);
        return edgeRepository.save(edge);
    }

    /**
     * 获取项目的所有节点
     */
    public List<KnowledgeGraphNode> getNodes(String projectId) {
        return nodeRepository.findByProjectId(projectId);
    }

    /**
     * 获取项目的所有边
     */
    public List<KnowledgeGraphEdge> getEdges(String projectId) {
        return edgeRepository.findByProjectId(projectId);
    }

    /**
     * 获取节点的邻居（深度1）
     */
    public Map<String, Object> getNeighbors(String projectId, Long nodeId) {
        Map<String, Object> result = new HashMap<>();
        List<KnowledgeGraphEdge> edges = edgeRepository.findByNodeId(nodeId);
        Set<Long> neighborIds = new HashSet<>();
        for (KnowledgeGraphEdge edge : edges) {
            if (edge.getFromNodeId().equals(nodeId)) {
                neighborIds.add(edge.getToNodeId());
            } else {
                neighborIds.add(edge.getFromNodeId());
            }
        }
        List<KnowledgeGraphNode> neighbors = neighborIds.stream()
            .map(id -> nodeRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        result.put("nodes", neighbors);
        result.put("edges", edges);
        return result;
    }

    /**
     * 搜索节点
     */
    public List<KnowledgeGraphNode> searchNodes(String projectId, String keyword) {
        return nodeRepository.searchByDisplayName(projectId, keyword);
    }

    /**
     * 从项目数据自动构建知识图谱
     */
    public Map<String, Object> buildFromProject(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        // 清空旧数据
        nodeRepository.deleteByProjectId(projectId);
        edgeRepository.deleteByProjectId(projectId);

        GameProject project = projectManager.getProject(projectId);
        if (project == null) {
            stats.put("error", "项目不存在");
            return stats;
        }

        int nodeCount = 0;
        int edgeCount = 0;

        // 1. 添加项目节点
        KnowledgeGraphNode projectNode = addNode(projectId,
            KnowledgeGraphNode.NodeType.PROJECT, projectId, project.getName(), null);
        nodeCount++;

        // 2. 添加 Agent 节点并关联
        List<Agent> agents = agentManager.getAgentsByProject(projectId);
        for (Agent agent : agents) {
            KnowledgeGraphNode agentNode = addNode(projectId,
                KnowledgeGraphNode.NodeType.AGENT, agent.getId(), agent.getName(), null);
            nodeCount++;
            edgeRepository.save(createEdge(projectId, agentNode.getId(), projectNode.getId(),
                KnowledgeGraphEdge.RelationType.BELONGS_TO, null));
            edgeCount++;
        }

        // 3. 添加里程碑节点并关联
        if (project.getMilestones() != null) {
            for (GameProject.GoalMilestone m : project.getMilestones()) {
                KnowledgeGraphNode msNode = addNode(projectId,
                    KnowledgeGraphNode.NodeType.MILESTONE, "ms_" + m.getTitle(), m.getTitle(), null);
                nodeCount++;
                edgeRepository.save(createEdge(projectId, msNode.getId(), projectNode.getId(),
                    KnowledgeGraphEdge.RelationType.BELONGS_TO, null));
                edgeCount++;
            }
        }

        // 4. 添加技能节点（从 Agent 能力）
        for (Agent agent : agents) {
            if (agent instanceof BaseAgent baseAgent) {
                // Agent 使用技能的关联
                KnowledgeGraphNode agentNode = nodeRepository.findByProjectIdAndNodeRefId(projectId, agent.getId()).orElse(null);
                if (agentNode != null) {
                    // 添加角色作为技能节点
                    KnowledgeGraphNode skillNode = addNode(projectId,
                        KnowledgeGraphNode.NodeType.SKILL, "skill_" + agent.getRole(), agent.getRole(), null);
                    nodeCount++;
                    edgeRepository.save(createEdge(projectId, agentNode.getId(), skillNode.getId(),
                        KnowledgeGraphEdge.RelationType.USES, null));
                    edgeCount++;
                }
            }
        }

        stats.put("nodeCount", nodeCount);
        stats.put("edgeCount", edgeCount);
        stats.put("projectId", projectId);
        log.info("知识图谱构建完成: project={}, nodes={}, edges={}", projectId, nodeCount, edgeCount);
        return stats;
    }

    /**
     * 获取图谱统计
     */
    public Map<String, Object> getStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodeCount", nodeRepository.countByProject(projectId));
        stats.put("edgeCount", edgeRepository.countByProject(projectId));
        // 按节点类型统计
        Map<String, Long> typeCounts = new HashMap<>();
        for (KnowledgeGraphNode.NodeType type : KnowledgeGraphNode.NodeType.values()) {
            long count = nodeRepository.findByProjectIdAndNodeType(projectId, type).size();
            if (count > 0) typeCounts.put(type.name(), count);
        }
        stats.put("typeCounts", typeCounts);
        return stats;
    }

    private KnowledgeGraphEdge createEdge(String projectId, Long from, Long to,
                                           KnowledgeGraphEdge.RelationType type, String props) {
        KnowledgeGraphEdge edge = new KnowledgeGraphEdge();
        edge.setProjectId(projectId);
        edge.setFromNodeId(from);
        edge.setToNodeId(to);
        edge.setRelationType(type);
        edge.setProperties(props);
        return edge;
    }
}
