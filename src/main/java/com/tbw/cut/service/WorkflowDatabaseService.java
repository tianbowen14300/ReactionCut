package com.tbw.cut.service;

import com.tbw.cut.entity.WorkflowConfiguration;
import com.tbw.cut.entity.WorkflowInstance;
import com.tbw.cut.entity.WorkflowStep;
import com.tbw.cut.workflow.model.WorkflowStatus;
import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.model.StepType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工作流数据库服务接口
 * 
 * 提供工作流相关数据的高级操作接口
 */
public interface WorkflowDatabaseService {
    
    // ==================== 工作流实例操作 ====================
    
    /**
     * 创建新的工作流实例
     * 
     * @param taskId 任务ID
     * @param workflowType 工作流类型
     * @param configurationId 配置ID
     * @return 创建的工作流实例
     */
    WorkflowInstance createWorkflowInstance(String taskId, String workflowType, Long configurationId);
    
    /**
     * 根据实例ID获取工作流实例
     * 
     * @param instanceId 实例ID
     * @return 工作流实例
     */
    Optional<WorkflowInstance> getWorkflowInstance(String instanceId);
    
    /**
     * 根据任务ID获取工作流实例
     * 
     * @param taskId 任务ID
     * @return 工作流实例
     */
    Optional<WorkflowInstance> getWorkflowInstanceByTaskId(String taskId);
    
    /**
     * 更新工作流实例状态
     * 
     * @param instanceId 实例ID
     * @param status 新状态
     * @param currentStep 当前步骤
     * @param progress 进度
     * @return 是否更新成功
     */
    boolean updateWorkflowStatus(String instanceId, WorkflowStatus status, String currentStep, Double progress);
    
    /**
     * 启动工作流实例
     * 
     * @param instanceId 实例ID
     * @return 是否启动成功
     */
    boolean startWorkflowInstance(String instanceId);
    
    /**
     * 完成工作流实例
     * 
     * @param instanceId 实例ID
     * @param status 最终状态
     * @return 是否完成成功
     */
    boolean completeWorkflowInstance(String instanceId, WorkflowStatus status);
    
    /**
     * 设置工作流实例错误信息
     * 
     * @param instanceId 实例ID
     * @param errorMessage 错误信息
     * @return 是否设置成功
     */
    boolean setWorkflowError(String instanceId, String errorMessage);
    
    /**
     * 获取正在运行的工作流实例
     * 
     * @return 正在运行的工作流实例列表
     */
    List<WorkflowInstance> getRunningWorkflowInstances();
    
    /**
     * 获取长时间运行的工作流实例
     * 
     * @param hours 运行小时数阈值
     * @return 长时间运行的工作流实例列表
     */
    List<WorkflowInstance> getLongRunningWorkflowInstances(int hours);
    
    // ==================== 工作流步骤操作 ====================
    
    /**
     * 为工作流实例创建步骤
     * 
     * @param instanceId 实例ID
     * @param stepName 步骤名称
     * @param stepType 步骤类型
     * @param stepOrder 步骤顺序
     * @param maxRetries 最大重试次数
     * @return 创建的工作流步骤
     */
    WorkflowStep createWorkflowStep(String instanceId, String stepName, StepType stepType, 
                                   Integer stepOrder, Integer maxRetries);
    
    /**
     * 批量创建工作流步骤
     * 
     * @param instanceId 实例ID
     * @param stepDefinitions 步骤定义列表
     * @return 创建的步骤数量
     */
    int createWorkflowSteps(String instanceId, List<StepDefinition> stepDefinitions);
    
    /**
     * 获取工作流实例的所有步骤
     * 
     * @param instanceId 实例ID
     * @return 工作流步骤列表
     */
    List<WorkflowStep> getWorkflowSteps(String instanceId);
    
    /**
     * 获取工作流实例的当前执行步骤
     * 
     * @param instanceId 实例ID
     * @return 当前执行的步骤
     */
    Optional<WorkflowStep> getCurrentRunningStep(String instanceId);
    
    /**
     * 获取工作流实例的下一个待执行步骤
     * 
     * @param instanceId 实例ID
     * @return 下一个待执行的步骤
     */
    Optional<WorkflowStep> getNextPendingStep(String instanceId);
    
    /**
     * 启动工作流步骤
     * 
     * @param stepId 步骤ID
     * @return 是否启动成功
     */
    boolean startWorkflowStep(String stepId);
    
    /**
     * 完成工作流步骤
     * 
     * @param stepId 步骤ID
     * @param status 最终状态
     * @param outputData 输出数据
     * @return 是否完成成功
     */
    boolean completeWorkflowStep(String stepId, StepStatus status, String outputData);
    
    /**
     * 设置工作流步骤错误信息
     * 
     * @param stepId 步骤ID
     * @param errorMessage 错误信息
     * @return 是否设置成功
     */
    boolean setStepError(String stepId, String errorMessage);
    
    /**
     * 更新步骤进度
     * 
     * @param stepId 步骤ID
     * @param progress 进度
     * @return 是否更新成功
     */
    boolean updateStepProgress(String stepId, Double progress);
    
    /**
     * 重试失败的步骤
     * 
     * @param stepId 步骤ID
     * @return 是否重试成功
     */
    boolean retryFailedStep(String stepId);
    
    /**
     * 获取可重试的失败步骤
     * 
     * @return 可重试的失败步骤列表
     */
    List<WorkflowStep> getRetryableFailedSteps();
    
    // ==================== 工作流配置操作 ====================
    
