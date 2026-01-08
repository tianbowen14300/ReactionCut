package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下载配置模型
 * 用于传递给下载组件的配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadConfig {
    
    /**
     * 连接超时时间（毫秒）
     */
    private Long connectionTimeout;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Long readTimeout;
    
    /**
     * 进度更新间隔（毫秒）
     */
    private Long progressUpdateInterval;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryAttempts;
    
    /**
     * 是否启用分段下载
     */
    private Boolean enableSegmentedDownload;
    
    /**
     * 最大分段数量
     */
    private Integer maxSegments;
    
    /**
     * 分段大小（字节）
     */
    private Long segmentSize;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 引用页面
     */
    private String referer;
    
    /**
     * 额外的HTTP头
     */
    private java.util.Map<String, String> headers;
    
    /**
     * 创建默认配置
     * @return 默认配置
     */
    public static DownloadConfig createDefault() {
        return DownloadConfig.builder()
            .connectionTimeout(30000L)
            .readTimeout(1800000L) // 30分钟
            .progressUpdateInterval(1000L)
            .maxRetryAttempts(3)
            .enableSegmentedDownload(true)
            .maxSegments(4)
            .segmentSize(10 * 1024 * 1024L) // 10MB
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
            .referer("https://www.bilibili.com/")
            .build();
    }
    
    /**
     * 从系统配置创建下载配置
     * @param systemConfig 系统配置
     * @return 下载配置
     */
    public static DownloadConfig fromSystemConfig(com.tbw.cut.config.DownloadConfig systemConfig) {
        return DownloadConfig.builder()
            .connectionTimeout((long) systemConfig.getTimeoutSeconds() * 1000)
            .readTimeout((long) systemConfig.getTimeoutSeconds() * 1000)
            .progressUpdateInterval(1000L)
            .maxRetryAttempts(systemConfig.getMaxRetryAttempts())
            .enableSegmentedDownload(systemConfig.isEnableSegmentedDownload())
            .maxSegments(systemConfig.getMaxSegments())
            .segmentSize(systemConfig.getDefaultSegmentSize())
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
            .referer("https://www.bilibili.com/")
            .build();
    }
}