package com.tbw.cut.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbw.cut.dto.SubmissionRequestDTO;
import com.tbw.cut.entity.WorkflowConfiguration;
import com.tbw.cut.entity.WorkflowInstance;
import com.tbw.cut.entity.WorkflowStep;
import com.tbw.cut.service.WorkflowDatabaseService;
import com.tbw.cut.service.WorkflowIntegrationService;
import com.tbw.cut.workflow.model.*;
import com.tbw.cut.workflow.service.WorkflowEngine;
import com.tbw.cut.workflow.service.ConfigurationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工作流集成服务实现类
 * 
 * 整合所有工作流组件，提供完整的工作流功能
 */
@Slf4j
@Service
public class WorkflowIntegrationServiceImpl implements WorkflowIntegrationService {
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private ConfigurationManager configurationManager;
    
    @Autowired
    private WorkflowDatabaseService workflowDatabaseService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String DOWNLOAD_SUBMISSION_WORKFLOW_TYPE = "DOWNLOAD_SUBMISSION";
    private static final String SUBMISSION_TASK_WORKFLOW_TYPE = "SUBMISSION_TASK";
    
    @Override
    @Transactional
    public String startDownloadSubmissionWorkflow(String bvid, Long userId, Long configurationId) {
        log.info("Starting download+submission workflow for BVID: {}, user: {}, config: {}", 
                bvid, userId, configurationId);
        
        try {
            // Get or create workflow configuration
            WorkflowConfig config = getOrCreateWorkflowConfig(userId, configurationId, DOWNLOAD_SUBMISSION_WORKFLOW_TYPE);
            
            // Create workflow instance in database
            WorkflowInstance instance = workflowDatabaseService.createWorkflowInstance(
                generateTaskId(bvid), DOWNLOAD_SUBMISSION_WORKFLOW_TYPE, configurationId);
            
            // Create workflow steps
            createDownloadSubmissionSteps(instance.getInstanceId(), config);
            
            // Start workflow execution
            com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                workflowEngine.startWorkflow(generateTaskId(bvid), config);
            
            // Update database with workflow engine instance ID
            workflowDatabaseService.updateWorkflowStatus(
                instance.getInstanceId(), WorkflowStatus.RUNNING, "DOWNLOADING", 0.0);
            
            log.info("Successfully started download+submission workflow: {}", instance.getInstanceId());
            return instance.getInstanceId();
            
        } catch (Exception e) {
            log.error("Failed to start download+submission workflow for BVID: {}", bvid, e);
            throw new RuntimeException("Failed to start workflow: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public String startSubmissionTaskWorkflow(SubmissionRequestDTO submissionRequest, Long userId, Long configurationId) {
        log.info("Starting submission task workflow for user: {}, config: {}", userId, configurationId);
        
        try {
            // Get or create workflow configuration
            WorkflowConfig config = getOrCreateWorkflowConfig(userId, configurationId, SUBMISSION_TASK_WORKFLOW_TYPE);
            
            // Create workflow instance in database
            WorkflowInstance instance = workflowDatabaseService.createWorkflowInstance(
                generateTaskId(submissionRequest.getTitle()), SUBMISSION_TASK_WORKFLOW_TYPE, configurationId);
            
            // Create workflow steps
            createSubmissionTaskSteps(instance.getInstanceId(), config, submissionRequest);
            
            // Start workflow execution
            com.tbw.cut.workflow.model.WorkflowInstance workflowInstance = 
                workflowEngine.startWorkflow(generateTaskId(submissionRequest.getTitle()), config);
            
            // Update database with workflow engine instance ID
            workflowDatabaseService.updateWorkflowStatus(
                instance.getInstanceId(), WorkflowStatus.RUNNING, "PROCESSING", 0.0);
            
            log.info("Successfully started submission task workflow: {}", instance.getInstanceId());
            return instance.getInstanceId();
            
        } catch (Exception e) {
            log.error("Failed to start submission task workflow", e);
            throw new RuntimeException("Failed to start workflow: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<WorkflowStatusInfo> getWorkflowStatus(String instanceId) {
        log.debug("Getting workflow status for instance: {}", instanceId);
        
        Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstance(instanceId);
        if (!instanceOpt.isPresent()) {
            log.warn("Workflow instance not found: {}", instanceId);
            return Optional.empty();
        }
        
        WorkflowInstance instance = instanceOpt.get();
        List<WorkflowStep> steps = workflowDatabaseService.getWorkflowSteps(instanceId);
        
        WorkflowStatusInfo statusInfo = new WorkflowStatusInfo();
        statusInfo.setInstanceId(instance.getInstanceId());
        statusInfo.setTaskId(instance.getTaskId());
        statusInfo.setWorkflowType(instance.getWorkflowType());
        statusInfo.setStatus(instance.getStatus());
        statusInfo.setCurrentStep(instance.getCurrentStep());
        statusInfo.setProgress(instance.getProgress());
        statusInfo.setErrorMessage(instance.getErrorMessage());
        statusInfo.setStartedAt(instance.getStartedAt());
        statusInfo.setCompletedAt(instance.getCompletedAt());
        
        // Convert steps to status info
        List<StepStatusInfo> stepStatusList = steps.stream()
            .map(this::convertToStepStatusInfo)
            .collect(Collectors.toList());
        statusInfo.setSteps(stepStatusList);
        
        return Optional.of(statusInfo);
    }
    
    @Override
    @Transactional
    public boolean pauseWorkflow(String instanceId, Long userId) {
        log.info("Pausing workflow: {} by user: {}", instanceId, userId);
        
        try {
            // Check if workflow exists and is running
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstance(instanceId);
            if (!instanceOpt.isPresent()) {
                log.warn("Workflow instance not found: {}", instanceId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            if (instance.getStatus() != WorkflowStatus.RUNNING) {
                log.warn("Workflow is not running, cannot pause: {} (status: {})", instanceId, instance.getStatus());
                return false;
            }
            
            // Pause workflow in engine
            boolean engineResult = workflowEngine.pauseWorkflow(instanceId);
            if (!engineResult) {
                log.error("Failed to pause workflow in engine: {}", instanceId);
                return false;
            }
            
            // Update database status
            boolean dbResult = workflowDatabaseService.updateWorkflowStatus(
                instanceId, WorkflowStatus.PAUSED, instance.getCurrentStep(), instance.getProgress());
            
            if (dbResult) {
                log.info("Successfully paused workflow: {}", instanceId);
                return true;
            } else {
                log.error("Failed to update workflow status in database: {}", instanceId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error pausing workflow: {}", instanceId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean resumeWorkflow(String instanceId, Long userId) {
        log.info("Resuming workflow: {} by user: {}", instanceId, userId);
        
        try {
            // Check if workflow exists and is paused
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstance(instanceId);
            if (!instanceOpt.isPresent()) {
                log.warn("Workflow instance not found: {}", instanceId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            if (instance.getStatus() != WorkflowStatus.PAUSED) {
                log.warn("Workflow is not paused, cannot resume: {} (status: {})", instanceId, instance.getStatus());
                return false;
            }
            
            // Resume workflow in engine
            boolean engineResult = workflowEngine.resumeWorkflow(instanceId);
            if (!engineResult) {
                log.error("Failed to resume workflow in engine: {}", instanceId);
                return false;
            }
            
            // Update database status
            boolean dbResult = workflowDatabaseService.updateWorkflowStatus(
                instanceId, WorkflowStatus.RUNNING, instance.getCurrentStep(), instance.getProgress());
            
            if (dbResult) {
                log.info("Successfully resumed workflow: {}", instanceId);
                return true;
            } else {
                log.error("Failed to update workflow status in database: {}", instanceId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error resuming workflow: {}", instanceId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean cancelWorkflow(String instanceId, Long userId) {
        log.info("Cancelling workflow: {} by user: {}", instanceId, userId);
        
        try {
            // Check if workflow exists
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstance(instanceId);
            if (!instanceOpt.isPresent()) {
                log.warn("Workflow instance not found: {}", instanceId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            if (instance.getStatus() == WorkflowStatus.COMPLETED || 
                instance.getStatus() == WorkflowStatus.CANCELLED) {
                log.warn("Workflow is already completed/cancelled: {} (status: {})", instanceId, instance.getStatus());
                return false;
            }
            
            // Cancel workflow in engine
            boolean engineResult = workflowEngine.cancelWorkflow(instanceId);
            if (!engineResult) {
                log.error("Failed to cancel workflow in engine: {}", instanceId);
                return false;
            }
            
            // Update database status
            boolean dbResult = workflowDatabaseService.completeWorkflowInstance(instanceId, WorkflowStatus.CANCELLED);
            
            if (dbResult) {
                log.info("Successfully cancelled workflow: {}", instanceId);
                return true;
            } else {
                log.error("Failed to update workflow status in database: {}", instanceId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error cancelling workflow: {}", instanceId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean retryFailedWorkflow(String instanceId, Long userId) {
        log.info("Retrying failed workflow: {} by user: {}", instanceId, userId);
        
        try {
            // Check if workflow exists and is failed
            Optional<WorkflowInstance> instanceOpt = workflowDatabaseService.getWorkflowInstance(instanceId);
            if (!instanceOpt.isPresent()) {
                log.warn("Workflow instance not found: {}", instanceId);
                return false;
            }
            
            WorkflowInstance instance = instanceOpt.get();
            if (instance.getStatus() != WorkflowStatus.FAILED) {
                log.warn("Workflow is not failed, cannot retry: {} (status: {})", instanceId, instance.getStatus());
                return false;
            }
            
            // Get failed steps and retry them
            List<WorkflowStep> failedSteps = workflowDatabaseService.getWorkflowSteps(instanceId)
                .stream()
                .filter(step -> step.getStatus() == StepStatus.FAILED && step.canRetry())
                .collect(Collectors.toList());
            
            if (failedSteps.isEmpty()) {
                log.warn("No retryable failed steps found for workflow: {}", instanceId);
                return false;
            }
            
            // Retry failed steps
            for (WorkflowStep step : failedSteps) {
                workflowDatabaseService.retryFailedStep(step.getStepId());
            }
            
            // Update workflow status to running
            boolean dbResult = workflowDatabaseService.updateWorkflowStatus(
                instanceId, WorkflowStatus.RUNNING, "RETRYING", instance.getProgress());
            
            if (dbResult) {
                log.info("Successfully set workflow for retry: {}", instanceId);
                return true;
            } else {
                log.error("Failed to update workflow status for retry: {}", instanceId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error retrying workflow: {}", instanceId, e);
            return false;
        }
    }
    
    @Override
    public List<WorkflowStatusInfo> getUserWorkflows(Long userId, WorkflowStatus status, int limit) {
        log.debug("Getting user workflows for user: {}, status: {}, limit: {}", userId, status, limit);
        
        // Note: This would require additional mapper methods to filter by user
        // For now, returning empty list as placeholder
        return new ArrayList<>();
    }
    
    @Override
    public WorkflowSystemStats getSystemWorkflowStats() {
        log.debug("Getting system workflow statistics");
        
        WorkflowDatabaseService.WorkflowStatistics dbStats = workflowDatabaseService.getWorkflowStatistics();
        
        WorkflowSystemStats stats = new WorkflowSystemStats();
        stats.setTotalWorkflows(dbStats.getTotalInstances());
        stats.setRunningWorkflows(dbStats.getRunningInstances());
        stats.setCompletedWorkflows(dbStats.getCompletedInstances());
        stats.setFailedWorkflows(dbStats.getFailedInstances());
        stats.setPausedWorkflows(0); // Would need additional query
        
        // Calculate success rate
        int totalCompleted = dbStats.getCompletedInstances() + dbStats.getFailedInstances();
        if (totalCompleted > 0) {
            stats.setSuccessRate((double) dbStats.getCompletedInstances() / totalCompleted * 100);
        } else {
            stats.setSuccessRate(0.0);
        }
        
        // Average execution time would require additional metrics collection
        stats.setAverageExecutionTime(0.0);
        
        return stats;
    }
    
    @Override
    @Transactional
    public int cleanupCompletedWorkflows(int daysToKeep) {
        log.info("Cleaning up completed workflows older than {} days", daysToKeep);
        return workflowDatabaseService.cleanupExpiredWorkflowData(daysToKeep);
    }
    
    @Override
    public List<WorkflowConfigurationOption> getWorkflowConfigurationOptions(Long userId, String workflowType) {
        log.debug("Getting workflow configuration options for user: {}, type: {}", userId, workflowType);
        
        List<WorkflowConfigurationOption> options = new ArrayList<>();
        
        // Get system default configurations
        Optional<WorkflowConfiguration> systemDefault = 
            workflowDatabaseService.getSystemDefaultConfiguration(workflowType);
        if (systemDefault.isPresent()) {
            WorkflowConfigurationOption option = new WorkflowConfigurationOption();
            option.setConfigId(systemDefault.get().getConfigId());
            option.setConfigName(systemDefault.get().getConfigName());
            option.setConfigType("系统默认");
            option.setDescription(systemDefault.get().getDescription());
            option.setDefault(true);
            option.setCreatedAt(systemDefault.get().getCreatedAt());
            options.add(option);
        }
        
        // Get user configurations
        List<WorkflowConfiguration> userConfigs = 
            workflowDatabaseService.getUserConfigurations(userId, workflowType);
        for (WorkflowConfiguration config : userConfigs) {
            WorkflowConfigurationOption option = new WorkflowConfigurationOption();
            option.setConfigId(config.getConfigId());
            option.setConfigName(config.getConfigName());
            option.setConfigType("用户模板");
            option.setDescription(config.getDescription());
            option.setDefault(false);
            option.setCreatedAt(config.getCreatedAt());
            options.add(option);
        }
        
        return options;
    }
    
    @Override
    @Transactional
    public Long saveWorkflowConfigurationTemplate(Long userId, String configName, String workflowType, 
                                                 WorkflowConfig config, String description) {
        log.info("Saving workflow configuration template: {} for user: {}", configName, userId);
        
        try {
            String configurationData = objectMapper.writeValueAsString(config);
            WorkflowConfiguration savedConfig = workflowDatabaseService.saveUserConfiguration(
                userId, configName, workflowType, configurationData, description);
            
            log.info("Successfully saved workflow configuration template: {}", savedConfig.getConfigId());
            return savedConfig.getConfigId();
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize workflow configuration", e);
            throw new RuntimeException("Failed to save configuration: " + e.getMessage(), e);
        }
    }
    
    @Override
    public WorkflowConfigValidationResult validateWorkflowConfig(WorkflowConfig config) {
        log.debug("Validating workflow configuration");
        
        WorkflowConfigValidationResult result = new WorkflowConfigValidationResult();
        
        try {
            // Use configuration manager to validate
            boolean isValid = configurationManager.validateConfig(config);
            result.setValid(isValid);
            
            if (!isValid) {
                result.addError("配置验证失败");
            }
            
            // Additional validation logic can be added here
            
        } catch (Exception e) {
            log.error("Error validating workflow configuration", e);
            result.setValid(false);
            result.addError("配置验证过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private WorkflowConfig getOrCreateWorkflowConfig(Long userId, Long configurationId, String workflowType) {
        if (configurationId != null) {
            // Use specified configuration
            Optional<WorkflowConfiguration> configOpt = workflowDatabaseService.getConfiguration(configurationId);
            if (configOpt.isPresent()) {
                return parseWorkflowConfig(configOpt.get().getConfigurationData());
            }
        }
        
        // Use system default configuration
        Optional<WorkflowConfiguration> defaultConfig = 
            workflowDatabaseService.getSystemDefaultConfiguration(workflowType);
        if (defaultConfig.isPresent()) {
            return parseWorkflowConfig(defaultConfig.get().getConfigurationData());
        }
        
        // Create default configuration
        return createDefaultWorkflowConfig(workflowType);
    }
    
    private WorkflowConfig parseWorkflowConfig(String configurationData) {
        try {
            return objectMapper.readValue(configurationData, WorkflowConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse workflow configuration", e);
            throw new RuntimeException("Invalid configuration data", e);
        }
    }
    
    private WorkflowConfig createDefaultWorkflowConfig(String workflowType) {
        // Create a basic default configuration
        WorkflowConfig config = new WorkflowConfig();
        config.setWorkflowType(workflowType);
        
        if (DOWNLOAD_SUBMISSION_WORKFLOW_TYPE.equals(workflowType)) {
            config.setEnableDirectSubmission(true);
            config.setEnableSegmentation(false);
        } else if (SUBMISSION_TASK_WORKFLOW_TYPE.equals(workflowType)) {
            config.setEnableDirectSubmission(false);
            config.setEnableSegmentation(true);
        }
        
        return config;
    }
    
    private void createDownloadSubmissionSteps(String instanceId, WorkflowConfig config) {
        List<WorkflowDatabaseService.StepDefinition> stepDefinitions = new ArrayList<>();
        
        stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
            "下载视频", StepType.DOWNLOADING, 1, 3));
        stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
            "视频剪辑", StepType.CLIPPING, 2, 3));
        stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
            "视频合并", StepType.MERGING, 3, 3));
        
        if (config.isEnableSegmentation()) {
            stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
                "视频分段", StepType.SEGMENTING, 4, 3));
            stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
                "视频上传", StepType.UPLOADING, 5, 3));
        } else {
            stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
                "视频上传", StepType.UPLOADING, 4, 3));
        }
        
        workflowDatabaseService.createWorkflowSteps(instanceId, stepDefinitions);
    }
    
    private void createSubmissionTaskSteps(String instanceId, WorkflowConfig config, SubmissionRequestDTO request) {
        List<WorkflowDatabaseService.StepDefinition> stepDefinitions = new ArrayList<>();
        
        stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
            "数据验证", StepType.VALIDATION, 1, 3));
        stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
            "视频剪辑", StepType.CLIPPING, 2, 3));
        stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
            "视频合并", StepType.MERGING, 3, 3));
        
        if (config.isEnableSegmentation()) {
            stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
                "视频分段", StepType.SEGMENTING, 4, 3));
            stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
                "视频上传", StepType.UPLOADING, 5, 3));
        } else {
            stepDefinitions.add(new WorkflowDatabaseService.StepDefinition(
                "视频上传", StepType.UPLOADING, 4, 3));
        }
        
        workflowDatabaseService.createWorkflowSteps(instanceId, stepDefinitions);
    }
    
    private StepStatusInfo convertToStepStatusInfo(WorkflowStep step) {
        StepStatusInfo statusInfo = new StepStatusInfo();
        statusInfo.setStepId(step.getStepId());
        statusInfo.setStepName(step.getStepName());
        statusInfo.setStepType(step.getStepType().toString());
        statusInfo.setStepOrder(step.getStepOrder());
        statusInfo.setStatus(step.getStatus().toString());
        statusInfo.setProgress(step.getProgress());
        statusInfo.setErrorMessage(step.getErrorMessage());
        statusInfo.setStartedAt(step.getStartedAt());
        statusInfo.setCompletedAt(step.getCompletedAt());
        return statusInfo;
    }
    
    private String generateTaskId(String identifier) {
        return "TASK_" + identifier.hashCode() + "_" + System.currentTimeMillis();
    }
}