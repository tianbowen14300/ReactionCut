package com.tbw.cut.service.download.queue;

import com.tbw.cut.service.download.model.*;
import com.tbw.cut.service.download.segmented.SegmentedDownloadManager;
import com.tbw.cut.service.download.logging.DownloadTimeLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 视频下载队列管理器
 * 管理视频级别的并发控制和队列调度
 */
@Slf4j
@Service
public class VideoDownloadQueueManager {
    
    @Value("${download.max-concurrent-videos:2}")
    private int maxConcurrentVideos;
    
    @Value("${download.queue-capacity:100}")
    private int queueCapacity;
    
    @Autowired
    private SegmentedDownloadManager segmentedDownloadManager;
    
    @Autowired
    private DownloadTimeLogger downloadTimeLogger;
    
    // 待下载队列（优先级队列）
    private final PriorityBlockingQueue<VideoDownloadTask> pendingQueue = 
        new PriorityBlockingQueue<>(100, (t1, t2) -> {
            // 优先级排序：数值小的优先级高
            int priorityCompare = Integer.compare(t1.getPriority(), t2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 相同优先级按创建时间排序
            return t1.getCreatedTime().compareTo(t2.getCreatedTime());
        });
    
    // 正在下载的任务
    private final Map<Long, VideoDownloadTask> activeDownloads = new ConcurrentHashMap<>();
    
    // 所有任务（用于状态查询）
    private final Map<Long, VideoDownloadTask> allTasks = new ConcurrentHashMap<>();
    
    // 任务ID生成器
    private final AtomicLong taskIdGenerator = new AtomicLong(System.currentTimeMillis());
    
    // 队列处理线程池
    private ScheduledExecutorService queueProcessor;
    
    @PostConstruct
    public void initialize() {
        // 创建队列处理线程
        this.queueProcessor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "video-queue-processor");
            t.setDaemon(true);
            return t;
        });
        
        // 定期检查队列状态
        queueProcessor.scheduleWithFixedDelay(this::processQueue, 1, 1, TimeUnit.SECONDS);
        
        log.info("VideoDownloadQueueManager initialized with max concurrent videos: {}", maxConcurrentVideos);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down VideoDownloadQueueManager");
        
        // 取消所有待处理任务
        while (!pendingQueue.isEmpty()) {
            VideoDownloadTask task = pendingQueue.poll();
            if (task != null) {
                task.cancel("System shutdown");
            }
        }
        
        // 取消所有活跃任务
        activeDownloads.values().forEach(task -> task.cancel("System shutdown"));
        
        // 关闭线程池
        if (queueProcessor != null) {
            queueProcessor.shutdown();
            try {
                if (!queueProcessor.awaitTermination(10, TimeUnit.SECONDS)) {
                    queueProcessor.shutdownNow();
                }
            } catch (InterruptedException e) {
                queueProcessor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 提交视频下载任务
     * @param request 下载请求
     * @return 下载任务Future
     */
    public CompletableFuture<DownloadResult> submitVideoDownload(VideoDownloadRequest request) {
        return submitVideoDownload(request, 0);
    }
    
    /**
     * 提交视频下载任务（带优先级）
     * @param request 下载请求
     * @param priority 优先级（数值越小优先级越高）
     * @return 下载任务Future
     */
    public CompletableFuture<DownloadResult> submitVideoDownload(VideoDownloadRequest request, int priority) {
        // 检查队列容量
        if (pendingQueue.size() + activeDownloads.size() >= queueCapacity) {
            CompletableFuture<DownloadResult> future = new CompletableFuture<>();
            future.complete(DownloadResult.failure("Download queue is full"));
            return future;
        }
        
        // 创建下载任务
        VideoDownloadTask task = VideoDownloadTask.create(request);
        task.setTaskId(taskIdGenerator.incrementAndGet());
        task.setPriority(priority);
        
        // 添加到任务映射
        allTasks.put(task.getTaskId(), task);
        
        log.info("Submitted video download task: {} (title: {}, priority: {})", 
            task.getTaskId(), request.getVideoTitle(), priority);
        
        // 检查是否可以立即开始下载
        if (activeDownloads.size() < maxConcurrentVideos) {
            startVideoDownload(task);
        } else {
            // 加入等待队列
            pendingQueue.offer(task);
            log.info("Task {} added to pending queue (queue size: {})", 
                task.getTaskId(), pendingQueue.size());
        }
        
        return task.getFuture();
    }
    
    /**
     * 开始视频下载
     * @param task 下载任务
     */
    private void startVideoDownload(VideoDownloadTask task) {
        activeDownloads.put(task.getTaskId(), task);
        task.setStatus(VideoDownloadTask.TaskStatus.DOWNLOADING);
        
        // 记录队列等待时长
        long waitTime = System.currentTimeMillis() - task.getCreatedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        downloadTimeLogger.logQueueWaitTime(task.getTaskId(), waitTime);
        
        // 记录下载开始
        VideoDownloadRequest request = task.getRequest();
        downloadTimeLogger.logDownloadStart(
            task.getTaskId(),
            request.getVideoTitle(),
            request.getVideoUrl(),
            request.getTotalEstimatedSize(),
            request.isEnableSegmentedDownload() ? 4 : 1 // 估算分段数
        );
        
        log.info("Starting video download: {} (active: {}/{})", 
            task.getTaskId(), activeDownloads.size(), maxConcurrentVideos);
        
        // 异步执行下载
        CompletableFuture<DownloadResult> downloadFuture = 
            segmentedDownloadManager.downloadVideo(task.getRequest());
        
        downloadFuture.whenComplete((result, throwable) -> {
            // 下载完成处理
            handleDownloadCompletion(task, result, throwable);
        });
    }
    
    /**
     * 处理下载完成
     * @param task 下载任务
     * @param result 下载结果
     * @param throwable 异常
     */
    private void handleDownloadCompletion(VideoDownloadTask task, DownloadResult result, Throwable throwable) {
        // 从活跃下载中移除
        activeDownloads.remove(task.getTaskId());
        
        // 更新任务状态
        if (throwable != null) {
            result = DownloadResult.failure("Download failed: " + throwable.getMessage());
        }
        
        // 记录下载完成
        downloadTimeLogger.logDownloadComplete(
            task.getTaskId(),
            result.isSuccess(),
            result.isSuccess() ? null : result.getErrorMessage(),
            result.isSuccess() ? task.getRequest().getTotalEstimatedSize() : 0
        );
        
        task.complete(result);
        
        log.info("Video download completed: {} (success: {}, active: {}/{})", 
            task.getTaskId(), result.isSuccess(), activeDownloads.size(), maxConcurrentVideos);
        
        // 启动队列中的下一个任务
        processQueue();
    }
    
    /**
     * 处理队列，启动待处理的任务
     */
    private void processQueue() {
        while (activeDownloads.size() < maxConcurrentVideos && !pendingQueue.isEmpty()) {
            VideoDownloadTask nextTask = pendingQueue.poll();
            if (nextTask != null && nextTask.getStatus() == VideoDownloadTask.TaskStatus.PENDING) {
                startVideoDownload(nextTask);
            }
        }
    }
    
    /**
     * 取消下载任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelDownload(Long taskId) {
        VideoDownloadTask task = allTasks.get(taskId);
        if (task == null) {
            return false;
        }
        
        // 从队列中移除
        pendingQueue.remove(task);
        
        // 从活跃下载中移除
        activeDownloads.remove(taskId);
        
        // 取消任务
        task.cancel("User cancelled");
        
        log.info("Cancelled video download task: {}", taskId);
        
        // 处理队列
        processQueue();
        
        return true;
    }
    
    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    public VideoDownloadTask getTaskStatus(Long taskId) {
        return allTasks.get(taskId);
    }
    
    /**
     * 获取队列状态
     * @return 队列状态
     */
    public QueueStatus getQueueStatus() {
        return QueueStatus.builder()
            .activeDownloads(activeDownloads.size())
            .pendingDownloads(pendingQueue.size())
            .maxConcurrentVideos(maxConcurrentVideos)
            .totalTasks(allTasks.size())
            .build();
    }
    
    /**
     * 更新最大并发数
     * @param newMaxConcurrent 新的最大并发数
     */
    public void updateMaxConcurrentVideos(int newMaxConcurrent) {
        if (newMaxConcurrent <= 0 || newMaxConcurrent > 10) {
            log.warn("Invalid max concurrent videos: {}, ignoring", newMaxConcurrent);
            return;
        }
        
        int oldMax = this.maxConcurrentVideos;
        this.maxConcurrentVideos = newMaxConcurrent;
        
        log.info("Updated max concurrent videos from {} to {}", oldMax, newMaxConcurrent);
        
        // 如果增加了并发数，尝试启动更多任务
        if (newMaxConcurrent > oldMax) {
            processQueue();
        }
    }
    
    /**
     * 清理已完成的任务（保留最近的100个）
     */
    public void cleanupCompletedTasks() {
        if (allTasks.size() <= 100) {
            return;
        }
        
        // 移除最老的已完成任务
        allTasks.entrySet().removeIf(entry -> {
            VideoDownloadTask task = entry.getValue();
            return task.getStatus() == VideoDownloadTask.TaskStatus.COMPLETED ||
                   task.getStatus() == VideoDownloadTask.TaskStatus.FAILED ||
                   task.getStatus() == VideoDownloadTask.TaskStatus.CANCELLED;
        });
        
        log.info("Cleaned up completed tasks, remaining: {}", allTasks.size());
    }
}