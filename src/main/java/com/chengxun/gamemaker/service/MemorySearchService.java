package com.chengxun.gamemaker.service;

import com.chengxun.gamemaker.config.SystemConstants;
import com.chengxun.gamemaker.web.service.SystemConfigService;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 记忆全文搜索服务
 * 基于 MySQL FULLTEXT 索引实现记忆的全文搜索
 *
 * 主要功能：
 * - 对记忆内容建立全文索引
 * - 支持中英文分词搜索（使用 ngram 解析器）
 * - 按相关性排序返回结果
 * - 自动同步记忆变更到索引
 *
 * 灵感来源：Agent 记忆全文搜索机制
 *
 * @author chengxun
 * @since 3.0.0
 */
@Service
public class MemorySearchService {

    private static final Logger log = LoggerFactory.getLogger(MemorySearchService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SystemConfigService configService;

    /**
     * 搜索记忆
     *
     * @param projectId 项目 ID
     * @param agentId Agent ID
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    public List<MemorySearchResult> search(String projectId, String agentId, String query) {
        int maxResults = configService.getInt("memory.search.max-results", 10);

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 清理查询词，防止 SQL 注入
        String cleanQuery = sanitizeQuery(query);

        String sql = """
            SELECT project_id, agent_id, category, memory_key, content,
                   MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) AS score
            FROM memory_fts
            WHERE project_id = ?
              AND agent_id = ?
              AND MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE)
            ORDER BY score DESC
            LIMIT ?
            """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                MemorySearchResult result = new MemorySearchResult();
                result.setProjectId(rs.getString("project_id"));
                result.setAgentId(rs.getString("agent_id"));
                result.setCategory(rs.getString("category"));
                result.setMemoryKey(rs.getString("memory_key"));
                result.setContent(rs.getString("content"));
                result.setScore(rs.getDouble("score"));
                return result;
            }, cleanQuery, projectId, agentId, cleanQuery, maxResults);
        } catch (Exception e) {
            log.warn("记忆全文搜索失败，回退到模糊搜索: {}", e.getMessage());
            return fallbackSearch(projectId, agentId, cleanQuery, maxResults);
        }
    }

    /**
     * 跨项目搜索记忆
     *
     * @param agentId Agent ID
     * @param query 搜索关键词
     * @return 搜索结果列表
     */
    public List<MemorySearchResult> searchGlobal(String agentId, String query) {
        int maxResults = configService.getInt("memory.search.max-results", 10);

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String cleanQuery = sanitizeQuery(query);

        String sql = """
            SELECT project_id, agent_id, category, memory_key, content,
                   MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) AS score
            FROM memory_fts
            WHERE agent_id = ?
              AND MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE)
            ORDER BY score DESC
            LIMIT ?
            """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                MemorySearchResult result = new MemorySearchResult();
                result.setProjectId(rs.getString("project_id"));
                result.setAgentId(rs.getString("agent_id"));
                result.setCategory(rs.getString("category"));
                result.setMemoryKey(rs.getString("memory_key"));
                result.setContent(rs.getString("content"));
                result.setScore(rs.getDouble("score"));
                return result;
            }, cleanQuery, agentId, cleanQuery, maxResults);
        } catch (Exception e) {
            log.warn("全局记忆搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 索引一条记忆
     *
     * @param projectId 项目 ID
     * @param agentId Agent ID
     * @param category 记忆分类
     * @param memoryKey 记忆键
     * @param content 记忆内容
     */
    public void indexMemory(String projectId, String agentId, String category, String memoryKey, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO memory_fts (project_id, agent_id, category, memory_key, content, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE content = VALUES(content), updated_at = NOW()
            """;

        try {
            jdbcTemplate.update(sql, projectId, agentId, category, memoryKey, content);
            log.debug("记忆已索引: {}/{}/{}/{}", projectId, agentId, category, memoryKey);
        } catch (Exception e) {
            log.error("索引记忆失败: {}/{}/{}/{}", projectId, agentId, category, memoryKey, e);
        }
    }

    /**
     * 删除一条记忆索引
     *
     * @param projectId 项目 ID
     * @param agentId Agent ID
     * @param category 记忆分类
     * @param memoryKey 记忆键
     */
    public void removeIndex(String projectId, String agentId, String category, String memoryKey) {
        String sql = "DELETE FROM memory_fts WHERE project_id = ? AND agent_id = ? AND category = ? AND memory_key = ?";
        try {
            jdbcTemplate.update(sql, projectId, agentId, category, memoryKey);
        } catch (Exception e) {
            log.error("删除记忆索引失败: {}/{}/{}/{}", projectId, agentId, category, memoryKey, e);
        }
    }

    /**
     * 重建指定 Agent 的记忆索引
     *
     * @param projectId 项目 ID
     * @param agentId Agent ID
     * @return 索引的记忆数量
     */
    public int rebuildIndex(String projectId, String agentId) {
        // 先删除旧索引
        String deleteSql = "DELETE FROM memory_fts WHERE project_id = ? AND agent_id = ?";
        jdbcTemplate.update(deleteSql, projectId, agentId);

        log.info("已清除记忆索引: project={}, agent={}", projectId, agentId);
        return 0; // 实际重建由 MemoryManager 调用 indexMemory 完成
    }

    /**
     * 模糊搜索回退方案
     * 当 FULLTEXT 索引不可用时使用 LIKE 搜索
     */
    private List<MemorySearchResult> fallbackSearch(String projectId, String agentId, String query, int limit) {
        String sql = """
            SELECT project_id, agent_id, category, memory_key, content
            FROM memory_fts
            WHERE project_id = ? AND agent_id = ? AND content LIKE ?
            LIMIT ?
            """;

        String likeQuery = "%" + query + "%";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MemorySearchResult result = new MemorySearchResult();
            result.setProjectId(rs.getString("project_id"));
            result.setAgentId(rs.getString("agent_id"));
            result.setCategory(rs.getString("category"));
            result.setMemoryKey(rs.getString("memory_key"));
            result.setContent(rs.getString("content"));
            result.setScore(0.5); // 固定分数
            return result;
        }, projectId, agentId, likeQuery, limit);
    }

    /**
     * 清理查询词，防止 SQL 注入
     */
    private String sanitizeQuery(String query) {
        if (query == null) return "";
        // 移除特殊字符，保留中英文和数字
        return query.replaceAll("[^\\w\\u4e00-\\u9fa5\\s]", " ").trim();
    }

    /**
     * 搜索结果
     */
    public static class MemorySearchResult {
        private String projectId;
        private String agentId;
        private String category;
        private String memoryKey;
        private String content;
        private double score;

        // Getters and setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getMemoryKey() { return memoryKey; }
        public void setMemoryKey(String memoryKey) { this.memoryKey = memoryKey; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}
