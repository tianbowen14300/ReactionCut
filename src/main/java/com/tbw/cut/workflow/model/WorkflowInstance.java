package com.tbw.cut.workflow.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 工作流实例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {
    
    /**
     * 实例ID
     */
    private String instanceId;
    
    /**
     * 关联的任务ID（投稿任务ID）
     */
    private String taskId;
    
    /**
     * 工作流配置
     */
    private WorkflowConfig config;
    
    /**
     * 工作流状态
     */
    private WorkflowStatus status;
    
    /**
     * 工作流步骤列表
     */
    @Builder.Default
    private List<WorkflowStep> steps = new ArrayList<>();
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 当前执行的步骤索引
     */
    @Builder.Default
    private int currentStepIndex = 0;
    
    /**
     * 资源分配信息
     */
    private ResourceAllocation resourceAllocation;
    
    /**
     * 创建新的工作流实例
     */
    public static WorkflowInstance create(String taskId, WorkflowConfig config) {
        WorkflowInstance instance = WorkflowInstance.builder()
                .instanceId(java.util.UUID.randomUUID().toString())
                .taskId(taskId)
                .config(config)
                .status(WorkflowStatus.PENDING)
                .build();
        
        // 根据配置初始化步骤
        instance.initializeSteps();
        
        return instance;
    }
    
    /**
     * 根据配置初始化步骤
     */
    private void initializeSteps() {
        steps.clear();
        
        // 剪辑步骤
        if (config.isEnableClipping()) {
            steps.add(WorkflowStep.create(StepType.CLIPPING));
        }
        
        // 合并步骤
        if (config.isEnableMerging()) {
            steps.add(WorkflowStep.create(StepType.MERGING));
        }
        
        // 分段步骤（仅在启用且不是直接投稿时）
        if (!config.isEnableDirectSubmission() && 
            config.getSegmentationConfig() != null && 
            config.getSegmentationConfig().isEnabled()) {
            steps.add(WorkflowStep.create(StepType.SEGMENTATION));
        }
        
        // 投稿步骤
        steps.add(WorkflowStep.create(StepType.SUBMISSION));
    }
    
    /**
     * 开始工作流
     */
    public void start() {
        this.status = WorkflowStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.currentStepIndex = 0;
    }
    
    /**
     * 完成工作流
     */
    public void complete() {
        this.status = WorkflowStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 标记工作流失败
     */
    public void fail(String errorMessage) {
        this.status = WorkflowStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
    
    /**
     * 取消工作流
     */
    public void cancel() {
        this.status = WorkflowStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 暂停工作流
     */
    public void pause() {
        this.status = WorkflowStatus.PAUSED;
    }
    
    /**
     * 恢复工作流
     */
    public void resume() {
        this.status = WorkflowStatus.RUNNING;
    }
    
    /**
     * 获取当前步骤
     */
    public WorkflowStep getCurrentStep() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }
    
    /**
     * 移动到下一步骤
     */
    public boolean moveToNextStep() {
        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            return true;
        }
        return false;
    }
    
    /**
     * 是否有下一步骤
     */
    public boolean hasNextStep() {
        return currentStepIndex < steps.size() - 1;
    }
    
    /**
     * 获取执行进度（0-100）
     */
    public int getProgress() {
        if (steps.isEmpty()) {
            return 0;
        }
        
        long completedSteps = steps.stream()
                .mapToLong(step -> step.getStatus() == StepStatus.COMPLETED ? 1 : 0)
                .sum();
        
        return (int) ((completedSteps * 100) / steps.size());
    }
    
    /**
     * 获取执行时长（毫秒）
     */
    public Long getDurationMs() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    /**
     * 是否可以执行
     */
    public boolean canExecute() {
        return status == WorkflowStatus.PENDING || status == WorkflowStatus.PAUSED;
    }
    
    /**
     * 是否正在执行
     */
    public boolean isRunning() {
        return status == WorkflowStatus.RUNNING;
    }
    
    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return status == WorkflowStatus.COMPLETED || 
               status == WorkflowStatus.FAILED || 
               status == WorkflowStatus.CANCELLED;
    }
    
    /**
     * 获取工作流类型
     * 
     * @return 工作流类型，如果配置为空则返回null
     */
    public String getWorkflowType() {
        return config != null ? config.getWorkflowType() : null;
    }
}