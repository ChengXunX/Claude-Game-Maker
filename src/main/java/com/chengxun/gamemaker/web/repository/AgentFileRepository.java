package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.AgentFile;
import com.chengxun.gamemaker.web.entity.AgentFile.FileDirection;
import com.chengxun.gamemaker.web.entity.AgentFile.FileSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent 文件仓库
 */
@Repository
public interface AgentFileRepository extends JpaRepository<AgentFile, Long> {

    /** 获取 Agent 的文件列表（排除已删除） */
    Page<AgentFile> findByAgentIdAndDeletedFalseOrderByCreatedAtDesc(String agentId, Pageable pageable);

    /** 获取 Agent 的文件列表（按方向） */
    Page<AgentFile> findByAgentIdAndDirectionAndDeletedFalseOrderByCreatedAtDesc(
        String agentId, FileDirection direction, Pageable pageable);

    /** 获取项目的文件列表 */
    Page<AgentFile> findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(String projectId, Pageable pageable);

    /** 查找同名文件的最新版本 */
    Optional<AgentFile> findFirstByAgentIdAndFileNameAndDeletedFalseOrderByVersionDesc(
        String agentId, String fileName);

    /** 获取文件的所有版本 */
    List<AgentFile> findByAgentIdAndFileNameAndDeletedFalseOrderByVersionDesc(String agentId, String fileName);

    /** 统计 Agent 文件总大小 */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM AgentFile f WHERE f.agentId = :agentId AND f.deleted = false")
    long sumFileSizeByAgentId(@Param("agentId") String agentId);

    /** 统计 Agent 文件数量 */
    long countByAgentIdAndDeletedFalse(String agentId);

    /** 按来源统计 */
    long countByAgentIdAndSourceAndDeletedFalse(String agentId, FileSource source);

    /** 搜索文件名 */
    @Query("SELECT f FROM AgentFile f WHERE f.agentId = :agentId AND f.deleted = false " +
           "AND LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY f.createdAt DESC")
    Page<AgentFile> searchByFileName(@Param("agentId") String agentId, @Param("keyword") String keyword, Pageable pageable);

    /** 获取最近的文件 */
    List<AgentFile> findTop10ByAgentIdAndDeletedFalseOrderByCreatedAtDesc(String agentId);
}
