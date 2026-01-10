package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务关联验证结果
 */
public class TaskRelationValidation {
    
    private String validationId;
    private LocalDateTime validationTime;
    private boolean isValid;
    private int totalRelations;
    private int validRelations;
    private int invalidRelations;
    private int orphanedDownloadTasks;
    private int orphanedSubmissionTasks;
    private List<TaskRelationIssue> issues;
    private List<String> recommendations;
    private Long executionTimeMs;
    
    public TaskRelationValidation() {
        this.validationTime = LocalDateTime.now();
        this.validationId = generateValidationId();
    }
    
    private String generateValidationId() {
        return "VALIDATION-" + System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getValidationId() {
        return validationId;
    }
    
    public void setValidationId(String validationId) {
        this.validationId = validationId;
    }
    
    public LocalDateTime getValidationTime() {
        return validationTime;
    }
    
    public void setValidationTime(LocalDateTime validationTime) {
        this.validationTime = validationTime;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public int getTotalRelations() {
        return totalRelations;
    }
    
    public void setTotalRelations(int totalRelations) {
        this.totalRelations = totalRelations;
    }
    
    public int getValidRelations() {
        return validRelations;
    }
    
    public void setValidRelations(int validRelations) {
        this.validRelations = validRelations;
    }
    
    public int getInvalidRelations() {
        return invalidRelations;
    }
    
    public void setInvalidRelations(int invalidRelations) {
        this.invalidRelations = invalidRelations;
    }
    
    public int getOrphanedDownloadTasks() {
        return orphanedDownloadTasks;
    }
    
    public void setOrphanedDownloadTasks(int orphanedDownloadTasks) {
        this.orphanedDownloadTasks = orphanedDownloadTasks;
    }
    
    public int getOrphanedSubmissionTasks() {
        return orphanedSubmissionTasks;
    }
    
    public void setOrphanedSubmissionTasks(int orphanedSubmissionTasks) {
        this.orphanedSubmissionTasks = orphanedSubmissionTasks;
    }
    
    public List<TaskRelationIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<TaskRelationIssue> issues) {
        this.issues = issues;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "TaskRelationValidation{" +
                "validationId='" + validationId + '\'' +
                ", isValid=" + isValid +
                ", totalRelations=" + totalRelations +
                ", validRelations=" + validRelations +
                ", invalidRelations=" + invalidRelations +
                ", orphanedDownloadTasks=" + orphanedDownloadTasks +
                ", orphanedSubmissionTasks=" + orphanedSubmissionTasks +
                '}';
    }
}