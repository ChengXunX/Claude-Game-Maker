package com.chengxun.gamemaker.web.repository;

import com.chengxun.gamemaker.web.entity.TokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Token使用统计数据访问层
 *
 * @author chengxun
 * @since 1.0.0
 */
@Repository
public interface TokenUsageRepository extends JpaRepository<TokenUsage, Long> {

    /**
     * 根据日期和Agent查找使用记录
     */
    Optional<TokenUsage> findByUsageDateAndAgentId(LocalDate usageDate, String agentId);

    /**
     * 根据日期范围查找
     */
    List<TokenUsage> findByUsageDateBetweenOrderByUsageDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * 根据Agent ID查找
     */
    List<TokenUsage> findByAgentIdOrderByUsageDateDesc(String agentId);

    /**
     * 根据用户ID查找
     */
    List<TokenUsage> findByUserIdOrderByUsageDateDesc(Long userId);

    /**
     * 统计日期范围内的总Token使用量
     */
    @Query("SELECT SUM(u.totalTokens) FROM TokenUsage u WHERE u.usageDate BETWEEN :start AND :end")
    Long sumTotalTokensByDateRange(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate);

    /**
     * 统计日期范围内的总成本
     */
    @Query("SELECT SUM(u.estimatedCost) FROM TokenUsage u WHERE u.usageDate BETWEEN :start AND :end")
    Double sumCostByDateRange(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate);

    /**
     * 统计各Agent的Token使用量
     */
    @Query("SELECT u.agentId, u.agentName, SUM(u.totalTokens), SUM(u.estimatedCost) " +
           "FROM TokenUsage u WHERE u.usageDate BETWEEN :start AND :end " +
           "GROUP BY u.agentId, u.agentName ORDER BY SUM(u.totalTokens) DESC")
    List<Object[]> sumByAgentAndDateRange(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate);

    /**
     * 统计每日Token使用量
     */
    @Query("SELECT u.usageDate, SUM(u.totalTokens), SUM(u.estimatedCost) " +
           "FROM TokenUsage u WHERE u.usageDate BETWEEN :start AND :end " +
           "GROUP BY u.usageDate ORDER BY u.usageDate")
    List<Object[]> sumByDateRange(@Param("start") LocalDate startDate, @Param("end") LocalDate endDate);

    /**
     * 获取最近的使用记录
     */
    @Query("SELECT u FROM TokenUsage u ORDER BY u.usageDate DESC, u.totalTokens DESC")
    List<TokenUsage> findRecentUsage();
}
