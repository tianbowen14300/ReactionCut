package com.tbw.cut.service;

import com.tbw.cut.dto.SubmissionRequestDTO;
import com.tbw.cut.entity.WorkflowConfiguration;
import com.tbw.cut.entity.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowConfig;
import com.tbw.cut.workflow.model.WorkflowStatus;

import java.util.List;
import java.util.Optional;

/**
 * 工作流集成服务接口
 * 
 * 提供完整的工作流集成功能，整合下载、处理、投稿等所有步骤
 */
public interface WorkflowIntegrationService {
    
    /**
     * 启动下载+投稿工作流
     * 
     * @param bvid 视频BVID
     * @param userId 用户ID
     * @param configurationId 配置ID（可选）
     * @return 工作流实例ID
     */
    String startDownloadSubmissionWorkflow(String bvid, Long userId, Long configurationId);
    
    /**
     * 启动新建投稿任务工作流
     * 
     * @param submissionRequest 投稿请求
     * @param userId 用户ID
     * @param configurationId 配置ID（可选）
     * @return 工作流实例ID
     */
    String startSubmissionTaskWorkflow(SubmissionRequestDTO submissionRequest, Long userId, Long configurationId);
    
    /**
     * 获取工作流状态
     * 
     * @param instanceId 工作流实例ID
     * @return 工作流状态信息
     */
    Optional<WorkflowStatusInfo> getWorkflowStatus(String instanceId);
    
    /**
     * 暂停工作流
     * 
     * @param instanceId 工作流实例ID
     * @param userId 用户ID
     * @return 是否暂停成功
     */
    boolean pauseWorkflow(String instanceId, Long userId);
    
    /**
     * 恢复工作流
     * 
     * @param instanceId 工作流实例ID
     * @param userId 用户ID
     * @return 是否恢复成功
     */
    boolean resumeWorkflow(String instanceId, Long userId);
    
    /**
     * 取消工作流
     * 
     * @param instanceId 工作流实例ID
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean cancelWorkflow(String instanceId, Long userId);
    
    /**
     * 重试失败的工作流
     * 
     * @param instanceId 工作流实例ID
     * @param userId 用户ID
     * @return 是否重试成功
     */
    boolean retryFailedWorkflow(String instanceId, Long userId);
    
    /**
     * 获取用户的工作流列表
     * 
     * @param userId 用户ID
     * @param status 状态过滤（可选）
     * @param limit 限制数量
     * @return 工作流列表
     */
    List<WorkflowStatusInfo> getUserWorkflows(Long userId, WorkflowStatus status, int limit);
    
    /**
     * 获取系统工作流统计
     * 
     * @return 工作流统计信息
     */
    WorkflowSystemStats getSystemWorkflowStats();
    
    /**
     * 清理完成的工作流
     * 
     * @param daysToKeep 保留天数
     * @return 清理的工作流数量
     */
    int cleanupCompletedWorkflows(int daysToKeep);
    
    /**
     * 获取工作流配置选项
     * 
     * @param userId 用户ID
     * @param workflowType 工作流类型
     * @return 可用的配置列表
     */
    List<WorkflowConfigurationOption> getWorkflowConfigurationOptions(Long userId, String workflowType);
    
    /**
     * 保存工作流配置模板
     * 
     * @param userId 用户ID
     * @param configName 配置名称
     * @param workflowType 工作流类型
     * @param config 工作流配置
     * @param description 配置描述
     * @return 保存的配置ID
     */
    Long saveWorkflowConfigurationTemplate(Long userId, String configName, String workflowType, 
                                          WorkflowConfig config, String description);
    
    /**
     * 验证工作流配置
     * 
     * @param config 工作流配置
     * @return 验证结果
     */
    WorkflowConfigValidationResult validateWorkflowConfig(WorkflowConfig config);
    
    // ==================== 内部类定义 ====================
    
