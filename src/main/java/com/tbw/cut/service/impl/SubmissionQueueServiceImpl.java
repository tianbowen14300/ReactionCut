package com.tbw.cut.service.impl;

import com.tbw.cut.dto.QueuePosition;
import com.tbw.cut.dto.QueueStatus;
import com.tbw.cut.entity.QueueTaskStatus;
import com.tbw.cut.entity.QueuedTask;
import com.tbw.cut.service.QueueManager;
import com.tbw.cut.service.QueuePersistenceService;
import com.tbw.cut.service.SubmissionQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 投稿队列服务实现
 */
@Slf4j
@Service
public class SubmissionQueueServiceImpl implements SubmissionQueueService {
    
    @Autowired
    private QueuePersistenceService queuePersistenceService;
    
    @Autowired
    private QueueManager queueManager;
    
    // 默认队列ID
    private static final String DEFAULT_QUEUE_ID = "bilibili-submission";
    
    // 预估每个任务的处理时间（分钟）
    private static final long ESTIMATED_TASK_DURATION_MINUTES = 10;
    
    @Override
    public QueuePosition enqueueTask(String taskId) {
        try {
            log.info("任务入队，任务ID: {}", taskId);
            
            // 检查任务是否已经在队列中
            if (isTaskInQueue(taskId)) {
                log.warn("任务已在队列中，任务ID: {}", taskId);
                return getTaskPosition(taskId);
            }
            
            // 创建队列任务
            QueuedTask queuedTask = new QueuedTask(taskId, DEFAULT_QUEUE_ID);
            queuedTask.setQueuedAt(LocalDateTime.now());
            queuedTask.setStatus(QueueTaskStatus.QUEUED);
            
            // 添加到队列
            boolean success = queuePersistenceService.enqueue(queuedTask);
            
            if (success) {
                // 获取队列位置
                int position = queuePersistenceService.getTaskPosition(taskId, DEFAULT_QUEUE_ID);
                int queueLength = queuePersistenceService.getQueueLength(DEFAULT_QUEUE_ID);
                
                QueuePosition queuePosition = new QueuePosition(taskId, position, queueLength);
                queuePosition.setQueuedAt(queuedTask.getQueuedAt());
                
                // 计算预估等待时间
                long estimatedWaitMinutes = (position - 1) * ESTIMATED_TASK_DURATION_MINUTES;
                queuePosition.setEstimatedWaitMinutes(estimatedWaitMinutes);
                
                log.info("任务入队成功，任务ID: {}, 位置: {}, 队列长度: {}, 预估等待: {}分钟", 
                    taskId, position, queueLength, estimatedWaitMinutes);
                
                return queuePosition;
            } else {
                log.error("任务入队失败，任务ID: {}", taskId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("任务入队时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    @Override
    public QueueStatus getQueueStatus() {
        try {
            return queueManager.getQueueStatus();
        } catch (Exception e) {
            log.error("获取队列状态时发生异常", e);
            return new QueueStatus(0, QueueStatus.ProcessingStatus.ERROR);
        }
    }
    
    @Override
    public QueuePosition getTaskPosition(String taskId) {
        try {
            int position = queuePersistenceService.getTaskPosition(taskId, DEFAULT_QUEUE_ID);
            
            if (position <= 0) {
                return null; // 任务不在队列中
            }
            
            int queueLength = queuePersistenceService.getQueueLength(DEFAULT_QUEUE_ID);
            QueuePosition queuePosition = new QueuePosition(taskId, position, queueLength);
            
            // 获取任务详细信息
            Optional<QueuedTask> task = queuePersistenceService.getTask(taskId);
            if (task.isPresent()) {
                queuePosition.setQueuedAt(task.get().getQueuedAt());
            }
            
            // 计算预估等待时间
            long estimatedWaitMinutes = (position - 1) * ESTIMATED_TASK_DURATION_MINUTES;
            queuePosition.setEstimatedWaitMinutes(estimatedWaitMinutes);
            
            return queuePosition;
            
        } catch (Exception e) {
            log.error("获取任务位置时发生异常，任务ID: {}", taskId, e);
            return null;
        }
    }
    
    @Override
    public boolean cancelQueuedTask(String taskId) {
        try {
            log.info("取消排队任务，任务ID: {}", taskId);
            
            // 检查任务是否在队列中
            if (!isTaskInQueue(taskId)) {
                log.warn("任务不在队列中，无法取消，任务ID: {}", taskId);
                return false;
            }
            
            // 检查任务是否正在处理
            String currentTaskId = queueManager.getCurrentTaskId();
            if (taskId.equals(currentTaskId)) {
                log.warn("任务正在处理中，无法取消，任务ID: {}", taskId);
                return false;
            }
            
            // 更新任务状态为已取消
            Optional<QueuedTask> task = queuePersistenceService.getTask(taskId);
            if (task.isPresent()) {
                QueuedTask queuedTask = task.get();
                queuedTask.setStatus(QueueTaskStatus.CANCELLED);
                queuedTask.setCompletedAt(LocalDateTime.now());
                queuePersistenceService.updateTask(taskId, queuedTask);
            }
            
            // 从队列中移除
            boolean success = queuePersistenceService.removeTask(taskId, DEFAULT_QUEUE_ID);
            
            if (success) {
                log.info("任务取消成功，任务ID: {}", taskId);
            } else {
                log.error("任务取消失败，任务ID: {}", taskId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("取消排队任务时发生异常，任务ID: {}", taskId, e);
            return false;
        }
    }
    
    @Override
    public boolean isTaskInQueue(String taskId) {
        try {
            int position = queuePersistenceService.getTaskPosition(taskId, DEFAULT_QUEUE_ID);
            return position > 0;
        } catch (Exception e) {
            log.error("检查任务是否在队列中时发生异常，任务ID: {}", taskId, e);
            return false;
        }
    }
    
    @Override
    public int getQueueLength() {
        try {
            return queuePersistenceService.getQueueLength(DEFAULT_QUEUE_ID);
        } catch (Exception e) {
            log.error("获取队列长度时发生异常", e);
            return 0;
        }
    }
    
    @Override
    public boolean clearQueue() {
        try {
            log.warn("清空队列");
            
            // 检查是否有任务正在处理
            if (queueManager.isProcessing()) {
                log.warn("有任务正在处理，无法清空队列");
                return false;
            }
            
            boolean success = queuePersistenceService.clearQueue(DEFAULT_QUEUE_ID);
            
            if (success) {
                log.info("队列清空成功");
            } else {
                log.error("队列清空失败");
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("清空队列时发生异常", e);
            return false;
        }
    }
    
    @Override
    public void startQueue() {
        try {
            log.info("启动队列处理");
            queueManager.startProcessing();
        } catch (Exception e) {
            log.error("启动队列处理时发生异常", e);
        }
    }
    
    @Override
    public void stopQueue() {
        try {
            log.info("停止队列处理");
            queueManager.stopProcessing();
        } catch (Exception e) {
            log.error("停止队列处理时发生异常", e);
        }
    }
    
    @Override
    public void pauseQueue() {
        try {
            log.info("暂停队列处理");
            queueManager.pauseProcessing();
        } catch (Exception e) {
            log.error("暂停队列处理时发生异常", e);
        }
    }
    
    @Override
    public void resumeQueue() {
        try {
            log.info("恢复队列处理");
            queueManager.resumeProcessing();
        } catch (Exception e) {
            log.error("恢复队列处理时发生异常", e);
        }
    }
}