package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;

/**
 * 工作流问题
 */
public class WorkflowIssue {
    
    private String issueId;
    private String stepName;
    private String issueType;
    private String description;
    private IssueSeverity severity;
    private LocalDateTime occurredTime;
    private String errorCode;
    private String errorMessage;
    private boolean resolved;
    private LocalDateTime resolvedTime;
    private String resolutionAction;
    
    public WorkflowIssue() {
        this.occurredTime = LocalDateTime.now();
        this.issueId = generateIssueId();
    }
    
    public WorkflowIssue(String stepName, String issueType, String description, IssueSeverity severity) {
        this();
        this.stepName = stepName;
        this.issueType = issueType;
        this.description = description;
        this.severity = severity;
    }
    
    private String generateIssueId() {
        return "WF-ISSUE-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getIssueId() {
        return issueId;
    }
    
    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }
    
    public String getStepName() {
        return stepName;
    }
    
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }
    
    public String getIssueType() {
        return issueType;
    }
    
    public void setIssueType(String issueType) {
        this.issueType = issueType;
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
    
    public LocalDateTime getOccurredTime() {
        return occurredTime;
    }
    
    public void setOccurredTime(LocalDateTime occurredTime) {
        this.occurredTime = occurredTime;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isResolved() {
        return resolved;
    }
    
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
        if (resolved && resolvedTime == null) {
            this.resolvedTime = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }
    
    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }
    
    public String getResolutionAction() {
        return resolutionAction;
    }
    
    public void setResolutionAction(String resolutionAction) {
        this.resolutionAction = resolutionAction;
    }
    
    @Override
    public String toString() {
        return "WorkflowIssue{" +
                "issueId='" + issueId + '\'' +
                ", stepName='" + stepName + '\'' +
                ", issueType='" + issueType + '\'' +
                ", severity=" + severity +
                ", resolved=" + resolved +
                '}';
    }
}