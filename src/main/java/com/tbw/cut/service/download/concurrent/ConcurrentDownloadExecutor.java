package com.tbw.cut.service.download.concurrent;

import com.tbw.cut.service.download.model.DownloadConfig;
import com.tbw.cut.service.download.model.VideoPart;
import com.tbw.cut.utils.EnhancedFFmpegUtil;
import com.tbw.cut.service.download.progress.ProgressTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.nio.file.Paths;

/**
 * 并发下载执行器
 * 管理多个下载任务的并发执行
 */
@Slf4j
@Service
public class ConcurrentDownloadExecutor {
    
    @Value("${download.max-concurrent-tasks:3}")
    private int maxConcurrentTasks;
    
    @Value("${download.max-concurrent-parts:2}")
    private int maxConcurrentParts;
    
    @Autowired
    private EnhancedFFmpegUtil enhancedFFmpegUtil;
    
    @Autowired
    private ProgressTracker progressTracker;
    
    private ThreadPoolExecutor downloadThreadPool;
    private Semaphore concurrencyLimiter;
    private final Map<Long, CompletableFuture<String>> activeTasks = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> pausedTasks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> filesInUse = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        // 创建线程池
        this.downloadThreadPool = new ThreadPoolExecutor(
            maxConcurrentTasks,
            maxConcurrentTasks * 2,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r, "download-executor-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建并发限制器
        this.concurrencyLimiter = new Semaphore(maxConcurrentTasks);
        
        log.info("ConcurrentDownloadExecutor initialized with max concurrent tasks: {}", maxConcurrentTasks);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ConcurrentDownloadExecutor");
        
        // 取消所有活跃任务
        activeTasks.values().forEach(future -> future.cancel(true));
        activeTasks.clear();
        
        // 关闭线程池
        downloadThreadPool.shutdown();
        try {
            if (!downloadThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                downloadThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 异步下载多个分P
     * @param newParts 新的分P列表
     * @param resumableParts 可恢复的分P列表
     * @param config 下载配置
     * @return 下载结果的Future
     */
    public CompletableFuture<List<String>> downloadPartsAsync(
            List<VideoPart> newParts, List<VideoPart> resumableParts, DownloadConfig config) {
        
        log.info("Starting concurrent download: {} new parts, {} resumable parts", 
            newParts.size(), resumableParts.size());
        
        // 创建所有分P的下载任务
        List<CompletableFuture<String>> partFutures = newParts.stream()
            .map(part -> downloadPartAsync(part, config, false))
            .collect(Collectors.toList());
        
        // 添加可恢复的分P任务
        List<CompletableFuture<String>> resumeFutures = resumableParts.stream()
            .map(part -> downloadPartAsync(part, config, true))
            .collect(Collectors.toList());
        
        partFutures.addAll(resumeFutures);
        
        // 等待所有分P下载完成
        return CompletableFuture.allOf(partFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> partFutures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result != null && !result.isEmpty())
                .collect(Collectors.toList()));
    }
    
    /**
     * 异步下载单个分P
     * @param part 分P信息
     * @param config 下载配置
     * @param isResume 是否为恢复下载
     * @return 下载结果的Future
     */
    private CompletableFuture<String> downloadPartAsync(VideoPart part, DownloadConfig config, boolean isResume) {
        Long taskId = generateTaskId(part);
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 获取并发许可
                concurrencyLimiter.acquire();
                
                // 标记文件正在使用
                if (part.getOutputPath() != null) {
                    filesInUse.put(part.getOutputPath(), true);
                }
                
                log.info("Starting {} download for part: {} (CID: {})", 
                    isResume ? "resume" : "new", part.getTitle(), part.getCid());
                
                // 检查是否被暂停
                if (pausedTasks.getOrDefault(taskId, false)) {
                    log.info("Task {} is paused, skipping download", taskId);
                    return null;
                }
                
                // 执行实际下载
                return executePartDownload(part, config, isResume);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Download interrupted for part: {}", part.getTitle());
                return null;
            } catch (Exception e) {
                log.error("Download failed for part: {}", part.getTitle(), e);
                throw new RuntimeException("Download failed: " + e.getMessage(), e);
            } finally {
                // 释放资源
                concurrencyLimiter.release();
                if (part.getOutputPath() != null) {
                    filesInUse.remove(part.getOutputPath());
                }
                activeTasks.remove(taskId);
            }
        }, downloadThreadPool);
        
