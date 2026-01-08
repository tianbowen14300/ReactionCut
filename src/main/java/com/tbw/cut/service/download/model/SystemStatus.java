package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统状态模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatus {
    
    /**
     * 活跃下载数量
     */
    private int activeDownloads;
    
    /**
     * 队列中的下载数量
     */
    private int queuedDownloads;
    
    /**
     * 系统资源信息
     */
    private SystemResourceInfo systemResources;
    
    /**
     * 系统资源信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemResourceInfo {
        
        /**
         * CPU使用率（百分比）
         */
        private double cpuUsage;
        
        /**
         * 内存使用率（百分比）
         */
        private double memoryUsage;
        
        /**
         * 磁盘使用率（百分比）
         */
        private double diskUsage;
        
        /**
         * 可用内存（MB）
         */
        private long availableMemoryMB;
        
        /**
         * 可用磁盘空间（GB）
         */
        private long availableDiskSpaceGB;
        
        /**
         * 网络带宽使用率（百分比）
         */
        private double networkUsage;
        
        /**
         * 当前并发数
         */
        private int currentConcurrency;
        
        /**
         * 最大并发数
         */
        private int maxConcurrency;
    }
}