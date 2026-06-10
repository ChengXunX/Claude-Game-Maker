package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.VersionEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 版本评估数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface VersionEvaluationRepository extends JpaRepository<VersionEvaluationEntity, Long> {

    /** 获取项目的所有评估记录（按时间倒序） */
    List<VersionEvaluationEntity> findByProjectIdOrderByEvaluatedAtDesc(String projectId);

    /** 获取项目指定里程碑的评估记录 */
    List<VersionEvaluationEntity> findByProjectIdAndMilestoneIdOrderByEvaluatedAtDesc(String projectId, String milestoneId);

    /** 获取项目的最新评估记录 */
    VersionEvaluationEntity findFirstByProjectIdOrderByEvaluatedAtDesc(String projectId);
}
