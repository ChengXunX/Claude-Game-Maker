package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.CodeReview;
import com.chengxun.gamemaker.web.entity.GitRepository;
import com.chengxun.gamemaker.web.repository.CodeReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码审查服务
 * 负责代码审查流程的管理
 *
 * 主要功能：
 * - 提交代码审查
 * - 自动代码审查
 * - 人工审查处理
 * - 审查统计
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    @Autowired
    private CodeReviewRepository reviewRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private GitRepositoryService gitRepositoryService;

    /**
     * 提交代码审查
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param projectId 项目ID
     * @param title 审查标题
     * @param description 描述
     * @param branch 分支
     * @param commitHash 提交哈希
     * @param changedFiles 变更文件
     * @param diffContent 代码差异
     * @return 审查记录
     */
    public CodeReview submitReview(String agentId, String agentName, String projectId,
                                    String title, String description, String branch,
                                    String commitHash, String changedFiles, String diffContent) {
        return submitReview(agentId, agentName, projectId, null, title, description,
            branch, commitHash, changedFiles, diffContent);
    }

    /**
     * 提交代码审查（指定Git仓库）
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param projectId 项目ID
     * @param gitRepositoryId Git仓库ID
     * @param title 审查标题
     * @param description 描述
     * @param branch 分支
     * @param commitHash 提交哈希
     * @param changedFiles 变更文件
     * @param diffContent 代码差异
     * @return 审查记录
     */
    public CodeReview submitReview(String agentId, String agentName, String projectId,
                                    Long gitRepositoryId, String title, String description,
                                    String branch, String commitHash, String changedFiles,
                                    String diffContent) {
        CodeReview review = new CodeReview();
        review.setAgentId(agentId);
        review.setAgentName(agentName);
        review.setProjectId(projectId);
        review.setTitle(title);
        review.setDescription(description);
        review.setBranch(branch);
        review.setCommitHash(commitHash);
        review.setChangedFiles(changedFiles);
        review.setDiffContent(diffContent);
        review.setStatus(CodeReview.Status.PENDING.name());
        review.setSubmittedAt(LocalDateTime.now());

        // 设置Git仓库信息
        if (gitRepositoryId != null) {
            GitRepository gitRepo = gitRepositoryService.getRepository(gitRepositoryId);
            if (gitRepo != null) {
                review.setGitRepositoryId(gitRepositoryId);
                review.setGitRepositoryName(gitRepo.getName());
                review.setRepositoryType(gitRepo.getRepositoryType());
            }
        }

        // 验证Agent是否有权限提交到该仓库
        if (gitRepositoryId != null && !gitRepositoryService.isAgentAssigned(gitRepositoryId, agentId)) {
            log.warn("Agent {} is not assigned to repository {}, but review is still submitted", agentId, gitRepositoryId);
        }

        CodeReview saved = reviewRepository.save(review);
        log.info("Code review submitted: {} by agent {} to repository {}", title, agentId,
            review.getGitRepositoryName());

        // 触发自动审查
        performAutoReview(saved);

        return saved;
    }

    /**
     * 执行自动代码审查
     */
    private void performAutoReview(CodeReview review) {
        try {
            review.setReviewType(CodeReview.ReviewType.AUTO.name());
            review.startReview("system");

            // 简单的自动审查逻辑
            int issues = 0;
            int warnings = 0;
            StringBuilder result = new StringBuilder();
            result.append("自动审查结果:\n\n");

            // 检查代码差异
            String diff = review.getDiffContent();
            if (diff != null && !diff.isEmpty()) {
                // 检查是否有TODO
                if (diff.contains("TODO") || diff.contains("FIXME")) {
                    warnings++;
                    result.append("- 发现TODO/FIXME注释\n");
                }

                // 检查是否有console.log
                if (diff.contains("console.log") || diff.contains("System.out.print")) {
                    warnings++;
                    result.append("- 发现调试输出语句\n");
                }

                // 检查是否有硬编码
                if (diff.contains("\"password\"") || diff.contains("\"secret\"")) {
                    issues++;
                    result.append("- 发现可能的硬编码敏感信息\n");
                }

                // 检查变更文件数量
                String files = review.getChangedFiles();
                if (files != null) {
                    int fileCount = files.split(",").length;
                    if (fileCount > 20) {
                        warnings++;
                        result.append("- 变更文件数量较多(").append(fileCount).append("个)\n");
                    }
                }
            }

            review.setIssueCount(issues);
            review.setWarningCount(warnings);
            review.setAutoReviewResult(result.toString());

            // 根据问题数量决定审查结果
            if (issues > 0) {
                review.requestChanges("自动发现问题，请修改后重新提交");
            } else if (warnings > 0) {
                review.approve(70, "审查通过，但有警告需要关注");
            } else {
                review.approve(90, "自动审查通过");
            }

            reviewRepository.save(review);
            log.info("Auto review completed for: {} (issues={}, warnings={})",
                review.getTitle(), issues, warnings);

        } catch (Exception e) {
            log.error("Auto review failed", e);
            review.setReviewComment("自动审查失败: " + e.getMessage());
            reviewRepository.save(review);
        }
    }

    /**
     * 人工审查
     * @param reviewId 审查ID
     * @param reviewer 审查人
     * @param approved 是否通过
     * @param score 评分
     * @param comment 审查意见
     */
    public void performManualReview(Long reviewId, String reviewer, boolean approved,
                                     Integer score, String comment) {
        CodeReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("审查记录不存在"));

        review.setReviewType(CodeReview.ReviewType.MIXED.name());
        review.startReview(reviewer);

        if (approved) {
            review.approve(score, comment);
        } else {
            review.requestChanges(comment);
        }

        reviewRepository.save(review);
        log.info("Manual review completed by {}: {} ({})", reviewer, review.getTitle(), approved ? "APPROVED" : "CHANGES_REQUESTED");
    }

    /**
     * 获取审查记录
     */
    public CodeReview getReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElse(null);
    }

    /**
     * 获取所有审查记录（分页）
     */
    public Page<CodeReview> getAllReviews(int page, int size) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 获取待审查记录
     */
    public List<CodeReview> getPendingReviews() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(CodeReview.Status.PENDING.name());
    }

    /**
     * 获取Agent的审查记录
     */
    public List<CodeReview> getAgentReviews(String agentId) {
        return reviewRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    /**
     * 获取项目的审查记录
     */
    public List<CodeReview> getProjectReviews(String projectId) {
        return reviewRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * 获取Git仓库的审查记录
     */
    public List<CodeReview> getRepositoryReviews(Long gitRepositoryId) {
        return reviewRepository.findByGitRepositoryIdOrderByCreatedAtDesc(gitRepositoryId);
    }

    /**
     * 获取Agent在指定仓库的审查记录
     */
    public List<CodeReview> getAgentRepositoryReviews(String agentId, Long gitRepositoryId) {
        return reviewRepository.findByAgentIdAndGitRepositoryIdOrderByCreatedAtDesc(agentId, gitRepositoryId);
    }

    /**
     * 获取仓库的待审查记录
     */
    public List<CodeReview> getRepositoryPendingReviews(Long gitRepositoryId) {
        return reviewRepository.findByGitRepositoryIdAndStatusOrderByCreatedAtDesc(
            gitRepositoryId, CodeReview.Status.PENDING.name());
    }

    /**
     * 获取审查统计
     */
    public Map<String, Object> getReviewStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态统计
        List<Object[]> statusCounts = reviewRepository.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("statusCounts", statusMap);

        // 各Agent统计
        List<Object[]> agentCounts = reviewRepository.countByAgent();
        List<Map<String, Object>> agentList = new ArrayList<>();
        for (Object[] row : agentCounts) {
            Map<String, Object> agent = new HashMap<>();
            agent.put("agentId", row[0]);
            agent.put("agentName", row[1]);
            agent.put("count", row[2]);
            agentList.add(agent);
        }
        stats.put("agentStats", agentList);

        // 平均评分
        Double avgScore = reviewRepository.calculateAverageScore();
        stats.put("averageScore", avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0);

        // 待审查数量
        Long pendingCount = statusMap.getOrDefault("PENDING", 0L);
        stats.put("pendingCount", pendingCount);

        return stats;
    }

    /**
     * 删除审查记录
     */
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
        log.info("Deleted code review: {}", reviewId);
    }
}
