package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流引擎状态
 */
public class WorkflowEngineStatus {
    
    private String engineId;
    private HealthStatus status;
    private LocalDateTime lastCheckTime;
    private String version;
    private int activeWorkflows;
    private int totalWorkflows;
    private int completedWorkflows;
    private int failedWorkflows;
    private Double averageExecutionTimeMs;
    private Map<String, Object> engineMetrics;
    private String statusMessage;
    private Long uptimeMs;
    private boolean isHealthy;
    
    public WorkflowEngineStatus() {
        this.lastCheckTime = LocalDateTime.now();
        this.engineId = "workflow-engine-" + System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getEngineId() {
        return engineId;
    }
    
    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }
    
    public HealthStatus getStatus() {
        return status;
    }
    
    public void setStatus(HealthStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }
    
    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public int getActiveWorkflows() {
        return activeWorkflows;
    }
    
    public void setActiveWorkflows(int activeWorkflows) {
        this.activeWorkflows = activeWorkflows;
    }
    
    public int getTotalWorkflows() {
        return totalWorkflows;
    }
    
    public void setTotalWorkflows(int totalWorkflows) {
        this.totalWorkflows = totalWorkflows;
    }
    
    public int getCompletedWorkflows() {
        return completedWorkflows;
    }
    
    public void setCompletedWorkflows(int completedWorkflows) {
        this.completedWorkflows = completedWorkflows;
    }
    
    public int getFailedWorkflows() {
        return failedWorkflows;
    }
    
    public void setFailedWorkflows(int failedWorkflows) {
        this.failedWorkflows = failedWorkflows;
    }
    
    public Double getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }
    
    public void setAverageExecutionTimeMs(Double averageExecutionTimeMs) {
        this.averageExecutionTimeMs = averageExecutionTimeMs;
    }
    
    public Map<String, Object> getEngineMetrics() {
        return engineMetrics;
    }
    
    public void setEngineMetrics(Map<String, Object> engineMetrics) {
        this.engineMetrics = engineMetrics;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public Long getUptimeMs() {
        return uptimeMs;
    }
    
    public void setUptimeMs(Long uptimeMs) {
        this.uptimeMs = uptimeMs;
    }
    
    public boolean isHealthy() {
        return isHealthy;
    }
    
    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }
    
    @Override
    public String toString() {
        return "WorkflowEngineStatus{" +
                "engineId='" + engineId + '\'' +
                ", status=" + status +
                ", activeWorkflows=" + activeWorkflows +
                ", totalWorkflows=" + totalWorkflows +
                ", completedWorkflows=" + completedWorkflows +
                ", failedWorkflows=" + failedWorkflows +
                ", isHealthy=" + isHealthy +
                '}';
    }
}