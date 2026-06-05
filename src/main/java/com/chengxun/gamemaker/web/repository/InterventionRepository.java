package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 人工干预记录数据访问层
 * 提供干预记录的CRUD操作和自定义查询
 */
@Repository
public interface InterventionRepository extends JpaRepository<Intervention, Long> {

    /**
     * 获取指定Agent的干预记录，按创建时间降序排列
     *
     * @param agentId Agent ID
     * @return 干预记录列表
     */
    List<Intervention> findByAgentIdOrderByCreatedAtDesc(String agentId);

    /**
     * 获取指定Agent的未处理干预记录
     *
     * @param agentId Agent ID
     * @return 未处理的干预记录列表
     */
    List<Intervention> findByAgentIdAndProcessedFalseOrderByCreatedAtDesc(String agentId);

    /**
     * 获取指定Agent的永久性干预记录
     *
     * @param agentId Agent ID
     * @return 永久性干预记录列表
     */
    @Query("SELECT i FROM Intervention i WHERE i.agentId = :agentId AND i.duration = 'PERMANENT' ORDER BY i.createdAt DESC")
    List<Intervention> findPermanentInterventions(@Param("agentId") String agentId);

    /**
     * 获取指定Agent的紧急干预记录
     *
     * @param agentId Agent ID
     * @return 紧急干预记录列表
     */
    @Query("SELECT i FROM Intervention i WHERE i.agentId = :agentId AND i.urgency IN ('HIGH', 'CRITICAL') AND i.processed = false ORDER BY i.urgency DESC, i.createdAt DESC")
    List<Intervention> findUrgentInterventions(@Param("agentId") String agentId);

    /**
     * 统计指定Agent的干预记录数量
     *
     * @param agentId Agent ID
     * @return 干预记录数量
     */
    long countByAgentId(String agentId);

    /**
     * 获取指定用户的干预记录
     *
     * @param userId 用户ID
     * @return 干预记录列表
     */
    List<Intervention> findByUserIdOrderByCreatedAtDesc(Long userId);
}
