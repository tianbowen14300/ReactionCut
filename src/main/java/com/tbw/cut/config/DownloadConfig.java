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
}