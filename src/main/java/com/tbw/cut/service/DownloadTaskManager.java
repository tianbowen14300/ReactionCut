package com.tbw.cut.service;

import com.tbw.cut.config.DownloadConfig;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.service.impl.VideoDownloadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class DownloadTaskManager {
    
    @Autowired
    private DownloadConfig downloadConfig;
    
    @Autowired
    private VideoDownloadService videoDownloadService;
    
    /**
     * 下载线程池
     */
    private ExecutorService downloadThreadPool;
    
    /**
     * 下载队列
     */
    private BlockingQueue<VideoDownload> downloadQueue;
    
    /**
     * 正在下载的任务映射
     */
    private ConcurrentHashMap<Long, VideoDownload> downloadingTasks;
    
    /**
     * 任务计数器
     */
    private AtomicInteger taskCounter;
    
    @PostConstruct
    public void init() {
        // 初始化线程池
        downloadThreadPool = Executors.newFixedThreadPool(downloadConfig.getThreads());
        
        // 初始化下载队列
        downloadQueue = new ArrayBlockingQueue<>(downloadConfig.getQueueSize());
        
        // 初始化正在下载的任务映射
        downloadingTasks = new ConcurrentHashMap<>();
        
        // 初始化任务计数器
        taskCounter = new AtomicInteger(0);
        
        // 启动队列处理器
        startQueueProcessor();
        
        log.info("下载任务管理器初始化完成，线程数: {}，队列大小: {}", 
                downloadConfig.getThreads(), downloadConfig.getQueueSize());
    }
    
    @PreDestroy
    public void destroy() {
        if (downloadThreadPool != null && !downloadThreadPool.isShutdown()) {
            downloadThreadPool.shutdown();
            try {
                if (!downloadThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    downloadThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                downloadThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 启动队列处理器
     */
    private void startQueueProcessor() {
        Thread queueProcessor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 从队列中取出任务
                    VideoDownload downloadTask = downloadQueue.take();
                    
                    // 将任务加入正在下载映射
                    downloadingTasks.put(downloadTask.getId(), downloadTask);
                    
                    // 更新任务状态为下载中
                    try {
                        VideoDownload taskToUpdate = videoDownloadService.getById(downloadTask.getId());
                        if (taskToUpdate != null) {
                            taskToUpdate.setStatus(1); // 下载中
                            taskToUpdate.setUpdateTime(java.time.LocalDateTime.now());
                            videoDownloadService.updateById(taskToUpdate);
                            log.info("成功更新任务状态为下载中，任务ID: {}", downloadTask.getId());
                        } else {
                            log.warn("未找到任务记录，任务ID: {}", downloadTask.getId());
                        }
                    } catch (Exception e) {
                        log.error("更新任务状态为下载中时发生错误，任务ID: {}", downloadTask.getId(), e);
                    }
                    
                    // 提交到线程池执行
                    downloadThreadPool.submit(new DownloadTaskRunnable(downloadTask));
                } catch (InterruptedException e) {
                    log.warn("队列处理器被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("处理下载队列任务时发生错误", e);
                }
            }
        });
        
        queueProcessor.setName("DownloadQueueProcessor");
        queueProcessor.setDaemon(true);
        queueProcessor.start();
    }
    
    /**
     * 将下载任务添加到队列
     * @param downloadTask 下载任务
     * @return 是否添加成功
     */
    public boolean addDownloadTask(VideoDownload downloadTask) {
        try {
            return downloadQueue.offer(downloadTask, 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("添加下载任务到队列时被中断");
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 获取队列中等待的任务数量
     * @return 等待任务数量
     */
    public int getPendingTaskCount() {
        return downloadQueue.size();
    }
    
    /**
     * 获取正在下载的任务数量
     * @return 正在下载任务数量
     */
    public int getDownloadingTaskCount() {
        return downloadingTasks.size();
    }
    
    /**
     * 获取正在下载的任务列表
     * @return 正在下载的任务列表
     */
    public java.util.List<VideoDownload> getDownloadingTasks() {
        return new java.util.ArrayList<>(downloadingTasks.values());
    }
    
    /**
     * 下载任务执行类
     */
    private class DownloadTaskRunnable implements Runnable {
        private final VideoDownload downloadTask;
        
        public DownloadTaskRunnable(VideoDownload downloadTask) {
            this.downloadTask = downloadTask;
        }
        
        @Override
        public void run() {
            try {
                // 模拟下载过程
                log.info("开始下载任务: {}", downloadTask.getId());
                
                // 模拟下载进度更新
                for (int i = 0; i <= 100; i += 10) {
                    Thread.sleep(1000); // 模拟下载时间
                    videoDownloadService.updateProgress(downloadTask.getId(), i);
                }
                
                // 模拟下载完成
                videoDownloadService.completeDownload(downloadTask.getId(), "/path/to/downloaded/file.mp4");
                log.info("下载任务完成: {}", downloadTask.getId());
            } catch (Exception e) {
                log.error("下载任务执行失败: {}", downloadTask.getId(), e);
                videoDownloadService.failDownload(downloadTask.getId(), e.getMessage());
            } finally {
                // 从正在下载映射中移除
                downloadingTasks.remove(downloadTask.getId());
            }
        }
    }
}