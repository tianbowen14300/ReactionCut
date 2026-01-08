package com.tbw.cut.service.download.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 视频下载任务模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDownloadTask {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 视频下载请求
     */
    private VideoDownloadRequest request;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 下载结果Future
     */
    private CompletableFuture<DownloadResult> future;
    
    /**
     * 优先级（数值越小优先级越高）
     */
    private int priority;
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,      // 待下载
        DOWNLOADING,  // 下载中
        COMPLETED,    // 已完成
        FAILED,       // 失败
        CANCELLED     // 已取消
    }
    
    /**
     * 创建新的下载任务
     * @param request 下载请求
     * @return 下载任务
     */
    public static VideoDownloadTask create(VideoDownloadRequest request) {
        return VideoDownloadTask.builder()
            .taskId(System.currentTimeMillis())
            .request(request)
            .status(TaskStatus.PENDING)
            .createdTime(LocalDateTime.now())
            .future(new CompletableFuture<>())
            .priority(0)
            .build();
    }
    
    /**
     * 设置任务状态
     * @param status 新状态
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
        
        switch (status) {
            case DOWNLOADING:
                this.startTime = LocalDateTime.now();
                break;
            case COMPLETED:
            case FAILED:
            case CANCELLED:
                this.completedTime = LocalDateTime.now();
                break;
        }
    }
    
    /**
     * 完成任务
     * @param result 下载结果
     */
    public void complete(DownloadResult result) {
        if (result.isSuccess()) {
            setStatus(TaskStatus.COMPLETED);
        } else {
            setStatus(TaskStatus.FAILED);
            this.errorMessage = result.getErrorMessage();
        }
        
        if (future != null && !future.isDone()) {
            future.complete(result);
        }
    }
    
    /**
     * 取消任务
     * @param reason 取消原因
     */
    public void cancel(String reason) {
        setStatus(TaskStatus.CANCELLED);
        this.errorMessage = reason;
        
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }
    
    /**
     * 获取任务耗时（毫秒）
     * @return 耗时毫秒数
     */
    public long getDurationMs() {
        if (startTime == null) {
            return 0;
        }
        
        LocalDateTime endTime = completedTime != null ? completedTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
}