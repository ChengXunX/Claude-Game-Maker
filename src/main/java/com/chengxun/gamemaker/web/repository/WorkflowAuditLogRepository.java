package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.WorkflowAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工作流审计日志数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface WorkflowAuditLogRepository extends JpaRepository<WorkflowAuditLogEntity, Long> {

    /** 查询指定实例的所有审计日志，按时间正序 */
    List<WorkflowAuditLogEntity> findByInstanceIdOrderByCreatedAtAsc(String instanceId);

    /** 查询指定实例中指定操作类型的日志 */
    List<WorkflowAuditLogEntity> findByInstanceIdAndActionOrderByCreatedAtAsc(String instanceId, String action);

    /** 查询指定操作者的所有操作日志 */
    List<WorkflowAuditLogEntity> findByActorTypeAndActorIdOrderByCreatedAtDesc(String actorType, String actorId);
}
