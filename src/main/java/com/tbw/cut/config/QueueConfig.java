package com.tbw.cut.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 队列配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "queue")
public class QueueConfig {
    
    /**
     * 处理配置
     */
    private Processing processing = new Processing();
    
    @Data
    public static class Processing {
        /**
         * 最大重试次数
         */
        private int maxRetryCount = 3;
        
        /**
         * 重试延迟时间（分钟）
         */
        private int retryDelayMinutes = 30;
        
        /**
         * 批次间隔时间（毫秒）
         */
        private long batchIntervalMs = 5000;
        
        /**
         * 任务间最小间隔（毫秒）
         */
        private long minTaskIntervalMs = 5000;
        
        /**
         * 预估每个任务处理时间（分钟）
         */
        private long estimatedTaskDurationMinutes = 10;
    }
}