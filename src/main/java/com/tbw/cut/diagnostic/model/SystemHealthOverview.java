package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统健康状态概览
 */
public class SystemHealthOverview {
    
    private LocalDateTime timestamp;
    private OverallHealth overallHealth;
    private String healthSummary;
    private Map<ComponentType, HealthStatus> componentHealthMap;
    private List<String> criticalIssues;
    private List<String> warnings;
    private PerformanceMetrics currentPerformance;
    private int totalActiveWorkflows;
    private int totalStuckWorkflows;
    private int totalOrphanedTasks;
    private Double systemLoadPercentage;
    private boolean requiresAttention;
    
    public SystemHealthOverview() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public OverallHealth getOverallHealth() {
        return overallHealth;
    }
    
    public void setOverallHealth(OverallHealth overallHealth) {
        this.overallHealth = overallHealth;
    }
    
    public String getHealthSummary() {
        return healthSummary;
    }
    
    public void setHealthSummary(String healthSummary) {
        this.healthSummary = healthSummary;
    }
    
    public Map<ComponentType, HealthStatus> getComponentHealthMap() {
        return componentHealthMap;
    }
    
    public void setComponentHealthMap(Map<ComponentType, HealthStatus> componentHealthMap) {
        this.componentHealthMap = componentHealthMap;
    }
    
    public List<String> getCriticalIssues() {
        return criticalIssues;
    }
    
    public void setCriticalIssues(List<String> criticalIssues) {
        this.criticalIssues = criticalIssues;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public PerformanceMetrics getCurrentPerformance() {
        return currentPerformance;
    }
    
    public void setCurrentPerformance(PerformanceMetrics currentPerformance) {
        this.currentPerformance = currentPerformance;
    }
    
    public int getTotalActiveWorkflows() {
        return totalActiveWorkflows;
    }
    
    public void setTotalActiveWorkflows(int totalActiveWorkflows) {
        this.totalActiveWorkflows = totalActiveWorkflows;
    }
    
    public int getTotalStuckWorkflows() {
        return totalStuckWorkflows;
    }
    
    public void setTotalStuckWorkflows(int totalStuckWorkflows) {
        this.totalStuckWorkflows = totalStuckWorkflows;
    }
    
    public int getTotalOrphanedTasks() {
        return totalOrphanedTasks;
    }
    
    public void setTotalOrphanedTasks(int totalOrphanedTasks) {
        this.totalOrphanedTasks = totalOrphanedTasks;
    }
    
    public Double getSystemLoadPercentage() {
        return systemLoadPercentage;
    }
    
    public void setSystemLoadPercentage(Double systemLoadPercentage) {
        this.systemLoadPercentage = systemLoadPercentage;
    }
    
    public boolean isRequiresAttention() {
        return requiresAttention;
    }
    
    public void setRequiresAttention(boolean requiresAttention) {
        this.requiresAttention = requiresAttention;
    }
    
    @Override
    public String toString() {
        return "SystemHealthOverview{" +
                "timestamp=" + timestamp +
                ", overallHealth=" + overallHealth +
                ", totalActiveWorkflows=" + totalActiveWorkflows +
                ", totalStuckWorkflows=" + totalStuckWorkflows +
                ", totalOrphanedTasks=" + totalOrphanedTasks +
                ", systemLoadPercentage=" + systemLoadPercentage +
                ", requiresAttention=" + requiresAttention +
                '}';
    }
}