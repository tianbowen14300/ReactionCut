package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;

/**
 * 孤立任务
 */
public class OrphanedTask {
    
    private String taskId;
    private TaskType taskType;
    private String taskName;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdateTime;
    private String reason;
    private String description;
    private boolean autoFixable;
    private String suggestedAction;
    private String relatedVideoId;
    private String relatedBvid;
    
    public OrphanedTask() {
    }
    
    public OrphanedTask(String taskId, TaskType taskType, String reason) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isAutoFixable() {
        return autoFixable;
    }
    
    public void setAutoFixable(boolean autoFixable) {
        this.autoFixable = autoFixable;
    }
    
    public String getSuggestedAction() {
        return suggestedAction;
    }
    
    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
    
    public String getRelatedVideoId() {
        return relatedVideoId;
    }
    
    public void setRelatedVideoId(String relatedVideoId) {
        this.relatedVideoId = relatedVideoId;
    }
    
    public String getRelatedBvid() {
        return relatedBvid;
    }
    
    public void setRelatedBvid(String relatedBvid) {
        this.relatedBvid = relatedBvid;
    }
    
    @Override
    public String toString() {
        return "OrphanedTask{" +
                "taskId='" + taskId + '\'' +
                ", taskType=" + taskType +
                ", taskName='" + taskName + '\'' +
                ", status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                ", autoFixable=" + autoFixable +
                '}';
    }
}