package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.WorkflowStepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流步骤执行数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface WorkflowStepExecutionRepository extends JpaRepository<WorkflowStepExecutionEntity, Long> {

    /** 查询指定实例的所有步骤执行记录 */
    List<WorkflowStepExecutionEntity> findByInstanceIdOrderByCreatedAtAsc(String instanceId);

    /** 查询指定实例中指定步骤的执行记录 */
    Optional<WorkflowStepExecutionEntity> findByInstanceIdAndStepId(String instanceId, String stepId);

    /** 查询指定实例中指定状态的步骤 */
    List<WorkflowStepExecutionEntity> findByInstanceIdAndStatus(String instanceId, String status);

    /** 查询指定Agent正在执行的步骤 */
    List<WorkflowStepExecutionEntity> findByAgentIdAndStatus(String agentId, String status);
}
