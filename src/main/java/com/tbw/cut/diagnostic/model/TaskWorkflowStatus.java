package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务工作流状态
 */
public class TaskWorkflowStatus {
    
    private Long downloadTaskId;
    private String submissionTaskId;
    private WorkflowStage currentStage;
    private List<WorkflowStepStatus> completedSteps;
    private List<WorkflowStepStatus> pendingSteps;
    private List<WorkflowIssue> issues;
    private LocalDateTime lastActivity;
    private String workflowInstanceId;
    private WorkflowExecutionStatus executionStatus;
    private Double progressPercentage;
    private String currentStepName;
    private LocalDateTime startTime;
    private LocalDateTime estimatedCompletionTime;
    
    public TaskWorkflowStatus() {
        this.lastActivity = LocalDateTime.now();
    }
    
    public TaskWorkflowStatus(Long downloadTaskId) {
        this();
        this.downloadTaskId = downloadTaskId;
    }
    
    // Getters and Setters
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
    
    public WorkflowStage getCurrentStage() {
        return currentStage;
    }
    
    public void setCurrentStage(WorkflowStage currentStage) {
        this.currentStage = currentStage;
    }
    
    public List<WorkflowStepStatus> getCompletedSteps() {
        return completedSteps;
    }
    
    public void setCompletedSteps(List<WorkflowStepStatus> completedSteps) {
        this.completedSteps = completedSteps;
    }
    
    public List<WorkflowStepStatus> getPendingSteps() {
        return pendingSteps;
    }
    
    public void setPendingSteps(List<WorkflowStepStatus> pendingSteps) {
        this.pendingSteps = pendingSteps;
    }
    
    public List<WorkflowIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<WorkflowIssue> issues) {
        this.issues = issues;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }
    
    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }
    
    public WorkflowExecutionStatus getExecutionStatus() {
        return executionStatus;
    }
    
    public void setExecutionStatus(WorkflowExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }
    
    public Double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
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
    
    public LocalDateTime getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }
    
    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }
    
    @Override
    public String toString() {
        return "TaskWorkflowStatus{" +
                "downloadTaskId=" + downloadTaskId +
                ", submissionTaskId='" + submissionTaskId + '\'' +
                ", currentStage=" + currentStage +
                ", executionStatus=" + executionStatus +
                ", progressPercentage=" + progressPercentage +
                ", currentStepName='" + currentStepName + '\'' +
                '}';
    }
}