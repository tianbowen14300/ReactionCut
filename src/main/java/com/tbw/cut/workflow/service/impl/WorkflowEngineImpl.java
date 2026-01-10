package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.model.WorkflowConfig;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStatus;
import com.tbw.cut.workflow.model.ResourceAllocation;
import com.tbw.cut.workflow.service.WorkflowEngine;
import com.tbw.cut.workflow.service.TaskOrchestrator;
import com.tbw.cut.workflow.service.ResourceMonitor;
import com.tbw.cut.workflow.service.LoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工作流引擎实现
 * 
 * 集成了资源监控和负载均衡功能，支持动态调整工作流执行策略
 */
@Slf4j
@Service
public class WorkflowEngineImpl implements WorkflowEngine {
    
    @Autowired
    private TaskOrchestrator taskOrchestrator;
    
    @Autowired
    private ResourceMonitor resourceMonitor;
    
    @Autowired
    private LoadBalancer loadBalancer;
    
    /**
     * 内存中的工作流实例存储
     * TODO: 后续可以替换为数据库持久化
     */
    private final Map<String, WorkflowInstance> workflowInstances = new ConcurrentHashMap<>();
    
    /**
     * 定时任务执行器，用于定期负载均衡
     */
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WorkflowEngine-Scheduler");
        t.setDaemon(true);
        return t;
    });
    
    @PostConstruct
    public void init() {
        // 启动定期负载均衡任务（每2分钟执行一次）
        scheduledExecutor.scheduleAtFixedRate(this::performPeriodicRebalancing, 
                2, 2, TimeUnit.MINUTES);
        
        log.info("工作流引擎已初始化，启用资源监控和负载均衡功能");
    }
    
    @Override
    public WorkflowInstance startWorkflow(String taskId, WorkflowConfig config) {
        log.info("启动工作流，taskId: {}, userId: {}", taskId, config.getUserId());
        
        try {
            // 验证配置
            if (!config.isValid()) {
                throw new IllegalArgumentException("工作流配置无效: " + config.getValidationError());
            }
            
            // 检查负载均衡器是否允许启动新工作流
            if (!loadBalancer.canStartNewWorkflow()) {
                String reason = loadBalancer.isNewWorkflowsPaused() ? 
                    "系统暂停新工作流启动" : "系统资源不足或达到并发限制";
                log.warn("无法启动新工作流，taskId: {}, 原因: {}", taskId, reason);
                throw new IllegalStateException("无法启动新工作流: " + reason);
            }
            
            // 检查是否已存在该任务的工作流
            WorkflowInstance existingWorkflow = getWorkflowByTaskId(taskId);
            if (existingWorkflow != null && !existingWorkflow.isCompleted()) {
                log.warn("任务 {} 已存在未完成的工作流: {}", taskId, existingWorkflow.getInstanceId());
                throw new IllegalStateException("任务已存在未完成的工作流");
            }
            
            // 创建工作流实例
            WorkflowInstance instance = WorkflowInstance.create(taskId, config);
            
            // 为工作流分配资源
            ResourceAllocation resourceAllocation = loadBalancer.allocateResources(instance);
            if (resourceAllocation == null) {
                log.warn("无法为工作流分配资源，taskId: {}", taskId);
                throw new IllegalStateException("无法为工作流分配资源");
            }
            
            // 将资源分配信息设置到工作流实例中
            instance.setResourceAllocation(resourceAllocation);
            workflowInstances.put(instance.getInstanceId(), instance);
            
            log.info("创建工作流实例: {}, 步骤数: {}, 资源分配: {}", 
                    instance.getInstanceId(), instance.getSteps().size(), resourceAllocation);
            
            // 异步执行工作流
            CompletableFuture.runAsync(() -> executeWorkflowAsync(instance))
                    .exceptionally(throwable -> {
                        log.error("工作流执行异常: {}", instance.getInstanceId(), throwable);
                        instance.fail("工作流执行异常: " + throwable.getMessage());
                        // 释放资源
                        loadBalancer.releaseResources(instance);
                        return null;
                    });
            
            return instance;
            
        } catch (Exception e) {
            log.error("启动工作流失败，taskId: {}", taskId, e);
            throw new RuntimeException("启动工作流失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public WorkflowStatus getWorkflowStatus(String instanceId) {
        WorkflowInstance instance = workflowInstances.get(instanceId);
        return instance != null ? instance.getStatus() : null;
    }
    
    @Override
    public WorkflowInstance getWorkflowInstance(String instanceId) {
        return workflowInstances.get(instanceId);
    }
    
    @Override
    public boolean cancelWorkflow(String instanceId) {
        log.info("取消工作流: {}", instanceId);
        
        WorkflowInstance instance = workflowInstances.get(instanceId);
        if (instance == null) {
            log.warn("工作流实例不存在: {}", instanceId);
            return false;
        }
        
        if (instance.isCompleted()) {
            log.warn("工作流已完成，无法取消: {}", instanceId);
            return false;
        }
        
        instance.cancel();
        
        // 释放资源
        loadBalancer.releaseResources(instance);
        
        log.info("工作流已取消: {}", instanceId);
        return true;
    }
    
    @Override
    public boolean pauseWorkflow(String instanceId) {
        log.info("暂停工作流: {}", instanceId);
        
        WorkflowInstance instance = workflowInstances.get(instanceId);
        if (instance == null) {
            log.warn("工作流实例不存在: {}", instanceId);
            return false;
        }
        
        if (!instance.isRunning()) {
            log.warn("工作流未在运行，无法暂停: {}", instanceId);
            return false;
        }
        
        instance.pause();
        log.info("工作流已暂停: {}", instanceId);
        return true;
    }
    
    @Override
    public boolean resumeWorkflow(String instanceId) {
        log.info("恢复工作流: {}", instanceId);
        
        WorkflowInstance instance = workflowInstances.get(instanceId);
        if (instance == null) {
            log.warn("工作流实例不存在: {}", instanceId);
            return false;
        }
        
        if (instance.getStatus() != WorkflowStatus.PAUSED) {
            log.warn("工作流未暂停，无法恢复: {}", instanceId);
            return false;
        }
        
        instance.resume();
        
        // 异步继续执行工作流
        CompletableFuture.runAsync(() -> executeWorkflowAsync(instance))
                .exceptionally(throwable -> {
                    log.error("恢复工作流执行异常: {}", instanceId, throwable);
                    instance.fail("恢复执行异常: " + throwable.getMessage());
                    return null;
                });
        
        log.info("工作流已恢复: {}", instanceId);
        return true;
    }
    
    @Override
    public List<WorkflowInstance> getActiveWorkflows() {
        return workflowInstances.values().stream()
                .filter(instance -> !instance.isCompleted())
                .collect(Collectors.toList());
    }
    
    @Override
    public WorkflowInstance getWorkflowByTaskId(String taskId) {
        return workflowInstances.values().stream()
                .filter(instance -> taskId.equals(instance.getTaskId()))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public int cleanupCompletedWorkflows(int olderThanHours) {
        log.info("清理 {} 小时前完成的工作流实例", olderThanHours);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(olderThanHours);
        
        List<String> toRemove = workflowInstances.values().stream()
                .filter(instance -> instance.isCompleted() && 
                                  instance.getEndTime() != null && 
                                  instance.getEndTime().isBefore(cutoffTime))
                .map(WorkflowInstance::getInstanceId)
                .collect(Collectors.toList());
        
        toRemove.forEach(workflowInstances::remove);
        
        log.info("清理了 {} 个已完成的工作流实例", toRemove.size());
        return toRemove.size();
    }
    
    /**
     * 异步执行工作流
     */
    private void executeWorkflowAsync(WorkflowInstance instance) {
        log.info("执行executeWorkflowAsync");
        try {
            System.out.println("DEBUG: Method entered for " + instance.getInstanceId());
            log.info("开始执行工作流: {}", instance.getInstanceId());
            
            // 启动工作流
            instance.start();
            
            // 委托给任务编排器执行
            taskOrchestrator.orchestrateTask(instance);
            
            // 如果没有异常，标记为完成
            if (instance.getStatus() == WorkflowStatus.RUNNING) {
                instance.complete();
                log.info("工作流执行完成: {}", instance.getInstanceId());
            }
            
        } catch (Exception e) {
            System.err.println("DEBUG: Caught Exception: " + e.getMessage());
            log.error("工作流执行失败: {}", instance.getInstanceId(), e);
            instance.fail("执行失败: " + e.getMessage());
        } catch (Throwable t) {
            System.err.println("DEBUG: Caught Throwable: " + t.getClass().getName());
        }
        finally {
            System.out.println("DEBUG: Finally block entered. Status: " + instance.getStatus());
            // 无论成功还是失败，都释放资源
            if (instance.isCompleted()) {
                loadBalancer.releaseResources(instance);
                log.debug("已释放工作流资源: {}", instance.getInstanceId());
            }
        }
    }
    
    /**
     * 定期执行负载均衡
     */
    private void performPeriodicRebalancing() {
        try {
            List<WorkflowInstance> activeWorkflows = getActiveWorkflows();
            if (!activeWorkflows.isEmpty()) {
                LoadBalancer.LoadBalancingResult result = loadBalancer.rebalanceWorkflows(activeWorkflows);
                if (result.getAdjustedWorkflows() > 0 || result.getPausedWorkflows() > 0 || result.getResumedWorkflows() > 0) {
                    log.info("定期负载均衡完成: {}", result);
                }
            }
        } catch (Exception e) {
            log.error("定期负载均衡执行异常", e);
        }
    }
}