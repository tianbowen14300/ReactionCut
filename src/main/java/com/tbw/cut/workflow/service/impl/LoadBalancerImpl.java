package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.service.LoadBalancer;
import com.tbw.cut.workflow.service.ResourceMonitor;
import com.tbw.cut.workflow.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 负载均衡服务实现类
 * 
 * 基于系统资源监控信息实现动态负载均衡，支持多种负载均衡策略
 */
@Slf4j
@Service
public class LoadBalancerImpl implements LoadBalancer, ResourceMonitor.ResourceChangeListener {
    
    @Autowired
    private ResourceMonitor resourceMonitor;
    
    private LoadBalancingStrategy currentStrategy;
    private final Map<String, ResourceAllocation> resourceAllocations;
    private final AtomicBoolean newWorkflowsPaused;
    private final AtomicInteger totalWorkflowsStarted;
    private final AtomicInteger totalWorkflowsRejected;
    private final AtomicInteger totalRebalanceOperations;
    private volatile int currentConcurrencyLimit;
    private volatile LocalDateTime lastRebalanceTime;
    
    public LoadBalancerImpl() {
        this.currentStrategy = LoadBalancingStrategy.getDefault();
        this.resourceAllocations = new ConcurrentHashMap<>();
        this.newWorkflowsPaused = new AtomicBoolean(false);
        this.totalWorkflowsStarted = new AtomicInteger(0);
        this.totalWorkflowsRejected = new AtomicInteger(0);
        this.totalRebalanceOperations = new AtomicInteger(0);
        this.currentConcurrencyLimit = 3; // 默认并发限制
        this.lastRebalanceTime = LocalDateTime.now();
    }
    
    @PostConstruct
    public void init() {
        // 注册资源监控监听器
        resourceMonitor.addResourceChangeListener(this);
        
        // 初始化并发限制
        this.currentConcurrencyLimit = resourceMonitor.getRecommendedConcurrency();
        
        log.info("负载均衡服务已初始化，策略: {}, 并发限制: {}", 
                currentStrategy.getDisplayName(), currentConcurrencyLimit);
    }
    
    @Override
    public boolean canStartNewWorkflow() {
        // 检查是否被暂停
        if (newWorkflowsPaused.get()) {
            log.debug("新工作流启动已被暂停");
            return false;
        }
        
        // 检查并发限制
        int currentActiveCount = resourceAllocations.size();
        if (currentActiveCount >= currentConcurrencyLimit) {
            log.debug("达到并发限制，当前活跃工作流: {}, 限制: {}", 
                    currentActiveCount, currentConcurrencyLimit);
            return false;
        }
        
        // 检查系统资源
        if (!resourceMonitor.isResourceAvailable()) {
            log.debug("系统资源不足，无法启动新工作流");
            return false;
        }
        
        return true;
    }
    
    @Override
    public int getRecommendedConcurrency() {
        return currentConcurrencyLimit;
    }
    
    @Override
    public ResourceAllocation allocateResources(WorkflowInstance workflowInstance) {
        if (!canStartNewWorkflow()) {
            totalWorkflowsRejected.incrementAndGet();
            log.warn("无法为工作流分配资源，工作流ID: {}", workflowInstance.getInstanceId());
            return null;
        }
        
        try {
            ResourceAllocation allocation = createResourceAllocation(workflowInstance);
            resourceAllocations.put(workflowInstance.getInstanceId(), allocation);
            totalWorkflowsStarted.incrementAndGet();
            
            log.info("为工作流分配资源成功: {}", allocation);
            return allocation;
        } catch (Exception e) {
            totalWorkflowsRejected.incrementAndGet();
            log.error("为工作流分配资源时发生异常，工作流ID: {}", 
                    workflowInstance.getInstanceId(), e);
            return null;
        }
    }
    
    @Override
    public void releaseResources(WorkflowInstance workflowInstance) {
        ResourceAllocation allocation = resourceAllocations.remove(workflowInstance.getInstanceId());
        if (allocation != null) {
            allocation.release();
            log.info("释放工作流资源: {}", allocation);
        } else {
            log.warn("未找到工作流的资源分配记录，工作流ID: {}", workflowInstance.getInstanceId());
        }
    }
    
