-- V39: 知识图谱节点表和边表
CREATE TABLE IF NOT EXISTS knowledge_graph_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    node_type VARCHAR(30) NOT NULL,
    node_ref_id VARCHAR(200),
    display_name VARCHAR(200),
    properties TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_nodes' AND INDEX_NAME = 'idx_kgn_project');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kgn_project ON knowledge_graph_nodes(project_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_nodes' AND INDEX_NAME = 'idx_kgn_type');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kgn_type ON knowledge_graph_nodes(node_type)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_nodes' AND INDEX_NAME = 'idx_kgn_ref');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kgn_ref ON knowledge_graph_nodes(node_ref_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS knowledge_graph_edges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(200) NOT NULL,
    from_node_id BIGINT NOT NULL,
    to_node_id BIGINT NOT NULL,
    relation_type VARCHAR(30) NOT NULL,
    properties TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_edges' AND INDEX_NAME = 'idx_kge_project');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kge_project ON knowledge_graph_edges(project_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_edges' AND INDEX_NAME = 'idx_kge_from');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kge_from ON knowledge_graph_edges(from_node_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_edges' AND INDEX_NAME = 'idx_kge_to');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kge_to ON knowledge_graph_edges(to_node_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_graph_edges' AND INDEX_NAME = 'idx_kge_type');
SET @sql = IF(@idx_exists = 0, 'CREATE INDEX idx_kge_type ON knowledge_graph_edges(relation_type)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
