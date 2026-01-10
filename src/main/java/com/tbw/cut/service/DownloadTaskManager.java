package com.tbw.cut.service;

import com.tbw.cut.config.DownloadConfig;
import com.tbw.cut.entity.DownloadTask;
import com.tbw.cut.entity.VideoDownload;
import com.tbw.cut.event.DownloadStatusChangeEvent;
import com.tbw.cut.service.impl.FrontendPartDownloadServiceImpl;
import com.tbw.cut.service.impl.VideoDownloadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    
    @Autowired
    private FrontendPartDownloadService frontendPartDownloadService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * 下载线程池
     */
    private ExecutorService downloadThreadPool;
    
    /**
     * 下载队列
     */
    private BlockingQueue<Object> downloadQueue;
    
    /**
     * 正在下载的任务映射
     */
    private ConcurrentHashMap<Long, Object> downloadingTasks;
    
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
                    Object downloadTask = downloadQueue.take();
                    
                    // 将任务加入正在下载映射
                    Long taskId = getTaskId(downloadTask);
                    if (taskId != null) {
                        downloadingTasks.put(taskId, downloadTask);
                    }
                    
                    // 提交到线程池执行
                    downloadThreadPool.submit(new DownloadTaskRunnable(downloadTask));
                } catch (InterruptedException e) {
                    log.warn("队列处理器被中断");
                    // 不设置中断状态，让循环继续运行
                    // Thread.currentThread().interrupt();
                    // break;
                } catch (Exception e) {
                    log.error("处理下载队列任务时发生错误", e);
                }
            }
            log.info("队列处理器线程结束");
        });
        
        queueProcessor.setName("DownloadQueueProcessor");
        // 移除守护线程设置，确保线程能够持续运行
        // queueProcessor.setDaemon(true);
        queueProcessor.start();
    }
    
    /**
     * 获取任务ID
     */
    private Long getTaskId(Object task) {
        if (task instanceof VideoDownload) {
            return ((VideoDownload) task).getId();
        } else if (task instanceof DownloadTask) {
            return ((DownloadTask) task).getId();
        }
        return null;
    }
    
    /**
     * 将下载任务添加到队列
     * @param downloadTask 下载任务
     * @return 是否添加成功
     */
    public boolean addDownloadTask(Object downloadTask) {
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
    public java.util.List<Object> getDownloadingTasks() {
        return new java.util.ArrayList<>(downloadingTasks.values());
    }
    
    /**
     * 下载任务执行类
     */
    private class DownloadTaskRunnable implements Runnable {
        private final Object downloadTask;
        
        public DownloadTaskRunnable(Object downloadTask) {
            this.downloadTask = downloadTask;
        }
        
        @Override
        public void run() {
            try {
                log.info("开始处理下载任务");
                
                // 根据任务类型执行相应的下载逻辑
                if (downloadTask instanceof DownloadTask) {
                    // 处理分P下载任务
                    DownloadTask task = (DownloadTask) downloadTask;
                    log.info("开始执行分P下载任务: {}", task.getId());
                    
                    // 直接执行下载逻辑
                    if (frontendPartDownloadService != null) {
                        frontendPartDownloadService.executeDownload(task);
                    } else {
                        log.error("FrontendPartDownloadService未正确注入");
                    }
                } else if (downloadTask instanceof VideoDownload) {
                    // 处理普通视频下载任务
                    VideoDownload task = (VideoDownload) downloadTask;
                    log.info("开始执行普通下载任务: {}", task.getId());
                    
                    // 更新任务状态为下载中
                    VideoDownload currentTask = videoDownloadService.getById(task.getId());
                    if (currentTask != null) {
                        currentTask.setStatus(1); // 下载中
                        currentTask.setUpdateTime(java.time.LocalDateTime.now());
                        videoDownloadService.updateById(currentTask);
                        log.info("成功更新任务状态为下载中，任务ID: {}", task.getId());
                    }
                    
                    // 这里可以添加具体的下载逻辑
                    // 模拟下载完成
                    Thread.sleep(1000); // 模拟处理时间
                    
                    // 更新任务为完成状态
                    VideoDownload completedTask = videoDownloadService.getById(task.getId());
                    if (completedTask != null) {
                        completedTask.setStatus(2); // 完成
                        completedTask.setProgress(100);
                        completedTask.setUpdateTime(java.time.LocalDateTime.now());
                        videoDownloadService.updateById(completedTask);
                        log.info("下载任务完成: {}", task.getId());
                        
                        // **新增：发布下载完成事件**
                        try {
                            DownloadStatusChangeEvent event = DownloadStatusChangeEvent.create(task.getId(), null, 2);
                            eventPublisher.publishEvent(event);
                            log.info("Published download completion event: taskId={}", task.getId());
                        } catch (Exception e) {
                            log.error("Failed to publish download completion event: taskId={}", task.getId(), e);
                            // 不抛出异常，避免影响主流程
                        }
                    }
                } else {
                    log.warn("未知的任务类型: {}", downloadTask.getClass().getName());
                }
            } catch (Exception e) {
                log.error("下载任务执行失败", e);
                // 处理任务失败的情况
                handleTaskFailure(downloadTask, e);
            } finally {
                // 从正在下载映射中移除
                Long taskId = getTaskId(downloadTask);
                if (taskId != null) {
                    downloadingTasks.remove(taskId);
                }
            }
        }
        
        /**
         * 处理任务失败的情况
         */
        private void handleTaskFailure(Object task, Exception e) {
            try {
                Long taskId = getTaskId(task);
                if (taskId != null) {
                    VideoDownload failedTask = videoDownloadService.getById(taskId);
                    if (failedTask != null) {
                        failedTask.setStatus(3); // 失败
                        failedTask.setUpdateTime(java.time.LocalDateTime.now());
                        videoDownloadService.updateById(failedTask);
                        
                        // **新增：发布下载失败事件**
                        try {
                            DownloadStatusChangeEvent event = DownloadStatusChangeEvent.create(taskId, null, 3);
                            eventPublisher.publishEvent(event);
                            log.info("Published download failure event: taskId={}", taskId);
                        } catch (Exception eventException) {
                            log.error("Failed to publish download failure event: taskId={}", taskId, eventException);
                            // 不抛出异常，避免影响主流程
                        }
                    }
                }
            } catch (Exception updateException) {
                log.error("更新任务失败状态时发生错误", updateException);
            }
        }
    }
}