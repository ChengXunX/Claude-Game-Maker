package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.GitRepository;
import com.chengxun.gamemaker.web.repository.GitRepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Git仓库管理服务
 * 负责管理项目下的多个Git仓库
 *
 * 主要功能：
 * - 创建和管理Git仓库
 * - 分配Agent到仓库
 * - 配置审查规则
 *
 * @author chengxun
 * @since 1.0.0
 */
@Service
@Transactional
public class GitRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryService.class);

    @Autowired
    private GitRepositoryRepository gitRepositoryRepository;

    /**
     * 创建Git仓库
     */
    public GitRepository createRepository(GitRepository repository) {
        repository.setCreatedAt(java.time.LocalDateTime.now());
        repository.setUpdatedAt(java.time.LocalDateTime.now());
        GitRepository saved = gitRepositoryRepository.save(repository);
        log.info("Created git repository: {} for project: {}", saved.getName(), saved.getProjectId());
        return saved;
    }

    /**
     * 更新Git仓库
     */
    public GitRepository updateRepository(GitRepository repository) {
        repository.setUpdatedAt(java.time.LocalDateTime.now());
        GitRepository saved = gitRepositoryRepository.save(repository);
        log.info("Updated git repository: {}", saved.getId());
        return saved;
    }

    /**
     * 删除Git仓库
     */
    public void deleteRepository(Long repositoryId) {
        gitRepositoryRepository.deleteById(repositoryId);
        log.info("Deleted git repository: {}", repositoryId);
    }

    /**
     * 获取Git仓库
     */
    public GitRepository getRepository(Long repositoryId) {
        return gitRepositoryRepository.findById(repositoryId).orElse(null);
    }

    /**
     * 获取项目的所有Git仓库
     */
    public List<GitRepository> getProjectRepositories(String projectId) {
        return gitRepositoryRepository.findByProjectIdOrderByRepositoryType(projectId);
    }

    /**
     * 获取项目的活跃仓库
     */
    public List<GitRepository> getActiveRepositories(String projectId) {
        return gitRepositoryRepository.findByProjectIdAndStatus(projectId, GitRepository.Status.ACTIVE.name());
    }

    /**
     * 根据类型获取仓库
     */
    public GitRepository getRepositoryByType(String projectId, String repositoryType) {
        return gitRepositoryRepository.findByProjectIdAndRepositoryType(projectId, repositoryType).orElse(null);
    }

    /**
     * 获取启用了自动审查的仓库
     */
    public List<GitRepository> getAutoReviewRepositories(String projectId) {
        return gitRepositoryRepository.findActiveWithAutoReview(projectId);
    }

    /**
     * 分配Agent到仓库
     * @param repositoryId 仓库ID
     * @param agentId Agent ID
     */
    public void assignAgent(Long repositoryId, String agentId) {
        GitRepository repository = getRepository(repositoryId);
        if (repository == null) {
            throw new RuntimeException("Git仓库不存在: " + repositoryId);
        }

        String assignedAgents = repository.getAssignedAgents();
        Set<String> agents = new HashSet<>();

        if (assignedAgents != null && !assignedAgents.isEmpty()) {
            agents = new HashSet<>(Arrays.asList(assignedAgents.split(",")));
        }

        agents.add(agentId);
        repository.setAssignedAgents(String.join(",", agents));
        gitRepositoryRepository.save(repository);

        log.info("Assigned agent {} to repository {}", agentId, repositoryId);
    }

    /**
     * 取消分配Agent
     */
    public void unassignAgent(Long repositoryId, String agentId) {
        GitRepository repository = getRepository(repositoryId);
        if (repository == null) {
            throw new RuntimeException("Git仓库不存在: " + repositoryId);
        }

        String assignedAgents = repository.getAssignedAgents();
        if (assignedAgents != null && !assignedAgents.isEmpty()) {
            Set<String> agents = new HashSet<>(Arrays.asList(assignedAgents.split(",")));
            agents.remove(agentId);
            repository.setAssignedAgents(String.join(",", agents));
            gitRepositoryRepository.save(repository);
        }

        log.info("Unassigned agent {} from repository {}", agentId, repositoryId);
    }

    /**
     * 获取仓库关联的Agent列表
     */
    public List<String> getAssignedAgents(Long repositoryId) {
        GitRepository repository = getRepository(repositoryId);
        if (repository == null || repository.getAssignedAgents() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(repository.getAssignedAgents().split(","));
    }

    /**
     * 检查Agent是否分配到仓库
     */
    public boolean isAgentAssigned(Long repositoryId, String agentId) {
        List<String> agents = getAssignedAgents(repositoryId);
        return agents.contains(agentId);
    }

    /**
     * 根据Agent ID查找其关联的仓库
     */
    public List<GitRepository> getRepositoriesByAgent(String agentId, String projectId) {
        List<GitRepository> allRepos = getProjectRepositories(projectId);
        return allRepos.stream()
            .filter(repo -> isAgentAssigned(repo.getId(), agentId))
            .collect(Collectors.toList());
    }

    /**
     * 更新审查规则
     */
    public void updateReviewRules(Long repositoryId, String reviewRules) {
        GitRepository repository = getRepository(repositoryId);
        if (repository != null) {
            repository.setReviewRules(reviewRules);
            gitRepositoryRepository.save(repository);
            log.info("Updated review rules for repository: {}", repositoryId);
        }
    }

    /**
     * 启用/禁用自动审查
     */
    public void toggleAutoReview(Long repositoryId, boolean enabled) {
        GitRepository repository = getRepository(repositoryId);
        if (repository != null) {
            repository.setAutoReviewEnabled(enabled);
            gitRepositoryRepository.save(repository);
            log.info("Auto review {} for repository: {}", enabled ? "enabled" : "disabled", repositoryId);
        }
    }

    /**
     * 获取仓库统计
     */
    public Map<String, Object> getRepositoryStatistics(String projectId) {
        Map<String, Object> stats = new HashMap<>();

        List<GitRepository> repositories = getProjectRepositories(projectId);
        stats.put("totalRepositories", repositories.size());

        // 按类型统计
        Map<String, Long> typeCount = repositories.stream()
            .collect(Collectors.groupingBy(GitRepository::getRepositoryType, Collectors.counting()));
        stats.put("typeCounts", typeCount);

        // 活跃仓库数
        long activeCount = repositories.stream()
            .filter(r -> GitRepository.Status.ACTIVE.name().equals(r.getStatus()))
            .count();
        stats.put("activeRepositories", activeCount);

        // 启用自动审查的仓库数
        long autoReviewCount = repositories.stream()
            .filter(GitRepository::getAutoReviewEnabled)
            .count();
        stats.put("autoReviewRepositories", autoReviewCount);

        return stats;
    }

    /**
     * 初始化项目的默认Git仓库结构
     * @param projectId 项目ID
     * @param projectWorkDir 项目工作目录
     */
    public void initializeDefaultRepositories(String projectId, String projectWorkDir) {
        // 创建策划文档仓库
        createRepositoryIfNotExists(projectId, "策划文档", GitRepository.RepositoryType.DESIGN.name(),
            projectWorkDir + "/design", "design", "策划文档目录，包含游戏设计文档");

        // 创建服务端代码仓库
        createRepositoryIfNotExists(projectId, "服务端", GitRepository.RepositoryType.SERVER.name(),
            projectWorkDir + "/server", "server", "服务端代码仓库");

        // 创建客户端代码仓库
        createRepositoryIfNotExists(projectId, "客户端", GitRepository.RepositoryType.CLIENT.name(),
            projectWorkDir + "/client", "client", "客户端代码仓库");

        // 创建共享库仓库
        createRepositoryIfNotExists(projectId, "共享库", GitRepository.RepositoryType.SHARED.name(),
            projectWorkDir + "/shared", "shared", "共享库代码仓库");

        log.info("Initialized default git repositories for project: {}", projectId);
    }

    /**
     * 创建仓库（如果不存在）
     */
    private void createRepositoryIfNotExists(String projectId, String name, String type,
                                              String localPath, String relativePath, String description) {
        Optional<GitRepository> existing = gitRepositoryRepository.findByProjectIdAndName(projectId, name);
        if (existing.isEmpty()) {
            GitRepository repository = new GitRepository();
            repository.setName(name);
            repository.setProjectId(projectId);
            repository.setRepositoryType(type);
            repository.setLocalPath(localPath);
            repository.setRelativePath(relativePath);
            repository.setDescription(description);
            repository.setStatus(GitRepository.Status.ACTIVE.name());
            repository.setAutoReviewEnabled(true);

            gitRepositoryRepository.save(repository);
            log.info("Created default repository: {} for project: {}", name, projectId);
        }
    }
}
