package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;

/**
 * 任务关联问题
 */
public class TaskRelationIssue {
    
    private String issueId;
    private TaskRelationIssueType issueType;
    private String description;
    private Long downloadTaskId;
    private String submissionTaskId;
    private String relationId;
    private IssueSeverity severity;
    private LocalDateTime detectedTime;
    private String suggestedFix;
    private boolean autoFixable;
    
    public TaskRelationIssue() {
        this.detectedTime = LocalDateTime.now();
        this.issueId = generateIssueId();
    }
    
    public TaskRelationIssue(TaskRelationIssueType issueType, String description, IssueSeverity severity) {
        this();
        this.issueType = issueType;
        this.description = description;
        this.severity = severity;
    }
    
    private String generateIssueId() {
        return "TR-ISSUE-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getIssueId() {
        return issueId;
    }
    
    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }
    
    public TaskRelationIssueType getIssueType() {
        return issueType;
    }
    
    public void setIssueType(TaskRelationIssueType issueType) {
        this.issueType = issueType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public String getRelationId() {
        return relationId;
    }
    
    public void setRelationId(String relationId) {
        this.relationId = relationId;
    }
    
    public IssueSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(IssueSeverity severity) {
        this.severity = severity;
    }
    
    public LocalDateTime getDetectedTime() {
        return detectedTime;
    }
    
    public void setDetectedTime(LocalDateTime detectedTime) {
        this.detectedTime = detectedTime;
    }
    
    public String getSuggestedFix() {
        return suggestedFix;
    }
    
    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }
    
    public boolean isAutoFixable() {
        return autoFixable;
    }
    
    public void setAutoFixable(boolean autoFixable) {
        this.autoFixable = autoFixable;
    }
    
    @Override
    public String toString() {
        return "TaskRelationIssue{" +
                "issueId='" + issueId + '\'' +
                ", issueType=" + issueType +
                ", downloadTaskId=" + downloadTaskId +
                ", submissionTaskId='" + submissionTaskId + '\'' +
                ", severity=" + severity +
                ", autoFixable=" + autoFixable +
                '}';
    }
}