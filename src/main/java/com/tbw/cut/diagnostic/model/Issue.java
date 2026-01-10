package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 诊断发现的问题
 */
public class Issue {
    
    private String issueId;
    private String title;
    private String description;
    private IssueSeverity severity;
    private IssueCategory category;
    private String componentName;
    private LocalDateTime detectedTime;
    private Map<String, Object> context;
    private String possibleCause;
    private String suggestedAction;
    private boolean autoFixable;
    
    public Issue() {
        this.detectedTime = LocalDateTime.now();
        this.issueId = generateIssueId();
    }
    
    public Issue(String title, String description, IssueSeverity severity, IssueCategory category) {
        this();
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.category = category;
    }
    
    private String generateIssueId() {
        return "ISSUE-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getIssueId() {
        return issueId;
    }
    
    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
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
    
    public IssueCategory getCategory() {
        return category;
    }
    
    public void setCategory(IssueCategory category) {
        this.category = category;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    
    public LocalDateTime getDetectedTime() {
        return detectedTime;
    }
    
    public void setDetectedTime(LocalDateTime detectedTime) {
        this.detectedTime = detectedTime;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    public String getPossibleCause() {
        return possibleCause;
    }
    
    public void setPossibleCause(String possibleCause) {
        this.possibleCause = possibleCause;
    }
    
    public String getSuggestedAction() {
        return suggestedAction;
    }
    
    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
    
    public boolean isAutoFixable() {
        return autoFixable;
    }
    
    public void setAutoFixable(boolean autoFixable) {
        this.autoFixable = autoFixable;
    }
    
    @Override
    public String toString() {
        return "Issue{" +
                "issueId='" + issueId + '\'' +
                ", title='" + title + '\'' +
                ", severity=" + severity +
                ", category=" + category +
                ", componentName='" + componentName + '\'' +
                ", autoFixable=" + autoFixable +
                '}';
    }
}