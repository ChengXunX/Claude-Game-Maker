package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.McpServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP Server 仓库
 */
@Repository
public interface McpServerRepository extends JpaRepository<McpServer, Long> {

    List<McpServer> findByEnabledTrueOrderByNameAsc();

    List<McpServer> findByProjectIdOrderByNameAsc(String projectId);

    List<McpServer> findByProjectIdIsNullOrderByNameAsc();

    List<McpServer> findByTemplateTrueOrderByNameAsc();

    List<McpServer> findAllByOrderByNameAsc();

    Optional<McpServer> findByTemplateKey(String templateKey);

    Optional<McpServer> findByNameAndProjectId(String name, String projectId);

    boolean existsByTemplateKey(String templateKey);
}
