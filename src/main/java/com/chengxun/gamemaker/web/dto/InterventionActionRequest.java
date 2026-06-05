package com.chengxun.gamemaker.web.dto;

import jakarta.validation.constraints.Size;

/**
 * 干预操作请求DTO
 * 用于确认、执行、拒绝、取消干预等操作
 *
 * @author chengxun
 * @since 1.0.0
 */
public class InterventionActionRequest {

    /** 确认/执行/拒绝/取消的原因或说明 */
    @Size(max = 5000, message = "操作说明不能超过5000字符")
    private String comment;

    public InterventionActionRequest() {}

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
