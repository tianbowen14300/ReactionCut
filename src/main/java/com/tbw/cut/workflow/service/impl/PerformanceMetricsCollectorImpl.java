package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.workflow.service.PerformanceMetricsCollector;
import com.tbw.cut.workflow.model.PerformanceMetrics;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 性能指标收集器实现
 * 
 * 收集和分析工作流执行的性能指标
 */
@Slf4j
@Service
public class PerformanceMetricsCollectorImpl implements PerformanceMetricsCollector {
    
    private final ObjectMapper objectMapper;
    
    // 工作流执行记录
    private final Map<String, WorkflowExecutionRecord> workflowRecords;
    private final Map<String, List<StepExecutionRecord>> stepRecords;
    private final Map<String, List<ResourceUsageRecord>> resourceRecords;
    private final List<ConcurrencyRecord> concurrencyRecords;
    
    // 统计计数器
    private final AtomicInteger totalWorkflows;
    private final AtomicInteger successfulWorkflows;
    private final AtomicInteger failedWorkflows;
    private final AtomicLong totalExecutionTime;
    
    // 性能数据保留时间（天）
    private static final int DEFAULT_RETENTION_DAYS = 30;
    
    public PerformanceMetricsCollectorImpl() {
        this.objectMapper = new ObjectMapper();
        this.workflowRecords = new ConcurrentHashMap<>();
        this.stepRecords = new ConcurrentHashMap<>();
        this.resourceRecords = new ConcurrentHashMap<>();
        this.concurrencyRecords = Collections.synchronizedList(new ArrayList<>());
        this.totalWorkflows = new AtomicInteger(0);
        this.successfulWorkflows = new AtomicInteger(0);
        this.failedWorkflows = new AtomicInteger(0);
        this.totalExecutionTime = new AtomicLong(0);
    }
    
