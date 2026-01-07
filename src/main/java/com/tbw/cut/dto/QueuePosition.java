package com.tbw.cut.dto;

import java.time.LocalDateTime;

/**
 * 队列位置DTO
 */
public class QueuePosition {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 队列位置（从1开始）
     */
    private int position;
    
    /**
     * 队列总长度
     */
    private int queueLength;
    
    /**
     * 预估等待时间（分钟）
     */
    private long estimatedWaitMinutes;
    
    /**
     * 入队时间
     */
    private LocalDateTime queuedAt;
    
    // 构造函数
    public QueuePosition() {}
    
    public QueuePosition(String taskId, int position, int queueLength) {
        this.taskId = taskId;
        this.position = position;
        this.queueLength = queueLength;
        this.queuedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否是下一个要处理的任务
     */
    public boolean isNext() {
        return position == 1;
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
    
    public int getQueueLength() {
        return queueLength;
    }
    
    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }
    
    public long getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }
    
    public void setEstimatedWaitMinutes(long estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }
    
    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }
    
    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }
    
    @Override
    public String toString() {
        return "QueuePosition{" +
                "taskId='" + taskId + '\'' +
                ", position=" + position +
                ", queueLength=" + queueLength +
                ", estimatedWaitMinutes=" + estimatedWaitMinutes +
                '}';
    }
}