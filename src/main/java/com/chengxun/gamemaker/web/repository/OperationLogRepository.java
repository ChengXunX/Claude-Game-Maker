package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    List<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<OperationLog> findTop20ByOrderByCreatedAtDesc();

    /**
     * 根据操作类型查找
     */
    List<OperationLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * 根据目标类型查找
     */
    List<OperationLog> findByTargetTypeOrderByCreatedAtDesc(String targetType);

    /**
     * 根据级别查找
     */
    List<OperationLog> findByLevelOrderByCreatedAtDesc(String level);

    /**
     * 根据时间范围查找
     */
    List<OperationLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定时间后的操作数
     */
    @Query("SELECT COUNT(l) FROM OperationLog l WHERE l.createdAt >= :since")
    Long countSince(@Param("since") LocalDateTime since);

    /**
     * 统计各操作类型的数量
     */
    @Query("SELECT l.action, COUNT(l) FROM OperationLog l GROUP BY l.action")
    List<Object[]> countByAction();

    /**
     * 统计各目标类型的数量
     */
    @Query("SELECT l.targetType, COUNT(l) FROM OperationLog l GROUP BY l.targetType")
    List<Object[]> countByTargetType();

    /**
     * 统计指定级别的日志数量
     */
    @Query("SELECT COUNT(l) FROM OperationLog l WHERE l.level = :level")
    Long countByLevel(@Param("level") String level);

    /**
     * 统计指定时间范围内的操作数
     */
    @Query("SELECT COUNT(l) FROM OperationLog l WHERE l.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 搜索关键词
     */
    @Query("SELECT l FROM OperationLog l WHERE " +
           "l.action LIKE %:keyword% OR " +
           "l.targetName LIKE %:keyword% OR " +
           "l.detail LIKE %:keyword% OR " +
           "l.username LIKE %:keyword% " +
           "ORDER BY l.createdAt DESC")
    List<OperationLog> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 删除指定时间之前的日志
     */
    @Modifying
    @Query("DELETE FROM OperationLog l WHERE l.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