    @PostConstruct
    public void init() {
        log.info("性能指标收集器已初始化");
        
        // 启动定期清理任务
        Timer cleanupTimer = new Timer("MetricsCleanup", true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupExpiredData(DEFAULT_RETENTION_DAYS);
            }
        }, 24 * 60 * 60 * 1000, 24 * 60 * 60 * 1000); // 每天执行一次
    }
    
    @Override
    public void recordWorkflowStart(WorkflowInstance workflowInstance) {
        String instanceId = workflowInstance.getInstanceId();
        WorkflowExecutionRecord record = new WorkflowExecutionRecord(
            instanceId,
            workflowInstance.getTaskId(),
            workflowInstance.getWorkflowType(),
            LocalDateTime.now(),
            null,
            false,
            0
        );
        
        workflowRecords.put(instanceId, record);
        totalWorkflows.incrementAndGet();
        
        log.debug("记录工作流开始: {}", instanceId);
    }
    
    @Override
    public void recordWorkflowCompletion(WorkflowInstance workflowInstance, boolean success) {
        String instanceId = workflowInstance.getInstanceId();
        WorkflowExecutionRecord record = workflowRecords.get(instanceId);
        
        if (record != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(record.startTime, endTime).toMillis();
            
            record.endTime = endTime;
            record.success = success;
            record.executionTimeMs = executionTime;
            
            if (success) {
                successfulWorkflows.incrementAndGet();
            } else {
                failedWorkflows.incrementAndGet();
            }
            
            totalExecutionTime.addAndGet(executionTime);
            
            log.debug("记录工作流完成: {}, 成功: {}, 耗时: {}ms", instanceId, success, executionTime);
        } else {
            log.warn("未找到工作流开始记录: {}", instanceId);
        }
    }
    
    @Override
    public void recordStepExecutionTime(String workflowInstanceId, String stepName, long executionTimeMs) {
        StepExecutionRecord record = new StepExecutionRecord(
            workflowInstanceId, stepName, executionTimeMs, LocalDateTime.now()
        );
        
        stepRecords.computeIfAbsent(stepName, k -> Collections.synchronizedList(new ArrayList<>()))
                  .add(record);
        
        log.debug("记录步骤执行时间: {} - {}, 耗时: {}ms", workflowInstanceId, stepName, executionTimeMs);
    }
    
    @Override
    public void recordResourceUsage(String workflowInstanceId, String resourceType, double usage) {
        ResourceUsageRecord record = new ResourceUsageRecord(
            workflowInstanceId, resourceType, usage, LocalDateTime.now()
        );
        
        resourceRecords.computeIfAbsent(resourceType, k -> Collections.synchronizedList(new ArrayList<>()))
                      .add(record);
        
        log.debug("记录资源使用: {} - {}, 使用量: {}", workflowInstanceId, resourceType, usage);
    }
    
    @Override
    public void recordConcurrencyMetrics(int concurrentCount, int queueLength, double throughput) {
        // 获取当前系统资源使用情况（简化处理）
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100;
        double cpuUsage = getCpuUsage(); // 简化的CPU使用率获取
        
        ConcurrencyRecord record = new ConcurrencyRecord(
            LocalDateTime.now(), concurrentCount, queueLength, throughput, cpuUsage, memoryUsage
        );
        
        concurrencyRecords.add(record);
        
        // 保持最近1000条记录
        if (concurrencyRecords.size() > 1000) {
            concurrencyRecords.remove(0);
        }
        
        log.debug("记录并发指标: 并发数={}, 队列长度={}, 吞吐量={}, CPU={}%, 内存={}%",
                concurrentCount, queueLength, throughput, cpuUsage, memoryUsage);
    }
    
    @Override
    public PerformanceMetrics getOverallMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(DEFAULT_RETENTION_DAYS);
        
        return calculateMetrics(startTime, now);
    }
    
    @Override
    public PerformanceMetrics getMetricsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return calculateMetrics(startTime, endTime);
    }
    
    @Override
    public Map<String, PerformanceMetrics> getMetricsByWorkflowType() {
        Map<String, PerformanceMetrics> result = new HashMap<>();
        
        // 按工作流类型分组
        Map<String, List<WorkflowExecutionRecord>> groupedRecords = workflowRecords.values().stream()
                .collect(Collectors.groupingBy(record -> record.workflowType));
        
        for (Map.Entry<String, List<WorkflowExecutionRecord>> entry : groupedRecords.entrySet()) {
            String workflowType = entry.getKey();
            List<WorkflowExecutionRecord> records = entry.getValue();
            
            PerformanceMetrics metrics = calculateMetricsFromRecords(records);
            result.put(workflowType, metrics);
        }
        
        return result;
    }
    
    @Override
    public Map<String, PerformanceMetricsCollector.StepExecutionStats> getStepExecutionStats() {
        Map<String, PerformanceMetricsCollector.StepExecutionStats> result = new HashMap<>();
        
        for (Map.Entry<String, List<StepExecutionRecord>> entry : stepRecords.entrySet()) {
            String stepName = entry.getKey();
            List<StepExecutionRecord> records = entry.getValue();
            
            if (!records.isEmpty()) {
                int count = records.size();
                long total = records.stream().mapToLong(r -> r.executionTimeMs).sum();
                long average = total / count;
                long min = records.stream().mapToLong(r -> r.executionTimeMs).min().orElse(0);
                long max = records.stream().mapToLong(r -> r.executionTimeMs).max().orElse(0);
                
                // 计算标准差
                double variance = records.stream()
                        .mapToDouble(r -> Math.pow(r.executionTimeMs - average, 2))
                        .average().orElse(0);
                double stdDev = Math.sqrt(variance);
                
                PerformanceMetricsCollector.StepExecutionStats stats = new PerformanceMetricsCollector.StepExecutionStats(
                    stepName, count, total, average, min, max, stdDev
                );
                result.put(stepName, stats);
            }
        }
        
        return result;
    }
    
    @Override
    public Map<String, PerformanceMetricsCollector.ResourceUsageStats> getResourceUsageStats() {
        Map<String, PerformanceMetricsCollector.ResourceUsageStats> result = new HashMap<>();
        
        for (Map.Entry<String, List<ResourceUsageRecord>> entry : resourceRecords.entrySet()) {
            String resourceType = entry.getKey();
            List<ResourceUsageRecord> records = entry.getValue();
            
            if (!records.isEmpty()) {
                int count = records.size();
                double total = records.stream().mapToDouble(r -> r.usage).sum();
                double average = total / count;
                double min = records.stream().mapToDouble(r -> r.usage).min().orElse(0);
                double max = records.stream().mapToDouble(r -> r.usage).max().orElse(0);
                LocalDateTime lastRecord = records.stream()
                        .map(r -> r.timestamp)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());
                
                PerformanceMetricsCollector.ResourceUsageStats stats = new PerformanceMetricsCollector.ResourceUsageStats(
                    resourceType, total, average, min, max, count, lastRecord
                );
                result.put(resourceType, stats);
            }
        }
        
        return result;
    }
    
    @Override
    public List<PerformanceMetricsCollector.ConcurrencyTrendData> getConcurrencyTrend(int minutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutes);
        
        return concurrencyRecords.stream()
                .filter(record -> record.timestamp.isAfter(cutoffTime))
                .map(record -> new PerformanceMetricsCollector.ConcurrencyTrendData(
                    record.timestamp, record.concurrentCount, record.queueLength,
                    record.throughput, record.cpuUsage, record.memoryUsage
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public void cleanupExpiredData(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        // 清理过期的工作流记录
        workflowRecords.entrySet().removeIf(entry -> {
            WorkflowExecutionRecord record = entry.getValue();
            return record.startTime.isBefore(cutoffTime);
        });
        
        // 清理过期的步骤记录
        stepRecords.values().forEach(records -> 
            records.removeIf(record -> record.timestamp.isBefore(cutoffTime))
        );
        
        // 清理过期的资源记录
        resourceRecords.values().forEach(records -> 
            records.removeIf(record -> record.timestamp.isBefore(cutoffTime))
        );
        
        // 清理过期的并发记录
        concurrencyRecords.removeIf(record -> record.timestamp.isBefore(cutoffTime));
        
        log.info("清理了 {} 天前的过期性能数据", retentionDays);
    }
    
    @Override
    public void resetMetrics() {
        workflowRecords.clear();
        stepRecords.clear();
        resourceRecords.clear();
        concurrencyRecords.clear();
        
        totalWorkflows.set(0);
        successfulWorkflows.set(0);
        failedWorkflows.set(0);
        totalExecutionTime.set(0);
        
        log.info("已重置所有性能指标");
    }
    
    @Override
    public String exportMetrics(String format) {
        try {
            PerformanceMetrics metrics = getOverallMetrics();
            
            switch (format.toUpperCase()) {
                case "JSON":
                    return objectMapper.writeValueAsString(metrics);
                case "CSV":
                    return exportToCsv(metrics);
                case "XML":
                    return exportToXml(metrics);
                default:
                    return metrics.toString();
            }
        } catch (Exception e) {
            log.error("导出性能指标时发生异常", e);
            return "导出失败: " + e.getMessage();
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 计算指定时间范围内的性能指标
     */
    private PerformanceMetrics calculateMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        List<WorkflowExecutionRecord> filteredRecords = workflowRecords.values().stream()
                .filter(record -> record.startTime.isAfter(startTime) && 
                                record.startTime.isBefore(endTime))
                .collect(Collectors.toList());
        
        return calculateMetricsFromRecords(filteredRecords);
    }
    
    /**
     * 从工作流记录计算性能指标
     */
    private PerformanceMetrics calculateMetricsFromRecords(List<WorkflowExecutionRecord> records) {
        if (records.isEmpty()) {
            return PerformanceMetrics.empty();
        }
        
        int total = records.size();
        int successful = (int) records.stream().filter(r -> r.success).count();
        int failed = total - successful;
        
        long totalTime = records.stream().mapToLong(r -> r.executionTimeMs).sum();
        double avgTime = (double) totalTime / total;
        long minTime = records.stream().mapToLong(r -> r.executionTimeMs).min().orElse(0);
        long maxTime = records.stream().mapToLong(r -> r.executionTimeMs).max().orElse(0);
        
        // 计算吞吐量（每分钟完成的工作流数）
        LocalDateTime minStartTime = records.stream()
                .map(r -> r.startTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        LocalDateTime maxEndTime = records.stream()
                .map(r -> r.endTime != null ? r.endTime : LocalDateTime.now())
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        
        long durationMinutes = java.time.Duration.between(minStartTime, maxEndTime).toMinutes();
        double throughput = durationMinutes > 0 ? (double) total / durationMinutes : 0;
        
        // 获取当前并发数和资源利用率
        int currentConcurrent = getCurrentConcurrentCount();
        double resourceUtilization = getCurrentResourceUtilization();
        
        // 构建资源使用和步骤执行时间映射
        Map<String, Double> resourceUsage = getAverageResourceUsage();
        Map<String, Long> stepTimes = getAverageStepExecutionTimes();
        
        return new PerformanceMetrics(
            total, successful, failed, totalTime, avgTime, minTime, maxTime,
            throughput, currentConcurrent, resourceUtilization,
            minStartTime, maxEndTime, resourceUsage, stepTimes
        );
    }
    
    /**
     * 获取当前并发数
     */
    private int getCurrentConcurrentCount() {
        return concurrencyRecords.isEmpty() ? 0 : 
               concurrencyRecords.get(concurrencyRecords.size() - 1).concurrentCount;
    }
    
    /**
     * 获取当前资源利用率
     */
    private double getCurrentResourceUtilization() {
        if (concurrencyRecords.isEmpty()) {
            return 0;
        }
        
        ConcurrencyRecord lastRecord = concurrencyRecords.get(concurrencyRecords.size() - 1);
        return (lastRecord.cpuUsage + lastRecord.memoryUsage) / 2;
    }
    
    /**
     * 获取平均资源使用情况
     */
    private Map<String, Double> getAverageResourceUsage() {
        Map<String, Double> result = new HashMap<>();
        
        for (Map.Entry<String, List<ResourceUsageRecord>> entry : resourceRecords.entrySet()) {
            String resourceType = entry.getKey();
            List<ResourceUsageRecord> records = entry.getValue();
            
            if (!records.isEmpty()) {
                double average = records.stream().mapToDouble(r -> r.usage).average().orElse(0);
                result.put(resourceType, average);
            }
        }
        
        return result;
    }
    
    /**
     * 获取平均步骤执行时间
     */
    private Map<String, Long> getAverageStepExecutionTimes() {
        Map<String, Long> result = new HashMap<>();
        
        for (Map.Entry<String, List<StepExecutionRecord>> entry : stepRecords.entrySet()) {
            String stepName = entry.getKey();
            List<StepExecutionRecord> records = entry.getValue();
            
            if (!records.isEmpty()) {
                long average = (long) records.stream().mapToLong(r -> r.executionTimeMs).average().orElse(0);
                result.put(stepName, average);
            }
        }
        
        return result;
    }
    
    /**
     * 获取CPU使用率（简化实现）
     */
    private double getCpuUsage() {
        // 简化的CPU使用率获取，实际应该使用系统API
        return Math.random() * 100; // 模拟CPU使用率
    }
    
    /**
     * 导出为CSV格式
     */
    private String exportToCsv(PerformanceMetrics metrics) {
        StringBuilder csv = new StringBuilder();
        csv.append("指标,值\n");
        csv.append("总工作流数,").append(metrics.getTotalWorkflows()).append("\n");
        csv.append("成功工作流数,").append(metrics.getSuccessfulWorkflows()).append("\n");
        csv.append("失败工作流数,").append(metrics.getFailedWorkflows()).append("\n");
        csv.append("成功率(%),").append(String.format("%.2f", metrics.getSuccessRate())).append("\n");
        csv.append("平均执行时间(ms),").append(String.format("%.2f", metrics.getAverageExecutionTime())).append("\n");
        csv.append("吞吐量(个/分钟),").append(String.format("%.2f", metrics.getThroughput())).append("\n");
        csv.append("资源利用率(%),").append(String.format("%.2f", metrics.getResourceUtilization())).append("\n");
        return csv.toString();
    }
    
    /**
     * 导出为XML格式
     */
    private String exportToXml(PerformanceMetrics metrics) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<PerformanceMetrics>\n");
        xml.append("  <TotalWorkflows>").append(metrics.getTotalWorkflows()).append("</TotalWorkflows>\n");
        xml.append("  <SuccessfulWorkflows>").append(metrics.getSuccessfulWorkflows()).append("</SuccessfulWorkflows>\n");
        xml.append("  <FailedWorkflows>").append(metrics.getFailedWorkflows()).append("</FailedWorkflows>\n");
        xml.append("  <SuccessRate>").append(String.format("%.2f", metrics.getSuccessRate())).append("</SuccessRate>\n");
        xml.append("  <AverageExecutionTime>").append(String.format("%.2f", metrics.getAverageExecutionTime())).append("</AverageExecutionTime>\n");
        xml.append("  <Throughput>").append(String.format("%.2f", metrics.getThroughput())).append("</Throughput>\n");
        xml.append("  <ResourceUtilization>").append(String.format("%.2f", metrics.getResourceUtilization())).append("</ResourceUtilization>\n");
        xml.append("</PerformanceMetrics>\n");
        return xml.toString();
    }
    
    // ==================== 内部记录类 ====================
    
    /**
     * 工作流执行记录
     */
    private static class WorkflowExecutionRecord {
        final String instanceId;
        final String taskId;
        final String workflowType;
        final LocalDateTime startTime;
        LocalDateTime endTime;
        boolean success;
        long executionTimeMs;
        
        WorkflowExecutionRecord(String instanceId, String taskId, String workflowType,
                              LocalDateTime startTime, LocalDateTime endTime, 
                              boolean success, long executionTimeMs) {
            this.instanceId = instanceId;
            this.taskId = taskId;
            this.workflowType = workflowType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.success = success;
            this.executionTimeMs = executionTimeMs;
        }
    }
    
    /**
     * 步骤执行记录
     */
    private static class StepExecutionRecord {
        final String workflowInstanceId;
        final String stepName;
        final long executionTimeMs;
        final LocalDateTime timestamp;
        
        StepExecutionRecord(String workflowInstanceId, String stepName, 
                          long executionTimeMs, LocalDateTime timestamp) {
            this.workflowInstanceId = workflowInstanceId;
            this.stepName = stepName;
            this.executionTimeMs = executionTimeMs;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 资源使用记录
     */
    private static class ResourceUsageRecord {
        final String workflowInstanceId;
        final String resourceType;
        final double usage;
        final LocalDateTime timestamp;
        
        ResourceUsageRecord(String workflowInstanceId, String resourceType, 
                          double usage, LocalDateTime timestamp) {
            this.workflowInstanceId = workflowInstanceId;
            this.resourceType = resourceType;
            this.usage = usage;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 并发记录
     */
    private static class ConcurrencyRecord {
        final LocalDateTime timestamp;
        final int concurrentCount;
        final int queueLength;
        final double throughput;
        final double cpuUsage;
        final double memoryUsage;
        
        ConcurrencyRecord(LocalDateTime timestamp, int concurrentCount, int queueLength,
                        double throughput, double cpuUsage, double memoryUsage) {
            this.timestamp = timestamp;
            this.concurrentCount = concurrentCount;
            this.queueLength = queueLength;
            this.throughput = throughput;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
        }
    }
}