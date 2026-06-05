package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.WorkflowApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流审批数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface WorkflowApprovalRepository extends JpaRepository<WorkflowApprovalEntity, Long> {

    /** 查询指定实例的所有审批记录 */
    List<WorkflowApprovalEntity> findByInstanceIdOrderByRequestedAtDesc(String instanceId);

    /** 查询指定实例中待审批的记录 */
    List<WorkflowApprovalEntity> findByInstanceIdAndStatus(String instanceId, String status);

    /** 查询指定步骤的审批记录 */
    Optional<WorkflowApprovalEntity> findByInstanceIdAndStepId(String instanceId, String stepId);

    /** 查询指定审批人的待审批列表 */
    List<WorkflowApprovalEntity> findByApproverIdAndStatusOrderByRequestedAtDesc(Long approverId, String status);

    /** 查询所有待审批的记录 */
    List<WorkflowApprovalEntity> findByStatusOrderByRequestedAtDesc(String status);
}
