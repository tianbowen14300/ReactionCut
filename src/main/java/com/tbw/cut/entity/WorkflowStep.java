package com.tbw.cut.entity;

import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.model.StepType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流步骤实体类
 * 
 * 对应数据库表：workflow_steps
 */
@Data
public class WorkflowStep {
    
    /**
     * 步骤ID (UUID)
     */
    private String stepId;
    
    /**
     * 工作流实例ID
     */
    private String instanceId;
    
    /**
     * 步骤名称
     */
    private String stepName;
    
    /**
     * 步骤类型
     */
    private StepType stepType;
    
    /**
     * 步骤执行顺序
     */
    private Integer stepOrder;
    
    /**
     * 步骤状态
     */
    private StepStatus status;
    
    /**
     * 步骤进度百分比 (0.00-100.00)
     */
    private Double progress;
    
    /**
     * 步骤输入数据 (JSON格式)
     */
    private String inputData;
    
    /**
     * 步骤输出数据 (JSON格式)
     */
    private String outputData;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetries;
    
    /**
     * 步骤开始时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 步骤完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 检查步骤是否正在运行
     * 
     * @return true如果正在运行，false如果未运行
     */
    public boolean isRunning() {
        return status == StepStatus.RUNNING;
    }
    
    /**
     * 检查步骤是否已完成
     * 
     * @return true如果已完成，false如果未完成
     */
    public boolean isCompleted() {
        return status == StepStatus.COMPLETED;
    }
    
    /**
     * 检查步骤是否失败
     * 
     * @return true如果失败，false如果未失败
     */
    public boolean isFailed() {
        return status == StepStatus.FAILED;
    }
    
    /**
     * 检查步骤是否被跳过
     * 
     * @return true如果被跳过，false如果未被跳过
     */
    public boolean isSkipped() {
        return status == StepStatus.SKIPPED;
    }
    
    /**
     * 检查步骤是否可以重试
     * 
     * @return true如果可以重试，false如果不能重试
     */
    public boolean canRetry() {
        return status == StepStatus.FAILED && 
               retryCount != null && maxRetries != null && 
               retryCount < maxRetries;
    }
    
    /**
     * 获取执行时长（秒）
     * 
     * @return 执行时长，如果未开始则返回0
     */
    public long getExecutionDurationSeconds() {
        if (startedAt == null) {
            return 0;
        }
        
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).getSeconds();
    }
    
    /**
     * 获取状态描述
     * 
     * @return 状态的中文描述
     */
    public String getStatusDescription() {
        if (status == null) {
            return "未知状态";
        }
        
        switch (status) {
            case PENDING:
                return "等待执行";
            case RUNNING:
                return "正在执行";
            case COMPLETED:
                return "执行完成";
            case FAILED:
                return "执行失败";
            case SKIPPED:
                return "已跳过";
            default:
                return status.toString();
        }
    }
    
    /**
     * 获取步骤类型描述
     * 
     * @return 步骤类型的中文描述
     */
    public String getStepTypeDescription() {
        if (stepType == null) {
            return "未知类型";
        }
        
        switch (stepType) {
            case CLIPPING:
                return "视频剪辑";
            case MERGING:
                return "视频合并";
            case SEGMENTATION:
                return "视频分段";
            case SUBMISSION:
                return "视频投稿";
            default:
                return stepType.toString();
        }
    }
    
    /**
     * 获取进度描述
     * 
     * @return 进度的格式化字符串
     */
    public String getProgressDescription() {
        if (progress == null) {
            return "0.00%";
        }
        return String.format("%.2f%%", progress);
    }
    
    /**
     * 获取重试信息描述
     * 
     * @return 重试信息的格式化字符串
     */
    public String getRetryDescription() {
        if (retryCount == null || maxRetries == null) {
            return "无重试信息";
        }
        return String.format("%d/%d", retryCount, maxRetries);
    }
    
    @Override
    public String toString() {
        return String.format(
            "WorkflowStep{stepId='%s', instanceId='%s', name='%s', type=%s, order=%d, status=%s, progress=%.2f%%}",
            stepId, instanceId, stepName, stepType, stepOrder, status, 
            progress != null ? progress : 0.0
        );
    }
}