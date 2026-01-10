package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;

/**
 * 重启结果
 */
public class RestartResult {
    
    private String restartId;
    private boolean success;
    private String workflowId;
    private LocalDateTime restartTime;
    private String description;
    private String errorMessage;
    private String previousState;
    private String newState;
    private Long executionTimeMs;
    
    public RestartResult() {
        this.restartTime = LocalDateTime.now();
        this.restartId = generateRestartId();
    }
    
    public RestartResult(String workflowId, boolean success) {
        this();
        this.workflowId = workflowId;
        this.success = success;
    }
    
    private String generateRestartId() {
        return "RESTART-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getRestartId() {
        return restartId;
    }
    
    public void setRestartId(String restartId) {
        this.restartId = restartId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }
    
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
    public LocalDateTime getRestartTime() {
        return restartTime;
    }
    
    public void setRestartTime(LocalDateTime restartTime) {
        this.restartTime = restartTime;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getPreviousState() {
        return previousState;
    }
    
    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }
    
    public String getNewState() {
        return newState;
    }
    
    public void setNewState(String newState) {
        this.newState = newState;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "RestartResult{" +
                "restartId='" + restartId + '\'' +
                ", success=" + success +
                ", workflowId='" + workflowId + '\'' +
                ", previousState='" + previousState + '\'' +
                ", newState='" + newState + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}