package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流性能指标
 */
public class WorkflowMetrics {
    
    private LocalDateTime timestamp;
    private int totalWorkflows;
    private int activeWorkflows;
    private int completedWorkflows;
    private int failedWorkflows;
    private int stuckWorkflows;
    private Double averageExecutionTimeMs;
    private Double successRate;
    private Double throughputPerHour;
    private Long maxExecutionTimeMs;
    private Long minExecutionTimeMs;
    private Map<String, Integer> workflowTypeDistribution;
    private Map<String, Double> stepAverageExecutionTimes;
    private int totalSteps;
    private int completedSteps;
    private int failedSteps;
    
    public WorkflowMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getTotalWorkflows() {
        return totalWorkflows;
    }
    
    public void setTotalWorkflows(int totalWorkflows) {
        this.totalWorkflows = totalWorkflows;
    }
    
    public int getActiveWorkflows() {
        return activeWorkflows;
    }
    
    public void setActiveWorkflows(int activeWorkflows) {
        this.activeWorkflows = activeWorkflows;
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
    
    public int getStuckWorkflows() {
        return stuckWorkflows;
    }
    
    public void setStuckWorkflows(int stuckWorkflows) {
        this.stuckWorkflows = stuckWorkflows;
    }
    
    public Double getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }
    
    public void setAverageExecutionTimeMs(Double averageExecutionTimeMs) {
        this.averageExecutionTimeMs = averageExecutionTimeMs;
    }
    
    public Double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }
    
    public Double getThroughputPerHour() {
        return throughputPerHour;
    }
    
    public void setThroughputPerHour(Double throughputPerHour) {
        this.throughputPerHour = throughputPerHour;
    }
    
    public Long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }
    
    public void setMaxExecutionTimeMs(Long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }
    
    public Long getMinExecutionTimeMs() {
        return minExecutionTimeMs;
    }
    
    public void setMinExecutionTimeMs(Long minExecutionTimeMs) {
        this.minExecutionTimeMs = minExecutionTimeMs;
    }
    
    public Map<String, Integer> getWorkflowTypeDistribution() {
        return workflowTypeDistribution;
    }
    
    public void setWorkflowTypeDistribution(Map<String, Integer> workflowTypeDistribution) {
        this.workflowTypeDistribution = workflowTypeDistribution;
    }
    
    public Map<String, Double> getStepAverageExecutionTimes() {
        return stepAverageExecutionTimes;
    }
    
    public void setStepAverageExecutionTimes(Map<String, Double> stepAverageExecutionTimes) {
        this.stepAverageExecutionTimes = stepAverageExecutionTimes;
    }
    
    public int getTotalSteps() {
        return totalSteps;
    }
    
    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }
    
    public int getCompletedSteps() {
        return completedSteps;
    }
    
    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }
    
    public int getFailedSteps() {
        return failedSteps;
    }
    
    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }
    
    @Override
    public String toString() {
        return "WorkflowMetrics{" +
                "timestamp=" + timestamp +
                ", totalWorkflows=" + totalWorkflows +
                ", activeWorkflows=" + activeWorkflows +
                ", completedWorkflows=" + completedWorkflows +
                ", failedWorkflows=" + failedWorkflows +
                ", stuckWorkflows=" + stuckWorkflows +
                ", averageExecutionTimeMs=" + averageExecutionTimeMs +
                ", successRate=" + successRate +
                ", throughputPerHour=" + throughputPerHour +
                '}';
    }
}