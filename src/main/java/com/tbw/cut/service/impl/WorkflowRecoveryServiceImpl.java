package com.tbw.cut.service.impl;

import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.service.TaskRelationService;
import com.tbw.cut.service.WorkflowConfigurationService;
import com.tbw.cut.service.WorkflowRecoveryService;
import com.tbw.cut.service.IntegrationService;
import com.tbw.cut.workflow.service.WorkflowEngine;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流恢复服务实现
 */
@Slf4j
@Service
public class WorkflowRecoveryServiceImpl implements WorkflowRecoveryService {
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    @Autowired
    private WorkflowConfigurationService workflowConfigurationService;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private IntegrationService integrationService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryFailedWorkflowStartup(Long downloadTaskId, String submissionTaskId) {
        log.info("Attempting to retry workflow startup: downloadTaskId={}, submissionTaskId={}", 
                downloadTaskId, submissionTaskId);
        
        try {
            // 检查下载状态
            if (!isDownloadCompleted(downloadTaskId)) {
                log.warn("Download not completed, cannot retry workflow startup: downloadTaskId={}", downloadTaskId);
                return false;
            }
            
            // 获取工作流配置
            WorkflowConfig config = workflowConfigurationService.getConfigForTask(submissionTaskId);
            if (config == null) {
                log.error("Workflow configuration not found, cannot retry: submissionTaskId={}", submissionTaskId);
                taskRelationService.recordWorkflowError(downloadTaskId, submissionTaskId, 
                        "Workflow configuration not found");
                return false;
            }
            
            // 验证文件存在性
            if (!validateSourceFilesExist(downloadTaskId)) {
                log.error("Source files validation failed, cannot retry workflow startup: downloadTaskId={}", 
                        downloadTaskId);
                taskRelationService.recordWorkflowError(downloadTaskId, submissionTaskId, 
                        "Source files validation failed");
                return false;
            }
            
            // 重新启动工作流
            WorkflowInstance instance = workflowEngine.startWorkflow(submissionTaskId, config);
            
            // 更新状态
            boolean updated = taskRelationService.updateWorkflowInfo(downloadTaskId, submissionTaskId, 
                    instance.getInstanceId(), TaskRelation.WorkflowStatus.WORKFLOW_STARTED.name());
            
            if (updated) {
                log.info("Workflow retry startup successful: instanceId={}, downloadTaskId={}, submissionTaskId={}", 
                        instance.getInstanceId(), downloadTaskId, submissionTaskId);
                return true;
            } else {
                log.error("Failed to update workflow status after successful startup");
                return false;
            }
            
        } catch (Exception e) {
            log.error("Workflow retry startup failed: downloadTaskId={}, submissionTaskId={}", 
                    downloadTaskId, submissionTaskId, e);
            
            // 记录错误信息
            taskRelationService.recordWorkflowError(downloadTaskId, submissionTaskId, 
                    "Retry failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRetryFailedWorkflows(int maxRetryCount) {
        log.info("Starting batch retry of failed workflows with maxRetryCount: {}", maxRetryCount);
        
        try {
            List<TaskRelation> retryableTasks = taskRelationService.findRetryableWorkflowTasks(maxRetryCount);
            int successCount = 0;
            
            for (TaskRelation task : retryableTasks) {
                try {
                    boolean success = retryFailedWorkflowStartup(task.getDownloadTaskId(), 
                            task.getSubmissionTaskId());
                    if (success) {
                        successCount++;
                    }
                    
                    // 添加延迟避免系统过载
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    log.error("Failed to retry workflow for task: downloadTaskId={}, submissionTaskId={}", 
                            task.getDownloadTaskId(), task.getSubmissionTaskId(), e);
                }
            }
            
            log.info("Batch retry completed: {} out of {} tasks retried successfully", 
                    successCount, retryableTasks.size());
            return successCount;
            
        } catch (Exception e) {
            log.error("Batch retry failed", e);
            return 0;
        }
    }
    
    @Override
    public boolean isDownloadCompleted(Long downloadTaskId) {
        try {
            // 这里需要根据实际的下载状态检查逻辑来实现
            // 暂时返回true，实际实现需要查询下载任务状态
            // TODO: 实现实际的下载完成状态检查
            log.debug("Checking download completion status for taskId: {}", downloadTaskId);
            return true;
        } catch (Exception e) {
            log.error("Failed to check download completion status: downloadTaskId={}", downloadTaskId, e);
            return false;
        }
    }
    
    @Override
    public int cleanupExpiredConfigurations() {
        log.info("Starting cleanup of expired workflow configurations");
        
        try {
            // 查找所有已完成或失败的任务
            List<TaskRelation> completedTasks = taskRelationService.findByWorkflowStatus(
                    TaskRelation.WorkflowStatus.WORKFLOW_COMPLETED);
            List<TaskRelation> failedTasks = taskRelationService.findByWorkflowStatus(
                    TaskRelation.WorkflowStatus.WORKFLOW_FAILED);
            
            int cleanupCount = 0;
            
            // 清理已完成任务的配置
            for (TaskRelation task : completedTasks) {
                try {
                    workflowConfigurationService.removeConfigForTask(task.getSubmissionTaskId());
                    cleanupCount++;
                } catch (Exception e) {
                    log.warn("Failed to cleanup config for completed task: {}", task.getSubmissionTaskId(), e);
                }
            }
            
            // 清理失败任务的配置（保留一段时间后清理）
            for (TaskRelation task : failedTasks) {
                try {
                    // 检查是否超过保留期限（例如7天）
                    if (task.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(7))) {
                        workflowConfigurationService.removeConfigForTask(task.getSubmissionTaskId());
                        cleanupCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to cleanup config for failed task: {}", task.getSubmissionTaskId(), e);
                }
            }
            
            log.info("Cleanup completed: {} configurations removed", cleanupCount);
            return cleanupCount;
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired configurations", e);
            return 0;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recoverIncompleteTasksAfterRestart() {
        log.info("Starting recovery of incomplete tasks after system restart");
        
        try {
            // 查找等待下载完成的任务
            List<TaskRelation> pendingTasks = taskRelationService.findPendingWorkflowTasks();
            int recoveredCount = 0;
            
            for (TaskRelation task : pendingTasks) {
                try {
                    // 检查下载是否已完成
                    if (isDownloadCompleted(task.getDownloadTaskId())) {
                        // 尝试启动工作流
                        boolean success = retryFailedWorkflowStartup(task.getDownloadTaskId(), 
                                task.getSubmissionTaskId());
                        if (success) {
                            recoveredCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to recover task: downloadTaskId={}, submissionTaskId={}", 
                            task.getDownloadTaskId(), task.getSubmissionTaskId(), e);
                }
            }
            
            log.info("Recovery completed: {} tasks recovered", recoveredCount);
            return recoveredCount;
            
        } catch (Exception e) {
            log.error("Failed to recover incomplete tasks", e);
            return 0;
        }
    }
    
    @Override
    public WorkflowRecoveryStats getRecoveryStats() {
        try {
            long pendingTasks = taskRelationService.countByWorkflowStatus(
                    TaskRelation.WorkflowStatus.PENDING_DOWNLOAD);
            long failedTasks = taskRelationService.countByWorkflowStatus(
                    TaskRelation.WorkflowStatus.WORKFLOW_STARTUP_FAILED) +
                    taskRelationService.countByWorkflowStatus(
                    TaskRelation.WorkflowStatus.WORKFLOW_FAILED);
            long retryableTasks = taskRelationService.findRetryableWorkflowTasks(3).size();
            
            // 过期配置数量需要实际实现
            long expiredConfigurations = 0; // TODO: 实现过期配置统计
            
            return new WorkflowRecoveryStats(pendingTasks, failedTasks, retryableTasks, expiredConfigurations);
            
        } catch (Exception e) {
            log.error("Failed to get recovery stats", e);
            return new WorkflowRecoveryStats(0, 0, 0, 0);
        }
    }
    
    /**
     * 验证源文件是否存在
     */
    private boolean validateSourceFilesExist(Long downloadTaskId) {
        try {
            // TODO: 实现实际的文件存在性验证逻辑
            // 这里需要根据downloadTaskId查询相关的文件路径并验证文件存在性
            log.debug("Validating source files for downloadTaskId: {}", downloadTaskId);
            return true;
        } catch (Exception e) {
            log.error("Failed to validate source files: downloadTaskId={}", downloadTaskId, e);
            return false;
        }
    }
}