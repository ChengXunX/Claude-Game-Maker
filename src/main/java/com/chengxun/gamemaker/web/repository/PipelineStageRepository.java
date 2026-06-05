package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 流水线阶段数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

    /**
     * 根据流水线ID查找所有阶段，按执行顺序排序
     */
    List<PipelineStage> findByPipelineIdOrderByStageOrderAsc(Long pipelineId);

    /**
     * 根据流水线ID和状态查找阶段
     */
    List<PipelineStage> findByPipelineIdAndStatus(Long pipelineId, String status);

    /**
     * 删除流水线的所有阶段
     */
    void deleteByPipelineId(Long pipelineId);
}
