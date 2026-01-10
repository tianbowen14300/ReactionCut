package com.tbw.cut.service.impl;

import com.tbw.cut.entity.WorkflowConfiguration;
import com.tbw.cut.entity.WorkflowInstance;
import com.tbw.cut.entity.WorkflowStep;
import com.tbw.cut.mapper.WorkflowConfigurationMapper;
import com.tbw.cut.mapper.WorkflowInstanceMapper;
import com.tbw.cut.mapper.WorkflowStepMapper;
import com.tbw.cut.service.WorkflowDatabaseService;
import com.tbw.cut.workflow.model.StepStatus;
import com.tbw.cut.workflow.model.StepType;
import com.tbw.cut.workflow.model.WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 工作流数据库服务实现类
 * 
 * 提供工作流相关数据的高级操作实现
 */
@Slf4j
@Service
public class WorkflowDatabaseServiceImpl implements WorkflowDatabaseService {
    
    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;
    
    @Autowired
    private WorkflowStepMapper workflowStepMapper;
    
    @Autowired
    private WorkflowConfigurationMapper workflowConfigurationMapper;
    
    // ==================== 工作流实例操作 ====================
    
    @Override
    @Transactional
    public WorkflowInstance createWorkflowInstance(String taskId, String workflowType, Long configurationId) {
        log.info("Creating workflow instance for task: {}, type: {}, config: {}", taskId, workflowType, configurationId);
        
        WorkflowInstance instance = new WorkflowInstance();
        instance.setInstanceId(UUID.randomUUID().toString());
        instance.setTaskId(taskId);
        instance.setWorkflowType(workflowType);
        instance.setStatus(WorkflowStatus.PENDING);
        instance.setProgress(0.0);
        instance.setConfigurationId(configurationId);
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        
        int result = workflowInstanceMapper.insert(instance);
        if (result > 0) {
            log.info("Successfully created workflow instance: {}", instance.getInstanceId());
            return instance;
        } else {
            log.error("Failed to create workflow instance for task: {}", taskId);
            throw new RuntimeException("Failed to create workflow instance");
        }
    }
    
    @Override
    public Optional<WorkflowInstance> getWorkflowInstance(String instanceId) {
        log.debug("Getting workflow instance: {}", instanceId);
        WorkflowInstance instance = workflowInstanceMapper.selectByInstanceId(instanceId);
        return Optional.ofNullable(instance);
    }
    
    @Override
    public Optional<WorkflowInstance> getWorkflowInstanceByTaskId(String taskId) {
        log.debug("Getting workflow instance by task ID: {}", taskId);
        WorkflowInstance instance = workflowInstanceMapper.selectByTaskId(taskId);
        return Optional.ofNullable(instance);
    }
    
