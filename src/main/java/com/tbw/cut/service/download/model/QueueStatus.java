package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 队列状态模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatus {
    
    /**
     * 正在下载的视频数量
     */
    private int activeDownloads;
    
    /**
     * 等待下载的视频数量
     */
    private int pendingDownloads;
    
    /**
     * 最大并发视频数量
     */
    private int maxConcurrentVideos;
    
    /**
     * 总任务数量
     */
    private int totalTasks;
    
    /**
     * 队列使用率
     */
    public double getQueueUtilization() {
        if (maxConcurrentVideos == 0) {
            return 0.0;
        }
        return (double) activeDownloads / maxConcurrentVideos;
    }
    
    /**
     * 是否队列已满
     */
    public boolean isQueueFull() {
        return activeDownloads >= maxConcurrentVideos;
    }
    
    /**
     * 是否有待处理任务
     */
    public boolean hasPendingTasks() {
        return pendingDownloads > 0;
    }
}