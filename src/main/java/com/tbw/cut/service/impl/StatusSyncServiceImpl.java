package com.tbw.cut.service.impl;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskRelation;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.service.StatusSyncService;
import com.tbw.cut.service.SubmissionTaskService;
import com.tbw.cut.service.TaskRelationService;
import com.tbw.cut.service.VideoDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 状态同步服务实现
 * 负责在下载任务和投稿任务之间同步状态变化
 */
@Slf4j
@Service
public class StatusSyncServiceImpl implements StatusSyncService {
    
    @Autowired
    private TaskRelationService taskRelationService;
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    @Autowired
    private SubmissionTaskService submissionTaskService;
    
    /**
     * 状态同步开关，可通过配置文件控制
     */
    @Value("${integration.status-sync.enabled:true}")
    private boolean statusSyncEnabled;
    
    /**
     * 状态同步失败重试次数
     */
    @Value("${integration.status-sync.retry-count:3}")
    private int retryCount;
    
    @Override
    public void handleDownloadStatusChange(Long downloadTaskId, Integer oldStatus, Integer newStatus) {
        if (!statusSyncEnabled) {
            log.debug("Status sync is disabled, skipping download status change for task: {}", downloadTaskId);
            return;
        }
        
        log.info("Handling download status change: taskId={}, oldStatus={}, newStatus={}", 
                downloadTaskId, oldStatus, newStatus);
        
        try {
            syncDownloadStatusToSubmission(downloadTaskId, newStatus);
        } catch (Exception e) {
            log.error("Failed to handle download status change for task: {}", downloadTaskId, e);
        }
    }
    
    @Override
    public void handleSubmissionStatusChange(String submissionTaskId, SubmissionTask.TaskStatus oldStatus, SubmissionTask.TaskStatus newStatus) {
        if (!statusSyncEnabled) {
            log.debug("Status sync is disabled, skipping submission status change for task: {}", submissionTaskId);
            return;
        }
        
        log.info("Handling submission status change: taskId={}, oldStatus={}, newStatus={}", 
                submissionTaskId, oldStatus, newStatus);
        
        try {
            syncSubmissionStatusToDownload(submissionTaskId, newStatus);
        } catch (Exception e) {
            log.error("Failed to handle submission status change for task: {}", submissionTaskId, e);
        }
    }
    
    @Override
    @Async
    public void syncDownloadStatusToSubmission(Long downloadTaskId, Integer downloadStatus) {
        log.debug("Syncing download status to submission: downloadTaskId={}, status={}", downloadTaskId, downloadStatus);
        
        try {
            Optional<TaskRelation> relationOpt = taskRelationService.findByDownloadTaskId(downloadTaskId);
            if (!relationOpt.isPresent()) {
                log.debug("No active relation found for download task: {}", downloadTaskId);
                return;
            }
            
            TaskRelation relation = relationOpt.get();
            SubmissionTask.TaskStatus newSubmissionStatus = mapDownloadStatusToSubmissionStatus(downloadStatus);
            
            if (newSubmissionStatus != null) {
                // 更新投稿任务状态
                submissionTaskService.updateTaskStatus(relation.getSubmissionTaskId(), newSubmissionStatus);
                log.info("Updated submission task {} status to {} based on download task {} status {}", 
                        relation.getSubmissionTaskId(), newSubmissionStatus, downloadTaskId, downloadStatus);
                
                // 更新关联状态
                updateRelationStatus(relation, downloadStatus, newSubmissionStatus);
            }
            
        } catch (Exception e) {
            log.error("Failed to sync download status to submission for task: {}", downloadTaskId, e);
            // 可以在这里实现重试逻辑或发送告警
        }
    }
    
    @Override
    @Async
    public void syncSubmissionStatusToDownload(String submissionTaskId, SubmissionTask.TaskStatus submissionStatus) {
        log.debug("Syncing submission status to download: submissionTaskId={}, status={}", submissionTaskId, submissionStatus);
        
        try {
            Optional<TaskRelation> relationOpt = taskRelationService.findBySubmissionTaskId(submissionTaskId);
            if (!relationOpt.isPresent()) {
                log.debug("No active relation found for submission task: {}", submissionTaskId);
                return;
            }
            
            TaskRelation relation = relationOpt.get();
            Integer newDownloadStatus = mapSubmissionStatusToDownloadStatus(submissionStatus);
            
            if (newDownloadStatus != null) {
                // 获取下载任务并更新状态
                VideoDownload downloadTask = videoDownloadService.getById(relation.getDownloadTaskId());
                if (downloadTask != null && !downloadTask.getStatus().equals(newDownloadStatus)) {
                    // 这里需要根据实际的VideoDownloadService接口来更新状态
                    // videoDownloadService.updateStatus(relation.getDownloadTaskId(), newDownloadStatus);
                    log.info("Would update download task {} status to {} based on submission task {} status {}", 
                            relation.getDownloadTaskId(), newDownloadStatus, submissionTaskId, submissionStatus);
                }
                
                // 更新关联状态
                updateRelationStatusFromSubmission(relation, submissionStatus);
            }
            
        } catch (Exception e) {
            log.error("Failed to sync submission status to download for task: {}", submissionTaskId, e);
        }
    }
    
