package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 详细进度信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedProgress {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 进度百分比
     */
    private int progressPercentage;
    
    /**
     * 已下载字节数
     */
    private long downloadedBytes;
    
    /**
     * 总字节数
     */
    private long totalBytes;
    
    /**
     * 下载速度（字节/秒）
     */
    private double downloadSpeedBps;
    
    /**
     * 预估剩余时间（毫秒）
     */
    private long estimatedRemainingTimeMs;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
    
    /**
     * 分P进度列表
     */
    private List<PartProgress> partProgresses;
    
    /**
     * 当前状态
     */
    private DownloadStatus status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 分P进度信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartProgress {
        private Long cid;
        private String partTitle;
        private int progressPercentage;
        private long downloadedBytes;
        private long totalBytes;
        private DownloadStatus status;
        private String errorMessage;
    }
    
    /**
     * 下载状态枚举
     */
    public enum DownloadStatus {
        PENDING,      // 待下载
        DOWNLOADING,  // 下载中
        PAUSED,       // 已暂停
        COMPLETED,    // 已完成
        FAILED,       // 失败
        CANCELLED     // 已取消
    }
}