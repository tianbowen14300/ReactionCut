package com.tbw.cut.dto;

import com.tbw.cut.workflow.model.WorkflowConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;

/**
 * 集成请求DTO
 * 包含下载请求和投稿请求的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationRequest {
    
    /**
     * 下载请求数据（原始前端格式）
     */
    private Map<String, Object> downloadRequestRaw;
    
    /**
     * 下载请求数据（DTO格式，用于向后兼容）
     */
    private VideoDownloadDTO downloadRequest;
    
    /**
     * 投稿请求数据
     */
    private SubmissionRequestDTO submissionRequest;
    
    /**
     * 工作流配置
     */
    private WorkflowConfig workflowConfig;
    
    /**
     * 是否启用投稿功能
     */
    private Boolean enableSubmission;
    
    /**
     * 请求来源标识
     */
    private String requestSource = "INTEGRATION_FORM";
    
    /**
     * 用户ID（用于权限验证）
     */
    private String userId;
    
    /**
     * 检查是否为有效的集成请求
     */
    public boolean isValidIntegrationRequest() {
        return (downloadRequest != null || downloadRequestRaw != null) && 
               enableSubmission != null && 
               enableSubmission && 
               submissionRequest != null;
    }
    
    /**
     * 检查是否仅为下载请求
     */
    public boolean isDownloadOnlyRequest() {
        return (downloadRequest != null || downloadRequestRaw != null) && 
               (enableSubmission == null || !enableSubmission);
    }
    
    /**
     * 获取有效的下载请求数据（优先使用原始格式）
     */
    public Object getEffectiveDownloadRequest() {
        return downloadRequestRaw != null ? downloadRequestRaw : downloadRequest;
    }
    
    /**
     * 检查是否有工作流配置
     */
    public boolean hasWorkflowConfig() {
        return workflowConfig != null;
    }
    
    /**
     * 获取有效的工作流配置（如果没有则返回默认配置）
     */
    public WorkflowConfig getEffectiveWorkflowConfig() {
        if (workflowConfig != null) {
            return workflowConfig;
        }
        
        // 返回下载+投稿的默认配置
        return WorkflowConfig.createForDownloadSubmission(userId != null ? userId : "anonymous");
    }
}