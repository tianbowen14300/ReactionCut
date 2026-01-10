package com.tbw.cut.workflow.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 资源隔离配置
 * 
 * 定义工作流执行时的资源隔离策略和限制
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceIsolationConfig {
    
    /**
     * 最大CPU使用率（百分比）
     */
    private double maxCpuUsage = 80.0;
    
    /**
     * 最大内存使用量（MB）
     */
    private long maxMemoryUsageMb = 1024;
    
    /**
     * 最大并发线程数
     */
    private int maxConcurrentThreads = 10;
    
    /**
     * 最大磁盘IO速率（MB/s）
     */
    private double maxDiskIoRate = 100.0;
    
    /**
     * 最大网络带宽（MB/s）
     */
    private double maxNetworkBandwidth = 50.0;
    
    /**
     * 资源监控间隔（毫秒）
     */
    private long monitoringIntervalMs = 5000;
    
    /**
     * 是否启用资源隔离
     */
    private boolean enabled = true;
    
    /**
     * 资源超限时的处理策略
     */
    private ResourceLimitStrategy limitStrategy = ResourceLimitStrategy.THROTTLE;
    
    /**
     * 资源超限阈值（百分比）
     */
    private double limitThreshold = 90.0;
    
    /**
     * 资源恢复阈值（百分比）
     */
    private double recoveryThreshold = 70.0;
    
    /**
     * 资源限制策略枚举
     */
    public enum ResourceLimitStrategy {
        /**
         * 节流 - 降低执行速度
         */
        THROTTLE,
        
        /**
         * 暂停 - 暂停执行直到资源可用
         */
        PAUSE,
        
        /**
         * 终止 - 终止当前执行
         */
        TERMINATE,
        
        /**
         * 队列 - 将任务放入队列等待
         */
        QUEUE
    }
    
    /**
     * 验证配置的有效性
     * 
     * @return 配置是否有效
     */
    public boolean isValid() {
        return maxCpuUsage > 0 && maxCpuUsage <= 100 &&
               maxMemoryUsageMb > 0 &&
               maxConcurrentThreads > 0 &&
               maxDiskIoRate > 0 &&
               maxNetworkBandwidth > 0 &&
               monitoringIntervalMs > 0 &&
               limitThreshold > 0 && limitThreshold <= 100 &&
               recoveryThreshold > 0 && recoveryThreshold <= 100 &&
               recoveryThreshold < limitThreshold;
    }
    
    /**
     * 获取默认配置
     * 
     * @return 默认资源隔离配置
     */
    public static ResourceIsolationConfig getDefault() {
        return ResourceIsolationConfig.builder()
                .maxCpuUsage(80.0)
                .maxMemoryUsageMb(1024)
                .maxConcurrentThreads(10)
                .maxDiskIoRate(100.0)
                .maxNetworkBandwidth(50.0)
                .monitoringIntervalMs(5000)
                .enabled(true)
                .limitStrategy(ResourceLimitStrategy.THROTTLE)
                .limitThreshold(90.0)
                .recoveryThreshold(70.0)
                .build();
    }
    
    /**
     * 获取高性能配置
     * 
     * @return 高性能资源隔离配置
     */
    public static ResourceIsolationConfig getHighPerformance() {
        return ResourceIsolationConfig.builder()
                .maxCpuUsage(95.0)
                .maxMemoryUsageMb(2048)
                .maxConcurrentThreads(20)
                .maxDiskIoRate(200.0)
                .maxNetworkBandwidth(100.0)
                .monitoringIntervalMs(3000)
                .enabled(true)
                .limitStrategy(ResourceLimitStrategy.THROTTLE)
                .limitThreshold(98.0)
                .recoveryThreshold(85.0)
                .build();
    }
    
    /**
     * 获取保守配置
     * 
     * @return 保守资源隔离配置
     */
    public static ResourceIsolationConfig getConservative() {
        return ResourceIsolationConfig.builder()
                .maxCpuUsage(60.0)
                .maxMemoryUsageMb(512)
                .maxConcurrentThreads(5)
                .maxDiskIoRate(50.0)
                .maxNetworkBandwidth(25.0)
                .monitoringIntervalMs(10000)
                .enabled(true)
                .limitStrategy(ResourceLimitStrategy.PAUSE)
                .limitThreshold(80.0)
                .recoveryThreshold(50.0)
                .build();
    }
}