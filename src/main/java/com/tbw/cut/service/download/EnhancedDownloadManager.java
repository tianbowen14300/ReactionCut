package com.tbw.cut.service.download;

import com.tbw.cut.service.download.concurrent.ConcurrentDownloadExecutor;
import com.tbw.cut.service.download.model.*;
import com.tbw.cut.service.download.progress.ProgressTracker;
import com.tbw.cut.service.download.retry.RetryManager;
import com.tbw.cut.service.download.resource.ResourceMonitor;
import com.tbw.cut.service.download.queue.VideoDownloadQueueManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 增强的下载管理器
 * 集成并发控制、进度跟踪、重试机制和资源监控
 */
@Slf4j
@Service
public class EnhancedDownloadManager {
    
    private final ConcurrentDownloadExecutor concurrentExecutor;
    private final ProgressTracker progressTracker;
    private final RetryManager retryManager;
    private final ResourceMonitor resourceMonitor;
    
    @Autowired(required = false)
    private VideoDownloadQueueManager videoQueueManager;
    
    @Autowired
    public EnhancedDownloadManager(
            ConcurrentDownloadExecutor concurrentExecutor,
            ProgressTracker progressTracker,
            RetryManager retryManager,
            ResourceMonitor resourceMonitor) {
        this.concurrentExecutor = concurrentExecutor;
        this.progressTracker = progressTracker;
        this.retryManager = retryManager;
        this.resourceMonitor = resourceMonitor;
    }
    
    /**
     * 开始下载视频（支持多分P并发下载）
     * @param videoUrl 视频URL
     * @param parts 分P列表
     * @param config 下载配置
     * @return 下载任务的Future
     */
    public CompletableFuture<DownloadResult> startDownload(
            String videoUrl, List<VideoPart> parts, DownloadConfig config) {
        
        log.info("Starting enhanced download for video: {}, parts: {}", videoUrl, parts.size());
        
        // 检查系统资源
        if (!resourceMonitor.hasEnoughResources(parts.size(), config)) {
            return CompletableFuture.completedFuture(
                DownloadResult.failure("Insufficient system resources"));
        }
        
        // 直接执行下载（移除断点续传功能）
        return concurrentExecutor.downloadPartsAsync(parts, java.util.Collections.emptyList(), config)
            .thenApply(results -> {
                log.info("Download completed for video: {}", videoUrl);
                return DownloadResult.success(results);
            })
            .exceptionally(throwable -> {
                log.error("Download failed for video: {}", videoUrl, throwable);
                return DownloadResult.failure(throwable.getMessage());
            });
    }
    
    /**
     * 通过视频队列管理器提交下载任务
     * @param request 视频下载请求
     * @return 下载任务的Future
     */
    public CompletableFuture<DownloadResult> submitVideoDownload(VideoDownloadRequest request) {
        if (videoQueueManager != null) {
            return videoQueueManager.submitVideoDownload(request);
        } else {
            // 降级到直接下载
            return startDownload(request.getVideoUrl(), request.getParts(), request.getConfig());
        }
    }
    
    /**
     * 通过视频队列管理器提交下载任务（带优先级）
     * @param request 视频下载请求
     * @param priority 优先级
     * @return 下载任务的Future
     */
    public CompletableFuture<DownloadResult> submitVideoDownload(VideoDownloadRequest request, int priority) {
        if (videoQueueManager != null) {
            return videoQueueManager.submitVideoDownload(request, priority);
        } else {
            // 降级到直接下载
            return startDownload(request.getVideoUrl(), request.getParts(), request.getConfig());
        }
    }
    
    /**
     * 暂停下载任务
     * @param taskId 任务ID
     */
    public void pauseDownload(Long taskId) {
        log.info("Pausing download for task: {}", taskId);
        concurrentExecutor.pauseTask(taskId);
    }
    
    /**
     * 取消下载任务
     * @param taskId 任务ID
     */
    public void cancelDownload(Long taskId) {
        log.info("Cancelling download for task: {}", taskId);
        
        // 尝试通过队列管理器取消
        if (videoQueueManager != null && videoQueueManager.cancelDownload(taskId)) {
            return;
        }
        
        // 降级到直接取消
        concurrentExecutor.cancelTask(taskId);
    }
    
    /**
     * 获取下载进度
     * @param taskId 任务ID
     * @return 详细进度信息
     */
    public DetailedProgress getDownloadProgress(Long taskId) {
        return progressTracker.getDetailedProgress(taskId);
    }
    
    /**
     * 获取视频任务状态
     * @param taskId 任务ID
     * @return 视频任务状态
     */
    public VideoDownloadTask getVideoTaskStatus(Long taskId) {
        if (videoQueueManager != null) {
            return videoQueueManager.getTaskStatus(taskId);
        }
        return null;
    }
    
    /**
     * 获取队列状态
     * @return 队列状态
     */
    public QueueStatus getQueueStatus() {
        if (videoQueueManager != null) {
            return videoQueueManager.getQueueStatus();
        }
        
        // 降级返回基本状态
        return QueueStatus.builder()
            .activeDownloads(concurrentExecutor.getActiveTaskCount())
            .pendingDownloads(concurrentExecutor.getQueuedTaskCount())
            .maxConcurrentVideos(1)
            .totalTasks(concurrentExecutor.getActiveTaskCount())
            .build();
    }
    
    /**
     * 暂停所有下载任务
     */
    public void pauseAllDownloads() {
        log.info("Pausing all downloads due to system resource constraints");
        concurrentExecutor.pauseAllTasks();
    }
    
    /**
     * 恢复所有暂停的下载任务
     */
    public void resumeAllDownloads() {
        log.info("Resuming all paused downloads");
        concurrentExecutor.resumeAllTasks();
    }
    
    /**
     * 检查文件是否正在使用中
     * @param filePath 文件路径
     * @return 是否正在使用
     */
    public boolean isFileInUse(String filePath) {
        return concurrentExecutor.isFileInUse(filePath);
    }
    
    /**
     * 更新并发数量
     * @param newConcurrency 新的并发数
     */
    public void updateConcurrency(int newConcurrency) {
        log.info("Updating download concurrency to: {}", newConcurrency);
        concurrentExecutor.updateConcurrency(newConcurrency);
    }
    
    /**
     * 更新视频级别最大并发数
     * @param newMaxConcurrent 新的最大并发数
     */
    public void updateMaxConcurrentVideos(int newMaxConcurrent) {
        if (videoQueueManager != null) {
            videoQueueManager.updateMaxConcurrentVideos(newMaxConcurrent);
        }
    }
    
    /**
     * 获取当前系统状态
     * @return 系统状态信息
     */
    public SystemStatus getSystemStatus() {
        return SystemStatus.builder()
            .activeDownloads(concurrentExecutor.getActiveTaskCount())
            .queuedDownloads(concurrentExecutor.getQueuedTaskCount())
            .systemResources(resourceMonitor.getCurrentResourceInfo())
            .build();
    }
}