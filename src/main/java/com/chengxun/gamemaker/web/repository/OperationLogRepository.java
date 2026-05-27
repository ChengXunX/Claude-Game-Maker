package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<OperationLog> findTop20ByOrderByCreatedAtDesc();
}
