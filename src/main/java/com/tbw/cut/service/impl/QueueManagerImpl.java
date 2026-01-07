package com.tbw.cut.service.impl;

import com.tbw.cut.dto.QueueStatus;
import com.tbw.cut.entity.QueueTaskStatus;
import com.tbw.cut.entity.QueuedTask;
import com.tbw.cut.service.QueueManager;
import com.tbw.cut.service.QueuePersistenceService;
import com.tbw.cut.service.TaskExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 队列管理器实现
 */
@Slf4j
@Service
public class QueueManagerImpl implements QueueManager {
    
    @Autowired
    private QueuePersistenceService queuePersistenceService;
    
    @Autowired
    private TaskExecutorService taskExecutorService;
    
    // 默认队列ID
    private static final String DEFAULT_QUEUE_ID = "bilibili-submission";
    
    // 处理状态
    private final AtomicReference<QueueStatus.ProcessingStatus> processingStatus = 
        new AtomicReference<>(QueueStatus.ProcessingStatus.IDLE);
    
    // 是否正在运行
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // 当前处理的任务ID
    private final AtomicReference<String> currentTaskId = new AtomicReference<>();
    
    // 处理线程
    private CompletableFuture<Void> processingFuture;
    
    // 任务间最小间隔（毫秒）
    private static final long MIN_TASK_INTERVAL_MS = 5000;
    
    @PostConstruct
    public void init() {
        log.info("队列管理器初始化");
        // 恢复队列状态
        queuePersistenceService.restoreQueue(DEFAULT_QUEUE_ID);
        // 自动启动处理
        startProcessing();
    }
    
    @PreDestroy
    public void destroy() {
        log.info("队列管理器销毁");
        stopProcessing();
    }
    
    @Override
    public void startProcessing() {
        if (running.compareAndSet(false, true)) {
            log.info("启动队列处理器");
            processingStatus.set(QueueStatus.ProcessingStatus.IDLE);
            
            // 启动异步处理循环
            processingFuture = CompletableFuture.runAsync(this::processingLoop);
        } else {
            log.warn("队列处理器已经在运行中");
        }
    }
    
    @Override
    public void stopProcessing() {
        if (running.compareAndSet(true, false)) {
            log.info("停止队列处理器");
            
            if (processingFuture != null && !processingFuture.isDone()) {
                processingFuture.cancel(true);
            }
            
            processingStatus.set(QueueStatus.ProcessingStatus.IDLE);
            currentTaskId.set(null);
        }
    }
    
    @Override
    public void processNextTask() {
        if (!running.get()) {
            log.warn("队列处理器未运行，无法处理任务");
            return;
        }
        
        if (processingStatus.get() == QueueStatus.ProcessingStatus.PROCESSING) {
            log.debug("正在处理其他任务，跳过");
            return;
        }
        
        try {
            // 获取下一个任务
            Optional<QueuedTask> nextTask = queuePersistenceService.dequeue(DEFAULT_QUEUE_ID);
            
            if (!nextTask.isPresent()) {
                // 队列为空，设置为空闲状态
                processingStatus.set(QueueStatus.ProcessingStatus.IDLE);
                currentTaskId.set(null);
                return;
            }
            
            QueuedTask task = nextTask.get();
            log.info("开始处理任务，任务ID: {}", task.getTaskId());
            
            // 更新状态
            processingStatus.set(QueueStatus.ProcessingStatus.PROCESSING);
            currentTaskId.set(task.getTaskId());
            
            // 更新任务状态为处理中
            task.setStatus(QueueTaskStatus.PROCESSING);
            task.setStartedAt(LocalDateTime.now());
            queuePersistenceService.updateTask(task.getTaskId(), task);
            
            // 异步执行任务
            CompletableFuture.runAsync(() -> executeTask(task));
            
        } catch (Exception e) {
            log.error("处理下一个任务时发生异常", e);
            processingStatus.set(QueueStatus.ProcessingStatus.ERROR);
        }
    }
    
    @Override
    public QueueStatus.ProcessingStatus getCurrentStatus() {
        return processingStatus.get();
    }
    
