package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.LoadBalancingStrategy;
import com.tbw.cut.workflow.model.ResourceAllocation;

import java.util.List;

/**
 * 负载均衡服务接口
 * 
 * 负责管理工作流实例的负载均衡，根据系统资源状况动态调整
 * 工作流的执行策略和资源分配。
 */
public interface LoadBalancer {
    
    /**
     * 检查是否可以启动新的工作流实例
     * 
     * @return true 如果可以启动新工作流，false 如果应该等待
     */
    boolean canStartNewWorkflow();
    
    /**
     * 获取当前建议的并发工作流数量
     * 
     * @return 建议的并发工作流数量
     */
    int getRecommendedConcurrency();
    
    /**
     * 为新工作流分配资源
     * 
     * @param workflowInstance 工作流实例
     * @return 资源分配结果
     */
    ResourceAllocation allocateResources(WorkflowInstance workflowInstance);
    
    /**
     * 释放工作流占用的资源
     * 
     * @param workflowInstance 工作流实例
     */
    void releaseResources(WorkflowInstance workflowInstance);
    
    /**
     * 重新平衡当前运行的工作流
     * 根据当前系统资源状况调整工作流的资源分配
     * 
     * @param activeWorkflows 当前活跃的工作流列表
     * @return 重新平衡的结果
     */
    LoadBalancingResult rebalanceWorkflows(List<WorkflowInstance> activeWorkflows);
    
    /**
     * 设置负载均衡策略
     * 
     * @param strategy 负载均衡策略
     */
    void setLoadBalancingStrategy(LoadBalancingStrategy strategy);
    
    /**
     * 获取当前负载均衡策略
     * 
     * @return 当前负载均衡策略
     */
    LoadBalancingStrategy getLoadBalancingStrategy();
    
    /**
     * 获取负载均衡统计信息
     * 
     * @return 负载均衡统计信息
     */
    LoadBalancingStats getLoadBalancingStats();
    
    /**
     * 暂停新工作流的启动
     * 在系统高负载时暂时停止接受新的工作流
     */
    void pauseNewWorkflows();
    
    /**
     * 恢复新工作流的启动
     */
    void resumeNewWorkflows();
    
    /**
     * 检查新工作流启动是否被暂停
     * 
     * @return true 如果新工作流启动被暂停，false 如果正常
     */
    boolean isNewWorkflowsPaused();
    
    /**
     * 负载均衡结果
     */
    class LoadBalancingResult {
        private final int adjustedWorkflows;
        private final int pausedWorkflows;
        private final int resumedWorkflows;
        private final String description;
        
        public LoadBalancingResult(int adjustedWorkflows, int pausedWorkflows, 
                                 int resumedWorkflows, String description) {
            this.adjustedWorkflows = adjustedWorkflows;
            this.pausedWorkflows = pausedWorkflows;
            this.resumedWorkflows = resumedWorkflows;
            this.description = description;
        }
        
        public int getAdjustedWorkflows() {
            return adjustedWorkflows;
        }
        
        public int getPausedWorkflows() {
            return pausedWorkflows;
        }
        
        public int getResumedWorkflows() {
            return resumedWorkflows;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return String.format("LoadBalancingResult{adjusted=%d, paused=%d, resumed=%d, desc='%s'}",
                    adjustedWorkflows, pausedWorkflows, resumedWorkflows, description);
        }
    }
    
    /**
     * 负载均衡统计信息
     */
    class LoadBalancingStats {
        private final int totalWorkflowsStarted;
        private final int totalWorkflowsRejected;
        private final int totalRebalanceOperations;
        private final int currentActiveWorkflows;
        private final int currentConcurrencyLimit;
        private final double averageResourceUtilization;
        private final java.time.LocalDateTime lastRebalanceTime;
        
        public LoadBalancingStats(int totalWorkflowsStarted, int totalWorkflowsRejected,
                                int totalRebalanceOperations, int currentActiveWorkflows,
                                int currentConcurrencyLimit, double averageResourceUtilization,
                                java.time.LocalDateTime lastRebalanceTime) {
            this.totalWorkflowsStarted = totalWorkflowsStarted;
            this.totalWorkflowsRejected = totalWorkflowsRejected;
            this.totalRebalanceOperations = totalRebalanceOperations;
            this.currentActiveWorkflows = currentActiveWorkflows;
            this.currentConcurrencyLimit = currentConcurrencyLimit;
            this.averageResourceUtilization = averageResourceUtilization;
            this.lastRebalanceTime = lastRebalanceTime;
        }
        
        public int getTotalWorkflowsStarted() {
            return totalWorkflowsStarted;
        }
        
        public int getTotalWorkflowsRejected() {
            return totalWorkflowsRejected;
        }
        
        public int getTotalRebalanceOperations() {
            return totalRebalanceOperations;
        }
        
        public int getCurrentActiveWorkflows() {
            return currentActiveWorkflows;
        }
        
        public int getCurrentConcurrencyLimit() {
            return currentConcurrencyLimit;
        }
        
        public double getAverageResourceUtilization() {
            return averageResourceUtilization;
        }
        
        public java.time.LocalDateTime getLastRebalanceTime() {
            return lastRebalanceTime;
        }
        
        @Override
        public String toString() {
            return String.format(
                "LoadBalancingStats{started=%d, rejected=%d, rebalanced=%d, " +
                "active=%d, limit=%d, utilization=%.1f%%, lastRebalance=%s}",
                totalWorkflowsStarted, totalWorkflowsRejected, totalRebalanceOperations,
                currentActiveWorkflows, currentConcurrencyLimit, averageResourceUtilization,
                lastRebalanceTime
            );
        }
    }
}