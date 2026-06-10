package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.QualityGateAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 质量门禁评估结果仓库
 */
@Repository
public interface QualityGateAssessmentRepository extends JpaRepository<QualityGateAssessment, Long> {

    /** 按项目查询评估记录，按时间倒序 */
    List<QualityGateAssessment> findByProjectIdOrderByAssessedAtDesc(String projectId);

    /** 获取项目最新的评估记录 */
    Optional<QualityGateAssessment> findFirstByProjectIdOrderByAssessedAtDesc(String projectId);

    /** 删除项目的所有评估记录 */
    void deleteByProjectId(String projectId);
}
