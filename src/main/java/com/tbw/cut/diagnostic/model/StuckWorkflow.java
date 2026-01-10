package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * 卡住的工作流
 */
public class StuckWorkflow {
    
    private String workflowInstanceId;
    private String workflowType;
    private WorkflowStage currentStage;
    private String currentStepName;
    private LocalDateTime startTime;
    private LocalDateTime lastActivityTime;
    private Duration stuckDuration;
    private String stuckReason;
    private StuckType stuckType;
    private String description;
    private IssueSeverity severity;
    private boolean autoRecoverable;
    private String suggestedAction;
    private Long downloadTaskId;
    private String submissionTaskId;
    
    public StuckWorkflow() {
    }
    
    public StuckWorkflow(String workflowInstanceId, String workflowType, StuckType stuckType, String stuckReason) {
        this.workflowInstanceId = workflowInstanceId;
        this.workflowType = workflowType;
        this.stuckType = stuckType;
        this.stuckReason = stuckReason;
    }
    
    // Getters and Setters
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }
    
    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }
    
    public String getWorkflowType() {
        return workflowType;
    }
    
    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }
    
    public WorkflowStage getCurrentStage() {
        return currentStage;
    }
    
    public void setCurrentStage(WorkflowStage currentStage) {
        this.currentStage = currentStage;
    }
    
    public String getCurrentStepName() {
        return currentStepName;
    }
    
    public void setCurrentStepName(String currentStepName) {
        this.currentStepName = currentStepName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }
    
    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
        if (lastActivityTime != null) {
            this.stuckDuration = Duration.between(lastActivityTime, LocalDateTime.now());
        }
    }
    
    public Duration getStuckDuration() {
        return stuckDuration;
    }
    
    public void setStuckDuration(Duration stuckDuration) {
        this.stuckDuration = stuckDuration;
    }
    
    public String getStuckReason() {
        return stuckReason;
    }
    
    public void setStuckReason(String stuckReason) {
        this.stuckReason = stuckReason;
    }
    
    public StuckType getStuckType() {
        return stuckType;
    }
    
    public void setStuckType(StuckType stuckType) {
        this.stuckType = stuckType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public IssueSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(IssueSeverity severity) {
        this.severity = severity;
    }
    
    public boolean isAutoRecoverable() {
        return autoRecoverable;
    }
    
    public void setAutoRecoverable(boolean autoRecoverable) {
        this.autoRecoverable = autoRecoverable;
    }
    
    public String getSuggestedAction() {
        return suggestedAction;
    }
    
    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
    
    public Long getDownloadTaskId() {
        return downloadTaskId;
    }
    
    public void setDownloadTaskId(Long downloadTaskId) {
        this.downloadTaskId = downloadTaskId;
    }
    
    public String getSubmissionTaskId() {
        return submissionTaskId;
    }
    
    public void setSubmissionTaskId(String submissionTaskId) {
        this.submissionTaskId = submissionTaskId;
    }
    
    @Override
    public String toString() {
        return "StuckWorkflow{" +
                "workflowInstanceId='" + workflowInstanceId + '\'' +
                ", workflowType='" + workflowType + '\'' +
                ", currentStage=" + currentStage +
                ", currentStepName='" + currentStepName + '\'' +
                ", stuckType=" + stuckType +
                ", stuckDuration=" + stuckDuration +
                ", severity=" + severity +
                ", autoRecoverable=" + autoRecoverable +
                '}';
    }
}