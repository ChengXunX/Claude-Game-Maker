package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.QualityPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QualityPredictionRepository extends JpaRepository<QualityPrediction, Long> {

    List<QualityPrediction> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<QualityPrediction> findFirstByProjectIdOrderByCreatedAtDesc(String projectId);

    @Query("SELECT AVG(p.passProbability) FROM QualityPrediction p WHERE p.projectId = :projectId")
    Double avgPassProbabilityByProject(@Param("projectId") String projectId);

    @Query("SELECT p FROM QualityPrediction p WHERE p.projectId = :projectId ORDER BY p.createdAt DESC")
    List<QualityPrediction> findHistoryByProject(@Param("projectId") String projectId);
}
