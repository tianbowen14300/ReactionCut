package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.service.ResourceMonitor;
import com.tbw.cut.workflow.model.SystemResourceInfo;
import com.tbw.cut.workflow.model.ResourceThreshold;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 系统资源监控服务实现类
 * 
 * 使用JMX API监控系统资源使用情况，提供实时的资源状态信息
 */
@Slf4j
@Service
public class ResourceMonitorImpl implements ResourceMonitor {
    
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final Runtime runtime;
    
    private ResourceThreshold resourceThreshold;
    private final List<ResourceChangeListener> listeners;
    private final Deque<SystemResourceInfo> resourceHistory;
    private final ScheduledExecutorService monitoringExecutor;
    
    private volatile boolean monitoring = false;
    private volatile SystemResourceInfo lastResourceInfo;
    private volatile boolean lastHighLoadState = false;
    
    public ResourceMonitorImpl() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.runtime = Runtime.getRuntime();
        
        this.resourceThreshold = ResourceThreshold.createDefault();
        this.listeners = new CopyOnWriteArrayList<>();
        this.resourceHistory = new ConcurrentLinkedDeque<>();
        this.monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ResourceMonitor");
            t.setDaemon(true);
            return t;
        });
    }
    
    @PostConstruct
    public void init() {
        log.info("初始化资源监控服务，阈值配置: {}", resourceThreshold);
        startMonitoring();
    }
    
    @PreDestroy
    public void destroy() {
        stopMonitoring();
        monitoringExecutor.shutdown();
        try {
            if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitoringExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public SystemResourceInfo getCurrentResourceInfo() {
        try {
            // 获取CPU使用率
            double cpuUsage = getCpuUsage();
            
            // 获取内存信息
            long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
            long freeMemory = runtime.freeMemory() / (1024 * 1024); // MB
            long usedMemory = totalMemory - freeMemory;
            
            // 获取磁盘信息
            File root = new File("/");
            long totalDiskSpace = root.getTotalSpace() / (1024 * 1024 * 1024); // GB
            long freeDiskSpace = root.getFreeSpace() / (1024 * 1024 * 1024); // GB
            long usedDiskSpace = totalDiskSpace - freeDiskSpace;
            
            // 获取线程信息
            int activeThreadCount = threadBean.getThreadCount();
            int maxThreadCount = getMaxThreadCount();
            
            // 获取活跃工作流数量（这里需要从工作流引擎获取，暂时使用估算值）
            int activeWorkflowCount = estimateActiveWorkflowCount();
            
            // 获取系统负载平均值
            double systemLoadAverage = osBean.getSystemLoadAverage();
            
            return new SystemResourceInfo(
                cpuUsage, totalMemory, usedMemory,
                totalDiskSpace, usedDiskSpace,
                activeThreadCount, maxThreadCount,
                activeWorkflowCount, systemLoadAverage
            );
        } catch (Exception e) {
            log.error("获取系统资源信息时发生异常", e);
            return createFallbackResourceInfo();
        }
    }
    
    @Override
    public boolean isResourceAvailable() {
        SystemResourceInfo resourceInfo = getCurrentResourceInfo();
        return resourceInfo.isResourceSufficient(resourceThreshold);
    }
    
    @Override
    public boolean isHighLoad() {
        SystemResourceInfo resourceInfo = getCurrentResourceInfo();
        return resourceInfo.isHighLoad(resourceThreshold);
    }
    
    @Override
    public int getRecommendedConcurrency() {
        SystemResourceInfo resourceInfo = getCurrentResourceInfo();
        double pressureScore = resourceInfo.getResourcePressureScore();
        
        // 根据资源压力分数计算建议的并发数量
        int recommendedConcurrency;
        
        if (pressureScore < 50) {
            // 低压力：使用最大并发数
            recommendedConcurrency = resourceThreshold.getMaxConcurrency();
        } else if (pressureScore < 70) {
            // 中等压力：使用默认并发数
            recommendedConcurrency = resourceThreshold.getDefaultConcurrency();
        } else if (pressureScore < 85) {
            // 高压力：减少并发数
            recommendedConcurrency = Math.max(
                resourceThreshold.getMinConcurrency(),
                resourceThreshold.getDefaultConcurrency() / 2
            );
        } else {
            // 极高压力：使用最小并发数
            recommendedConcurrency = resourceThreshold.getMinConcurrency();
        }
        
        log.debug("资源压力分数: {}, 建议并发数: {}", pressureScore, recommendedConcurrency);
        return recommendedConcurrency;
    }
    
    @Override
    public void setResourceThreshold(ResourceThreshold threshold) {
        if (threshold != null && threshold.isValid()) {
            this.resourceThreshold = threshold;
            log.info("更新资源阈值配置: {}", threshold);
            
            // 重启监控以应用新的配置
            if (monitoring) {
                stopMonitoring();
                startMonitoring();
            }
        } else {
            log.warn("无效的资源阈值配置，保持当前配置不变");
        }
    }
    
    @Override
    public ResourceThreshold getResourceThreshold() {
        return resourceThreshold;
    }
    
    @Override
    public void startMonitoring() {
        if (!monitoring) {
            monitoring = true;
            monitoringExecutor.scheduleAtFixedRate(
                this::collectResourceInfo,
                0,
                resourceThreshold.getMonitoringIntervalSeconds(),
                TimeUnit.SECONDS
            );
            log.info("资源监控已启动，监控间隔: {}秒", resourceThreshold.getMonitoringIntervalSeconds());
        }
    }
    
    @Override
    public void stopMonitoring() {
        if (monitoring) {
            monitoring = false;
            log.info("资源监控已停止");
        }
    }
    
    @Override
    public List<SystemResourceInfo> getResourceHistory(int minutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutes);
        return resourceHistory.stream()
                .filter(info -> info.getTimestamp().isAfter(cutoffTime))
                .sorted(Comparator.comparing(SystemResourceInfo::getTimestamp))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    @Override
    public void addResourceChangeListener(ResourceChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
            log.debug("添加资源状态变化监听器: {}", listener.getClass().getSimpleName());
        }
    }
    
    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            log.debug("移除资源状态变化监听器: {}", listener.getClass().getSimpleName());
        }
    }
    
    /**
     * 定期收集资源信息
     */
    private void collectResourceInfo() {
        try {
            SystemResourceInfo currentInfo = getCurrentResourceInfo();
            SystemResourceInfo oldInfo = lastResourceInfo;
            
            // 更新历史记录
            resourceHistory.addLast(currentInfo);
            cleanupOldHistory();
            
            // 检查高负载状态变化
            boolean currentHighLoadState = currentInfo.isHighLoad(resourceThreshold);
            if (currentHighLoadState != lastHighLoadState) {
                if (currentHighLoadState) {
                    notifyHighLoadDetected(currentInfo);
                } else {
                    notifyLoadRecovered(currentInfo);
                }
                lastHighLoadState = currentHighLoadState;
            }
            
            // 通知资源状态变化
            if (oldInfo != null) {
                notifyResourceChanged(oldInfo, currentInfo);
            }
            
            lastResourceInfo = currentInfo;
            
            log.debug("收集资源信息: {}", currentInfo);
        } catch (Exception e) {
            log.error("收集资源信息时发生异常", e);
        }
    }
    
    /**
     * 清理过期的历史记录
     */
    private void cleanupOldHistory() {
        LocalDateTime cutoffTime = LocalDateTime.now()
                .minusMinutes(resourceThreshold.getHistoryRetentionMinutes());
        
        while (!resourceHistory.isEmpty() && 
               resourceHistory.peekFirst().getTimestamp().isBefore(cutoffTime)) {
            resourceHistory.removeFirst();
        }
    }
    
    /**
     * 获取CPU使用率
     */
    private double getCpuUsage() {
        try {
            // 尝试使用反射获取更精确的CPU使用率
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
                return cpuUsage >= 0 ? cpuUsage : 0;
            }
        } catch (Exception e) {
            log.debug("无法获取精确的CPU使用率，使用估算值", e);
        }
        
        // 使用系统负载平均值作为CPU使用率的估算
        double loadAverage = osBean.getSystemLoadAverage();
        if (loadAverage >= 0) {
            int availableProcessors = osBean.getAvailableProcessors();
            return Math.min(100, (loadAverage / availableProcessors) * 100);
        }
        
        return 0;
    }
    
    /**
     * 获取最大线程数
     */
    private int getMaxThreadCount() {
        // 使用JVM可用处理器数量的倍数作为最大线程数估算
        return osBean.getAvailableProcessors() * 50;
    }
    
    /**
     * 估算活跃工作流数量
     */
    private int estimateActiveWorkflowCount() {
        // TODO: 这里应该从工作流引擎获取真实的活跃工作流数量
        // 暂时使用线程数量的估算
        int activeThreads = threadBean.getThreadCount();
        int systemThreads = 20; // 估算的系统线程数量
        return Math.max(0, (activeThreads - systemThreads) / 5);
    }
    
    /**
     * 创建备用的资源信息（当获取真实信息失败时使用）
     */
    private SystemResourceInfo createFallbackResourceInfo() {
        return new SystemResourceInfo(
            0, 1024, 512, // 默认内存信息
            100, 50, // 默认磁盘信息
            10, 100, // 默认线程信息
            0, 0 // 默认工作流和负载信息
        );
    }
    
    /**
     * 通知资源状态变化
     */
    private void notifyResourceChanged(SystemResourceInfo oldInfo, SystemResourceInfo newInfo) {
        for (ResourceChangeListener listener : listeners) {
            try {
                listener.onResourceChanged(oldInfo, newInfo);
            } catch (Exception e) {
                log.error("通知资源状态变化时发生异常", e);
            }
        }
    }
    
    /**
     * 通知检测到高负载
     */
    private void notifyHighLoadDetected(SystemResourceInfo resourceInfo) {
        log.warn("检测到系统高负载状态: {}", resourceInfo);
        for (ResourceChangeListener listener : listeners) {
            try {
                listener.onHighLoadDetected(resourceInfo);
            } catch (Exception e) {
                log.error("通知高负载检测时发生异常", e);
            }
        }
    }
    
    /**
     * 通知负载恢复
     */
    private void notifyLoadRecovered(SystemResourceInfo resourceInfo) {
        log.info("系统负载已恢复正常: {}", resourceInfo);
        for (ResourceChangeListener listener : listeners) {
            try {
                listener.onLoadRecovered(resourceInfo);
            } catch (Exception e) {
                log.error("通知负载恢复时发生异常", e);
            }
        }
    }
}