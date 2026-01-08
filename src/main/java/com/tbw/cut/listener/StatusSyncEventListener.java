package com.tbw.cut.listener;

import com.tbw.cut.event.DownloadStatusChangeEvent;
import com.tbw.cut.event.SubmissionStatusChangeEvent;
import com.tbw.cut.service.StatusSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 状态同步事件监听器
 * 监听下载和投稿任务的状态变化事件，触发状态同步
 */
@Slf4j
@Component
public class StatusSyncEventListener {
    
    @Autowired
    private StatusSyncService statusSyncService;
    
    /**
     * 监听下载状态变化事件
     */
    @EventListener
    @Async
    public void handleDownloadStatusChange(DownloadStatusChangeEvent event) {
        if (!event.isStatusChanged()) {
            log.debug("Download status not changed, skipping sync for task: {}", event.getTaskId());
            return;
        }
        
        log.info("Received download status change event: taskId={}, oldStatus={}, newStatus={}", 
                event.getTaskId(), event.getOldStatus(), event.getNewStatus());
        
        try {
            statusSyncService.handleDownloadStatusChange(
                    event.getTaskId(), 
                    event.getOldStatus(), 
                    event.getNewStatus()
            );
        } catch (Exception e) {
            log.error("Failed to handle download status change event for task: {}", event.getTaskId(), e);
        }
    }
    
    /**
     * 监听投稿状态变化事件
     */
    @EventListener
    @Async
    public void handleSubmissionStatusChange(SubmissionStatusChangeEvent event) {
        if (!event.isStatusChanged()) {
            log.debug("Submission status not changed, skipping sync for task: {}", event.getTaskId());
            return;
        }
        
        log.info("Received submission status change event: taskId={}, oldStatus={}, newStatus={}", 
                event.getTaskId(), event.getOldStatus(), event.getNewStatus());
        
        try {
            statusSyncService.handleSubmissionStatusChange(
                    event.getTaskId(), 
                    event.getOldStatus(), 
                    event.getNewStatus()
            );
        } catch (Exception e) {
            log.error("Failed to handle submission status change event for task: {}", event.getTaskId(), e);
        }
    }
}