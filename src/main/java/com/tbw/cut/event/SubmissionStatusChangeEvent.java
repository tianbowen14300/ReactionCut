package com.tbw.cut.event;

import com.tbw.cut.entity.SubmissionTask;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 投稿状态变化事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionStatusChangeEvent {
    
    /**
     * 投稿任务ID
     */
    private String taskId;
    
    /**
     * 旧状态
     */
    private SubmissionTask.TaskStatus oldStatus;
    
    /**
     * 新状态
     */
    private SubmissionTask.TaskStatus newStatus;
    
    /**
     * 事件发生时间
     */
    private Long timestamp;
    
    /**
     * 事件来源
     */
    private String source;
    
    /**
     * 创建投稿状态变化事件
     */
    public static SubmissionStatusChangeEvent create(String taskId, SubmissionTask.TaskStatus oldStatus, SubmissionTask.TaskStatus newStatus) {
        return SubmissionStatusChangeEvent.builder()
                .taskId(taskId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .timestamp(System.currentTimeMillis())
                .source("SubmissionTaskService")
                .build();
    }
    
    /**
     * 检查是否为状态改变事件
     */
    public boolean isStatusChanged() {
        return oldStatus != null && newStatus != null && !oldStatus.equals(newStatus);
    }
    
    /**
     * 检查是否为完成事件
     */
    public boolean isCompletedEvent() {
        return newStatus != null && newStatus == SubmissionTask.TaskStatus.COMPLETED;
    }
    
    /**
     * 检查是否为失败事件
     */
    public boolean isFailedEvent() {
        return newStatus != null && newStatus == SubmissionTask.TaskStatus.FAILED;
    }
    
    /**
     * 检查是否为等待下载事件
     */
    public boolean isWaitingDownloadEvent() {
        return newStatus != null && newStatus == SubmissionTask.TaskStatus.WAITING_DOWNLOAD;
    }
}