package com.tbw.cut.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 下载功能配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "download")
public class DownloadConfig {
    
    /**
     * 最大并发任务数
     */
    private int maxConcurrentTasks = 3;
    
    /**
     * 最大并发分P数
     */
    private int maxConcurrentParts = 2;
    
    /**
     * 下载超时时间（秒）
     */
    private int timeoutSeconds = 300;
    
    /**
     * 最大重试次数
     */
    private int maxRetryAttempts = 3;
    
    /**
     * 是否启用断点续传
     */
    private boolean enableResume = true;
    
    /**
     * 是否启用增强下载功能
     */
    private boolean enableEnhanced = true;
    
    /**
     * 资源监控间隔（秒）
     */
    private int resourceMonitorInterval = 30;
    
    /**
     * CPU使用率阈值（百分比）
     */
    private double cpuThreshold = 80.0;
    
    /**
     * 内存使用率阈值（百分比）
     */
    private double memoryThreshold = 85.0;
    
    /**
     * 磁盘使用率阈值（百分比）
     */
    private double diskThreshold = 90.0;
    
    /**
     * 线程数（兼容旧配置）
     */
    private int threads = 3;
    
    /**
     * 队列大小（兼容旧配置）
     */
    private int queueSize = 10;
    
    /**
     * 获取线程数（兼容方法）
     * @return 线程数
     */
    public int getThreads() {
        return threads > 0 ? threads : maxConcurrentTasks;
    }
    
    /**
     * 获取队列大小（兼容方法）
     * @return 队列大小
     */
    public int getQueueSize() {
        return queueSize > 0 ? queueSize : (maxConcurrentTasks * 2);
    }
    
    // ========== 新增配置项 ==========
    
    /**
     * 最大并发视频数量（视频级别控制）
     */
    private int maxConcurrentVideos = 2;
    
    /**
     * 视频下载队列容量
     */
    private int queueCapacity = 100;
    
    /**
     * 是否启用分段下载
     */
    private boolean enableSegmentedDownload = true;
    
    /**
     * 分段下载的最小文件大小（MB）
     */
    private long minSegmentFileSizeMB = 50;
    
    /**
     * 最大分段数量
     */
    private int maxSegments = 8;
    
    /**
     * 默认分段大小（MB）
     */
    private long defaultSegmentSizeMB = 10;
    
    /**
     * 网络带宽检测超时（秒）
     */
    private int bandwidthDetectionTimeout = 10;
    
    /**
     * 是否启用自适应线程数计算
     */
    private boolean enableAdaptiveThreads = true;
    
    /**
     * 线程数计算的基础因子
     */
    private double threadCalculationFactor = 1.5;
    
    /**
     * 性能监控历史数据保留天数
     */
    private int performanceHistoryDays = 7;
    
    // ========== WebSocket进度更新控制 ==========
    
    /**
     * WebSocket进度更新最小间隔（毫秒）
     */
    private long progressUpdateIntervalMs = 1000;
    
    /**
     * 进度变化最小阈值（百分比）
     */
    private int progressChangeThreshold = 5;
    
    /**
     * 是否启用进度更新节流
     */
    private boolean enableProgressThrottling = true;
    
    /**
     * 获取WebSocket进度更新最小间隔（毫秒）
     * @return 更新间隔
     */
    public long getProgressUpdateIntervalMs() {
        return progressUpdateIntervalMs;
    }
    
    /**
     * 获取进度变化最小阈值（百分比）
     * @return 进度变化阈值
     */
    public int getProgressChangeThreshold() {
        return progressChangeThreshold;
    }
    
    /**
     * 是否启用进度更新节流
     * @return 是否启用节流
     */
    public boolean isEnableProgressThrottling() {
        return enableProgressThrottling;
    }
    
    /**
     * 获取分段下载的最小文件大小（字节）
     * @return 最小文件大小
     */
    public long getMinSegmentFileSize() {
        return minSegmentFileSizeMB * 1024 * 1024;
    }
    
    /**
     * 获取默认分段大小（字节）
     * @return 分段大小
     */
    public long getDefaultSegmentSize() {
        return defaultSegmentSizeMB * 1024 * 1024;
    }
    
    /**
     * 获取是否启用分段下载
     * @return 是否启用分段下载
     */
    public boolean getEnableSegmentedDownload() {
        return enableSegmentedDownload;
    }
    
    /**
     * 是否启用分段下载（布尔值getter）
     * @return 是否启用分段下载
     */
    public boolean isEnableSegmentedDownload() {
        return enableSegmentedDownload;
    }
}