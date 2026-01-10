package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * 工作流步骤状态
 */
public class WorkflowStepStatus {
    
    private String stepName;
    private String stepType;
    private WorkflowStepState state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration executionDuration;
    private String errorMessage;
    private String errorStackTrace;
    private Double progressPercentage;
    private String statusMessage;
    private int retryCount;
    private int maxRetries;
    
    public WorkflowStepStatus() {
    }
    
    public WorkflowStepStatus(String stepName, String stepType, WorkflowStepState state) {
        this.stepName = stepName;
        this.stepType = stepType;
        this.state = state;
    }
    
    // Getters and Setters
    public String getStepName() {
        return stepName;
    }
    
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }
    
    public String getStepType() {
        return stepType;
    }
    
    public void setStepType(String stepType) {
        this.stepType = stepType;
    }
    
    public WorkflowStepState getState() {
        return state;
    }
    
    public void setState(WorkflowStepState state) {
        this.state = state;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.executionDuration = Duration.between(startTime, endTime);
        }
    }
    
    public Duration getExecutionDuration() {
        return executionDuration;
    }
    
    public void setExecutionDuration(Duration executionDuration) {
        this.executionDuration = executionDuration;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorStackTrace() {
        return errorStackTrace;
    }
    
    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }
    
    public Double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    @Override
    public String toString() {
        return "WorkflowStepStatus{" +
                "stepName='" + stepName + '\'' +
                ", stepType='" + stepType + '\'' +
                ", state=" + state +
                ", progressPercentage=" + progressPercentage +
                ", retryCount=" + retryCount +
                '}';
    }
}