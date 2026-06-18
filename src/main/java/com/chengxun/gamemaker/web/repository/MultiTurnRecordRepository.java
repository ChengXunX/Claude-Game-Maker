package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.MultiTurnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MultiTurnRecordRepository extends JpaRepository<MultiTurnRecord, Long> {

    List<MultiTurnRecord> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<MultiTurnRecord> findByAgentIdOrderByCreatedAtDesc(String agentId);

    @Query("SELECT r FROM MultiTurnRecord r WHERE r.projectId = :projectId AND r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<MultiTurnRecord> findRecentByProject(@Param("projectId") String projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM MultiTurnRecord r WHERE r.projectId = :projectId AND r.status = 'PASSED'")
    long countPassedByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(r) FROM MultiTurnRecord r WHERE r.projectId = :projectId")
    long countByProject(@Param("projectId") String projectId);

    @Query("SELECT AVG(r.turnNumber) FROM MultiTurnRecord r WHERE r.projectId = :projectId AND r.status = 'PASSED'")
    Double avgTurnsByProject(@Param("projectId") String projectId);
}
