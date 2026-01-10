package com.tbw.cut.workflow.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 性能指标模型
 * 
 * 包含工作流执行的各种性能指标数据
 */
public class PerformanceMetrics {
    
    private final int totalWorkflows;
    private final int successfulWorkflows;
    private final int failedWorkflows;
    private final double successRate;
    private final long totalExecutionTime;
    private final double averageExecutionTime;
    private final long minExecutionTime;
    private final long maxExecutionTime;
    private final double throughput;
    private final int concurrentExecutions;
    private final double resourceUtilization;
    private final LocalDateTime collectionStartTime;
    private final LocalDateTime collectionEndTime;
    private final Map<String, Double> resourceUsage;
    private final Map<String, Long> stepExecutionTimes;
    
    public PerformanceMetrics(int totalWorkflows, int successfulWorkflows, int failedWorkflows,
                            long totalExecutionTime, double averageExecutionTime, 
                            long minExecutionTime, long maxExecutionTime, double throughput,
                            int concurrentExecutions, double resourceUtilization,
                            LocalDateTime collectionStartTime, LocalDateTime collectionEndTime,
                            Map<String, Double> resourceUsage, Map<String, Long> stepExecutionTimes) {
        this.totalWorkflows = totalWorkflows;
        this.successfulWorkflows = successfulWorkflows;
        this.failedWorkflows = failedWorkflows;
        this.successRate = totalWorkflows > 0 ? (double) successfulWorkflows / totalWorkflows * 100 : 0;
        this.totalExecutionTime = totalExecutionTime;
        this.averageExecutionTime = averageExecutionTime;
        this.minExecutionTime = minExecutionTime;
        this.maxExecutionTime = maxExecutionTime;
        this.throughput = throughput;
        this.concurrentExecutions = concurrentExecutions;
        this.resourceUtilization = resourceUtilization;
        this.collectionStartTime = collectionStartTime;
        this.collectionEndTime = collectionEndTime;
        this.resourceUsage = resourceUsage;
        this.stepExecutionTimes = stepExecutionTimes;
    }
    
    public int getTotalWorkflows() {
        return totalWorkflows;
    }
    
    public int getSuccessfulWorkflows() {
        return successfulWorkflows;
    }
    
    public int getFailedWorkflows() {
        return failedWorkflows;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public double getFailureRate() {
        return 100 - successRate;
    }
    
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    public double getAverageExecutionTime() {
        return averageExecutionTime;
    }
    
    public long getMinExecutionTime() {
        return minExecutionTime;
    }
    
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }
    
    public double getThroughput() {
        return throughput;
    }
    
    public int getConcurrentExecutions() {
        return concurrentExecutions;
    }
    
    public double getResourceUtilization() {
        return resourceUtilization;
    }
    
    public LocalDateTime getCollectionStartTime() {
        return collectionStartTime;
    }
    
    public LocalDateTime getCollectionEndTime() {
        return collectionEndTime;
    }
    
    public Map<String, Double> getResourceUsage() {
        return resourceUsage;
    }
    
    public Map<String, Long> getStepExecutionTimes() {
        return stepExecutionTimes;
    }
    
    /**
     * 获取收集时间范围（分钟）
     * 
     * @return 时间范围分钟数
     */
    public long getCollectionDurationMinutes() {
        if (collectionStartTime != null && collectionEndTime != null) {
            return java.time.Duration.between(collectionStartTime, collectionEndTime).toMinutes();
        }
        return 0;
    }
    
    /**
     * 检查性能是否良好
     * 
     * @return true如果性能良好，false如果需要优化
     */
    public boolean isPerformanceGood() {
        return successRate >= 95.0 && 
               averageExecutionTime <= 30000 && // 30秒内
               resourceUtilization <= 80.0; // 80%以下
    }
    
    /**
     * 获取性能等级
     * 
     * @return 性能等级字符串
     */
    public String getPerformanceGrade() {
        if (successRate >= 98 && averageExecutionTime <= 10000 && resourceUtilization <= 60) {
            return "优秀";
        } else if (successRate >= 95 && averageExecutionTime <= 20000 && resourceUtilization <= 75) {
            return "良好";
        } else if (successRate >= 90 && averageExecutionTime <= 30000 && resourceUtilization <= 85) {
            return "一般";
        } else if (successRate >= 80 && averageExecutionTime <= 60000 && resourceUtilization <= 95) {
            return "较差";
        } else {
            return "很差";
        }
    }
    
    /**
     * 获取性能摘要
     * 
     * @return 性能摘要字符串
     */
    public String getPerformanceSummary() {
        return String.format(
            "性能摘要: 总数=%d, 成功=%d(%.1f%%), 失败=%d(%.1f%%), 平均耗时=%.2fms, " +
            "吞吐量=%.2f/min, 并发数=%d, 资源利用率=%.1f%%, 等级=%s",
            totalWorkflows, successfulWorkflows, successRate, failedWorkflows, getFailureRate(),
            averageExecutionTime, throughput, concurrentExecutions, resourceUtilization, getPerformanceGrade()
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{total=%d, success=%d(%.1f%%), failed=%d, avgTime=%.2fms, " +
            "throughput=%.2f, concurrent=%d, utilization=%.1f%%, grade=%s}",
            totalWorkflows, successfulWorkflows, successRate, failedWorkflows, averageExecutionTime,
            throughput, concurrentExecutions, resourceUtilization, getPerformanceGrade()
        );
    }
    
    /**
     * 创建空的性能指标
     * 
     * @return 空的性能指标实例
     */
    public static PerformanceMetrics empty() {
        return new PerformanceMetrics(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            LocalDateTime.now(), LocalDateTime.now(),
            java.util.Collections.emptyMap(), java.util.Collections.emptyMap()
        );
    }
    
    /**
     * 创建基本性能指标
     * 
     * @param totalWorkflows 总工作流数
     * @param successfulWorkflows 成功工作流数
     * @param totalExecutionTime 总执行时间
     * @param averageExecutionTime 平均执行时间
     * @return 性能指标实例
     */
    public static PerformanceMetrics basic(int totalWorkflows, int successfulWorkflows, 
                                         long totalExecutionTime, double averageExecutionTime) {
        int failedWorkflows = totalWorkflows - successfulWorkflows;
        return new PerformanceMetrics(
            totalWorkflows, successfulWorkflows, failedWorkflows,
            totalExecutionTime, averageExecutionTime, 0, 0, 0, 0, 0,
            LocalDateTime.now().minusHours(1), LocalDateTime.now(),
            java.util.Collections.emptyMap(), java.util.Collections.emptyMap()
        );
    }
}