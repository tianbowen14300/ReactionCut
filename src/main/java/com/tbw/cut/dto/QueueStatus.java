package com.tbw.cut.dto;

import com.tbw.cut.entity.QueueTaskStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 队列状态DTO
 */
public class QueueStatus {
    /**
     * 队列长度
     */
    private int queueLength;
    
    /**
     * 当前处理状态
     */
    private ProcessingStatus processingStatus;
    
    /**
     * 当前正在处理的任务ID
     */
    private String currentTaskId;
    
    /**
     * 队列中的任务列表
     */
    private List<QueueTaskInfo> queuedTasks;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
    
    /**
     * 队列是否正在运行
     */
    private boolean isRunning;
    
    /**
     * 处理状态枚举
     */
    public enum ProcessingStatus {
        IDLE,           // 空闲
        PROCESSING,     // 正在处理
        PAUSED,         // 暂停
        ERROR           // 错误状态
    }
    
    /**
     * 队列任务信息
     */
    public static class QueueTaskInfo {
        private String taskId;
        private int position;
        private QueueTaskStatus status;
        private LocalDateTime queuedAt;
        private int retryCount;
        
        // 构造函数
        public QueueTaskInfo() {}
        
        public QueueTaskInfo(String taskId, int position, QueueTaskStatus status, LocalDateTime queuedAt) {
            this.taskId = taskId;
            this.position = position;
            this.status = status;
            this.queuedAt = queuedAt;
        }
        
        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
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
        
        public LocalDateTime getQueuedAt() {
            return queuedAt;
        }
        
        public void setQueuedAt(LocalDateTime queuedAt) {
            this.queuedAt = queuedAt;
        }
        
        public int getRetryCount() {
            return retryCount;
        }
        
        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
    }
    
    // 构造函数
    public QueueStatus() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public QueueStatus(int queueLength, ProcessingStatus processingStatus) {
        this.queueLength = queueLength;
        this.processingStatus = processingStatus;
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 检查队列是否为空
     */
    public boolean isEmpty() {
        return queueLength == 0;
    }
    
    /**
     * 检查是否正在处理任务
     */
    public boolean isProcessing() {
        return processingStatus == ProcessingStatus.PROCESSING;
    }
    
    // Getters and Setters
    public int getQueueLength() {
        return queueLength;
    }
    
    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }
    
    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }
    
    public String getCurrentTaskId() {
        return currentTaskId;
    }
    
    public void setCurrentTaskId(String currentTaskId) {
        this.currentTaskId = currentTaskId;
    }
    
    public List<QueueTaskInfo> getQueuedTasks() {
        return queuedTasks;
    }
    
    public void setQueuedTasks(List<QueueTaskInfo> queuedTasks) {
        this.queuedTasks = queuedTasks;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setRunning(boolean running) {
        isRunning = running;
    }
    
    @Override
    public String toString() {
        return "QueueStatus{" +
                "queueLength=" + queueLength +
                ", processingStatus=" + processingStatus +
                ", currentTaskId='" + currentTaskId + '\'' +
                ", isRunning=" + isRunning +
                '}';
    }
}