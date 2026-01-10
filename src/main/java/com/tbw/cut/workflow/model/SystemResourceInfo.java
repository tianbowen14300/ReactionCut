package com.tbw.cut.workflow.model;

import java.time.LocalDateTime;

/**
 * 系统资源信息模型
 * 
 * 包含CPU使用率、内存使用情况、磁盘空间等系统资源信息
 */
public class SystemResourceInfo {
    
    private LocalDateTime timestamp;
    private double cpuUsagePercent;
    private long totalMemoryMB;
    private long usedMemoryMB;
    private long freeMemoryMB;
    private double memoryUsagePercent;
    private long totalDiskSpaceGB;
    private long usedDiskSpaceGB;
    private long freeDiskSpaceGB;
    private double diskUsagePercent;
    private int activeThreadCount;
    private int maxThreadCount;
    private double threadUsagePercent;
    private int activeWorkflowCount;
    private double systemLoadAverage;
    
    // 构造函数
    public SystemResourceInfo() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SystemResourceInfo(double cpuUsagePercent, long totalMemoryMB, long usedMemoryMB,
                             long totalDiskSpaceGB, long usedDiskSpaceGB, int activeThreadCount,
                             int maxThreadCount, int activeWorkflowCount, double systemLoadAverage) {
        this();
        this.cpuUsagePercent = cpuUsagePercent;
        this.totalMemoryMB = totalMemoryMB;
        this.usedMemoryMB = usedMemoryMB;
        this.freeMemoryMB = totalMemoryMB - usedMemoryMB;
        this.memoryUsagePercent = (double) usedMemoryMB / totalMemoryMB * 100;
        this.totalDiskSpaceGB = totalDiskSpaceGB;
        this.usedDiskSpaceGB = usedDiskSpaceGB;
        this.freeDiskSpaceGB = totalDiskSpaceGB - usedDiskSpaceGB;
        this.diskUsagePercent = (double) usedDiskSpaceGB / totalDiskSpaceGB * 100;
        this.activeThreadCount = activeThreadCount;
        this.maxThreadCount = maxThreadCount;
        this.threadUsagePercent = (double) activeThreadCount / maxThreadCount * 100;
        this.activeWorkflowCount = activeWorkflowCount;
        this.systemLoadAverage = systemLoadAverage;
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }
    
    public void setCpuUsagePercent(double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }
    
    public long getTotalMemoryMB() {
        return totalMemoryMB;
    }
    
    public void setTotalMemoryMB(long totalMemoryMB) {
        this.totalMemoryMB = totalMemoryMB;
    }
    
    public long getUsedMemoryMB() {
        return usedMemoryMB;
    }
    
    public void setUsedMemoryMB(long usedMemoryMB) {
        this.usedMemoryMB = usedMemoryMB;
        this.freeMemoryMB = this.totalMemoryMB - usedMemoryMB;
        this.memoryUsagePercent = (double) usedMemoryMB / this.totalMemoryMB * 100;
    }
    
    public long getFreeMemoryMB() {
        return freeMemoryMB;
    }
    
    public void setFreeMemoryMB(long freeMemoryMB) {
        this.freeMemoryMB = freeMemoryMB;
    }
    
    public double getMemoryUsagePercent() {
        return memoryUsagePercent;
    }
    
    public void setMemoryUsagePercent(double memoryUsagePercent) {
        this.memoryUsagePercent = memoryUsagePercent;
    }
    
    public long getTotalDiskSpaceGB() {
        return totalDiskSpaceGB;
    }
    
    public void setTotalDiskSpaceGB(long totalDiskSpaceGB) {
        this.totalDiskSpaceGB = totalDiskSpaceGB;
    }
    
    public long getUsedDiskSpaceGB() {
        return usedDiskSpaceGB;
    }
    
    public void setUsedDiskSpaceGB(long usedDiskSpaceGB) {
        this.usedDiskSpaceGB = usedDiskSpaceGB;
        this.freeDiskSpaceGB = this.totalDiskSpaceGB - usedDiskSpaceGB;
        this.diskUsagePercent = (double) usedDiskSpaceGB / this.totalDiskSpaceGB * 100;
    }
    