        activeTasks.put(taskId, future);
        return future;
    }
    
    /**
     * 执行分P下载
     * @param part 分P信息
     * @param config 下载配置
     * @param isResume 是否为恢复下载
     * @return 下载文件路径
     */
    private String executePartDownload(VideoPart part, DownloadConfig config, boolean isResume) {
        log.info("执行{}下载: cid={}, databaseId={}, title={}, url={}", 
                isResume ? "恢复" : "新", part.getCid(), part.getDatabaseId(), part.getTitle(), part.getUrl());
        
        try {
            Long taskId = generateTaskId(part);
            log.info("生成的任务ID: {}, 对应数据库记录ID: {}", taskId, part.getDatabaseId());
            
            // 获取视频总时长
            long totalDuration = enhancedFFmpegUtil.getDurationByFFprobe(part.getUrl());
            log.info("获取到视频总时长: {} 微秒", totalDuration);
            
            // 确定输出目录和文件名
            String outputDir = Paths.get(part.getOutputPath()).getParent().toString();
            String outputFileName = Paths.get(part.getOutputPath()).getFileName().toString();
            
            log.info("开始下载: taskId={}, outputDir={}, outputFileName={}", taskId, outputDir, outputFileName);
            
            // 使用增强的FFmpeg工具进行下载，带进度回调
            String downloadedPath = enhancedFFmpegUtil.downloadVideoToDirectoryWithProgress(
                part.getUrl(), 
                outputFileName, 
                outputDir, 
                totalDuration,
                new com.tbw.cut.utils.FFmpegUtil.ProgressCallback() {
                    @Override
                    public void onProgress(int progress) {
                        // 更新进度跟踪器 - 使用百分比计算字节数
                        long estimatedCurrentBytes = totalDuration > 0 ? (totalDuration * progress / 100) : 0;
                        log.info("FFmpeg进度回调: taskId={}, progress={}%, estimatedBytes={}", taskId, progress, estimatedCurrentBytes);
                        progressTracker.updateProgress(taskId, estimatedCurrentBytes, totalDuration);
                    }
                }
            );
            
            if (downloadedPath != null) {
                // 验证下载文件
                java.io.File downloadedFile = new java.io.File(downloadedPath);
                if (downloadedFile.exists() && downloadedFile.length() > 0) {
                    log.info("分P下载成功: {} -> {}", part.getTitle(), downloadedPath);
                    
                    // 更新最终进度为100%
                    progressTracker.updateProgress(taskId, totalDuration, totalDuration);
                    
                    return downloadedPath;
                } else {
                    throw new RuntimeException("下载文件验证失败，文件不存在或大小为0");
                }
            } else {
                throw new RuntimeException("下载失败，返回路径为空");
            }
            
        } catch (Exception e) {
            log.error("下载分P失败: {}", part.getTitle(), e);
            throw new RuntimeException("下载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 暂停任务
     * @param taskId 任务ID
     */
    public void pauseTask(Long taskId) {
        log.info("Pausing task: {}", taskId);
        pausedTasks.put(taskId, true);
        
        CompletableFuture<String> future = activeTasks.get(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }
    
    /**
     * 取消任务
     * @param taskId 任务ID
     */
    public void cancelTask(Long taskId) {
        log.info("Cancelling task: {}", taskId);
        pausedTasks.remove(taskId);
        
        CompletableFuture<String> future = activeTasks.remove(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }
    
    /**
     * 暂停所有任务
     */
    public void pauseAllTasks() {
        log.info("Pausing all active tasks");
        activeTasks.keySet().forEach(taskId -> pausedTasks.put(taskId, true));
        activeTasks.values().forEach(future -> future.cancel(true));
    }
    
    /**
     * 恢复所有暂停的任务
     */
    public void resumeAllTasks() {
        log.info("Resuming all paused tasks");
        pausedTasks.clear();
    }
    
    /**
     * 检查文件是否正在使用
     * @param filePath 文件路径
     * @return 是否正在使用
     */
    public boolean isFileInUse(String filePath) {
        return filesInUse.getOrDefault(filePath, false);
    }
    
    /**
     * 更新并发数量
     * @param newConcurrency 新的并发数
     */
    public void updateConcurrency(int newConcurrency) {
        if (newConcurrency <= 0 || newConcurrency > 10) {
            log.warn("Invalid concurrency value: {}, ignoring", newConcurrency);
            return;
        }
        
        this.maxConcurrentTasks = newConcurrency;
        
        // 更新线程池大小
        downloadThreadPool.setCorePoolSize(newConcurrency);
        downloadThreadPool.setMaximumPoolSize(newConcurrency * 2);
        
        // 更新信号量
        int currentPermits = concurrencyLimiter.availablePermits() + concurrencyLimiter.getQueueLength();
        int permitsToAdd = newConcurrency - currentPermits;
        
        if (permitsToAdd > 0) {
            concurrencyLimiter.release(permitsToAdd);
        } else if (permitsToAdd < 0) {
            try {
                concurrencyLimiter.acquire(-permitsToAdd);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("Updated concurrency to: {}", newConcurrency);
    }
    
    /**
     * 获取活跃任务数量
     * @return 活跃任务数量
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }
    
    /**
     * 获取队列中的任务数量
     * @return 队列任务数量
     */
    public int getQueuedTaskCount() {
        return downloadThreadPool.getQueue().size();
    }
    
    /**
     * 生成任务ID
     * @param part 分P信息
     * @return 任务ID
     */
    private Long generateTaskId(VideoPart part) {
        // 优先使用数据库ID，确保与VideoDownload记录关联
        if (part.getDatabaseId() != null) {
            return part.getDatabaseId();
        }
        // 如果没有数据库ID，使用CID作为备选
        return part.getCid() != null ? part.getCid() : System.currentTimeMillis();
    }
}