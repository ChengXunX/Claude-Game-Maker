package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.DismissalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 解雇申请数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface DismissalRequestRepository extends JpaRepository<DismissalRequest, Long> {

    /**
     * 根据申请编号查找
     */
    Optional<DismissalRequest> findByRequestNo(String requestNo);

    /**
     * 根据被解雇Agent ID查找
     */
    List<DismissalRequest> findByAgentIdOrderByCreatedAtDesc(String agentId);

    /**
     * 根据制作人ID查找
     */
    List<DismissalRequest> findByProducerIdOrderByCreatedAtDesc(String producerId);

    /**
     * 根据状态查找
     */
    List<DismissalRequest> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 查找待审批的申请
     */
    List<DismissalRequest> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    /**
     * 统计各状态的申请数量
     */
    @Query("SELECT d.status, COUNT(d) FROM DismissalRequest d GROUP BY d.status")
    List<Object[]> countByStatus();

    /**
     * 检查Agent是否有待审批的解雇申请
     */
    @Query("SELECT COUNT(d) > 0 FROM DismissalRequest d WHERE d.agentId = :agentId AND d.status = 'PENDING'")
    boolean hasPendingDismissalRequest(@Param("agentId") String agentId);

    /**
     * 查找最近的申请
     */
    @Query("SELECT d FROM DismissalRequest d ORDER BY d.createdAt DESC")
    List<DismissalRequest> findRecentRequests();
}
