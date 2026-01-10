package com.tbw.cut.workflow.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流步骤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {
    
    /**
     * 步骤ID
     */
    private String stepId;
    
    /**
     * 步骤类型
     */
    private StepType type;
    
    /**
     * 步骤状态
     */
    private StepStatus status;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 输入路径
     */
    private String inputPath;
    
    /**
     * 输出路径
     */
    private String outputPath;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 步骤配置（JSON格式）
     */
    private String configJson;
    
    /**
     * 执行结果（JSON格式）
     */
    private String resultJson;
    
    /**
     * 创建新的步骤
     */
    public static WorkflowStep create(StepType type) {
        return WorkflowStep.builder()
                .stepId(java.util.UUID.randomUUID().toString())
                .type(type)
                .status(StepStatus.PENDING)
                .build();
    }
    
    /**
     * 标记步骤开始
     */
    public void markStarted() {
        this.status = StepStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    /**
     * 标记步骤完成
     */
    public void markCompleted(String outputPath) {
        this.status = StepStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.outputPath = outputPath;
    }
    
    /**
     * 标记步骤失败
     */
    public void markFailed(String errorMessage) {
        this.status = StepStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
    
    /**
     * 标记步骤跳过
     */
    public void markSkipped(String reason) {
        this.status = StepStatus.SKIPPED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = reason;
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
}