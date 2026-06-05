package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.NotificationTemplate;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Category;
import com.chengxun.gamemaker.web.entity.NotificationTemplate.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * 根据模板编码查找
     *
     * @param templateCode 模板编码
     * @return 模板
     */
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    /**
     * 根据渠道查找启用的模板
     *
     * @param channel 通知渠道
     * @return 模板列表
     */
    List<NotificationTemplate> findByChannelAndEnabledTrue(Channel channel);

    /**
     * 根据分类查找启用的模板
     *
     * @param category 模板分类
     * @return 模板列表
     */
    List<NotificationTemplate> findByCategoryAndEnabledTrue(Category category);

    /**
     * 根据渠道和分类查找启用的模板
     *
     * @param channel 通知渠道
     * @param category 模板分类
     * @return 模板列表
     */
    List<NotificationTemplate> findByChannelAndCategoryAndEnabledTrue(Channel channel, Category category);

    /**
     * 查找所有启用的模板
     *
     * @return 模板列表
     */
    List<NotificationTemplate> findByEnabledTrue();

    /**
     * 检查模板编码是否存在
     *
     * @param templateCode 模板编码
     * @return 是否存在
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * 根据渠道查找模板
     *
     * @param channel 通知渠道
     * @return 模板列表
     */
    List<NotificationTemplate> findByChannel(Channel channel);
}
