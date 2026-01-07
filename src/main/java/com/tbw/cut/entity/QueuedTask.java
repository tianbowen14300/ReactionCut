package com.tbw.cut.entity;

import java.time.LocalDateTime;

/**
 * 队列任务实体
 */
public class QueuedTask {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 队列ID（用于标识队列）
     */
    private String queueId;
    
    /**
     * 入队时间
     */
    private LocalDateTime queuedAt;
    
    /**
     * 队列位置
     */
    private int position;
    
    /**
     * 任务状态
     */
    private QueueTaskStatus status;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 最后重试时间
     */
    private LocalDateTime lastRetryAt;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 开始处理时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    // 构造函数
    public QueuedTask() {}
    
    public QueuedTask(String taskId, String queueId) {
        this.taskId = taskId;
        this.queueId = queueId;
        this.queuedAt = LocalDateTime.now();
        this.status = QueueTaskStatus.QUEUED;
        this.retryCount = 0;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getQueueId() {
        return queueId;
    }
    
    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }
    
    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }
    
    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public QueueTaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(QueueTaskStatus status) {
        this.status = status;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }
    
    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return "QueuedTask{" +
                "taskId='" + taskId + '\'' +
                ", queueId='" + queueId + '\'' +
                ", position=" + position +
                ", status=" + status +
                ", retryCount=" + retryCount +
                '}';
    }
}