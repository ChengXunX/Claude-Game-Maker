package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.RecruitmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 招聘申请数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface RecruitmentRequestRepository extends JpaRepository<RecruitmentRequest, Long> {

    /**
     * 根据申请编号查找
     */
    Optional<RecruitmentRequest> findByRequestNo(String requestNo);

    /**
     * 根据状态查找
     */
    List<RecruitmentRequest> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 根据制作人ID查找
     */
    List<RecruitmentRequest> findByProducerIdOrderByCreatedAtDesc(String producerId);

    /**
     * 查找待审批的申请
     */
    List<RecruitmentRequest> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    /**
     * 统计各状态的申请数量
     */
    @Query("SELECT r.status, COUNT(r) FROM RecruitmentRequest r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * 查找指定制作人的待审批申请
     */
    @Query("SELECT r FROM RecruitmentRequest r WHERE r.producerId = :producerId AND r.status = 'PENDING' ORDER BY r.createdAt DESC")
    List<RecruitmentRequest> findPendingByProducerId(@Param("producerId") String producerId);

    /**
     * 查找最近的申请
     */
    @Query("SELECT r FROM RecruitmentRequest r ORDER BY r.createdAt DESC")
    List<RecruitmentRequest> findRecentRequests();

    /**
     * 统计制作人的申请数量
     */
    @Query("SELECT r.producerId, r.producerName, COUNT(r) FROM RecruitmentRequest r GROUP BY r.producerId, r.producerName")
    List<Object[]> countByProducer();

    /**
     * 查找需要重新申请的记录
     */
    @Query("SELECT r FROM RecruitmentRequest r WHERE r.originalRequestId = :originalId ORDER BY r.revisionCount DESC")
    List<RecruitmentRequest> findRevisionsByOriginalId(@Param("originalId") Long originalId);
}
