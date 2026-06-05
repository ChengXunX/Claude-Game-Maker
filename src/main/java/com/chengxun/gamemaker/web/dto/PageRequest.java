package com.chengxun.gamemaker.web.dto;

/**
 * 标准分页请求参数
 *
 * @author chengxun
 * @since 2.0.0
 */
public class PageRequest {

    /** 页码（从 0 开始） */
    private int page = 0;

    /** 每页大小 */
    private int size = 20;

    /** 排序字段 */
    private String sortBy = "createdAt";

    /** 排序方向 */
    private String sortDir = "desc";

    /** 搜索关键词 */
    private String keyword;

    public PageRequest() {}

    public PageRequest(int page, int size) {
        this.page = Math.max(0, page);
        this.size = Math.min(100, Math.max(1, size));
    }

    /** 转换为 Spring Data PageRequest */
    public org.springframework.data.domain.PageRequest toPageRequest() {
        org.springframework.data.domain.Sort sort = "asc".equalsIgnoreCase(sortDir)
            ? org.springframework.data.domain.Sort.by(sortBy).ascending()
            : org.springframework.data.domain.Sort.by(sortBy).descending();
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }

    // Getters & Setters
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.min(100, Math.max(1, size)); }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
