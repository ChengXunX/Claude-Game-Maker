package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.WorkflowTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工作流模板数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplateEntity, String> {

    /**
     * 查询所有非内置模板（用户自定义模板）
     */
    List<WorkflowTemplateEntity> findByBuiltinFalse();

    /**
     * 查询所有模板（包含内置标记）
     */
    List<WorkflowTemplateEntity> findAllByOrderByIdAsc();
}
