package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.OperationLogArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 操作日志归档数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface OperationLogArchiveRepository extends JpaRepository<OperationLogArchive, Long> {

    /**
     * 统计归档表中的记录数
     */
    @Query("SELECT COUNT(a) FROM OperationLogArchive a")
    long countAll();

    /**
     * 统计指定时间之前的归档记录数
     */
    @Query("SELECT COUNT(a) FROM OperationLogArchive a WHERE a.createdAt < :cutoff")
    long countBefore(@Param("cutoff") LocalDateTime cutoff);
}