    /**
     * 获取系统默认配置
     * 
     * @param workflowType 工作流类型
     * @return 系统默认配置
     */
    Optional<WorkflowConfiguration> getSystemDefaultConfiguration(String workflowType);
    
    /**
     * 获取用户配置模板
     * 
     * @param userId 用户ID
     * @param workflowType 工作流类型
     * @return 用户配置模板列表
     */
    List<WorkflowConfiguration> getUserConfigurations(Long userId, String workflowType);
    
    /**
     * 保存用户配置模板
     * 
     * @param userId 用户ID
     * @param configName 配置名称
     * @param workflowType 工作流类型
     * @param configurationData 配置数据
     * @param description 配置描述
     * @return 保存的配置
     */
    WorkflowConfiguration saveUserConfiguration(Long userId, String configName, String workflowType, 
                                               String configurationData, String description);
    
    /**
     * 创建实例专用配置
     * 
     * @param configName 配置名称
     * @param workflowType 工作流类型
     * @param configurationData 配置数据
     * @param createdBy 创建者用户ID
     * @return 创建的配置
     */
    WorkflowConfiguration createInstanceConfiguration(String configName, String workflowType, 
                                                     String configurationData, Long createdBy);
    
    /**
     * 获取配置
     * 
     * @param configId 配置ID
     * @return 配置
     */
    Optional<WorkflowConfiguration> getConfiguration(Long configId);
    
    /**
     * 更新配置数据
     * 
     * @param configId 配置ID
     * @param configurationData 新的配置数据
     * @return 是否更新成功
     */
    boolean updateConfigurationData(Long configId, String configurationData);
    
    /**
     * 删除配置
     * 
     * @param configId 配置ID
     * @return 是否删除成功
     */
    boolean deleteConfiguration(Long configId);
    
    /**
     * 搜索配置
     * 
     * @param keyword 关键词
     * @param userId 用户ID（可选）
     * @return 匹配的配置列表
     */
    List<WorkflowConfiguration> searchConfigurations(String keyword, Long userId);
    
    // ==================== 统计和监控操作 ====================
    
    /**
     * 获取工作流实例统计信息
     * 
     * @return 统计信息
     */
    WorkflowStatistics getWorkflowStatistics();
    
    /**
     * 获取用户的工作流统计信息
     * 
     * @param userId 用户ID
     * @return 用户统计信息
     */
    UserWorkflowStatistics getUserWorkflowStatistics(Long userId);
    
    /**
     * 清理过期的工作流数据
     * 
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    int cleanupExpiredWorkflowData(int daysToKeep);
    
    // ==================== 内部类定义 ====================
    
    /**
     * 步骤定义
     */
    class StepDefinition {
        private String stepName;
        private StepType stepType;
        private Integer stepOrder;
        private Integer maxRetries;
        private String inputData;
        
        public StepDefinition(String stepName, StepType stepType, Integer stepOrder, Integer maxRetries) {
            this.stepName = stepName;
            this.stepType = stepType;
            this.stepOrder = stepOrder;
            this.maxRetries = maxRetries;
        }
        
        // Getters and setters
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        
        public StepType getStepType() { return stepType; }
        public void setStepType(StepType stepType) { this.stepType = stepType; }
        
        public Integer getStepOrder() { return stepOrder; }
        public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
        
        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
        
        public String getInputData() { return inputData; }
        public void setInputData(String inputData) { this.inputData = inputData; }
    }
    
    /**
     * 工作流统计信息
     */
    class WorkflowStatistics {
        private int totalInstances;
        private int runningInstances;
        private int completedInstances;
        private int failedInstances;
        private int totalConfigurations;
        private int activeConfigurations;
        
        // Getters and setters
        public int getTotalInstances() { return totalInstances; }
        public void setTotalInstances(int totalInstances) { this.totalInstances = totalInstances; }
        
        public int getRunningInstances() { return runningInstances; }
        public void setRunningInstances(int runningInstances) { this.runningInstances = runningInstances; }
        
        public int getCompletedInstances() { return completedInstances; }
        public void setCompletedInstances(int completedInstances) { this.completedInstances = completedInstances; }
        
        public int getFailedInstances() { return failedInstances; }
        public void setFailedInstances(int failedInstances) { this.failedInstances = failedInstances; }
        
        public int getTotalConfigurations() { return totalConfigurations; }
        public void setTotalConfigurations(int totalConfigurations) { this.totalConfigurations = totalConfigurations; }
        
        public int getActiveConfigurations() { return activeConfigurations; }
        public void setActiveConfigurations(int activeConfigurations) { this.activeConfigurations = activeConfigurations; }
    }
    
    /**
     * 用户工作流统计信息
     */
    class UserWorkflowStatistics {
        private Long userId;
        private int totalInstances;
        private int runningInstances;
        private int completedInstances;
        private int failedInstances;
        private int userConfigurations;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public int getTotalInstances() { return totalInstances; }
        public void setTotalInstances(int totalInstances) { this.totalInstances = totalInstances; }
        
        public int getRunningInstances() { return runningInstances; }
        public void setRunningInstances(int runningInstances) { this.runningInstances = runningInstances; }
        
        public int getCompletedInstances() { return completedInstances; }
        public void setCompletedInstances(int completedInstances) { this.completedInstances = completedInstances; }
        
        public int getFailedInstances() { return failedInstances; }
        public void setFailedInstances(int failedInstances) { this.failedInstances = failedInstances; }
        
        public int getUserConfigurations() { return userConfigurations; }
        public void setUserConfigurations(int userConfigurations) { this.userConfigurations = userConfigurations; }
    }
}