    @Override
    public QueueStatus getQueueStatus() {
        QueueStatus status = new QueueStatus();
        status.setQueueLength(queuePersistenceService.getQueueLength(DEFAULT_QUEUE_ID));
        status.setProcessingStatus(processingStatus.get());
        status.setCurrentTaskId(currentTaskId.get());
        status.setRunning(running.get());
        status.setLastUpdated(LocalDateTime.now());
        
        // 获取队列中的任务列表
        List<QueuedTask> queuedTasks = queuePersistenceService.getAllQueuedTasks(DEFAULT_QUEUE_ID);
        List<QueueStatus.QueueTaskInfo> taskInfos = queuedTasks.stream()
            .map(task -> new QueueStatus.QueueTaskInfo(
                task.getTaskId(), 
                task.getPosition(), 
                task.getStatus(), 
                task.getQueuedAt()))
            .collect(Collectors.toList());
        
        status.setQueuedTasks(taskInfos);
        
        return status;
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public boolean isProcessing() {
        return processingStatus.get() == QueueStatus.ProcessingStatus.PROCESSING;
    }
    
    @Override
    public String getCurrentTaskId() {
        return currentTaskId.get();
    }
    
    @Override
    public void pauseProcessing() {
        log.info("暂停队列处理");
        processingStatus.set(QueueStatus.ProcessingStatus.PAUSED);
    }
    
    @Override
    public void resumeProcessing() {
        log.info("恢复队列处理");
        if (running.get()) {
            processingStatus.set(QueueStatus.ProcessingStatus.IDLE);
        }
    }
    
    @Override
    public void forceStopCurrentTask() {
        String taskId = currentTaskId.get();
        if (taskId != null) {
            log.warn("强制停止当前任务，任务ID: {}", taskId);
            
            // 更新任务状态为失败
            Optional<QueuedTask> task = queuePersistenceService.getTask(taskId);
            if (task.isPresent()) {
                QueuedTask queuedTask = task.get();
                queuedTask.setStatus(QueueTaskStatus.FAILED);
                queuedTask.setErrorMessage("任务被强制停止");
                queuedTask.setCompletedAt(LocalDateTime.now());
                queuePersistenceService.updateTask(taskId, queuedTask);
            }
            
            // 重置状态
            processingStatus.set(QueueStatus.ProcessingStatus.IDLE);
            currentTaskId.set(null);
        }
    }
    
    /**
     * 处理循环
     */
    private void processingLoop() {
        log.info("队列处理循环启动");
        
        while (running.get()) {
            try {
                // 检查是否暂停
                if (processingStatus.get() == QueueStatus.ProcessingStatus.PAUSED) {
                    Thread.sleep(1000);
                    continue;
                }
                
                // 处理下一个任务
                processNextTask();
                
                // 等待一段时间再检查下一个任务
                Thread.sleep(2000);
                
            } catch (InterruptedException e) {
                log.info("队列处理循环被中断");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("队列处理循环发生异常", e);
                processingStatus.set(QueueStatus.ProcessingStatus.ERROR);
                
                try {
                    Thread.sleep(5000); // 错误后等待5秒再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("队列处理循环结束");
    }
    
    /**
     * 执行任务
     */
    private void executeTask(QueuedTask queuedTask) {
        String taskId = queuedTask.getTaskId();
        
        try {
            log.info("开始执行任务，任务ID: {}", taskId);
            
            // 调用任务执行服务
            boolean success = taskExecutorService.executeSubmissionTask(taskId);
            
            // 更新任务状态
            queuedTask.setCompletedAt(LocalDateTime.now());
            
            if (success) {
                queuedTask.setStatus(QueueTaskStatus.COMPLETED);
                log.info("任务执行成功，任务ID: {}", taskId);
            } else {
                queuedTask.setStatus(QueueTaskStatus.FAILED);
                queuedTask.setErrorMessage("任务执行失败");
                log.error("任务执行失败，任务ID: {}", taskId);
            }
            
            queuePersistenceService.updateTask(taskId, queuedTask);
            
        } catch (Exception e) {
            log.error("执行任务时发生异常，任务ID: {}", taskId, e);
            
            // 更新任务状态为失败
            queuedTask.setStatus(QueueTaskStatus.FAILED);
            queuedTask.setErrorMessage("任务执行异常: " + e.getMessage());
            queuedTask.setCompletedAt(LocalDateTime.now());
            queuePersistenceService.updateTask(taskId, queuedTask);
            
        } finally {
            // 重置处理状态
            processingStatus.set(QueueStatus.ProcessingStatus.IDLE);
            currentTaskId.set(null);
            
            // 任务间间隔
            try {
                Thread.sleep(MIN_TASK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}