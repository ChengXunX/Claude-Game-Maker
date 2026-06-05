package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 流水线数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, Long> {

    /**
     * 根据流水线编号查找
     */
    Optional<Pipeline> findByPipelineNo(String pipelineNo);

    /**
     * 根据项目ID查找流水线
     */
    List<Pipeline> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /**
     * 根据状态查找流水线
     */
    List<Pipeline> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 查找正在执行的流水线
     */
    @Query("SELECT p FROM Pipeline p WHERE p.status IN ('RUNNING', 'PENDING') ORDER BY p.createdAt DESC")
    List<Pipeline> findRunningPipelines();

    /**
     * 统计各状态的流水线数量
     */
    @Query("SELECT p.status, COUNT(p) FROM Pipeline p GROUP BY p.status")
    List<Object[]> countByStatus();

    /**
     * 统计各项目的流水线数量
     */
    @Query("SELECT p.projectId, p.projectName, COUNT(p) FROM Pipeline p GROUP BY p.projectId, p.projectName")
    List<Object[]> countByProject();

    /**
     * 查找最近的流水线记录
     */
    @Query("SELECT p FROM Pipeline p ORDER BY p.createdAt DESC")
    List<Pipeline> findRecentPipelines();

    /**
     * 查找项目的最近成功流水线
     */
    Optional<Pipeline> findFirstByProjectIdAndStatusOrderByCompletedAtDesc(String projectId, String status);
}
