package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.WorkflowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工作流实例数据访问层
 *
 * @author chengxun
 * @since 2.0.0
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, String> {

    /** 按项目查询实例列表，按创建时间倒序 */
    List<WorkflowInstanceEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);

    /** 按状态查询实例列表 */
    List<WorkflowInstanceEntity> findByStatusOrderByCreatedAtDesc(String status);

    /** 查询未完成的实例（用于启动时恢复） */
    List<WorkflowInstanceEntity> findByStatusIn(List<String> statuses);

    /** 按模板ID查询实例 */
    List<WorkflowInstanceEntity> findByTemplateIdOrderByCreatedAtDesc(String templateId);
}
