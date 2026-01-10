package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 诊断报告
 * 包含完整的系统健康检查结果
 */
public class DiagnosticReport {
    
    private LocalDateTime timestamp;
    private OverallHealth overallHealth;
    private List<ComponentStatus> componentStatuses;
    private List<Issue> identifiedIssues;
    private List<Recommendation> recommendations;
    private PerformanceMetrics performanceMetrics;
    private String reportId;
    private Long executionTimeMs;
    
    public DiagnosticReport() {
        this.timestamp = LocalDateTime.now();
        this.reportId = generateReportId();
    }
    
    private String generateReportId() {
        return "DIAG-" + System.currentTimeMillis();
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
    
    public List<ComponentStatus> getComponentStatuses() {
        return componentStatuses;
    }
    
    public void setComponentStatuses(List<ComponentStatus> componentStatuses) {
        this.componentStatuses = componentStatuses;
    }
    
    public List<Issue> getIdentifiedIssues() {
        return identifiedIssues;
    }
    
    public void setIdentifiedIssues(List<Issue> identifiedIssues) {
        this.identifiedIssues = identifiedIssues;
    }
    
    public List<Recommendation> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }
    
    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }
    
    public String getReportId() {
        return reportId;
    }
    
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "DiagnosticReport{" +
                "reportId='" + reportId + '\'' +
                ", timestamp=" + timestamp +
                ", overallHealth=" + overallHealth +
                ", issuesCount=" + (identifiedIssues != null ? identifiedIssues.size() : 0) +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}