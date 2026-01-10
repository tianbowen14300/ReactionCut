package com.tbw.cut.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作流性能指标实体类
 * 
 * 对应数据库表：workflow_performance_metrics
 */
@Data
public class WorkflowPerformanceMetrics {
    
    /**
     * 指标ID
     */
    private Long metricId;
    
    /**
     * 工作流实例ID
     */
    private String instanceId;
    
    /**
     * 步骤ID (可选)
     */
    private String stepId;
    
    /**
     * 指标名称
     */
    private String metricName;
    
    /**
     * 指标值
     */
    private BigDecimal metricValue;
    
    /**
     * 指标单位
     */
    private String metricUnit;
    
    /**
     * 指标类型
     */
    private MetricType metricType;
    
    /**
     * 测量时间
     */
    private LocalDateTime measurementTime;
    
    /**
     * 附加指标数据 (JSON格式)
     */
    private String additionalData;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 指标类型枚举
     */
    public enum MetricType {
        DURATION("持续时间"),
        THROUGHPUT("吞吐量"),
        RESOURCE_USAGE("资源使用"),
        COUNT("计数"),
        PERCENTAGE("百分比");
        
        private final String description;
        
        MetricType(String description) {
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
     * 检查是否为持续时间指标
     * 
     * @return true如果是持续时间指标，false如果不是
     */
    public boolean isDurationMetric() {
        return metricType == MetricType.DURATION;
    }
    
    /**
     * 检查是否为吞吐量指标
     * 
     * @return true如果是吞吐量指标，false如果不是
     */
    public boolean isThroughputMetric() {
        return metricType == MetricType.THROUGHPUT;
    }
    
    /**
     * 检查是否为资源使用指标
     * 
     * @return true如果是资源使用指标，false如果不是
     */
    public boolean isResourceUsageMetric() {
        return metricType == MetricType.RESOURCE_USAGE;
    }
    
    /**
     * 检查是否为计数指标
     * 
     * @return true如果是计数指标，false如果不是
     */
    public boolean isCountMetric() {
        return metricType == MetricType.COUNT;
    }
    
    /**
     * 检查是否为百分比指标
     * 
     * @return true如果是百分比指标，false如果不是
     */
    public boolean isPercentageMetric() {
        return metricType == MetricType.PERCENTAGE;
    }
    
    /**
     * 检查是否有步骤关联
     * 
     * @return true如果有步骤关联，false如果没有
     */
    public boolean hasStepAssociation() {
        return stepId != null && !stepId.trim().isEmpty();
    }
    
    /**
     * 检查是否有附加数据
     * 
     * @return true如果有附加数据，false如果没有
     */
    public boolean hasAdditionalData() {
        return additionalData != null && !additionalData.trim().isEmpty();
    }
    
    /**
     * 获取指标类型描述
     * 
     * @return 指标类型的中文描述
     */
    public String getMetricTypeDescription() {
        return metricType != null ? metricType.getDescription() : "未知类型";
    }
    
    /**
     * 获取格式化的指标值
     * 
     * @return 格式化的指标值字符串
     */
    public String getFormattedMetricValue() {
        if (metricValue == null) {
            return "0";
        }
        
        String unit = metricUnit != null ? metricUnit : "";
        
        if (isPercentageMetric()) {
            return String.format("%.2f%%", metricValue);
        } else if (isDurationMetric()) {
            if ("seconds".equals(unit) || "s".equals(unit)) {
                return String.format("%.2f秒", metricValue);
            } else if ("milliseconds".equals(unit) || "ms".equals(unit)) {
                return String.format("%.0f毫秒", metricValue);
            }
        } else if (isResourceUsageMetric()) {
            if ("MB".equals(unit)) {
                return String.format("%.2fMB", metricValue);
            } else if ("GB".equals(unit)) {
                return String.format("%.2fGB", metricValue);
            } else if ("percent".equals(unit) || "%".equals(unit)) {
                return String.format("%.2f%%", metricValue);
            }
        }
        
        return metricValue.toString() + (unit.isEmpty() ? "" : " " + unit);
    }
    
    /**
     * 创建持续时间指标
     * 
     * @param instanceId 实例ID
     * @param metricName 指标名称
     * @param durationSeconds 持续时间（秒）
     * @return 持续时间指标实例
     */
    public static WorkflowPerformanceMetrics createDurationMetric(String instanceId, String metricName, 
                                                                 double durationSeconds) {
        WorkflowPerformanceMetrics metric = new WorkflowPerformanceMetrics();
        metric.setInstanceId(instanceId);
        metric.setMetricName(metricName);
        metric.setMetricValue(BigDecimal.valueOf(durationSeconds));
        metric.setMetricUnit("seconds");
        metric.setMetricType(MetricType.DURATION);
        metric.setMeasurementTime(LocalDateTime.now());
        metric.setCreatedAt(LocalDateTime.now());
        return metric;
    }
    
    /**
     * 创建步骤持续时间指标
     * 
     * @param instanceId 实例ID
     * @param stepId 步骤ID
     * @param metricName 指标名称
     * @param durationSeconds 持续时间（秒）
     * @return 步骤持续时间指标实例
     */
    public static WorkflowPerformanceMetrics createStepDurationMetric(String instanceId, String stepId, 
                                                                     String metricName, double durationSeconds) {
        WorkflowPerformanceMetrics metric = createDurationMetric(instanceId, metricName, durationSeconds);
        metric.setStepId(stepId);
        return metric;
    }
    
    /**
     * 创建资源使用指标
     * 
     * @param instanceId 实例ID
     * @param metricName 指标名称
     * @param value 指标值
     * @param unit 单位
     * @return 资源使用指标实例
     */
    public static WorkflowPerformanceMetrics createResourceUsageMetric(String instanceId, String metricName, 
                                                                      double value, String unit) {
        WorkflowPerformanceMetrics metric = new WorkflowPerformanceMetrics();
        metric.setInstanceId(instanceId);
        metric.setMetricName(metricName);
        metric.setMetricValue(BigDecimal.valueOf(value));
        metric.setMetricUnit(unit);
        metric.setMetricType(MetricType.RESOURCE_USAGE);
        metric.setMeasurementTime(LocalDateTime.now());
        metric.setCreatedAt(LocalDateTime.now());
        return metric;
    }
    
    /**
     * 创建吞吐量指标
     * 
     * @param instanceId 实例ID
     * @param metricName 指标名称
     * @param throughput 吞吐量值
     * @param unit 单位
     * @return 吞吐量指标实例
     */
    public static WorkflowPerformanceMetrics createThroughputMetric(String instanceId, String metricName, 
                                                                   double throughput, String unit) {
        WorkflowPerformanceMetrics metric = new WorkflowPerformanceMetrics();
        metric.setInstanceId(instanceId);
        metric.setMetricName(metricName);
        metric.setMetricValue(BigDecimal.valueOf(throughput));
        metric.setMetricUnit(unit);
        metric.setMetricType(MetricType.THROUGHPUT);
        metric.setMeasurementTime(LocalDateTime.now());
        metric.setCreatedAt(LocalDateTime.now());
        return metric;
    }
    
    /**
     * 创建计数指标
     * 
     * @param instanceId 实例ID
     * @param metricName 指标名称
     * @param count 计数值
     * @return 计数指标实例
     */
    public static WorkflowPerformanceMetrics createCountMetric(String instanceId, String metricName, long count) {
        WorkflowPerformanceMetrics metric = new WorkflowPerformanceMetrics();
        metric.setInstanceId(instanceId);
        metric.setMetricName(metricName);
        metric.setMetricValue(BigDecimal.valueOf(count));
        metric.setMetricUnit("count");
        metric.setMetricType(MetricType.COUNT);
        metric.setMeasurementTime(LocalDateTime.now());
        metric.setCreatedAt(LocalDateTime.now());
        return metric;
    }
    
    /**
     * 创建百分比指标
     * 
     * @param instanceId 实例ID
     * @param metricName 指标名称
     * @param percentage 百分比值
     * @return 百分比指标实例
     */
    public static WorkflowPerformanceMetrics createPercentageMetric(String instanceId, String metricName, 
                                                                   double percentage) {
        WorkflowPerformanceMetrics metric = new WorkflowPerformanceMetrics();
        metric.setInstanceId(instanceId);
        metric.setMetricName(metricName);
        metric.setMetricValue(BigDecimal.valueOf(percentage));
        metric.setMetricUnit("percent");
        metric.setMetricType(MetricType.PERCENTAGE);
        metric.setMeasurementTime(LocalDateTime.now());
        metric.setCreatedAt(LocalDateTime.now());
        return metric;
    }
    
    @Override
    public String toString() {
        return String.format(
            "WorkflowPerformanceMetrics{metricId=%d, instanceId='%s', stepId='%s', name='%s', type=%s, value=%s}",
            metricId, instanceId, stepId, metricName, metricType, getFormattedMetricValue()
        );
    }
}