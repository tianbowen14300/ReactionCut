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
}