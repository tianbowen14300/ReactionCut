package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下载配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadConfig {
    
    /**
     * 下载名称
     */
    private String downloadName;
    
    /**
     * 下载路径
     */
    private String downloadPath;
    
    /**
     * 分辨率
     */
    private String resolution;
    
    /**
     * 编码格式
     */
    private String codec;
    
    /**
     * 流媒体格式
     */
    private String format;
    
    /**
     * 下载内容类型
     */
    private String content;
    
    /**
     * 最大并发数
     */
    @Builder.Default
    private Integer maxConcurrency = 3;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * 连接超时时间（毫秒）
     */
    @Builder.Default
    private Long connectionTimeout = 30000L;
    
    /**
     * 读取超时时间（毫秒）
     */
    @Builder.Default
    private Long readTimeout = 60000L;
    
    /**
     * 是否启用断点续传
     */
    @Builder.Default
    private Boolean enableResume = true;
    
    /**
     * 进度更新间隔（毫秒）
     */
    @Builder.Default
    private Long progressUpdateInterval = 1000L;
    
    /**
     * 临时文件后缀
     */
    @Builder.Default
    private String tempFileSuffix = ".partial";
    
    /**
     * 线程数（兼容旧版本）
     */
    public Integer getThreads() {
        return maxConcurrency;
    }
    
    /**
     * 队列大小（兼容旧版本）
     */
    public Integer getQueueSize() {
        return maxConcurrency * 2; // 默认为并发数的2倍
    }
    
    /**
     * Builder扩展方法
     */
    public static class DownloadConfigBuilder {
        public DownloadConfigBuilder maxConcurrentTasks(Integer maxConcurrentTasks) {
            return maxConcurrency(maxConcurrentTasks);
        }
        
        public DownloadConfigBuilder maxRetryAttempts(Integer maxRetryAttempts) {
            return maxRetries(maxRetryAttempts);
        }
        
        public DownloadConfigBuilder timeoutSeconds(int timeoutSeconds) {
            return connectionTimeout((long) timeoutSeconds * 1000);
        }
    }
}