    @Override
    @Transactional
    public boolean updateWorkflowStatus(String instanceId, WorkflowStatus status, String currentStep, Double progress) {
        log.info("Updating workflow status: {} -> {}, step: {}, progress: {}%", 
                instanceId, status, currentStep, progress);
        
        int result = workflowInstanceMapper.updateStatus(instanceId, status, currentStep, progress);
        if (result > 0) {
            log.debug("Successfully updated workflow status for instance: {}", instanceId);
            return true;
        } else {
            log.warn("Failed to update workflow status for instance: {}", instanceId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean startWorkflowInstance(String instanceId) {
        log.info("Starting workflow instance: {}", instanceId);
        
        int result = workflowInstanceMapper.updateStartedAt(instanceId, LocalDateTime.now());
        if (result > 0) {
            log.info("Successfully started workflow instance: {}", instanceId);
            return true;
        } else {
            log.error("Failed to start workflow instance: {}", instanceId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean completeWorkflowInstance(String instanceId, WorkflowStatus status) {
        log.info("Completing workflow instance: {} with status: {}", instanceId, status);
        
        int result = workflowInstanceMapper.updateCompletedAt(instanceId, LocalDateTime.now(), status);
        if (result > 0) {
            log.info("Successfully completed workflow instance: {}", instanceId);
            return true;
        } else {
            log.error("Failed to complete workflow instance: {}", instanceId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean setWorkflowError(String instanceId, String errorMessage) {
        log.error("Setting error for workflow instance: {} - {}", instanceId, errorMessage);
        
        int result = workflowInstanceMapper.updateErrorMessage(instanceId, errorMessage);
        if (result > 0) {
            log.debug("Successfully set error message for workflow instance: {}", instanceId);
            return true;
        } else {
            log.error("Failed to set error message for workflow instance: {}", instanceId);
            return false;
        }
    }
    
    @Override
    public List<WorkflowInstance> getRunningWorkflowInstances() {
        log.debug("Getting running workflow instances");
        return workflowInstanceMapper.selectRunningInstances();
    }
    
    @Override
    public List<WorkflowInstance> getLongRunningWorkflowInstances(int hours) {
        log.debug("Getting long running workflow instances (>= {} hours)", hours);
        return workflowInstanceMapper.selectLongRunningInstances(hours);
    }
    
    // ==================== 工作流步骤操作 ====================
    
    @Override
    @Transactional
    public WorkflowStep createWorkflowStep(String instanceId, String stepName, StepType stepType, 
                                          Integer stepOrder, Integer maxRetries) {
        log.info("Creating workflow step: {} for instance: {}, order: {}", stepName, instanceId, stepOrder);
        
        WorkflowStep step = new WorkflowStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setInstanceId(instanceId);
        step.setStepName(stepName);
        step.setStepType(stepType);
        step.setStepOrder(stepOrder);
        step.setStatus(StepStatus.PENDING);
        step.setProgress(0.0);
        step.setRetryCount(0);
        step.setMaxRetries(maxRetries != null ? maxRetries : 3);
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        
        int result = workflowStepMapper.insert(step);
        if (result > 0) {
            log.info("Successfully created workflow step: {}", step.getStepId());
            return step;
        } else {
            log.error("Failed to create workflow step: {} for instance: {}", stepName, instanceId);
            throw new RuntimeException("Failed to create workflow step");
        }
    }
    
    @Override
    @Transactional
    public int createWorkflowSteps(String instanceId, List<StepDefinition> stepDefinitions) {
        log.info("Creating {} workflow steps for instance: {}", stepDefinitions.size(), instanceId);
        
        List<WorkflowStep> steps = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (StepDefinition definition : stepDefinitions) {
            WorkflowStep step = new WorkflowStep();
            step.setStepId(UUID.randomUUID().toString());
            step.setInstanceId(instanceId);
            step.setStepName(definition.getStepName());
            step.setStepType(definition.getStepType());
            step.setStepOrder(definition.getStepOrder());
            step.setStatus(StepStatus.PENDING);
            step.setProgress(0.0);
            step.setRetryCount(0);
            step.setMaxRetries(definition.getMaxRetries() != null ? definition.getMaxRetries() : 3);
            step.setInputData(definition.getInputData());
            step.setCreatedAt(now);
            step.setUpdatedAt(now);
            steps.add(step);
        }
        
        int result = workflowStepMapper.batchInsert(steps);
        if (result > 0) {
            log.info("Successfully created {} workflow steps for instance: {}", result, instanceId);
        } else {
            log.error("Failed to create workflow steps for instance: {}", instanceId);
        }
        
        return result;
    }
    
    @Override
    public List<WorkflowStep> getWorkflowSteps(String instanceId) {
        log.debug("Getting workflow steps for instance: {}", instanceId);
        return workflowStepMapper.selectByInstanceId(instanceId);
    }
    
    @Override
    public Optional<WorkflowStep> getCurrentRunningStep(String instanceId) {
        log.debug("Getting current running step for instance: {}", instanceId);
        WorkflowStep step = workflowStepMapper.selectCurrentRunningStep(instanceId);
        return Optional.ofNullable(step);
    }
    
    @Override
    public Optional<WorkflowStep> getNextPendingStep(String instanceId) {
        log.debug("Getting next pending step for instance: {}", instanceId);
        WorkflowStep step = workflowStepMapper.selectNextPendingStep(instanceId);
        return Optional.ofNullable(step);
    }
    
    @Override
    @Transactional
    public boolean startWorkflowStep(String stepId) {
        log.info("Starting workflow step: {}", stepId);
        
        int result = workflowStepMapper.updateStartedAt(stepId, LocalDateTime.now());
        if (result > 0) {
            log.info("Successfully started workflow step: {}", stepId);
            return true;
        } else {
            log.error("Failed to start workflow step: {}", stepId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean completeWorkflowStep(String stepId, StepStatus status, String outputData) {
        log.info("Completing workflow step: {} with status: {}", stepId, status);
        
        // Update completion time and status
        int result = workflowStepMapper.updateCompletedAt(stepId, LocalDateTime.now(), status);
        
        // Update output data if provided
        if (result > 0 && outputData != null) {
            workflowStepMapper.updateOutputData(stepId, outputData);
        }
        
        if (result > 0) {
            log.info("Successfully completed workflow step: {}", stepId);
            return true;
        } else {
            log.error("Failed to complete workflow step: {}", stepId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean setStepError(String stepId, String errorMessage) {
        log.error("Setting error for workflow step: {} - {}", stepId, errorMessage);
        
        int result = workflowStepMapper.updateErrorMessage(stepId, errorMessage);
        if (result > 0) {
            log.debug("Successfully set error message for workflow step: {}", stepId);
            return true;
        } else {
            log.error("Failed to set error message for workflow step: {}", stepId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean updateStepProgress(String stepId, Double progress) {
        log.debug("Updating step progress: {} -> {}%", stepId, progress);
        
        int result = workflowStepMapper.updateProgress(stepId, progress);
        return result > 0;
    }
    
    @Override
    @Transactional
    public boolean retryFailedStep(String stepId) {
        log.info("Retrying failed workflow step: {}", stepId);
        
        int result = workflowStepMapper.incrementRetryCount(stepId);
        if (result > 0) {
            log.info("Successfully set step for retry: {}", stepId);
            return true;
        } else {
            log.error("Failed to set step for retry: {}", stepId);
            return false;
        }
    }
    
    @Override
    public List<WorkflowStep> getRetryableFailedSteps() {
        log.debug("Getting retryable failed steps");
        return workflowStepMapper.selectRetryableFailedSteps();
    }
    
    // ==================== 工作流配置操作 ====================
    
    @Override
    public Optional<WorkflowConfiguration> getSystemDefaultConfiguration(String workflowType) {
        log.debug("Getting system default configuration for workflow type: {}", workflowType);
        WorkflowConfiguration config = workflowConfigurationMapper.selectLatestSystemDefault(workflowType);
        return Optional.ofNullable(config);
    }
    
    @Override
    public List<WorkflowConfiguration> getUserConfigurations(Long userId, String workflowType) {
        log.debug("Getting user configurations for user: {}, workflow type: {}", userId, workflowType);
        return workflowConfigurationMapper.selectUserConfigsByWorkflowType(userId, workflowType);
    }
    
    @Override
    @Transactional
    public WorkflowConfiguration saveUserConfiguration(Long userId, String configName, String workflowType, 
                                                      String configurationData, String description) {
        log.info("Saving user configuration: {} for user: {}", configName, userId);
        
        // Check if configuration name already exists for this user
        if (workflowConfigurationMapper.existsUserTemplate(userId, configName)) {
            log.error("Configuration name already exists for user: {} - {}", userId, configName);
            throw new RuntimeException("Configuration name already exists");
        }
        
        WorkflowConfiguration config = WorkflowConfiguration.createUserTemplate(
            configName, userId, workflowType, configurationData, description);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        
        int result = workflowConfigurationMapper.insert(config);
        if (result > 0) {
            log.info("Successfully saved user configuration: {}", config.getConfigId());
            return config;
        } else {
            log.error("Failed to save user configuration for user: {}", userId);
            throw new RuntimeException("Failed to save user configuration");
        }
    }
    
    @Override
    @Transactional
    public WorkflowConfiguration createInstanceConfiguration(String configName, String workflowType, 
                                                            String configurationData, Long createdBy) {
        log.info("Creating instance configuration: {} by user: {}", configName, createdBy);
        
        WorkflowConfiguration config = WorkflowConfiguration.createInstanceSpecific(
            configName, workflowType, configurationData, createdBy);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        
        int result = workflowConfigurationMapper.insert(config);
        if (result > 0) {
            log.info("Successfully created instance configuration: {}", config.getConfigId());
            return config;
        } else {
            log.error("Failed to create instance configuration");
            throw new RuntimeException("Failed to create instance configuration");
        }
    }
    
    @Override
    public Optional<WorkflowConfiguration> getConfiguration(Long configId) {
        log.debug("Getting configuration: {}", configId);
        WorkflowConfiguration config = workflowConfigurationMapper.selectByConfigId(configId);
        return Optional.ofNullable(config);
    }
    
    @Override
    @Transactional
    public boolean updateConfigurationData(Long configId, String configurationData) {
        log.info("Updating configuration data for config: {}", configId);
        
        int result = workflowConfigurationMapper.updateConfigurationData(configId, configurationData);
        if (result > 0) {
            log.info("Successfully updated configuration data: {}", configId);
            return true;
        } else {
            log.error("Failed to update configuration data: {}", configId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deleteConfiguration(Long configId) {
        log.info("Deleting configuration: {}", configId);
        
        // Check if configuration can be deleted
        Optional<WorkflowConfiguration> configOpt = getConfiguration(configId);
        if (configOpt.isPresent() && !configOpt.get().canBeDeleted()) {
            log.error("Configuration cannot be deleted: {}", configId);
            return false;
        }
        
        int result = workflowConfigurationMapper.deleteByConfigId(configId);
        if (result > 0) {
            log.info("Successfully deleted configuration: {}", configId);
            return true;
        } else {
            log.error("Failed to delete configuration: {}", configId);
            return false;
        }
    }
    
    @Override
    public List<WorkflowConfiguration> searchConfigurations(String keyword, Long userId) {
        log.debug("Searching configurations with keyword: {} for user: {}", keyword, userId);
        return workflowConfigurationMapper.searchConfigurations(keyword, userId);
    }
    
    // ==================== 统计和监控操作 ====================
    
    @Override
    public WorkflowStatistics getWorkflowStatistics() {
        log.debug("Getting workflow statistics");
        
        WorkflowStatistics stats = new WorkflowStatistics();
        stats.setTotalInstances(workflowInstanceMapper.countAll());
        stats.setRunningInstances(workflowInstanceMapper.countByStatus(WorkflowStatus.RUNNING));
        stats.setCompletedInstances(workflowInstanceMapper.countByStatus(WorkflowStatus.COMPLETED));
        stats.setFailedInstances(workflowInstanceMapper.countByStatus(WorkflowStatus.FAILED));
        stats.setTotalConfigurations(workflowConfigurationMapper.countAll());
        stats.setActiveConfigurations(workflowConfigurationMapper.selectActiveConfigurations().size());
        
        return stats;
    }
    
    @Override
    public UserWorkflowStatistics getUserWorkflowStatistics(Long userId) {
        log.debug("Getting workflow statistics for user: {}", userId);
        
        UserWorkflowStatistics stats = new UserWorkflowStatistics();
        stats.setUserId(userId);
        stats.setUserConfigurations(workflowConfigurationMapper.countUserTemplates(userId));
        
        // Note: User-specific instance statistics would require additional mapper methods
        // For now, returning basic configuration statistics
        
        return stats;
    }
    
    @Override
    @Transactional
    public int cleanupExpiredWorkflowData(int daysToKeep) {
        log.info("Cleaning up workflow data older than {} days", daysToKeep);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        // Get expired instances
        List<WorkflowInstance> expiredInstances = workflowInstanceMapper.selectByTimeRange(
            LocalDateTime.of(2000, 1, 1, 0, 0), cutoffDate);
        
        int cleanedCount = 0;
        for (WorkflowInstance instance : expiredInstances) {
            // Only cleanup completed or failed instances
            if (instance.getStatus() == WorkflowStatus.COMPLETED || 
                instance.getStatus() == WorkflowStatus.FAILED) {
                
                // Delete steps first (due to foreign key constraints)
                workflowStepMapper.deleteByInstanceId(instance.getInstanceId());
                
                // Delete instance
                workflowInstanceMapper.deleteByInstanceId(instance.getInstanceId());
                
                cleanedCount++;
            }
        }
        
        log.info("Cleaned up {} expired workflow instances", cleanedCount);
        return cleanedCount;
    }
}