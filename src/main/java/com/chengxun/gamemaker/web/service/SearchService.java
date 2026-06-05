package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.*;
import com.chengxun.gamemaker.web.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局搜索服务
 * 支持搜索项目、任务、日志、Agent等
 *
 * 主要功能：
 * - 跨模块搜索
 * - 统一搜索结果格式
 * - 搜索建议
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private AgentPerformanceRepository performanceRepository;

    @Autowired
    private AlertRecordRepository alertRecordRepository;

    @Autowired
    private CodeReviewRepository reviewRepository;

    @Autowired
    private OperationLogRepository logRepository;

    @Autowired
    private TokenUsageRepository tokenUsageRepository;

    /**
     * 全局搜索
     * @param keyword 搜索关键词
     * @param limit 每类结果限制数量
     * @return 搜索结果
     */
    public Map<String, List<Map<String, Object>>> search(String keyword, int limit) {
        Map<String, List<Map<String, Object>>> results = new HashMap<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String searchKeyword = keyword.trim().toLowerCase();

        // 搜索Agent
        List<Map<String, Object>> agentResults = searchAgents(searchKeyword, limit);
        if (!agentResults.isEmpty()) {
            results.put("agents", agentResults);
        }

        // 搜索告警
        List<Map<String, Object>> alertResults = searchAlerts(searchKeyword, limit);
        if (!alertResults.isEmpty()) {
            results.put("alerts", alertResults);
        }

        // 搜索代码审查
        List<Map<String, Object>> reviewResults = searchReviews(searchKeyword, limit);
        if (!reviewResults.isEmpty()) {
            results.put("reviews", reviewResults);
        }

        // 搜索操作日志
        List<Map<String, Object>> logResults = searchLogs(searchKeyword, limit);
        if (!logResults.isEmpty()) {
            results.put("logs", logResults);
        }

        log.info("Search completed for '{}': found {} result types", keyword, results.size());
        return results;
    }

    /**
     * 搜索Agent
     */
    private List<Map<String, Object>> searchAgents(String keyword, int limit) {
        return performanceRepository.findAll().stream()
            .filter(p -> matchesKeyword(p.getAgentName(), keyword) ||
                         matchesKeyword(p.getAgentId(), keyword) ||
                         matchesKeyword(p.getAgentRole(), keyword))
            .limit(limit)
            .map(p -> {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "agent");
                result.put("id", p.getAgentId());
                result.put("title", p.getAgentName());
                result.put("subtitle", p.getAgentRole());
                result.put("score", p.getOverallScore());
                result.put("link", "/performance/agent/" + p.getAgentId());
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * 搜索告警
     */
    private List<Map<String, Object>> searchAlerts(String keyword, int limit) {
        return alertRecordRepository.findAll().stream()
            .filter(a -> matchesKeyword(a.getTitle(), keyword) ||
                         matchesKeyword(a.getDetail(), keyword) ||
                         matchesKeyword(a.getAgentName(), keyword))
            .limit(limit)
            .map(a -> {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "alert");
                result.put("id", a.getId().toString());
                result.put("title", a.getTitle());
                result.put("subtitle", a.getPriorityDescription() + " - " + a.getStatusDescription());
                result.put("status", a.getStatus());
                result.put("link", "/alerts/" + a.getId());
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * 搜索代码审查
     */
    private List<Map<String, Object>> searchReviews(String keyword, int limit) {
        return reviewRepository.findAll().stream()
            .filter(r -> matchesKeyword(r.getTitle(), keyword) ||
                         matchesKeyword(r.getDescription(), keyword) ||
                         matchesKeyword(r.getAgentName(), keyword))
            .limit(limit)
            .map(r -> {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "review");
                result.put("id", r.getId().toString());
                result.put("title", r.getTitle());
                result.put("subtitle", r.getAgentName() + " - " + r.getStatusDescription());
                result.put("status", r.getStatus());
                result.put("link", "/reviews/" + r.getId());
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * 搜索操作日志
     */
    private List<Map<String, Object>> searchLogs(String keyword, int limit) {
        return logRepository.searchByKeyword(keyword).stream()
            .limit(limit)
            .map(l -> {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "log");
                result.put("id", l.getId().toString());
                result.put("title", l.getActionDescription());
                result.put("subtitle", l.getDetail() != null ? l.getDetail().substring(0, Math.min(50, l.getDetail().length())) : "");
                result.put("user", l.getUsername());
                result.put("time", l.getCreatedAt());
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * 检查是否匹配关键词
     */
    private boolean matchesKeyword(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase().contains(keyword);
    }

    /**
     * 获取搜索建议
     * @param prefix 前缀
     * @return 建议列表
     */
    public List<String> getSearchSuggestions(String prefix) {
        Set<String> suggestions = new LinkedHashSet<>();

        if (prefix == null || prefix.trim().isEmpty()) {
            return new ArrayList<>(suggestions);
        }

        String searchPrefix = prefix.trim().toLowerCase();

        // 从Agent名称获取建议
        performanceRepository.findAll().stream()
            .map(AgentPerformance::getAgentName)
            .filter(name -> name != null && name.toLowerCase().startsWith(searchPrefix))
            .limit(5)
            .forEach(suggestions::add);

        // 从告警标题获取建议
        alertRecordRepository.findAll().stream()
            .map(AlertRecord::getTitle)
            .filter(title -> title != null && title.toLowerCase().contains(searchPrefix))
            .limit(3)
            .forEach(suggestions::add);

        return new ArrayList<>(suggestions).stream()
            .limit(10)
            .collect(Collectors.toList());
    }
}
