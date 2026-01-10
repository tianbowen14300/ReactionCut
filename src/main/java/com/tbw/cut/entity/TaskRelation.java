package com.tbw.cut.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 任务关联实体类
 * 管理视频下载任务与投稿任务之间的关联关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_relations")
public class TaskRelation {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 下载任务ID
     */
    @TableField("download_task_id")
    private Long downloadTaskId;
    
    /**
     * 投稿任务ID
     */
    @TableField("submission_task_id")
    private String submissionTaskId;
    
    /**
     * 关联类型
     */
    @TableField("relation_type")
    private RelationType relationType;
    
    /**
     * 关联状态
     */
    @TableField("status")
    private RelationStatus status;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 工作流实例ID
     */
    @TableField("workflow_instance_id")
    private String workflowInstanceId;
    
    /**
     * 工作流状态
     */
    @TableField("workflow_status")
    private WorkflowStatus workflowStatus;
    
    /**
     * 工作流启动时间
     */
    @TableField("workflow_started_at")
    private LocalDateTime workflowStartedAt;
    
    /**
     * 最后错误信息
     */
    @TableField("last_error_message")
    private String lastErrorMessage;
    
    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;
    
    /**
     * 关联类型枚举
     */
    public enum RelationType {
        /**
         * 集成创建 - 通过集成表单同时创建的关联
         */
        INTEGRATED,
        
        /**
         * 手动关联 - 用户手动建立的关联
         */
        MANUAL
    }
    
    /**
     * 关联状态枚举
     */
    public enum RelationStatus {
        /**
         * 活跃状态 - 关联正常，任务进行中
         */
        ACTIVE,
        
        /**
         * 已完成 - 两个任务都已完成
         */
        COMPLETED,
        
        /**
         * 失败状态 - 任一任务失败导致关联失效
         */
        FAILED
    }
    
    /**
     * 工作流状态枚举
     */
    public enum WorkflowStatus {
        /**
         * 等待下载完成
         */
        PENDING_DOWNLOAD,
        
        /**
         * 工作流已启动
         */
        WORKFLOW_STARTED,
        
        /**
         * 工作流运行中
         */
        WORKFLOW_RUNNING,
        
        /**
         * 工作流已完成
         */
        WORKFLOW_COMPLETED,
        
        /**
         * 工作流失败
         */
        WORKFLOW_FAILED,
        
        /**
         * 工作流启动失败
         */
        WORKFLOW_STARTUP_FAILED
    }
    
    /**
     * 创建集成关联的便捷方法
     */
    public static TaskRelation createIntegratedRelation(Long downloadTaskId, String submissionTaskId) {
        return TaskRelation.builder()
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationType(RelationType.INTEGRATED)
                .status(RelationStatus.ACTIVE)
                .workflowStatus(WorkflowStatus.PENDING_DOWNLOAD)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建手动关联的便捷方法
     */
    public static TaskRelation createManualRelation(Long downloadTaskId, String submissionTaskId) {
        return TaskRelation.builder()
                .downloadTaskId(downloadTaskId)
                .submissionTaskId(submissionTaskId)
                .relationType(RelationType.MANUAL)
                .status(RelationStatus.ACTIVE)
                .workflowStatus(WorkflowStatus.PENDING_DOWNLOAD)
                .retryCount(0)
                .build();
    }
    
    /**
     * 检查关联是否为活跃状态
     */
    public boolean isActive() {
        return RelationStatus.ACTIVE.equals(this.status);
    }
    
    /**
     * 检查关联是否为集成类型
     */
    public boolean isIntegrated() {
        return RelationType.INTEGRATED.equals(this.relationType);
    }
    
    /**
     * 标记关联为完成状态
     */
    public void markCompleted() {
        this.status = RelationStatus.COMPLETED;
    }
    
    /**
     * 标记关联为失败状态
     */
    public void markFailed() {
        this.status = RelationStatus.FAILED;
    }
    
    /**
     * 检查工作流是否等待下载完成
     */
    public boolean isPendingDownload() {
        return WorkflowStatus.PENDING_DOWNLOAD.equals(this.workflowStatus);
    }
    
    /**
     * 检查工作流是否已启动
     */
    public boolean isWorkflowStarted() {
        return WorkflowStatus.WORKFLOW_STARTED.equals(this.workflowStatus) ||
               WorkflowStatus.WORKFLOW_RUNNING.equals(this.workflowStatus);
    }
    
    /**
     * 检查工作流是否已完成
     */
    public boolean isWorkflowCompleted() {
        return WorkflowStatus.WORKFLOW_COMPLETED.equals(this.workflowStatus);
    }
    
    /**
     * 检查工作流是否失败
     */
    public boolean isWorkflowFailed() {
        return WorkflowStatus.WORKFLOW_FAILED.equals(this.workflowStatus) ||
               WorkflowStatus.WORKFLOW_STARTUP_FAILED.equals(this.workflowStatus);
    }
    
    /**
     * 更新工作流信息
     */
    public void updateWorkflowInfo(String workflowInstanceId, WorkflowStatus workflowStatus) {
        this.workflowInstanceId = workflowInstanceId;
        this.workflowStatus = workflowStatus;
        this.workflowStartedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 记录工作流错误
     */
    public void recordWorkflowError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 重置重试计数
     */
    public void resetRetryCount() {
        this.retryCount = 0;
        this.lastErrorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }
}