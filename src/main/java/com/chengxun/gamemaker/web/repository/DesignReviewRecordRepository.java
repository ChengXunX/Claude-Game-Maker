package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.DesignReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignReviewRecordRepository extends JpaRepository<DesignReviewRecord, Long> {
    List<DesignReviewRecord> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
