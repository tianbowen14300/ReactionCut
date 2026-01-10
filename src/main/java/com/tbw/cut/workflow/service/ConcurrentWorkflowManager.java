package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.ConcurrentExecutionResult;
import com.tbw.cut.workflow.model.ResourceIsolationConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 并发工作流管理器接口
 * 
 * 负责管理多个工作流实例的并发执行，提供资源隔离和冲突避免机制
 */
public interface ConcurrentWorkflowManager {
    
    /**
     * 并发执行多个工作流实例
     * 
     * @param workflowInstances 要执行的工作流实例列表
     * @return 并发执行结果的Future
     */
    CompletableFuture<ConcurrentExecutionResult> executeConcurrently(List<WorkflowInstance> workflowInstances);
    
    /**
     * 异步执行单个工作流实例
     * 
     * @param workflowInstance 工作流实例
     * @return 执行结果的Future
     */
    CompletableFuture<WorkflowInstance> executeAsync(WorkflowInstance workflowInstance);
    
    /**
     * 检查工作流实例之间是否存在资源冲突
     * 
     * @param workflowInstances 工作流实例列表
     * @return 冲突检测结果
     */
    ConflictDetectionResult detectConflicts(List<WorkflowInstance> workflowInstances);
    
    /**
     * 为工作流实例分配隔离的资源
     * 
     * @param workflowInstance 工作流实例
     * @param isolationConfig 资源隔离配置
     * @return 是否分配成功
     */
    boolean allocateIsolatedResources(WorkflowInstance workflowInstance, ResourceIsolationConfig isolationConfig);
    
    /**
     * 释放工作流实例的隔离资源
     * 
     * @param workflowInstance 工作流实例
     */
    void releaseIsolatedResources(WorkflowInstance workflowInstance);
    
    /**
     * 获取当前并发执行的工作流数量
     * 
     * @return 并发执行数量
     */
    int getCurrentConcurrentCount();
    
    /**
     * 获取最大并发执行数量
     * 
     * @return 最大并发数量
     */
    int getMaxConcurrentCount();
    
    /**
     * 设置最大并发执行数量
     * 
     * @param maxConcurrentCount 最大并发数量
     */
    void setMaxConcurrentCount(int maxConcurrentCount);
    
    /**
     * 获取并发执行性能统计
     * 
     * @return 性能统计信息
     */
    ConcurrentExecutionStats getExecutionStats();
    
    /**
     * 暂停所有并发执行
     */
    void pauseAllExecution();
    
    /**
     * 恢复所有并发执行
     */
    void resumeAllExecution();
    
    /**
     * 检查并发执行是否被暂停
     * 
     * @return true如果被暂停，false如果正常运行
     */
    boolean isExecutionPaused();
    
    /**
     * 冲突检测结果
     */
    class ConflictDetectionResult {
        private final boolean hasConflicts;
        private final List<ConflictInfo> conflicts;
        private final String description;
        
        public ConflictDetectionResult(boolean hasConflicts, List<ConflictInfo> conflicts, String description) {
            this.hasConflicts = hasConflicts;
            this.conflicts = conflicts;
            this.description = description;
        }
        
        public boolean hasConflicts() {
            return hasConflicts;
        }
        
        public List<ConflictInfo> getConflicts() {
            return conflicts;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return String.format("ConflictDetectionResult{hasConflicts=%s, conflictCount=%d, desc='%s'}",
                    hasConflicts, conflicts != null ? conflicts.size() : 0, description);
        }
    }
    
    /**
     * 冲突信息
     */
    class ConflictInfo {
        private final String workflowId1;
        private final String workflowId2;
        private final ConflictType conflictType;
        private final String resourcePath;
        private final String description;
        
        public ConflictInfo(String workflowId1, String workflowId2, ConflictType conflictType, 
                           String resourcePath, String description) {
            this.workflowId1 = workflowId1;
            this.workflowId2 = workflowId2;
            this.conflictType = conflictType;
            this.resourcePath = resourcePath;
            this.description = description;
        }
        
        public String getWorkflowId1() {
            return workflowId1;
        }
        
        public String getWorkflowId2() {
            return workflowId2;
        }
        
        public ConflictType getConflictType() {
            return conflictType;
        }
        
        public String getResourcePath() {
            return resourcePath;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return String.format("ConflictInfo{%s vs %s, type=%s, resource='%s', desc='%s'}",
                    workflowId1, workflowId2, conflictType, resourcePath, description);
        }
    }
    
    /**
     * 冲突类型枚举
     */
    enum ConflictType {
        FILE_ACCESS("文件访问冲突"),
        RESOURCE_COMPETITION("资源竞争冲突"),
        DEPENDENCY_CONFLICT("依赖冲突"),
        OUTPUT_PATH_CONFLICT("输出路径冲突"),
        PROCESSING_CONFLICT("处理冲突");
        
        private final String description;
        
        ConflictType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * 并发执行统计信息
     */
    class ConcurrentExecutionStats {
        private final int totalExecuted;
        private final int successfulExecutions;
        private final int failedExecutions;
        private final int conflictDetected;
        private final double averageExecutionTime;
        private final double concurrencyUtilization;
        private final java.time.LocalDateTime lastExecutionTime;
        
        public ConcurrentExecutionStats(int totalExecuted, int successfulExecutions, int failedExecutions,
                                       int conflictDetected, double averageExecutionTime, 
                                       double concurrencyUtilization, java.time.LocalDateTime lastExecutionTime) {
            this.totalExecuted = totalExecuted;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.conflictDetected = conflictDetected;
            this.averageExecutionTime = averageExecutionTime;
            this.concurrencyUtilization = concurrencyUtilization;
            this.lastExecutionTime = lastExecutionTime;
        }
        
        public int getTotalExecuted() {
            return totalExecuted;
        }
        
        public int getSuccessfulExecutions() {
            return successfulExecutions;
        }
        
        public int getFailedExecutions() {
            return failedExecutions;
        }
        
        public int getConflictDetected() {
            return conflictDetected;
        }
        
        public double getAverageExecutionTime() {
            return averageExecutionTime;
        }
        
        public double getConcurrencyUtilization() {
            return concurrencyUtilization;
        }
        
        public java.time.LocalDateTime getLastExecutionTime() {
            return lastExecutionTime;
        }
        
        public double getSuccessRate() {
            return totalExecuted > 0 ? (double) successfulExecutions / totalExecuted * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ConcurrentExecutionStats{total=%d, success=%d(%.1f%%), failed=%d, conflicts=%d, " +
                "avgTime=%.2fms, utilization=%.1f%%, lastExecution=%s}",
                totalExecuted, successfulExecutions, getSuccessRate(), failedExecutions, conflictDetected,
                averageExecutionTime, concurrencyUtilization, lastExecutionTime
            );
        }
    }
}