package com.tbw.cut.service;

import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.entity.SubmissionTask;

/**
 * 状态同步服务接口
 * 负责在下载任务和投稿任务之间同步状态变化
 */
public interface StatusSyncService {
    
    /**
     * 处理下载状态变化事件
     * @param downloadTaskId 下载任务ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    void handleDownloadStatusChange(Long downloadTaskId, Integer oldStatus, Integer newStatus);
    
    /**
     * 处理投稿状态变化事件
     * @param submissionTaskId 投稿任务ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    void handleSubmissionStatusChange(String submissionTaskId, SubmissionTask.TaskStatus oldStatus, SubmissionTask.TaskStatus newStatus);
    
    /**
     * 同步下载任务状态到投稿任务
     * @param downloadTaskId 下载任务ID
     * @param downloadStatus 下载任务状态
     */
    void syncDownloadStatusToSubmission(Long downloadTaskId, Integer downloadStatus);
    
    /**
     * 同步投稿任务状态到下载任务
     * @param submissionTaskId 投稿任务ID
     * @param submissionStatus 投稿任务状态
     */
    void syncSubmissionStatusToDownload(String submissionTaskId, SubmissionTask.TaskStatus submissionStatus);
    
    /**
     * 检查并修复状态不一致的任务关联
     */
    void checkAndRepairInconsistentStatus();
    
    /**
     * 启用状态同步
     */
    void enableStatusSync();
    
    /**
     * 禁用状态同步
     */
    void disableStatusSync();
    
    /**
     * 检查状态同步是否启用
     * @return 是否启用状态同步
     */
    boolean isStatusSyncEnabled();
}