    /**
     * 工作流状态信息
     */
    class WorkflowStatusInfo {
        private String instanceId;
        private String taskId;
        private String workflowType;
        private WorkflowStatus status;
        private String currentStep;
        private Double progress;
        private String errorMessage;
        private java.time.LocalDateTime startedAt;
        private java.time.LocalDateTime completedAt;
        private List<StepStatusInfo> steps;
        
        // Getters and setters
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getWorkflowType() { return workflowType; }
        public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }
        
        public WorkflowStatus getStatus() { return status; }
        public void setStatus(WorkflowStatus status) { this.status = status; }
        
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        
        public Double getProgress() { return progress; }
        public void setProgress(Double progress) { this.progress = progress; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public java.time.LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(java.time.LocalDateTime startedAt) { this.startedAt = startedAt; }
        
        public java.time.LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(java.time.LocalDateTime completedAt) { this.completedAt = completedAt; }
        
        public List<StepStatusInfo> getSteps() { return steps; }
        public void setSteps(List<StepStatusInfo> steps) { this.steps = steps; }
    }
    
    /**
     * 步骤状态信息
     */
    class StepStatusInfo {
        private String stepId;
        private String stepName;
        private String stepType;
        private Integer stepOrder;
        private String status;
        private Double progress;
        private String errorMessage;
        private java.time.LocalDateTime startedAt;
        private java.time.LocalDateTime completedAt;
        
        // Getters and setters
        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }
        
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        
        public String getStepType() { return stepType; }
        public void setStepType(String stepType) { this.stepType = stepType; }
        
        public Integer getStepOrder() { return stepOrder; }
        public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Double getProgress() { return progress; }
        public void setProgress(Double progress) { this.progress = progress; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public java.time.LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(java.time.LocalDateTime startedAt) { this.startedAt = startedAt; }
        
        public java.time.LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(java.time.LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
    
    /**
     * 工作流系统统计
     */
    class WorkflowSystemStats {
        private int totalWorkflows;
        private int runningWorkflows;
        private int completedWorkflows;
        private int failedWorkflows;
        private int pausedWorkflows;
        private double averageExecutionTime;
        private double successRate;
        
        // Getters and setters
        public int getTotalWorkflows() { return totalWorkflows; }
        public void setTotalWorkflows(int totalWorkflows) { this.totalWorkflows = totalWorkflows; }
        
        public int getRunningWorkflows() { return runningWorkflows; }
        public void setRunningWorkflows(int runningWorkflows) { this.runningWorkflows = runningWorkflows; }
        
        public int getCompletedWorkflows() { return completedWorkflows; }
        public void setCompletedWorkflows(int completedWorkflows) { this.completedWorkflows = completedWorkflows; }
        
        public int getFailedWorkflows() { return failedWorkflows; }
        public void setFailedWorkflows(int failedWorkflows) { this.failedWorkflows = failedWorkflows; }
        
        public int getPausedWorkflows() { return pausedWorkflows; }
        public void setPausedWorkflows(int pausedWorkflows) { this.pausedWorkflows = pausedWorkflows; }
        
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
    
    /**
     * 工作流配置选项
     */
    class WorkflowConfigurationOption {
        private Long configId;
        private String configName;
        private String configType;
        private String description;
        private boolean isDefault;
        private java.time.LocalDateTime createdAt;
        
        // Getters and setters
        public Long getConfigId() { return configId; }
        public void setConfigId(Long configId) { this.configId = configId; }
        
        public String getConfigName() { return configName; }
        public void setConfigName(String configName) { this.configName = configName; }
        
        public String getConfigType() { return configType; }
        public void setConfigType(String configType) { this.configType = configType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
        
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 工作流配置验证结果
     */
    class WorkflowConfigValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        public WorkflowConfigValidationResult() {
            this.errors = new java.util.ArrayList<>();
            this.warnings = new java.util.ArrayList<>();
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        
        public void addError(String error) { this.errors.add(error); }
        public void addWarning(String warning) { this.warnings.add(warning); }
        
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }
}