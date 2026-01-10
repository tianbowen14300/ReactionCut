package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;

/**
 * 修复建议
 */
public class Recommendation {
    
    private String recommendationId;
    private String title;
    private String description;
    private RecommendationType type;
    private RecommendationPriority priority;
    private String relatedIssueId;
    private String actionSteps;
    private boolean automated;
    private String estimatedEffort;
    private LocalDateTime createdTime;
    
    public Recommendation() {
        this.createdTime = LocalDateTime.now();
        this.recommendationId = generateRecommendationId();
    }
    
    public Recommendation(String title, String description, RecommendationType type, RecommendationPriority priority) {
        this();
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
    }
    
    private String generateRecommendationId() {
        return "REC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getRecommendationId() {
        return recommendationId;
    }
    
    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
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
    
    public RecommendationType getType() {
        return type;
    }
    
    public void setType(RecommendationType type) {
        this.type = type;
    }
    
    public RecommendationPriority getPriority() {
        return priority;
    }
    
    public void setPriority(RecommendationPriority priority) {
        this.priority = priority;
    }
    
    public String getRelatedIssueId() {
        return relatedIssueId;
    }
    
    public void setRelatedIssueId(String relatedIssueId) {
        this.relatedIssueId = relatedIssueId;
    }
    
    public String getActionSteps() {
        return actionSteps;
    }
    
    public void setActionSteps(String actionSteps) {
        this.actionSteps = actionSteps;
    }
    
    public boolean isAutomated() {
        return automated;
    }
    
    public void setAutomated(boolean automated) {
        this.automated = automated;
    }
    
    public String getEstimatedEffort() {
        return estimatedEffort;
    }
    
    public void setEstimatedEffort(String estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    @Override
    public String toString() {
        return "Recommendation{" +
                "recommendationId='" + recommendationId + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", priority=" + priority +
                ", automated=" + automated +
                '}';
    }
}