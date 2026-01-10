package com.tbw.cut.workflow.model;

/**
 * 资源阈值配置模型
 * 
 * 定义系统资源使用的各种阈值，用于判断资源可用性和负载状态
 */
public class ResourceThreshold {
    
    // 资源可用性阈值（超过此阈值认为资源不足）
    private double cpuThresholdPercent = 80.0;
    private double memoryThresholdPercent = 85.0;
    private double diskThresholdPercent = 90.0;
    private double threadThresholdPercent = 80.0;
    
    // 高负载阈值（超过此阈值认为系统处于高负载状态）
    private double highLoadCpuPercent = 90.0;
    private double highLoadMemoryPercent = 95.0;
    private double highLoadDiskPercent = 95.0;
    private double highLoadThreadPercent = 90.0;
    
    // 并发控制参数
    private int minConcurrency = 1;
    private int maxConcurrency = 10;
    private int defaultConcurrency = 3;
    
    // 监控参数
    private int monitoringIntervalSeconds = 30;
    private int historyRetentionMinutes = 60;
    
    // 构造函数
    public ResourceThreshold() {
    }
    
    public ResourceThreshold(double cpuThresholdPercent, double memoryThresholdPercent,
                           double diskThresholdPercent, double threadThresholdPercent) {
        this.cpuThresholdPercent = cpuThresholdPercent;
        this.memoryThresholdPercent = memoryThresholdPercent;
        this.diskThresholdPercent = diskThresholdPercent;
        this.threadThresholdPercent = threadThresholdPercent;
    }
    
    // Getters and Setters
    public double getCpuThresholdPercent() {
        return cpuThresholdPercent;
    }
    
    public void setCpuThresholdPercent(double cpuThresholdPercent) {
        this.cpuThresholdPercent = cpuThresholdPercent;
    }
    
    public double getMemoryThresholdPercent() {
        return memoryThresholdPercent;
    }
    
    public void setMemoryThresholdPercent(double memoryThresholdPercent) {
        this.memoryThresholdPercent = memoryThresholdPercent;
    }
    
    public double getDiskThresholdPercent() {
        return diskThresholdPercent;
    }
    
    public void setDiskThresholdPercent(double diskThresholdPercent) {
        this.diskThresholdPercent = diskThresholdPercent;
    }
    
    public double getThreadThresholdPercent() {
        return threadThresholdPercent;
    }
    
    public void setThreadThresholdPercent(double threadThresholdPercent) {
        this.threadThresholdPercent = threadThresholdPercent;
    }
    
    public double getHighLoadCpuPercent() {
        return highLoadCpuPercent;
    }
    
    public void setHighLoadCpuPercent(double highLoadCpuPercent) {
        this.highLoadCpuPercent = highLoadCpuPercent;
    }
    
    public double getHighLoadMemoryPercent() {
        return highLoadMemoryPercent;
    }
    
    public void setHighLoadMemoryPercent(double highLoadMemoryPercent) {
        this.highLoadMemoryPercent = highLoadMemoryPercent;
    }
    
    public double getHighLoadDiskPercent() {
        return highLoadDiskPercent;
    }
    
    public void setHighLoadDiskPercent(double highLoadDiskPercent) {
        this.highLoadDiskPercent = highLoadDiskPercent;
    }
    
    public double getHighLoadThreadPercent() {
        return highLoadThreadPercent;
    }
    
    public void setHighLoadThreadPercent(double highLoadThreadPercent) {
        this.highLoadThreadPercent = highLoadThreadPercent;
    }
    
    public int getMinConcurrency() {
        return minConcurrency;
    }
    
    public void setMinConcurrency(int minConcurrency) {
        this.minConcurrency = minConcurrency;
    }
    
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
    
    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
    
    public int getDefaultConcurrency() {
        return defaultConcurrency;
    }
    
    public void setDefaultConcurrency(int defaultConcurrency) {
        this.defaultConcurrency = defaultConcurrency;
    }
    
    public int getMonitoringIntervalSeconds() {
        return monitoringIntervalSeconds;
    }
    
    public void setMonitoringIntervalSeconds(int monitoringIntervalSeconds) {
        this.monitoringIntervalSeconds = monitoringIntervalSeconds;
    }
    
    public int getHistoryRetentionMinutes() {
        return historyRetentionMinutes;
    }
    
    public void setHistoryRetentionMinutes(int historyRetentionMinutes) {
        this.historyRetentionMinutes = historyRetentionMinutes;
    }
    