    @Override
    public LoadBalancingResult rebalanceWorkflows(List<WorkflowInstance> activeWorkflows) {
        try {
            SystemResourceInfo resourceInfo = resourceMonitor.getCurrentResourceInfo();
            int adjustedCount = 0;
            int pausedCount = 0;
            int resumedCount = 0;
            
            // 根据当前资源状况调整策略
            LoadBalancingStrategy recommendedStrategy = LoadBalancingStrategy.recommend(
                resourceInfo.getResourcePressureScore(), activeWorkflows.size());
            
            if (recommendedStrategy != currentStrategy) {
                log.info("切换负载均衡策略: {} -> {}", 
                        currentStrategy.getDisplayName(), recommendedStrategy.getDisplayName());
                currentStrategy = recommendedStrategy;
                adjustedCount++;
            }
            
            // 更新并发限制
            int newConcurrencyLimit = resourceMonitor.getRecommendedConcurrency();
            if (newConcurrencyLimit != currentConcurrencyLimit) {
                log.info("调整并发限制: {} -> {}", currentConcurrencyLimit, newConcurrencyLimit);
                currentConcurrencyLimit = newConcurrencyLimit;
                adjustedCount++;
            }
            
            // 根据策略调整工作流资源分配
            switch (currentStrategy) {
                case RESOURCE_PRIORITY:
                    adjustedCount += rebalanceByPriority(activeWorkflows);
                    break;
                case LEAST_CONNECTIONS:
                    adjustedCount += rebalanceByLeastConnections(activeWorkflows);
                    break;
                case FAIR_SHARE:
                    adjustedCount += rebalanceByFairShare(activeWorkflows);
                    break;
                case ADAPTIVE:
                    adjustedCount += rebalanceAdaptively(activeWorkflows, resourceInfo);
                    break;
                default:
                    // 其他策略暂时不需要特殊处理
                    break;
            }
            
            // 检查是否需要暂停或恢复工作流
            if (resourceInfo.isHighLoad(resourceMonitor.getResourceThreshold())) {
                if (!newWorkflowsPaused.get()) {
                    pauseNewWorkflows();
                    pausedCount++;
                }
            } else {
                if (newWorkflowsPaused.get()) {
                    resumeNewWorkflows();
                    resumedCount++;
                }
            }
            
            totalRebalanceOperations.incrementAndGet();
            lastRebalanceTime = LocalDateTime.now();
            
            String description = String.format(
                "重新平衡完成，策略: %s, 并发限制: %d, 资源压力: %.1f%%",
                currentStrategy.getDisplayName(), currentConcurrencyLimit,
                resourceInfo.getResourcePressureScore()
            );
            
            LoadBalancingResult result = new LoadBalancingResult(
                adjustedCount, pausedCount, resumedCount, description);
            
            log.info("负载均衡重新平衡结果: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("重新平衡工作流时发生异常", e);
            return new LoadBalancingResult(0, 0, 0, "重新平衡失败: " + e.getMessage());
        }
    }
    
    @Override
    public void setLoadBalancingStrategy(LoadBalancingStrategy strategy) {
        if (strategy != null) {
            LoadBalancingStrategy oldStrategy = this.currentStrategy;
            this.currentStrategy = strategy;
            log.info("负载均衡策略已更新: {} -> {}", 
                    oldStrategy.getDisplayName(), strategy.getDisplayName());
        }
    }
    
    @Override
    public LoadBalancingStrategy getLoadBalancingStrategy() {
        return currentStrategy;
    }
    
    @Override
    public LoadBalancingStats getLoadBalancingStats() {
        SystemResourceInfo resourceInfo = resourceMonitor.getCurrentResourceInfo();
        double averageUtilization = resourceInfo.getResourcePressureScore();
        
        return new LoadBalancingStats(
            totalWorkflowsStarted.get(),
            totalWorkflowsRejected.get(),
            totalRebalanceOperations.get(),
            resourceAllocations.size(),
            currentConcurrencyLimit,
            averageUtilization,
            lastRebalanceTime
        );
    }
    
    @Override
    public void pauseNewWorkflows() {
        if (newWorkflowsPaused.compareAndSet(false, true)) {
            log.warn("已暂停新工作流的启动");
        }
    }
    
    @Override
    public void resumeNewWorkflows() {
        if (newWorkflowsPaused.compareAndSet(true, false)) {
            log.info("已恢复新工作流的启动");
        }
    }
    
    @Override
    public boolean isNewWorkflowsPaused() {
        return newWorkflowsPaused.get();
    }
    
    // ==================== ResourceChangeListener 实现 ====================
    
    @Override
    public void onResourceChanged(SystemResourceInfo oldInfo, SystemResourceInfo newInfo) {
        // 当资源状态发生显著变化时，考虑调整并发限制
        double oldPressure = oldInfo.getResourcePressureScore();
        double newPressure = newInfo.getResourcePressureScore();
        
        if (Math.abs(newPressure - oldPressure) > 10) {
            int newConcurrencyLimit = resourceMonitor.getRecommendedConcurrency();
            if (newConcurrencyLimit != currentConcurrencyLimit) {
                log.info("根据资源变化调整并发限制: {} -> {} (压力变化: {:.1f}% -> {:.1f}%)",
                        currentConcurrencyLimit, newConcurrencyLimit, oldPressure, newPressure);
                currentConcurrencyLimit = newConcurrencyLimit;
            }
        }
    }
    
    @Override
    public void onHighLoadDetected(SystemResourceInfo resourceInfo) {
        log.warn("检测到高负载，暂停新工作流启动，资源信息: {}", resourceInfo);
        pauseNewWorkflows();
        
        // 降低并发限制
        int reducedLimit = Math.max(1, currentConcurrencyLimit / 2);
        if (reducedLimit != currentConcurrencyLimit) {
            log.warn("由于高负载降低并发限制: {} -> {}", currentConcurrencyLimit, reducedLimit);
            currentConcurrencyLimit = reducedLimit;
        }
    }
    
    @Override
    public void onLoadRecovered(SystemResourceInfo resourceInfo) {
        log.info("系统负载已恢复，恢复新工作流启动，资源信息: {}", resourceInfo);
        resumeNewWorkflows();
        
        // 恢复正常并发限制
        int normalLimit = resourceMonitor.getRecommendedConcurrency();
        if (normalLimit != currentConcurrencyLimit) {
            log.info("负载恢复后调整并发限制: {} -> {}", currentConcurrencyLimit, normalLimit);
            currentConcurrencyLimit = normalLimit;
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 为工作流实例创建资源分配
     */
    private ResourceAllocation createResourceAllocation(WorkflowInstance workflowInstance) {
        SystemResourceInfo resourceInfo = resourceMonitor.getCurrentResourceInfo();
        
        // 根据系统资源状况选择分配策略
        ResourceAllocation allocation;
        if (resourceInfo.getResourcePressureScore() < 50) {
            // 低压力：分配高性能资源
            allocation = ResourceAllocation.createHighPerformance(
                workflowInstance.getInstanceId(), workflowInstance.getTaskId());
        } else if (resourceInfo.getResourcePressureScore() < 80) {
            // 中等压力：分配默认资源
            allocation = ResourceAllocation.createDefault(
                workflowInstance.getInstanceId(), workflowInstance.getTaskId());
        } else {
            // 高压力：分配轻量级资源
            allocation = ResourceAllocation.createLightweight(
                workflowInstance.getInstanceId(), workflowInstance.getTaskId());
        }
        
        allocation.setAllocationReason(String.format(
            "策略: %s, 资源压力: %.1f%%", 
            currentStrategy.getDisplayName(), resourceInfo.getResourcePressureScore()));
        
        return allocation;
    }
    
    /**
     * 按优先级重新平衡
     */
    private int rebalanceByPriority(List<WorkflowInstance> activeWorkflows) {
        // 按优先级排序，高优先级的工作流获得更多资源
        List<WorkflowInstance> sortedWorkflows = activeWorkflows.stream()
                .sorted((w1, w2) -> Integer.compare(getWorkflowPriority(w2), getWorkflowPriority(w1)))
                .collect(Collectors.toList());
        
        int adjustedCount = 0;
        for (int i = 0; i < sortedWorkflows.size(); i++) {
            WorkflowInstance workflow = sortedWorkflows.get(i);
            ResourceAllocation allocation = resourceAllocations.get(workflow.getInstanceId());
            if (allocation != null) {
                int newPriority = Math.max(1, 10 - i); // 前面的工作流优先级更高
                if (allocation.getPriority() != newPriority) {
                    allocation.setPriority(newPriority);
                    adjustedCount++;
                }
            }
        }
        
        return adjustedCount;
    }
    
    /**
     * 按最少连接重新平衡
     */
    private int rebalanceByLeastConnections(List<WorkflowInstance> activeWorkflows) {
        // 为资源占用最少的工作流分配更多资源
        List<ResourceAllocation> sortedAllocations = resourceAllocations.values().stream()
                .sorted(Comparator.comparing(ResourceAllocation::getWeightScore))
                .collect(Collectors.toList());
        
        int adjustedCount = 0;
        for (int i = 0; i < sortedAllocations.size(); i++) {
            ResourceAllocation allocation = sortedAllocations.get(i);
            if (i < sortedAllocations.size() / 2) {
                // 前一半分配更多线程
                int newThreads = Math.min(20, allocation.getAllocatedThreads() + 2);
                if (allocation.getAllocatedThreads() != newThreads) {
                    allocation.setAllocatedThreads(newThreads);
                    adjustedCount++;
                }
            }
        }
        
        return adjustedCount;
    }
    
    /**
     * 按公平分享重新平衡
     */
    private int rebalanceByFairShare(List<WorkflowInstance> activeWorkflows) {
        if (activeWorkflows.isEmpty()) {
            return 0;
        }
        
        // 平均分配系统资源
        SystemResourceInfo resourceInfo = resourceMonitor.getCurrentResourceInfo();
        int totalCpuCores = resourceInfo.getActiveThreadCount() / 10; // 估算CPU核心数
        long totalMemoryMB = resourceInfo.getTotalMemoryMB();
        
        int cpuPerWorkflow = Math.max(1, totalCpuCores / activeWorkflows.size());
        long memoryPerWorkflow = Math.max(512, totalMemoryMB / activeWorkflows.size() / 2);
        
        int adjustedCount = 0;
        for (WorkflowInstance workflow : activeWorkflows) {
            ResourceAllocation allocation = resourceAllocations.get(workflow.getInstanceId());
            if (allocation != null) {
                if (allocation.getAllocatedCpuCores() != cpuPerWorkflow) {
                    allocation.setAllocatedCpuCores(cpuPerWorkflow);
                    adjustedCount++;
                }
                if (allocation.getAllocatedMemoryMB() != memoryPerWorkflow) {
                    allocation.setAllocatedMemoryMB(memoryPerWorkflow);
                    adjustedCount++;
                }
            }
        }
        
        return adjustedCount;
    }
    
    /**
     * 自适应重新平衡
     */
    private int rebalanceAdaptively(List<WorkflowInstance> activeWorkflows, SystemResourceInfo resourceInfo) {
        double pressureScore = resourceInfo.getResourcePressureScore();
        
        if (pressureScore < 30) {
            return rebalanceByFairShare(activeWorkflows);
        } else if (pressureScore < 70) {
            return rebalanceByPriority(activeWorkflows);
        } else {
            return rebalanceByLeastConnections(activeWorkflows);
        }
    }
    
    /**
     * 获取工作流优先级
     */
    private int getWorkflowPriority(WorkflowInstance workflow) {
        // 根据工作流的创建时间和配置计算优先级
        // 这里简化处理，实际应该根据业务需求定义优先级规则
        return 5; // 默认中等优先级
    }
}