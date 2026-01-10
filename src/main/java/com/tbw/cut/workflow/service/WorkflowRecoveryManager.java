package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStep;

/**
 * 工作流恢复管理器接口
 * 
 * 负责处理工作流执行过程中的各种错误情况，提供恢复机制和重试策略。
 * 支持不同类型错误的分类处理，自动重试机制，以及手动干预和恢复功能。
 */
public interface WorkflowRecoveryManager {
    
    /**
     * 错误类型枚举
     */
    enum ErrorType {
        TEMPORARY_ERROR,      // 临时错误，可以重试
        CONFIGURATION_ERROR,  // 配置错误，需要用户干预
        RESOURCE_ERROR,       // 资源错误，需要等待资源可用
        FATAL_ERROR          // 致命错误，无法恢复
    }
    
    /**
     * 恢复策略枚举
     */
    enum RecoveryStrategy {
        RETRY,               // 重试
        PAUSE_FOR_INTERVENTION, // 暂停等待用户干预
        WAIT_FOR_RESOURCE,   // 等待资源可用
        MARK_AS_FAILED,      // 标记为失败
        SKIP_STEP           // 跳过当前步骤
    }
    
    /**
     * 从工作流失败中恢复
     * 
     * @param instance 失败的工作流实例
     * @param error 错误异常
     * @return 是否成功启动恢复流程
     */
    boolean recoverFromFailure(WorkflowInstance instance, Exception error);
    
    /**
     * 从步骤失败中恢复
     * 
     * @param instance 工作流实例
     * @param step 失败的步骤
     * @param error 错误异常
     * @return 是否成功启动恢复流程
     */
    boolean recoverFromStepFailure(WorkflowInstance instance, WorkflowStep step, Exception error);
    
    /**
     * 分类错误类型
     * 
     * @param error 错误异常
     * @return 错误类型
     */
    ErrorType classifyError(Exception error);
    
    /**
     * 确定恢复策略
     * 
     * @param errorType 错误类型
     * @param instance 工作流实例
     * @param step 失败的步骤（可选）
     * @return 恢复策略
     */
    RecoveryStrategy determineRecoveryStrategy(ErrorType errorType, WorkflowInstance instance, WorkflowStep step);
    
    /**
     * 安排重试
     * 
     * @param instance 工作流实例
     * @param step 需要重试的步骤（可选，null表示重试整个工作流）
     * @return 是否成功安排重试
     */
    boolean scheduleRetry(WorkflowInstance instance, WorkflowStep step);
    
    /**
     * 暂停工作流等待用户干预
     * 
     * @param instance 工作流实例
     * @param reason 暂停原因
     * @return 是否成功暂停
     */
    boolean pauseWorkflowForUserIntervention(WorkflowInstance instance, String reason);
    
    /**
     * 等待资源可用
     * 
     * @param instance 工作流实例
     * @param resourceType 资源类型
     * @return 是否成功安排等待
     */
    boolean waitForResourceAvailability(WorkflowInstance instance, String resourceType);
    
    /**
     * 标记工作流为失败
     * 
     * @param instance 工作流实例
     * @param reason 失败原因
     * @return 是否成功标记
     */
    boolean markWorkflowAsFailed(WorkflowInstance instance, String reason);
    
    /**
     * 跳过失败的步骤
     * 
     * @param instance 工作流实例
     * @param step 要跳过的步骤
     * @param reason 跳过原因
     * @return 是否成功跳过
     */
    boolean skipFailedStep(WorkflowInstance instance, WorkflowStep step, String reason);
    
    /**
     * 手动恢复工作流
     * 
     * @param instanceId 工作流实例ID
     * @param fromStepId 从哪个步骤开始恢复（可选）
     * @return 是否成功启动恢复
     */
    boolean manualRecover(String instanceId, String fromStepId);
    
    /**
     * 获取工作流的恢复历史
     * 
     * @param instanceId 工作流实例ID
     * @return 恢复历史记录
     */
    java.util.List<RecoveryRecord> getRecoveryHistory(String instanceId);
    
    /**
     * 恢复记录
     */
    class RecoveryRecord {
        private String instanceId;
        private String stepId;
        private ErrorType errorType;
        private RecoveryStrategy strategy;
        private String errorMessage;
        private String recoveryReason;
        private java.time.LocalDateTime recoveryTime;
        private boolean successful;
        
        // Constructors
        public RecoveryRecord() {}
        
        public RecoveryRecord(String instanceId, String stepId, ErrorType errorType, 
                            RecoveryStrategy strategy, String errorMessage) {
            this.instanceId = instanceId;
            this.stepId = stepId;
            this.errorType = errorType;
            this.strategy = strategy;
            this.errorMessage = errorMessage;
            this.recoveryTime = java.time.LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        
        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }
        
        public ErrorType getErrorType() { return errorType; }
        public void setErrorType(ErrorType errorType) { this.errorType = errorType; }
        
        public RecoveryStrategy getStrategy() { return strategy; }
        public void setStrategy(RecoveryStrategy strategy) { this.strategy = strategy; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getRecoveryReason() { return recoveryReason; }
        public void setRecoveryReason(String recoveryReason) { this.recoveryReason = recoveryReason; }
        
        public java.time.LocalDateTime getRecoveryTime() { return recoveryTime; }
        public void setRecoveryTime(java.time.LocalDateTime recoveryTime) { this.recoveryTime = recoveryTime; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
    }
}