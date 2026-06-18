package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.IterationAdaptRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IterationAdaptRecordRepository extends JpaRepository<IterationAdaptRecord, Long> {

    List<IterationAdaptRecord> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<IterationAdaptRecord> findFirstByProjectIdAndAppliedTrueOrderByCreatedAtDesc(String projectId);

    @Query("SELECT r FROM IterationAdaptRecord r WHERE r.projectId = :projectId AND r.applied = true ORDER BY r.createdAt DESC")
    List<IterationAdaptRecord> findAppliedByProject(@Param("projectId") String projectId);
}
