package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批请求数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    /**
     * 查找项目的所有待审批请求
     */
    List<ApprovalRequest> findByProjectIdAndStatusOrderByPriorityAscCreatedAtAsc(
        String projectId, ApprovalRequest.ApprovalStatus status);

    /**
     * 查找所有待审批请求
     */
    List<ApprovalRequest> findByStatusOrderByPriorityAscCreatedAtAsc(
        ApprovalRequest.ApprovalStatus status);

    /**
     * 查找请求者的所有请求
     */
    List<ApprovalRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    /**
     * 查找指定类型的请求
     */
    List<ApprovalRequest> findByRequestTypeAndStatus(String requestType, ApprovalRequest.ApprovalStatus status);

    /**
     * 查找已过期的待审批请求
     */
    @Query("SELECT r FROM ApprovalRequest r WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<ApprovalRequest> findExpiredPendingRequests(@Param("now") LocalDateTime now);

    /**
     * 批量更新过期请求状态
     */
    @Modifying
    @Query("UPDATE ApprovalRequest r SET r.status = 'EXPIRED' WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    int expirePendingRequests(@Param("now") LocalDateTime now);

    /**
     * 统计项目的待审批请求数量
     */
    long countByProjectIdAndStatus(String projectId, ApprovalRequest.ApprovalStatus status);

    /**
     * 统计所有待审批请求数量
     */
    long countByStatus(ApprovalRequest.ApprovalStatus status);

    /**
     * 按时间倒序获取所有审批请求
     */
    List<ApprovalRequest> findAllByOrderByCreatedAtDesc();

    /**
     * 查找项目的所有审批请求（不限状态）
     */
    List<ApprovalRequest> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