    @Override
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void checkAndRepairInconsistentStatus() {
        if (!statusSyncEnabled) {
            return;
        }
        
        log.debug("Checking for inconsistent task status...");
        
        try {
            List<TaskRelation> activeRelations = taskRelationService.findAllActiveRelations();
            int repairedCount = 0;
            
            for (TaskRelation relation : activeRelations) {
                try {
                    if (repairInconsistentRelation(relation)) {
                        repairedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to repair relation: {}", relation.getId(), e);
                }
            }
            
            if (repairedCount > 0) {
                log.info("Repaired {} inconsistent task relations", repairedCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to check and repair inconsistent status", e);
        }
    }
    
    @Override
    public void enableStatusSync() {
        this.statusSyncEnabled = true;
        log.info("Status sync enabled");
    }
    
    @Override
    public void disableStatusSync() {
        this.statusSyncEnabled = false;
        log.info("Status sync disabled");
    }
    
    @Override
    public boolean isStatusSyncEnabled() {
        return statusSyncEnabled;
    }
    
    /**
     * 映射下载状态到投稿状态
     */
    private SubmissionTask.TaskStatus mapDownloadStatusToSubmissionStatus(Integer downloadStatus) {
        switch (downloadStatus) {
            case 0: // PENDING
                return SubmissionTask.TaskStatus.WAITING_DOWNLOAD;
            case 1: // DOWNLOADING
                return SubmissionTask.TaskStatus.WAITING_DOWNLOAD;
            case 2: // COMPLETED
                return SubmissionTask.TaskStatus.PENDING;
            case 3: // FAILED
                return SubmissionTask.TaskStatus.FAILED;
            default:
                log.warn("Unknown download status: {}", downloadStatus);
                return null;
        }
    }
    
    /**
     * 映射投稿状态到下载状态
     */
    private Integer mapSubmissionStatusToDownloadStatus(SubmissionTask.TaskStatus submissionStatus) {
        switch (submissionStatus) {
            case WAITING_DOWNLOAD:
                return 0; // PENDING
            case PENDING:
            case CLIPPING:
            case MERGING:
            case SEGMENTING:
            case UPLOADING:
                return 2; // COMPLETED (下载已完成，投稿进行中)
            case COMPLETED:
                return 2; // COMPLETED
            case FAILED:
                return 3; // FAILED
            default:
                log.warn("Unknown submission status: {}", submissionStatus);
                return null;
        }
    }
    
    /**
     * 更新关联状态（基于下载状态变化）
     */
    private void updateRelationStatus(TaskRelation relation, Integer downloadStatus, SubmissionTask.TaskStatus submissionStatus) {
        TaskRelation.RelationStatus newStatus = null;
        
        if (downloadStatus == 3 || submissionStatus == SubmissionTask.TaskStatus.FAILED) {
            // 任一任务失败，关联状态设为失败
            newStatus = TaskRelation.RelationStatus.FAILED;
        } else if (downloadStatus == 2 && submissionStatus == SubmissionTask.TaskStatus.COMPLETED) {
            // 两个任务都完成，关联状态设为完成
            newStatus = TaskRelation.RelationStatus.COMPLETED;
        } else {
            // 其他情况保持活跃状态
            newStatus = TaskRelation.RelationStatus.ACTIVE;
        }
        
        if (newStatus != relation.getStatus()) {
            taskRelationService.updateStatus(relation.getId(), newStatus);
            log.debug("Updated relation {} status to {}", relation.getId(), newStatus);
        }
    }
    
    /**
     * 更新关联状态（基于投稿状态变化）
     */
    private void updateRelationStatusFromSubmission(TaskRelation relation, SubmissionTask.TaskStatus submissionStatus) {
        TaskRelation.RelationStatus newStatus = null;
        
        if (submissionStatus == SubmissionTask.TaskStatus.FAILED) {
            newStatus = TaskRelation.RelationStatus.FAILED;
        } else if (submissionStatus == SubmissionTask.TaskStatus.COMPLETED) {
            // 检查下载任务是否也完成了
            VideoDownload downloadTask = videoDownloadService.getById(relation.getDownloadTaskId());
            if (downloadTask != null && downloadTask.getStatus() == 2) {
                newStatus = TaskRelation.RelationStatus.COMPLETED;
            }
        }
        
        if (newStatus != null && newStatus != relation.getStatus()) {
            taskRelationService.updateStatus(relation.getId(), newStatus);
            log.debug("Updated relation {} status to {} based on submission status", relation.getId(), newStatus);
        }
    }
    
    /**
     * 修复不一致的关联关系
     */
    private boolean repairInconsistentRelation(TaskRelation relation) {
        try {
            // 获取下载任务和投稿任务的当前状态
            VideoDownload downloadTask = videoDownloadService.getById(relation.getDownloadTaskId());
            SubmissionTask submissionTask = submissionTaskService.getTaskDetail(relation.getSubmissionTaskId());
            
            if (downloadTask == null || submissionTask == null) {
                // 如果任一任务不存在，标记关联为失败
                taskRelationService.updateStatus(relation.getId(), TaskRelation.RelationStatus.FAILED);
                log.warn("Marked relation {} as failed due to missing task", relation.getId());
                return true;
            }
            
            // 检查状态是否一致
            SubmissionTask.TaskStatus expectedSubmissionStatus = mapDownloadStatusToSubmissionStatus(downloadTask.getStatus());
            if (expectedSubmissionStatus != null && !expectedSubmissionStatus.equals(submissionTask.getStatus())) {
                // 状态不一致，同步状态
                submissionTaskService.updateTaskStatus(submissionTask.getTaskId(), expectedSubmissionStatus);
                log.info("Repaired inconsistent status: submission task {} updated from {} to {}", 
                        submissionTask.getTaskId(), submissionTask.getStatus(), expectedSubmissionStatus);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Failed to repair inconsistent relation: {}", relation.getId(), e);
            return false;
        }
    }
}