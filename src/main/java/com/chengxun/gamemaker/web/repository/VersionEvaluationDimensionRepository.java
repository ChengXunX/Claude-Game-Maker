package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.VersionEvaluationDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 版本评估维度仓库
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface VersionEvaluationDimensionRepository extends JpaRepository<VersionEvaluationDimension, Long> {

    /**
     * 获取所有启用的评估维度（按显示顺序排序）
     */
    List<VersionEvaluationDimension> findByEnabledTrueOrderByDisplayOrderAsc();

    /**
     * 根据维度标识查找
     */
    Optional<VersionEvaluationDimension> findByDimensionKey(String dimensionKey);

    /**
     * 获取所有评估维度（按显示顺序排序）
     */
    List<VersionEvaluationDimension> findAllByOrderByDisplayOrderAsc();

    /**
     * 统计启用的维度数量
     */
    long countByEnabledTrue();

    /**
     * 获取所有系统内置维度
     */
    List<VersionEvaluationDimension> findBySystemBuiltinTrue();
}
