package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.GameTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 游戏模板数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface GameTemplateRepository extends JpaRepository<GameTemplateEntity, String> {

    /**
     * 查询所有非内置模板（用户自定义模板）
     */
    List<GameTemplateEntity> findByBuiltinFalse();

    /**
     * 查询所有模板
     */
    List<GameTemplateEntity> findAllByOrderByIdAsc();
}
