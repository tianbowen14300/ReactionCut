package com.tbw.cut.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工作流时序配置属性
 * 定义工作流时序相关的配置参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow.timing")
public class WorkflowTimingProperties {
    
    /**
     * 是否启用事件驱动的工作流启动
     * 默认启用
     */
    private boolean eventDrivenEnabled = true;
    
    /**
     * 是否启用降级到立即启动模式
     * 当事件驱动失败时的降级选项
     * 默认关闭
     */
    private boolean fallbackToImmediateStart = false;
    
    /**
     * 事件处理超时时间（秒）
     * 超过此时间未收到事件将触发告警
     * 默认300秒（5分钟）
     */
    private int eventTimeoutSeconds = 300;
    
    /**
     * 最大重试次数
     * 工作流启动失败时的最大重试次数
     * 默认3次
     */
    private int maxRetryAttempts = 3;
    
    /**
     * 重试间隔时间（毫秒）
     * 重试之间的等待时间
     * 默认5000毫秒（5秒）
     */
    private long retryIntervalMillis = 5000;
    
    /**
     * 批量处理大小
     * 批量处理失败任务时的批次大小
     * 默认10
     */
    private int batchProcessingSize = 10;
    
    /**
     * 配置过期时间（小时）
     * 工作流配置在内存中的过期时间
     * 默认24小时
     */
    private int configExpiryHours = 24;
    
    /**
     * 失败事件保留天数
     * 失败事件记录的保留时间
     * 默认7天
     */
    private int failedEventRetentionDays = 7;
    
    /**
     * 是否启用定时任务
     * 控制批量状态更新等定时任务的开关
     * 默认启用
     */
    private boolean scheduledTasksEnabled = true;
    
    /**
     * 定时任务执行间隔配置
     */
    private ScheduledTaskIntervals scheduledTasks = new ScheduledTaskIntervals();
    
    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();
    
    /**
     * 告警配置
     */
    private AlertConfig alert = new AlertConfig();
    
    /**
     * 定时任务间隔配置
     */
    @Data
    public static class ScheduledTaskIntervals {
        
        /**
         * 检查待处理工作流任务的间隔（毫秒）
         * 默认30秒
         */
        private long pendingTaskCheckInterval = 30000;
        
        /**
         * 重试失败工作流的间隔（毫秒）
         * 默认5分钟
         */
        private long failedWorkflowRetryInterval = 300000;
        
        /**
         * 清理过期配置的间隔（毫秒）
         * 默认1小时
         */
        private long configCleanupInterval = 3600000;
        
        /**
         * 输出统计信息的间隔（毫秒）
         * 默认10分钟
         */
        private long statsReportInterval = 600000;
    }
    
    /**
     * 监控配置
     */
    @Data
    public static class MonitoringConfig {
        
        /**
         * 是否启用监控
         * 默认启用
         */
        private boolean enabled = true;
        
        /**
         * 监控指标收集间隔（毫秒）
         * 默认1分钟
         */
        private long metricsCollectionInterval = 60000;
        
        /**
         * 性能阈值配置
         */
        private PerformanceThresholds thresholds = new PerformanceThresholds();
    }
    
    /**
     * 性能阈值配置
     */
    @Data
    public static class PerformanceThresholds {
        
        /**
         * 工作流启动延迟告警阈值（毫秒）
         * 超过此时间将触发告警
         * 默认5分钟
         */
        private long workflowStartupDelayThreshold = 300000;
        
        /**
         * 工作流启动失败率告警阈值（百分比）
         * 超过此比例将触发告警
         * 默认5%
         */
        private double workflowStartupFailureRateThreshold = 5.0;
        
        /**
         * 事件发布失败率告警阈值（百分比）
         * 超过此比例将触发告警
         * 默认1%
         */
        private double eventPublishFailureRateThreshold = 1.0;
        
        /**
         * 待处理任务数量告警阈值
         * 超过此数量将触发告警
         * 默认100
         */
        private int pendingTaskCountThreshold = 100;
    }
    
    /**
     * 告警配置
     */
    @Data
    public static class AlertConfig {
        
        /**
         * 是否启用告警
         * 默认启用
         */
        private boolean enabled = true;
        
        /**
         * 告警发送间隔（毫秒）
         * 避免重复告警的最小间隔
         * 默认15分钟
         */
        private long alertInterval = 900000;
        
        /**
         * 告警接收人邮箱列表
         */
        private java.util.List<String> recipients = new java.util.ArrayList<>();
        
        /**
         * 钉钉机器人Webhook URL
         */
        private String dingTalkWebhook;
        
        /**
         * 企业微信机器人Webhook URL
         */
        private String wechatWebhook;
    }
    
    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (eventTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Event timeout seconds must be positive");
        }
        
        if (maxRetryAttempts < 0) {
            throw new IllegalArgumentException("Max retry attempts cannot be negative");
        }
        
        if (retryIntervalMillis <= 0) {
            throw new IllegalArgumentException("Retry interval must be positive");
        }
        
        if (batchProcessingSize <= 0) {
            throw new IllegalArgumentException("Batch processing size must be positive");
        }
        
        if (configExpiryHours <= 0) {
            throw new IllegalArgumentException("Config expiry hours must be positive");
        }
        
        if (failedEventRetentionDays <= 0) {
            throw new IllegalArgumentException("Failed event retention days must be positive");
        }
    }
    
    /**
     * 获取事件超时时间（毫秒）
     */
    public long getEventTimeoutMillis() {
        return eventTimeoutSeconds * 1000L;
    }
    
    /**
     * 获取配置过期时间（毫秒）
     */
    public long getConfigExpiryMillis() {
        return configExpiryHours * 3600L * 1000L;
    }
    
    /**
     * 检查是否应该启用降级模式
     */
    public boolean shouldFallbackToImmediateStart() {
        return !eventDrivenEnabled || fallbackToImmediateStart;
    }
    
    /**
     * 检查是否应该执行定时任务
     */
    public boolean shouldExecuteScheduledTasks() {
        return scheduledTasksEnabled;
    }
}