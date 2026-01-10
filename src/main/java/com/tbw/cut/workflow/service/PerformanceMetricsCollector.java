package com.tbw.cut.workflow.service;

import com.tbw.cut.workflow.model.PerformanceMetrics;
import com.tbw.cut.workflow.model.WorkflowInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 性能指标收集器接口
 * 
 * 负责收集和分析工作流执行的性能指标
 */
public interface PerformanceMetricsCollector {
    
    /**
     * 记录工作流开始执行
     * 
     * @param workflowInstance 工作流实例
     */
    void recordWorkflowStart(WorkflowInstance workflowInstance);
    
    /**
     * 记录工作流完成执行
     * 
     * @param workflowInstance 工作流实例
     * @param success 是否成功完成
     */
    void recordWorkflowCompletion(WorkflowInstance workflowInstance, boolean success);
    
    /**
     * 记录工作流步骤执行时间
     * 
     * @param workflowInstanceId 工作流实例ID
     * @param stepName 步骤名称
     * @param executionTimeMs 执行时间（毫秒）
     */
    void recordStepExecutionTime(String workflowInstanceId, String stepName, long executionTimeMs);
    
    /**
     * 记录资源使用情况
     * 
     * @param workflowInstanceId 工作流实例ID
     * @param resourceType 资源类型
     * @param usage 使用量
     */
    void recordResourceUsage(String workflowInstanceId, String resourceType, double usage);
    
    /**
     * 记录并发执行指标
     * 
     * @param concurrentCount 并发数量
     * @param queueLength 队列长度
     * @param throughput 吞吐量
     */
    void recordConcurrencyMetrics(int concurrentCount, int queueLength, double throughput);
    
    /**
     * 获取整体性能指标
     * 
     * @return 性能指标
     */
    PerformanceMetrics getOverallMetrics();
    
    /**
     * 获取指定时间范围内的性能指标
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性能指标
     */
    PerformanceMetrics getMetricsInTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取工作流类型的性能统计
     * 
     * @return 按工作流类型分组的性能统计
     */
    Map<String, PerformanceMetrics> getMetricsByWorkflowType();
    
    /**
     * 获取步骤执行时间统计
     * 
     * @return 按步骤名称分组的执行时间统计
     */
    Map<String, StepExecutionStats> getStepExecutionStats();
    
    /**
     * 获取资源使用统计
     * 
     * @return 按资源类型分组的使用统计
     */
    Map<String, ResourceUsageStats> getResourceUsageStats();
    
    /**
     * 获取并发性能趋势
     * 
     * @param minutes 最近多少分钟的数据
     * @return 并发性能趋势数据
     */
    List<ConcurrencyTrendData> getConcurrencyTrend(int minutes);
    
    /**
     * 清理过期的性能数据
     * 
     * @param retentionDays 保留天数
     */
    void cleanupExpiredData(int retentionDays);
    
    /**
     * 重置所有性能指标
     */
    void resetMetrics();
    
    /**
     * 导出性能指标数据
     * 
     * @param format 导出格式 (JSON, CSV, XML)
     * @return 导出的数据字符串
     */
    String exportMetrics(String format);
    
    /**
     * 步骤执行统计
     */
    class StepExecutionStats {
        private final String stepName;
        private final int executionCount;
        private final long totalExecutionTime;
        private final long averageExecutionTime;
        private final long minExecutionTime;
        private final long maxExecutionTime;
        private final double standardDeviation;
        
        public StepExecutionStats(String stepName, int executionCount, long totalExecutionTime,
                                long averageExecutionTime, long minExecutionTime, long maxExecutionTime,
                                double standardDeviation) {
            this.stepName = stepName;
            this.executionCount = executionCount;
            this.totalExecutionTime = totalExecutionTime;
            this.averageExecutionTime = averageExecutionTime;
            this.minExecutionTime = minExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.standardDeviation = standardDeviation;
        }
        
        public String getStepName() {
            return stepName;
        }
        
        public int getExecutionCount() {
            return executionCount;
        }
        
        public long getTotalExecutionTime() {
            return totalExecutionTime;
        }
        
        public long getAverageExecutionTime() {
            return averageExecutionTime;
        }
        
        public long getMinExecutionTime() {
            return minExecutionTime;
        }
        
        public long getMaxExecutionTime() {
            return maxExecutionTime;
        }
        
        public double getStandardDeviation() {
            return standardDeviation;
        }
        
        @Override
        public String toString() {
            return String.format(
                "StepExecutionStats{step='%s', count=%d, total=%dms, avg=%dms, min=%dms, max=%dms, stdDev=%.2f}",
                stepName, executionCount, totalExecutionTime, averageExecutionTime,
                minExecutionTime, maxExecutionTime, standardDeviation
            );
        }
    }
    
    /**
     * 资源使用统计
     */
    class ResourceUsageStats {
        private final String resourceType;
        private final double totalUsage;
        private final double averageUsage;
        private final double minUsage;
        private final double maxUsage;
        private final int sampleCount;
        private final LocalDateTime lastRecordTime;
        
        public ResourceUsageStats(String resourceType, double totalUsage, double averageUsage,
                                double minUsage, double maxUsage, int sampleCount, LocalDateTime lastRecordTime) {
            this.resourceType = resourceType;
            this.totalUsage = totalUsage;
            this.averageUsage = averageUsage;
            this.minUsage = minUsage;
            this.maxUsage = maxUsage;
            this.sampleCount = sampleCount;
            this.lastRecordTime = lastRecordTime;
        }
        
        public String getResourceType() {
            return resourceType;
        }
        
        public double getTotalUsage() {
            return totalUsage;
        }
        
        public double getAverageUsage() {
            return averageUsage;
        }
        
        public double getMinUsage() {
            return minUsage;
        }
        
        public double getMaxUsage() {
            return maxUsage;
        }
        
        public int getSampleCount() {
            return sampleCount;
        }
        
        public LocalDateTime getLastRecordTime() {
            return lastRecordTime;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ResourceUsageStats{type='%s', total=%.2f, avg=%.2f, min=%.2f, max=%.2f, samples=%d, lastRecord=%s}",
                resourceType, totalUsage, averageUsage, minUsage, maxUsage, sampleCount, lastRecordTime
            );
        }
    }
    
    /**
     * 并发性能趋势数据
     */
    class ConcurrencyTrendData {
        private final LocalDateTime timestamp;
        private final int concurrentCount;
        private final int queueLength;
        private final double throughput;
        private final double cpuUsage;
        private final double memoryUsage;
        
        public ConcurrencyTrendData(LocalDateTime timestamp, int concurrentCount, int queueLength,
                                  double throughput, double cpuUsage, double memoryUsage) {
            this.timestamp = timestamp;
            this.concurrentCount = concurrentCount;
            this.queueLength = queueLength;
            this.throughput = throughput;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public int getConcurrentCount() {
            return concurrentCount;
        }
        
        public int getQueueLength() {
            return queueLength;
        }
        
        public double getThroughput() {
            return throughput;
        }
        
        public double getCpuUsage() {
            return cpuUsage;
        }
        
        public double getMemoryUsage() {
            return memoryUsage;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ConcurrencyTrendData{time=%s, concurrent=%d, queue=%d, throughput=%.2f, cpu=%.1f%%, mem=%.1f%%}",
                timestamp, concurrentCount, queueLength, throughput, cpuUsage, memoryUsage
            );
        }
    }
}