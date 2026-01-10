package com.tbw.cut.service;

import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.service.TaskRelationService;
import com.tbw.cut.service.WorkflowRecoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 批量状态更新服务
 * 定期检查和更新工作流状态
 */
@Slf4j
@Service
public class BatchStatusUpdateService {
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    @Autowired
    private WorkflowRecoveryService workflowRecoveryService;
    
    /**
     * 定期检查待处理的工作流任务
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    public void checkPendingWorkflowTasks() {
        try {
            log.debug("Starting scheduled check of pending workflow tasks");
            
            List<TaskRelation> pendingTasks = taskRelationService.findPendingWorkflowTasks();
            if (pendingTasks.isEmpty()) {
                log.debug("No pending workflow tasks found");
                return;
            }
            
            log.info("Found {} pending workflow tasks", pendingTasks.size());
            int triggeredCount = 0;
            
            for (TaskRelation task : pendingTasks) {
                try {
                    // 检查下载状态并触发工作流
                    if (workflowRecoveryService.isDownloadCompleted(task.getDownloadTaskId())) {
                        boolean success = workflowRecoveryService.retryFailedWorkflowStartup(
                                task.getDownloadTaskId(), task.getSubmissionTaskId());
                        if (success) {
                            triggeredCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process pending task: downloadTaskId={}, submissionTaskId={}", 
                            task.getDownloadTaskId(), task.getSubmissionTaskId(), e);
                }
            }
            
            if (triggeredCount > 0) {
                log.info("Successfully triggered {} workflows from pending tasks", triggeredCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to check pending workflow tasks", e);
        }
    }
    
    /**
     * 定期重试失败的工作流启动
     * 每5分钟执行一次
     */
    @Scheduled(fixedDelay = 300000)
    public void retryFailedWorkflows() {
        try {
            log.debug("Starting scheduled retry of failed workflows");
            
            int retryCount = workflowRecoveryService.batchRetryFailedWorkflows(3);
            if (retryCount > 0) {
                log.info("Successfully retried {} failed workflows", retryCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to retry failed workflows", e);
        }
    }
    
    /**
     * 定期清理过期配置
     * 每小时执行一次
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupExpiredConfigurations() {
        try {
            log.debug("Starting scheduled cleanup of expired configurations");
            
            int cleanupCount = workflowRecoveryService.cleanupExpiredConfigurations();
            if (cleanupCount > 0) {
                log.info("Cleaned up {} expired workflow configurations", cleanupCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired configurations", e);
        }
    }
    
    /**
     * 系统启动后恢复未完成任务
     * 在应用启动后延迟1分钟执行一次
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void recoverIncompleteTasksOnStartup() {
        try {
            log.info("Starting recovery of incomplete tasks after system startup");
            
            int recoveredCount = workflowRecoveryService.recoverIncompleteTasksAfterRestart();
            if (recoveredCount > 0) {
                log.info("Recovered {} incomplete tasks after system startup", recoveredCount);
            } else {
                log.info("No incomplete tasks found to recover");
            }
            
        } catch (Exception e) {
            log.error("Failed to recover incomplete tasks on startup", e);
        }
    }
    
    /**
     * 定期输出工作流恢复统计信息
     * 每10分钟执行一次
     */
    @Scheduled(fixedDelay = 600000)
    public void logRecoveryStats() {
        try {
            WorkflowRecoveryService.WorkflowRecoveryStats stats = workflowRecoveryService.getRecoveryStats();
            
            if (stats.getPendingTasks() > 0 || stats.getFailedTasks() > 0 || stats.getRetryableTasks() > 0) {
                log.info("Workflow Recovery Stats - Pending: {}, Failed: {}, Retryable: {}, Expired Configs: {}", 
                        stats.getPendingTasks(), stats.getFailedTasks(), 
                        stats.getRetryableTasks(), stats.getExpiredConfigurations());
            }
            
        } catch (Exception e) {
            log.error("Failed to log recovery stats", e);
        }
    }
}