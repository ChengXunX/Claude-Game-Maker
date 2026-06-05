package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.McpTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP Tool 仓库
 */
@Repository
public interface McpToolRepository extends JpaRepository<McpTool, Long> {

    List<McpTool> findByServerIdAndEnabledTrueOrderByToolNameAsc(Long serverId);

    List<McpTool> findByServerIdOrderByToolNameAsc(Long serverId);

    List<McpTool> findByEnabledTrueOrderByToolNameAsc();

    Optional<McpTool> findByServerIdAndToolName(Long serverId, String toolName);

    List<McpTool> findByCategoryAndEnabledTrueOrderByToolNameAsc(String category);

    long countByServerId(Long serverId);
}
