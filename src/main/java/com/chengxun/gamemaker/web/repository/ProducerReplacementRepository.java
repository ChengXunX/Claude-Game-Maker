package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.ProducerReplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 制作人更换记录数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface ProducerReplacementRepository extends JpaRepository<ProducerReplacement, Long> {

    /**
     * 根据更换编号查找
     */
    Optional<ProducerReplacement> findByReplacementNo(String replacementNo);

    /**
     * 根据项目ID查找
     */
    List<ProducerReplacement> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /**
     * 根据原制作人ID查找
     */
    List<ProducerReplacement> findByOldProducerIdOrderByCreatedAtDesc(String oldProducerId);

    /**
     * 根据新制作人ID查找
     */
    List<ProducerReplacement> findByNewProducerIdOrderByCreatedAtDesc(String newProducerId);

    /**
     * 查找最近的更换记录
     */
    @Query("SELECT r FROM ProducerReplacement r ORDER BY r.createdAt DESC")
    List<ProducerReplacement> findRecentReplacements();

    /**
     * 统计项目的更换次数
     */
    @Query("SELECT COUNT(r) FROM ProducerReplacement r WHERE r.projectId = :projectId")
    Long countByProjectId(@Param("projectId") String projectId);
}
