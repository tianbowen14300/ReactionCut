package com.tbw.cut.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 集成操作结果DTO
 * 包含下载任务和投稿任务的创建结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationResult {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 下载任务ID
     */
    private Long downloadTaskId;
    
    /**
     * 投稿任务ID
     */
    private String submissionTaskId;
    
    /**
     * 任务关联ID
     */
    private Long relationId;
    
    /**
     * 工作流实例ID（如果使用工作流引擎）
     */
    private String workflowInstanceId;
    
    /**
     * 工作流状态
     */
    private WorkflowStatus workflowStatus;
    
    /**
     * 工作流状态枚举
     */
    public enum WorkflowStatus {
        PENDING_DOWNLOAD("等待下载完成"),     // 等待下载完成
        WORKFLOW_STARTED("工作流已启动"),     // 工作流已启动
        WORKFLOW_RUNNING("工作流运行中"),     // 工作流运行中
        WORKFLOW_COMPLETED("工作流已完成"),   // 工作流已完成
        WORKFLOW_FAILED("工作流失败"),        // 工作流失败
        WORKFLOW_STARTUP_FAILED("工作流启动失败"); // 工作流启动失败
        
        private final String description;
        
        WorkflowStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 错误代码（失败时）
     */
    private String errorCode;
    
    /**
     * 详细错误信息（失败时）
     */
    private String errorDetails;
    
    /**
     * 创建成功结果
     */
    public static IntegrationResult success(Long downloadTaskId, String submissionTaskId, Long relationId) {
        return IntegrationResult.builder()
                .success(true)
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationId(relationId)
                .message("集成任务创建成功")
                .build();
    }
    
    /**
     * 创建带工作流的成功结果
     */
    public static IntegrationResult successWithWorkflow(Long downloadTaskId, String submissionTaskId, Long relationId, String workflowInstanceId) {
        return IntegrationResult.builder()
                .success(true)
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationId(relationId)
                .workflowInstanceId(workflowInstanceId)
                .workflowStatus(WorkflowStatus.WORKFLOW_STARTED)
                .message("集成任务和工作流创建成功")
                .build();
    }
    
    /**
     * 创建等待工作流启动的成功结果
     */
    public static IntegrationResult successWithPendingWorkflow(Long downloadTaskId, 
                                                              String submissionTaskId, 
                                                              Long relationId) {
        return IntegrationResult.builder()
                .success(true)
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationId(relationId)
                .workflowStatus(WorkflowStatus.PENDING_DOWNLOAD)
                .message("任务创建成功，工作流将在下载完成后启动")
                .build();
    }
    
    /**
     * 创建工作流已启动的成功结果
     */
    public static IntegrationResult successWithStartedWorkflow(Long downloadTaskId, 
                                                              String submissionTaskId, 
                                                              Long relationId, 
                                                              String workflowInstanceId) {
        return IntegrationResult.builder()
                .success(true)
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationId(relationId)
                .workflowInstanceId(workflowInstanceId)
                .workflowStatus(WorkflowStatus.WORKFLOW_STARTED)
                .message("任务创建成功，工作流已启动")
                .build();
    }
    
    /**
     * 创建仅下载成功结果
     */
    public static IntegrationResult downloadOnlySuccess(Long downloadTaskId) {
        return IntegrationResult.builder()
                .success(true)
                .downloadTaskId(downloadTaskId)
                .message("下载任务创建成功")
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static IntegrationResult failure(String message, String errorCode) {
        return IntegrationResult.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
    
    /**
     * 创建失败结果（带详细错误信息）
     */
    public static IntegrationResult failure(String message, String errorCode, String errorDetails) {
        return IntegrationResult.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .build();
    }
    
    /**
     * 检查是否为集成操作结果
     */
    public boolean isIntegratedResult() {
        return success && downloadTaskId != null && submissionTaskId != null;
    }
    
    /**
     * 检查是否为仅下载操作结果
     */
    public boolean isDownloadOnlyResult() {
        return success && downloadTaskId != null && submissionTaskId == null;
    }
    
    /**
     * 检查是否使用了工作流引擎
     */
    public boolean hasWorkflowInstance() {
        return workflowInstanceId != null && !workflowInstanceId.trim().isEmpty();
    }
    
    /**
     * 检查工作流是否处于等待状态
     */
    public boolean isWorkflowPending() {
        return workflowStatus == WorkflowStatus.PENDING_DOWNLOAD;
    }
    
    /**
     * 检查工作流是否已启动
     */
    public boolean isWorkflowStarted() {
        return workflowStatus == WorkflowStatus.WORKFLOW_STARTED || 
               workflowStatus == WorkflowStatus.WORKFLOW_RUNNING;
    }
    
    /**
     * 检查工作流是否已完成
     */
    public boolean isWorkflowCompleted() {
        return workflowStatus == WorkflowStatus.WORKFLOW_COMPLETED;
    }
    
    /**
     * 检查工作流是否失败
     */
    public boolean isWorkflowFailed() {
        return workflowStatus == WorkflowStatus.WORKFLOW_FAILED || 
               workflowStatus == WorkflowStatus.WORKFLOW_STARTUP_FAILED;
    }
    
    /**
     * 获取工作流状态描述
     */
    public String getWorkflowStatusDescription() {
        return workflowStatus != null ? workflowStatus.getDescription() : "未知状态";
    }
}