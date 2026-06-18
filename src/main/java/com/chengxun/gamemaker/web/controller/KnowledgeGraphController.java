package com.chengxun.gamemaker.web.controller;

import com.chengxun.gamemaker.service.KnowledgeGraphService;
import com.chengxun.gamemaker.web.entity.KnowledgeGraphEdge;
import com.chengxun.gamemaker.web.entity.KnowledgeGraphNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱控制器
 * 提供知识图谱的查询、构建和搜索接口
 *
 * @author chengxun
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/knowledge-graph")
@Tag(name = "知识图谱", description = "知识图谱管理接口")
public class KnowledgeGraphController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/{projectId}")
    @Operation(summary = "获取项目知识图谱", description = "获取项目的完整知识图谱（节点+边）")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:view', 'PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> getGraph(@PathVariable String projectId) {
        List<KnowledgeGraphNode> nodes = knowledgeGraphService.getNodes(projectId);
        List<KnowledgeGraphEdge> edges = knowledgeGraphService.getEdges(projectId);
        Map<String, Object> stats = knowledgeGraphService.getStats(projectId);
        return ResponseEntity.ok(Map.of("nodes", nodes, "edges", edges, "stats", stats));
    }

    @GetMapping("/{projectId}/nodes")
    @Operation(summary = "获取节点列表")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:view', 'PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<List<KnowledgeGraphNode>> getNodes(@PathVariable String projectId) {
        return ResponseEntity.ok(knowledgeGraphService.getNodes(projectId));
    }

    @GetMapping("/{projectId}/edges")
    @Operation(summary = "获取边列表")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:view', 'PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<List<KnowledgeGraphEdge>> getEdges(@PathVariable String projectId) {
        return ResponseEntity.ok(knowledgeGraphService.getEdges(projectId));
    }

    @GetMapping("/{projectId}/neighbors/{nodeId}")
    @Operation(summary = "获取邻居节点")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:view', 'PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> getNeighbors(@PathVariable String projectId, @PathVariable Long nodeId) {
        return ResponseEntity.ok(knowledgeGraphService.getNeighbors(projectId, nodeId));
    }

    @GetMapping("/{projectId}/search")
    @Operation(summary = "搜索节点")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:view', 'PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<List<KnowledgeGraphNode>> search(@PathVariable String projectId, @RequestParam String keyword) {
        return ResponseEntity.ok(knowledgeGraphService.searchNodes(projectId, keyword));
    }

    @PostMapping("/{projectId}/build")
    @Operation(summary = "构建知识图谱", description = "从项目数据自动构建知识图谱")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> build(@PathVariable String projectId) {
        return ResponseEntity.ok(knowledgeGraphService.buildFromProject(projectId));
    }

    @GetMapping("/{projectId}/stats")
    @Operation(summary = "获取图谱统计")
    @PreAuthorize("hasAnyAuthority('PERM_knowledge:view', 'PERM_knowledge:manage', 'PERM_admin:manage', '*')")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String projectId) {
        return ResponseEntity.ok(knowledgeGraphService.getStats(projectId));
    }
}
