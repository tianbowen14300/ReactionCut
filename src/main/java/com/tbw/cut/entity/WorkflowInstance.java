package com.tbw.cut.entity;

import com.tbw.cut.workflow.model.WorkflowStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例实体类
 * 
 * 对应数据库表：workflow_instances
 */
@Data
public class WorkflowInstance {
    
    /**
     * 工作流实例ID (UUID)
     */
    private String instanceId;
    
    /**
     * 关联的任务ID
     */
    private String taskId;
    
    /**
     * 工作流类型
     */
    private String workflowType;
    
    /**
     * 工作流状态
     */
    private WorkflowStatus status;
    
    /**
     * 当前执行步骤
     */
    private String currentStep;
    
    /**
     * 整体进度百分比 (0.00-100.00)
     */
    private Double progress;
    
    /**
     * 关联的配置ID
     */
    private Long configurationId;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
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
     * 检查工作流是否正在运行
     * 
     * @return true如果正在运行，false如果未运行
     */
    public boolean isRunning() {
        return status == WorkflowStatus.RUNNING;
    }
    
    /**
     * 检查工作流是否已完成
     * 
     * @return true如果已完成，false如果未完成
     */
    public boolean isCompleted() {
        return status == WorkflowStatus.COMPLETED;
    }
    
    /**
     * 检查工作流是否失败
     * 
     * @return true如果失败，false如果未失败
     */
    public boolean isFailed() {
        return status == WorkflowStatus.FAILED;
    }
    
    /**
     * 检查工作流是否被取消
     * 
     * @return true如果被取消，false如果未被取消
     */
    public boolean isCancelled() {
        return status == WorkflowStatus.CANCELLED;
    }
    
    /**
     * 检查工作流是否可以被取消
     * 
     * @return true如果可以取消，false如果不能取消
     */
    public boolean isCancellable() {
        return status == WorkflowStatus.PENDING || 
               status == WorkflowStatus.RUNNING || 
               status == WorkflowStatus.PAUSED;
    }
    
    /**
     * 检查工作流是否可以被暂停
     * 
     * @return true如果可以暂停，false如果不能暂停
     */
    public boolean isPausable() {
        return status == WorkflowStatus.RUNNING;
    }
    
    /**
     * 检查工作流是否可以被恢复
     * 
     * @return true如果可以恢复，false如果不能恢复
     */
    public boolean isResumable() {
        return status == WorkflowStatus.PAUSED;
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
            case PAUSED:
                return "已暂停";
            case COMPLETED:
                return "执行完成";
            case FAILED:
                return "执行失败";
            case CANCELLED:
                return "已取消";
            default:
                return status.toString();
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
    
    @Override
    public String toString() {
        return String.format(
            "WorkflowInstance{instanceId='%s', taskId='%s', type='%s', status=%s, progress=%.2f%%, currentStep='%s'}",
            instanceId, taskId, workflowType, status, 
            progress != null ? progress : 0.0, currentStep
        );
    }
}