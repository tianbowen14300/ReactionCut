package com.tbw.cut.workflow.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConfig {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 是否启用直接投稿（跳过分段）
     */
    @Builder.Default
    private boolean enableDirectSubmission = true;
    
    /**
     * 分段配置
     */
    @Builder.Default
    private SegmentationConfig segmentationConfig = SegmentationConfig.createDefault();
    
    /**
     * 是否启用剪辑步骤
     */
    @Builder.Default
    private boolean enableClipping = true;
    
    /**
     * 是否启用合并步骤
     */
    @Builder.Default
    private boolean enableMerging = true;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建默认配置
     */
    public static WorkflowConfig createDefault(String userId) {
        return WorkflowConfig.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建下载+投稿默认配置
     */
    public static WorkflowConfig createForDownloadSubmission(String userId) {
        return WorkflowConfig.builder()
                .userId(userId)
                .enableDirectSubmission(true)
                .segmentationConfig(SegmentationConfig.createDefault())
                .enableClipping(true)
                .enableMerging(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        if (segmentationConfig != null && !segmentationConfig.isValid()) {
            return false;
        }
        
        // 如果启用直接投稿，分段必须禁用
        if (enableDirectSubmission && segmentationConfig != null && segmentationConfig.isEnabled()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取验证错误信息
     */
    public String getValidationError() {
        if (segmentationConfig != null && !segmentationConfig.isValid()) {
            return "分段配置无效: " + segmentationConfig.getValidationError();
        }
        
        if (enableDirectSubmission && segmentationConfig != null && segmentationConfig.isEnabled()) {
            return "启用直接投稿时不能同时启用分段处理";
        }
        
        return null;
    }
    
    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 工作流类型
     */
    private String workflowType;
    
    /**
     * 设置工作流类型
     * 
     * @param workflowType 工作流类型
     */
    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
        updateTimestamp();
    }
    
    /**
     * 获取工作流类型
     * 
     * @return 工作流类型
     */
    public String getWorkflowType() {
        return workflowType;
    }
    
    /**
     * 设置是否启用分段处理
     * 
     * @param enableSegmentation 是否启用分段处理
     */
    public void setEnableSegmentation(boolean enableSegmentation) {
        if (this.segmentationConfig == null) {
            this.segmentationConfig = SegmentationConfig.createDefault();
        }
        this.segmentationConfig.setEnabled(enableSegmentation);
        updateTimestamp();
    }
    
    /**
     * 检查是否启用分段处理
     * 
     * @return 是否启用分段处理
     */
    public boolean isEnableSegmentation() {
        return segmentationConfig != null && segmentationConfig.isEnabled();
    }
}