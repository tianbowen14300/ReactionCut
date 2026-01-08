package com.tbw.cut.event;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 下载状态变化事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadStatusChangeEvent {
    
    /**
     * 下载任务ID
     */
    private Long taskId;
    
    /**
     * 旧状态
     */
    private Integer oldStatus;
    
    /**
     * 新状态
     */
    private Integer newStatus;
    
    /**
     * 事件发生时间
     */
    private Long timestamp;
    
    /**
     * 事件来源
     */
    private String source;
    
    /**
     * 创建下载状态变化事件
     */
    public static DownloadStatusChangeEvent create(Long taskId, Integer oldStatus, Integer newStatus) {
        return DownloadStatusChangeEvent.builder()
                .taskId(taskId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .timestamp(System.currentTimeMillis())
                .source("VideoDownloadService")
                .build();
    }
    
    /**
     * 检查是否为状态改变事件
     */
    public boolean isStatusChanged() {
        return oldStatus != null && newStatus != null && !oldStatus.equals(newStatus);
    }
    
    /**
     * 检查是否为完成事件
     */
    public boolean isCompletedEvent() {
        return newStatus != null && newStatus == 2;
    }
    
    /**
     * 检查是否为失败事件
     */
    public boolean isFailedEvent() {
        return newStatus != null && newStatus == 3;
    }
}