    /**
     * 创建默认的资源阈值配置
     * 
     * @return 默认资源阈值配置
     */
    public static ResourceThreshold createDefault() {
        ResourceThreshold threshold = new ResourceThreshold();
        threshold.setCpuThresholdPercent(80.0);
        threshold.setMemoryThresholdPercent(85.0);
        threshold.setDiskThresholdPercent(90.0);
        threshold.setThreadThresholdPercent(80.0);
        threshold.setHighLoadCpuPercent(90.0);
        threshold.setHighLoadMemoryPercent(95.0);
        threshold.setHighLoadDiskPercent(95.0);
        threshold.setHighLoadThreadPercent(90.0);
        threshold.setMinConcurrency(1);
        threshold.setMaxConcurrency(10);
        threshold.setDefaultConcurrency(3);
        threshold.setMonitoringIntervalSeconds(30);
        threshold.setHistoryRetentionMinutes(60);
        return threshold;
    }
    
    /**
     * 创建保守的资源阈值配置（更低的阈值，更安全）
     * 
     * @return 保守的资源阈值配置
     */
    public static ResourceThreshold createConservative() {
        ResourceThreshold threshold = new ResourceThreshold();
        threshold.setCpuThresholdPercent(70.0);
        threshold.setMemoryThresholdPercent(75.0);
        threshold.setDiskThresholdPercent(85.0);
        threshold.setThreadThresholdPercent(70.0);
        threshold.setHighLoadCpuPercent(80.0);
        threshold.setHighLoadMemoryPercent(85.0);
        threshold.setHighLoadDiskPercent(90.0);
        threshold.setHighLoadThreadPercent(80.0);
        threshold.setMinConcurrency(1);
        threshold.setMaxConcurrency(5);
        threshold.setDefaultConcurrency(2);
        threshold.setMonitoringIntervalSeconds(15);
        threshold.setHistoryRetentionMinutes(120);
        return threshold;
    }
    
    /**
     * 创建激进的资源阈值配置（更高的阈值，更高的性能）
     * 
     * @return 激进的资源阈值配置
     */
    public static ResourceThreshold createAggressive() {
        ResourceThreshold threshold = new ResourceThreshold();
        threshold.setCpuThresholdPercent(90.0);
        threshold.setMemoryThresholdPercent(90.0);
        threshold.setDiskThresholdPercent(95.0);
        threshold.setThreadThresholdPercent(90.0);
        threshold.setHighLoadCpuPercent(95.0);
        threshold.setHighLoadMemoryPercent(98.0);
        threshold.setHighLoadDiskPercent(98.0);
        threshold.setHighLoadThreadPercent(95.0);
        threshold.setMinConcurrency(2);
        threshold.setMaxConcurrency(20);
        threshold.setDefaultConcurrency(5);
        threshold.setMonitoringIntervalSeconds(60);
        threshold.setHistoryRetentionMinutes(30);
        return threshold;
    }
    
    /**
     * 验证阈值配置的有效性
     * 
     * @return true 如果配置有效，false 如果配置无效
     */
    public boolean isValid() {
        return cpuThresholdPercent > 0 && cpuThresholdPercent <= 100 &&
               memoryThresholdPercent > 0 && memoryThresholdPercent <= 100 &&
               diskThresholdPercent > 0 && diskThresholdPercent <= 100 &&
               threadThresholdPercent > 0 && threadThresholdPercent <= 100 &&
               highLoadCpuPercent >= cpuThresholdPercent &&
               highLoadMemoryPercent >= memoryThresholdPercent &&
               highLoadDiskPercent >= diskThresholdPercent &&
               highLoadThreadPercent >= threadThresholdPercent &&
               minConcurrency > 0 && maxConcurrency >= minConcurrency &&
               defaultConcurrency >= minConcurrency && defaultConcurrency <= maxConcurrency &&
               monitoringIntervalSeconds > 0 && historyRetentionMinutes > 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ResourceThreshold{CPU=%.1f%%(%.1f%%), Memory=%.1f%%(%.1f%%), " +
            "Disk=%.1f%%(%.1f%%), Thread=%.1f%%(%.1f%%), Concurrency=%d-%d(%d), " +
            "Monitor=%ds, History=%dm}",
            cpuThresholdPercent, highLoadCpuPercent,
            memoryThresholdPercent, highLoadMemoryPercent,
            diskThresholdPercent, highLoadDiskPercent,
            threadThresholdPercent, highLoadThreadPercent,
            minConcurrency, maxConcurrency, defaultConcurrency,
            monitoringIntervalSeconds, historyRetentionMinutes
        );
    }
}