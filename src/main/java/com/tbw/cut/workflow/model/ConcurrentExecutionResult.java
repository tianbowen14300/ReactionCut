package com.tbw.cut.workflow.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 并发执行结果模型
 * 
 * 包含多个工作流实例并发执行的结果信息
 */
public class ConcurrentExecutionResult {
    
    private final List<WorkflowInstance> successfulWorkflows;
    private final List<WorkflowInstance> failedWorkflows;
    private final Map<String, Exception> executionErrors;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long totalExecutionTimeMs;
    private final int concurrentCount;
    private final double averageExecutionTime;
    private final boolean hasConflicts;
    private final List<String> conflictDescriptions;
    
    public ConcurrentExecutionResult(List<WorkflowInstance> successfulWorkflows,
                                   List<WorkflowInstance> failedWorkflows,
                                   Map<String, Exception> executionErrors,
                                   LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   long totalExecutionTimeMs,
                                   int concurrentCount,
                                   boolean hasConflicts,
                                   List<String> conflictDescriptions) {
        this.successfulWorkflows = successfulWorkflows;
        this.failedWorkflows = failedWorkflows;
        this.executionErrors = executionErrors;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.concurrentCount = concurrentCount;
        this.averageExecutionTime = concurrentCount > 0 ? (double) totalExecutionTimeMs / concurrentCount : 0;
        this.hasConflicts = hasConflicts;
        this.conflictDescriptions = conflictDescriptions;
    }
    
    public List<WorkflowInstance> getSuccessfulWorkflows() {
        return successfulWorkflows;
    }
    
    public List<WorkflowInstance> getFailedWorkflows() {
        return failedWorkflows;
    }
    
    public Map<String, Exception> getExecutionErrors() {
        return executionErrors;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public int getConcurrentCount() {
        return concurrentCount;
    }
    
    public double getAverageExecutionTime() {
        return averageExecutionTime;
    }
    
    public boolean hasConflicts() {
        return hasConflicts;
    }
    
    public List<String> getConflictDescriptions() {
        return conflictDescriptions;
    }
    
    /**
     * 获取成功率
     * 
     * @return 成功率百分比
     */
    public double getSuccessRate() {
        int total = successfulWorkflows.size() + failedWorkflows.size();
        return total > 0 ? (double) successfulWorkflows.size() / total * 100 : 0;
    }
    
    /**
     * 获取失败率
     * 
     * @return 失败率百分比
     */
    public double getFailureRate() {
        return 100 - getSuccessRate();
    }
    
    /**
     * 检查是否所有工作流都成功执行
     * 
     * @return true如果全部成功，false如果有失败
     */
    public boolean isAllSuccessful() {
        return failedWorkflows.isEmpty();
    }
    
    /**
     * 检查是否有工作流执行失败
     * 
     * @return true如果有失败，false如果全部成功
     */
    public boolean hasFailures() {
        return !failedWorkflows.isEmpty();
    }
    
    /**
     * 获取总的工作流数量
     * 
     * @return 总数量
     */
    public int getTotalWorkflowCount() {
        return successfulWorkflows.size() + failedWorkflows.size();
    }
    
    /**
     * 获取执行摘要
     * 
     * @return 执行摘要字符串
     */
    public String getExecutionSummary() {
        return String.format(
            "并发执行完成: 总数=%d, 成功=%d(%.1f%%), 失败=%d(%.1f%%), 耗时=%dms, 平均=%.2fms, 冲突=%s",
            getTotalWorkflowCount(), successfulWorkflows.size(), getSuccessRate(),
            failedWorkflows.size(), getFailureRate(), totalExecutionTimeMs, averageExecutionTime,
            hasConflicts ? "是" : "否"
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "ConcurrentExecutionResult{successful=%d, failed=%d, totalTime=%dms, " +
            "avgTime=%.2fms, successRate=%.1f%%, hasConflicts=%s}",
            successfulWorkflows.size(), failedWorkflows.size(), totalExecutionTimeMs,
            averageExecutionTime, getSuccessRate(), hasConflicts
        );
    }
    
    /**
     * 创建成功的并发执行结果
     * 
     * @param successfulWorkflows 成功的工作流列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 并发执行结果
     */
    public static ConcurrentExecutionResult success(List<WorkflowInstance> successfulWorkflows,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        long totalTime = java.time.Duration.between(startTime, endTime).toMillis();
        return new ConcurrentExecutionResult(
            successfulWorkflows, java.util.Collections.emptyList(), java.util.Collections.emptyMap(),
            startTime, endTime, totalTime, successfulWorkflows.size(),
            false, java.util.Collections.emptyList()
        );
    }
    
    /**
     * 创建部分成功的并发执行结果
     * 
     * @param successfulWorkflows 成功的工作流列表
     * @param failedWorkflows 失败的工作流列表
     * @param executionErrors 执行错误映射
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 并发执行结果
     */
    public static ConcurrentExecutionResult partial(List<WorkflowInstance> successfulWorkflows,
                                                   List<WorkflowInstance> failedWorkflows,
                                                   Map<String, Exception> executionErrors,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        long totalTime = java.time.Duration.between(startTime, endTime).toMillis();
        int totalCount = successfulWorkflows.size() + failedWorkflows.size();
        return new ConcurrentExecutionResult(
            successfulWorkflows, failedWorkflows, executionErrors,
            startTime, endTime, totalTime, totalCount,
            false, java.util.Collections.emptyList()
        );
    }
    
    /**
     * 创建有冲突的并发执行结果
     * 
     * @param successfulWorkflows 成功的工作流列表
     * @param failedWorkflows 失败的工作流列表
     * @param executionErrors 执行错误映射
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param conflictDescriptions 冲突描述列表
     * @return 并发执行结果
     */
    public static ConcurrentExecutionResult withConflicts(List<WorkflowInstance> successfulWorkflows,
                                                         List<WorkflowInstance> failedWorkflows,
                                                         Map<String, Exception> executionErrors,
                                                         LocalDateTime startTime, LocalDateTime endTime,
                                                         List<String> conflictDescriptions) {
        long totalTime = java.time.Duration.between(startTime, endTime).toMillis();
        int totalCount = successfulWorkflows.size() + failedWorkflows.size();
        return new ConcurrentExecutionResult(
            successfulWorkflows, failedWorkflows, executionErrors,
            startTime, endTime, totalTime, totalCount,
            true, conflictDescriptions
        );
    }
}