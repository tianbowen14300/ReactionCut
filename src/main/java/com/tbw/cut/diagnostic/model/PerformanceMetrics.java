package com.tbw.cut.diagnostic.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 性能指标
 */
public class PerformanceMetrics {
    
    private LocalDateTime timestamp;
    private Double cpuUsagePercentage;
    private Long memoryUsedMB;
    private Long memoryTotalMB;
    private Double memoryUsagePercentage;
    private Long diskUsedGB;
    private Long diskTotalGB;
    private Double diskUsagePercentage;
    private Integer activeThreads;
    private Integer totalThreads;
    private Long networkInKB;
    private Long networkOutKB;
    private Map<String, Object> customMetrics;
    private Long responseTimeMs;
    private Integer throughputPerSecond;
    private Integer errorRate;
    
    public PerformanceMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Double getCpuUsagePercentage() {
        return cpuUsagePercentage;
    }
    
    public void setCpuUsagePercentage(Double cpuUsagePercentage) {
        this.cpuUsagePercentage = cpuUsagePercentage;
    }
    
    public Long getMemoryUsedMB() {
        return memoryUsedMB;
    }
    
    public void setMemoryUsedMB(Long memoryUsedMB) {
        this.memoryUsedMB = memoryUsedMB;
    }
    
    public Long getMemoryTotalMB() {
        return memoryTotalMB;
    }
    
    public void setMemoryTotalMB(Long memoryTotalMB) {
        this.memoryTotalMB = memoryTotalMB;
    }
    
    public Double getMemoryUsagePercentage() {
        return memoryUsagePercentage;
    }
    
    public void setMemoryUsagePercentage(Double memoryUsagePercentage) {
        this.memoryUsagePercentage = memoryUsagePercentage;
    }
    
    public Long getDiskUsedGB() {
        return diskUsedGB;
    }
    
    public void setDiskUsedGB(Long diskUsedGB) {
        this.diskUsedGB = diskUsedGB;
    }
    
    public Long getDiskTotalGB() {
        return diskTotalGB;
    }
    
    public void setDiskTotalGB(Long diskTotalGB) {
        this.diskTotalGB = diskTotalGB;
    }
    
    public Double getDiskUsagePercentage() {
        return diskUsagePercentage;
    }
    
    public void setDiskUsagePercentage(Double diskUsagePercentage) {
        this.diskUsagePercentage = diskUsagePercentage;
    }
    
    public Integer getActiveThreads() {
        return activeThreads;
    }
    
    public void setActiveThreads(Integer activeThreads) {
        this.activeThreads = activeThreads;
    }
    
    public Integer getTotalThreads() {
        return totalThreads;
    }
    
    public void setTotalThreads(Integer totalThreads) {
        this.totalThreads = totalThreads;
    }
    
    public Long getNetworkInKB() {
        return networkInKB;
    }
    
    public void setNetworkInKB(Long networkInKB) {
        this.networkInKB = networkInKB;
    }
    
    public Long getNetworkOutKB() {
        return networkOutKB;
    }
    
    public void setNetworkOutKB(Long networkOutKB) {
        this.networkOutKB = networkOutKB;
    }
    
    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }
    
    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public Integer getThroughputPerSecond() {
        return throughputPerSecond;
    }
    
    public void setThroughputPerSecond(Integer throughputPerSecond) {
        this.throughputPerSecond = throughputPerSecond;
    }
    
    public Integer getErrorRate() {
        return errorRate;
    }
    
    public void setErrorRate(Integer errorRate) {
        this.errorRate = errorRate;
    }
    
    @Override
    public String toString() {
        return "PerformanceMetrics{" +
                "timestamp=" + timestamp +
                ", cpuUsagePercentage=" + cpuUsagePercentage +
                ", memoryUsagePercentage=" + memoryUsagePercentage +
                ", diskUsagePercentage=" + diskUsagePercentage +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}