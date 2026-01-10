package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流实例状态
 */
public class WorkflowInstanceStatus {
    
    private String workflowInstanceId;
    private String workflowType;
    private WorkflowStage currentStage;
    private WorkflowExecutionStatus executionStatus;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    private LocalDateTime estimatedCompletionTime;
    private Double progressPercentage;
    private List<WorkflowStepStatus> steps;
    private List<WorkflowIssue> issues;
    private String currentStepName;
    private Long totalExecutionTimeMs;
    private boolean isStuck;
    private String stuckReason;
    
    public WorkflowInstanceStatus() {
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public WorkflowInstanceStatus(String workflowInstanceId, String workflowType) {
        this();
        this.workflowInstanceId = workflowInstanceId;
        this.workflowType = workflowType;
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
    
    public WorkflowExecutionStatus getExecutionStatus() {
        return executionStatus;
    }
    
    public void setExecutionStatus(WorkflowExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public LocalDateTime getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }
    
    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }
    
    public Double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public List<WorkflowStepStatus> getSteps() {
        return steps;
    }
    
    public void setSteps(List<WorkflowStepStatus> steps) {
        this.steps = steps;
    }
    
    public List<WorkflowIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<WorkflowIssue> issues) {
        this.issues = issues;
    }
    
    public String getCurrentStepName() {
        return currentStepName;
    }
    
    public void setCurrentStepName(String currentStepName) {
        this.currentStepName = currentStepName;
    }
    
    public Long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public void setTotalExecutionTimeMs(Long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }
    
    public boolean isStuck() {
        return isStuck;
    }
    
    public void setStuck(boolean stuck) {
        isStuck = stuck;
    }
    
    public String getStuckReason() {
        return stuckReason;
    }
    
    public void setStuckReason(String stuckReason) {
        this.stuckReason = stuckReason;
    }
    
    @Override
    public String toString() {
        return "WorkflowInstanceStatus{" +
                "workflowInstanceId='" + workflowInstanceId + '\'' +
                ", workflowType='" + workflowType + '\'' +
                ", currentStage=" + currentStage +
                ", executionStatus=" + executionStatus +
                ", progressPercentage=" + progressPercentage +
                ", currentStepName='" + currentStepName + '\'' +
                ", isStuck=" + isStuck +
                '}';
    }
}