    public long getFreeDiskSpaceGB() {
        return freeDiskSpaceGB;
    }
    
    public void setFreeDiskSpaceGB(long freeDiskSpaceGB) {
        this.freeDiskSpaceGB = freeDiskSpaceGB;
    }
    
    public double getDiskUsagePercent() {
        return diskUsagePercent;
    }
    
    public void setDiskUsagePercent(double diskUsagePercent) {
        this.diskUsagePercent = diskUsagePercent;
    }
    
    public int getActiveThreadCount() {
        return activeThreadCount;
    }
    
    public void setActiveThreadCount(int activeThreadCount) {
        this.activeThreadCount = activeThreadCount;
        if (this.maxThreadCount > 0) {
            this.threadUsagePercent = (double) activeThreadCount / this.maxThreadCount * 100;
        }
    }
    
    public int getMaxThreadCount() {
        return maxThreadCount;
    }
    
    public void setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
        if (maxThreadCount > 0) {
            this.threadUsagePercent = (double) this.activeThreadCount / maxThreadCount * 100;
        }
    }
    
    public double getThreadUsagePercent() {
        return threadUsagePercent;
    }
    
    public void setThreadUsagePercent(double threadUsagePercent) {
        this.threadUsagePercent = threadUsagePercent;
    }
    
    public int getActiveWorkflowCount() {
        return activeWorkflowCount;
    }
    
    public void setActiveWorkflowCount(int activeWorkflowCount) {
        this.activeWorkflowCount = activeWorkflowCount;
    }
    
    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }
    
    public void setSystemLoadAverage(double systemLoadAverage) {
        this.systemLoadAverage = systemLoadAverage;
    }
    
    /**
     * 检查资源是否充足
     * 
     * @param threshold 资源阈值
     * @return true 如果资源充足，false 如果资源不足
     */
    public boolean isResourceSufficient(ResourceThreshold threshold) {
        return cpuUsagePercent < threshold.getCpuThresholdPercent() &&
               memoryUsagePercent < threshold.getMemoryThresholdPercent() &&
               diskUsagePercent < threshold.getDiskThresholdPercent() &&
               threadUsagePercent < threshold.getThreadThresholdPercent();
    }
    
    /**
     * 检查是否处于高负载状态
     * 
     * @param threshold 资源阈值
     * @return true 如果处于高负载状态，false 如果负载正常
     */
    public boolean isHighLoad(ResourceThreshold threshold) {
        return cpuUsagePercent > threshold.getHighLoadCpuPercent() ||
               memoryUsagePercent > threshold.getHighLoadMemoryPercent() ||
               diskUsagePercent > threshold.getHighLoadDiskPercent() ||
               threadUsagePercent > threshold.getHighLoadThreadPercent();
    }
    
    /**
     * 计算资源压力分数（0-100，越高表示压力越大）
     * 
     * @return 资源压力分数
     */
    public double getResourcePressureScore() {
        // 加权计算各项资源的压力分数
        double cpuWeight = 0.3;
        double memoryWeight = 0.3;
        double diskWeight = 0.2;
        double threadWeight = 0.2;
        
        return cpuUsagePercent * cpuWeight +
               memoryUsagePercent * memoryWeight +
               diskUsagePercent * diskWeight +
               threadUsagePercent * threadWeight;
    }
    
    @Override
    public String toString() {
        return String.format(
            "SystemResourceInfo{timestamp=%s, CPU=%.1f%%, Memory=%d/%dMB(%.1f%%), " +
            "Disk=%d/%dGB(%.1f%%), Threads=%d/%d(%.1f%%), ActiveWorkflows=%d, LoadAvg=%.2f}",
            timestamp, cpuUsagePercent, usedMemoryMB, totalMemoryMB, memoryUsagePercent,
            usedDiskSpaceGB, totalDiskSpaceGB, diskUsagePercent,
            activeThreadCount, maxThreadCount, threadUsagePercent,
            activeWorkflowCount, systemLoadAverage
        );
    }
}