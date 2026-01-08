package com.tbw.cut.service.download.resource;

import com.tbw.cut.service.download.concurrent.ConcurrentDownloadExecutor;
import com.tbw.cut.service.download.model.DownloadConfig;
import com.tbw.cut.service.download.model.SystemStatus.SystemResourceInfo;
import com.tbw.cut.service.download.model.VideoPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

/**
 * 资源监控器
 * 监控系统资源使用情况并动态调整下载策略
 */
@Slf4j
@Component
public class ResourceMonitor {
    
    @Value("${download.max-concurrent-tasks:3}")
    private int maxConcurrentTasks;
    
    @Value("${download.min-free-space-gb:1}")
    private long minFreeSpaceGb;
    
    @Value("${download.memory-threshold:0.8}")
    private double memoryThreshold;
    
    @Value("${download.cpu-threshold:0.8}")
    private double cpuThreshold;
    
    @Autowired
    private ConcurrentDownloadExecutor concurrentExecutor;
    
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    
    public ResourceMonitor() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }
    
    /**
     * 定期监控系统资源
     */
    @Scheduled(fixedDelay = 5000) // 每5秒检查一次
    public void monitorResources() {
        try {
            SystemResourceInfo resourceInfo = getCurrentResourceInfo();
            
            // 检查是否需要调整并发数
            adjustConcurrencyBasedOnResources(resourceInfo);
            
            // 检查磁盘空间
            checkDiskSpace(resourceInfo);
            
        } catch (Exception e) {
            log.error("Error monitoring system resources", e);
        }
    }
    
    /**
     * 检查是否有足够的系统资源
     * @param partsCount 分P数量
     * @param config 下载配置
     * @return 是否有足够资源
     */
    public boolean hasEnoughResources(int partsCount, DownloadConfig config) {
        SystemResourceInfo resourceInfo = getCurrentResourceInfo();
        
        // 检查内存使用率
        if (resourceInfo.getMemoryUsage() > memoryThreshold) {
            log.warn("Memory usage too high: {:.1f}%, rejecting download request", 
                resourceInfo.getMemoryUsage() * 100);
            return false;
        }
        
        // 检查CPU使用率
        if (resourceInfo.getCpuUsage() > cpuThreshold) {
            log.warn("CPU usage too high: {:.1f}%, rejecting download request", 
                resourceInfo.getCpuUsage() * 100);
            return false;
        }
        
        // 检查磁盘空间
        long requiredSpaceGB = estimateRequiredDiskSpace(partsCount) / (1024 * 1024 * 1024);
        if (resourceInfo.getAvailableDiskSpaceGB() < requiredSpaceGB) {
            log.warn("Insufficient disk space: required {} GB, available {} GB", 
                requiredSpaceGB,
                resourceInfo.getAvailableDiskSpaceGB());
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取当前系统资源信息
     * @return 系统资源信息
     */
    public SystemResourceInfo getCurrentResourceInfo() {
        // 获取CPU使用率
        double cpuUsage = getCpuUsage();
        
        // 获取内存使用率
        double memoryUsage = getMemoryUsage();
        
        // 获取可用磁盘空间（转换为GB）
        long availableDiskSpaceBytes = getAvailableDiskSpace();
        long availableDiskSpaceGB = availableDiskSpaceBytes / (1024 * 1024 * 1024);
        
        // 获取可用内存（转换为MB）
        long availableMemoryBytes = Runtime.getRuntime().freeMemory();
        long availableMemoryMB = availableMemoryBytes / (1024 * 1024);
        
        // 获取当前并发任务数
        int currentConcurrency = concurrentExecutor.getActiveTaskCount();
        
        return SystemResourceInfo.builder()
            .cpuUsage(cpuUsage)
            .memoryUsage(memoryUsage)
            .diskUsage(0.0) // 暂时不计算磁盘使用率
            .availableMemoryMB(availableMemoryMB)
            .availableDiskSpaceGB(availableDiskSpaceGB)
            .networkUsage(0.0) // 暂时不实现网络监控
            .currentConcurrency(currentConcurrency)
            .maxConcurrency(maxConcurrentTasks)
            .build();
    }
    
    /**
     * 根据资源使用情况调整并发数
     * @param resourceInfo 资源信息
     */
    private void adjustConcurrencyBasedOnResources(SystemResourceInfo resourceInfo) {
        int currentConcurrency = resourceInfo.getCurrentConcurrency();
        int newConcurrency = currentConcurrency;
        
        // 如果内存或CPU使用率过高，减少并发数
        if (resourceInfo.getMemoryUsage() > memoryThreshold || 
            resourceInfo.getCpuUsage() > cpuThreshold) {
            
            newConcurrency = Math.max(1, currentConcurrency - 1);
            log.info("High resource usage detected, reducing concurrency from {} to {}", 
                currentConcurrency, newConcurrency);
                
        } else if (resourceInfo.getMemoryUsage() < 0.5 && 
                   resourceInfo.getCpuUsage() < 0.5 && 
                   currentConcurrency < maxConcurrentTasks) {
            
            // 如果资源使用率较低，可以增加并发数
            newConcurrency = Math.min(maxConcurrentTasks, currentConcurrency + 1);
            log.info("Low resource usage detected, increasing concurrency from {} to {}", 
                currentConcurrency, newConcurrency);
        }
        
        if (newConcurrency != currentConcurrency) {
            concurrentExecutor.updateConcurrency(newConcurrency);
        }
    }
    
    /**
     * 检查磁盘空间
     * @param resourceInfo 资源信息
     */
    private void checkDiskSpace(SystemResourceInfo resourceInfo) {
        if (resourceInfo.getAvailableDiskSpaceGB() < minFreeSpaceGb) {
            log.warn("Low disk space detected: {} GB available, minimum required: {} GB", 
                resourceInfo.getAvailableDiskSpaceGB(),
                minFreeSpaceGb);
            
            // 暂停所有下载任务
            concurrentExecutor.pauseAllTasks();
            
            // 发布低磁盘空间事件
            publishLowDiskSpaceEvent(resourceInfo.getAvailableDiskSpaceGB() * 1024 * 1024 * 1024);
        }
    }
    
    /**
     * 获取CPU使用率
     * @return CPU使用率 (0.0 - 1.0)
     */
    private double getCpuUsage() {
        try {
            // 使用OperatingSystemMXBean获取CPU使用率
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuUsage = sunOsBean.getProcessCpuLoad();
                return cpuUsage >= 0 ? cpuUsage : 0.0;
            }
        } catch (Exception e) {
            log.debug("Failed to get CPU usage", e);
        }
        return 0.0;
    }
    
    /**
     * 获取内存使用率
     * @return 内存使用率 (0.0 - 1.0)
     */
    private double getMemoryUsage() {
        try {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() + 
                             memoryBean.getNonHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() + 
                            memoryBean.getNonHeapMemoryUsage().getMax();
            
            if (maxMemory > 0) {
                return (double) usedMemory / maxMemory;
            }
        } catch (Exception e) {
            log.debug("Failed to get memory usage", e);
        }
        return 0.0;
    }
    
    /**
     * 获取可用磁盘空间
     * @return 可用磁盘空间（字节）
     */
    private long getAvailableDiskSpace() {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            return tempDir.getFreeSpace();
        } catch (Exception e) {
            log.debug("Failed to get available disk space", e);
            return Long.MAX_VALUE; // 如果无法获取，假设有足够空间
        }
    }
    
    /**
     * 估算所需磁盘空间
     * @param partsCount 分P数量
     * @return 估算的磁盘空间需求（字节）
     */
    private long estimateRequiredDiskSpace(int partsCount) {
        // 假设每个分P平均需要500MB空间
        long avgPartSize = 500L * 1024 * 1024; // 500MB
        return partsCount * avgPartSize + (minFreeSpaceGb * 1024 * 1024 * 1024);
    }
    
    /**
     * 发布低磁盘空间事件
     * @param availableSpace 可用空间
     */
    private void publishLowDiskSpaceEvent(long availableSpace) {
        // 这里可以发布Spring事件或者调用通知服务
        log.error("Low disk space event: {} bytes available", availableSpace);
        // TODO: 实现事件发布机制
    }
}