package com.tbw.cut.workflow.service.impl;

import com.tbw.cut.dto.QueuePosition;
import com.tbw.cut.dto.QueueStatus;
import com.tbw.cut.entity.QueueTaskStatus;
import com.tbw.cut.service.SubmissionQueueService;
import com.tbw.cut.workflow.service.EnhancedSubmissionQueue;
import com.tbw.cut.workflow.model.QueuedSubmissionTask;
import com.tbw.cut.workflow.model.WorkflowInstance;
import com.tbw.cut.workflow.model.WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 增强的投稿队列实现
 * 集成工作流完成后的投稿触发和队列优先级管理
 */
@Slf4j
@Service
public class EnhancedSubmissionQueueImpl implements EnhancedSubmissionQueue {
    
    @Autowired
    private SubmissionQueueService submissionQueueService;
    
    /**
     * 优先级队列存储
     * TODO: 后续可以替换为数据库存储
     */
    private final Map<String, QueuedSubmissionTask> enhancedTasks = new ConcurrentHashMap<>();
    
    /**
     * 暂停的优先级集合
     */
    private final Set<Integer> pausedPriorities = ConcurrentHashMap.newKeySet();
    
    /**
     * 任务计数器
     */
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    
    /**
     * 是否启用优先级处理
     */
    private volatile boolean priorityProcessingEnabled = true;
    
    @Override
    public QueuePosition enqueueFromWorkflow(WorkflowInstance workflowInstance) {
        if (workflowInstance == null) {
            throw new IllegalArgumentException("工作流实例不能为空");
        }
        
        if (workflowInstance.getStatus() != WorkflowStatus.COMPLETED) {
            throw new IllegalArgumentException("只能为已完成的工作流创建投稿任务");
        }
        
        String taskId = workflowInstance.getTaskId();
        log.info("工作流完成，自动创建投稿任务: workflowId={}, taskId={}", 
                workflowInstance.getInstanceId(), taskId);
        
        // 创建工作流触发的任务
        QueuedSubmissionTask enhancedTask = QueuedSubmissionTask.createFromWorkflow(
                taskId, workflowInstance.getInstanceId());
        
        // 根据工作流配置设置优先级
        if (workflowInstance.getConfig() != null && workflowInstance.getConfig().isEnableDirectSubmission()) {
            // 直接投稿任务设置为高优先级
            enhancedTask.setPriority(QueuedSubmissionTask.Priority.HIGH.getValue());
            enhancedTask.setDescription("工作流直接投稿任务（高优先级）");
        }
        
        // 保存增强任务信息
        enhancedTasks.put(taskId, enhancedTask);
        
        // 调用原有队列服务入队
        QueuePosition position = submissionQueueService.enqueueTask(taskId);
        
        if (position != null) {
            // 如果启用优先级处理，重新排序队列
            if (priorityProcessingEnabled) {
                reorderQueueByPriority();
            }
            
            log.info("工作流触发的投稿任务入队成功: taskId={}, position={}, priority={}", 
                    taskId, position.getPosition(), enhancedTask.getPriorityDescription());
        }
        
        return position;
    }
    
    @Override
    public QueuePosition enqueueWithPriority(String taskId, int priority) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        
        log.info("带优先级入队任务: taskId={}, priority={}", taskId, priority);
        
        // 创建增强任务
        QueuedSubmissionTask enhancedTask = QueuedSubmissionTask.builder()
                .taskId(taskId)
                .priority(priority)
                .queuedAt(LocalDateTime.now())
                .description("优先级投稿任务")
                .build();
        
        // 保存增强任务信息
        enhancedTasks.put(taskId, enhancedTask);
        
        // 调用原有队列服务入队
        QueuePosition position = submissionQueueService.enqueueTask(taskId);
        
        if (position != null && priorityProcessingEnabled) {
            // 重新排序队列
            reorderQueueByPriority();
            
            // 重新获取位置（排序后可能发生变化）
            position = submissionQueueService.getTaskPosition(taskId);
        }
        
        log.info("优先级任务入队成功: taskId={}, priority={}, position={}", 
                taskId, QueuedSubmissionTask.Priority.fromValue(priority).getDescription(), 
                position != null ? position.getPosition() : "unknown");
        
        return position;
    }
    
    @Override
    public List<QueuePosition> enqueueBatch(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        log.info("批量入队任务: count={}", taskIds.size());
        
        List<QueuePosition> positions = new ArrayList<>();
        
        for (String taskId : taskIds) {
            try {
                // 创建普通优先级任务
                QueuedSubmissionTask enhancedTask = QueuedSubmissionTask.createNormal(taskId);
                enhancedTask.setDescription("批量投稿任务");
                
                // 保存增强任务信息
                enhancedTasks.put(taskId, enhancedTask);
                
                // 入队
                QueuePosition position = submissionQueueService.enqueueTask(taskId);
                if (position != null) {
                    positions.add(position);
                }
                
            } catch (Exception e) {
                log.error("批量入队任务失败: taskId={}", taskId, e);
            }
        }
        
        // 批量入队后重新排序
        if (priorityProcessingEnabled && !positions.isEmpty()) {
            reorderQueueByPriority();
        }
        
        log.info("批量入队完成: 成功={}, 总数={}", positions.size(), taskIds.size());
        return positions;
    }
    
    @Override
    public EnhancedQueueStatus getEnhancedQueueStatus() {
        // 获取基础队列状态
        QueueStatus baseStatus = submissionQueueService.getQueueStatus();
        
        // 创建增强状态
        EnhancedQueueStatus enhancedStatus = new EnhancedQueueStatus();
        enhancedStatus.setQueueLength(baseStatus.getQueueLength());
        enhancedStatus.setProcessingStatus(baseStatus.getProcessingStatus());
        enhancedStatus.setCurrentTaskId(baseStatus.getCurrentTaskId());
        enhancedStatus.setQueuedTasks(baseStatus.getQueuedTasks());
        enhancedStatus.setLastUpdated(baseStatus.getLastUpdated());
        enhancedStatus.setRunning(baseStatus.isRunning());
        
        // 计算优先级统计
        int highPriorityCount = 0;
        int normalPriorityCount = 0;
        int lowPriorityCount = 0;
        int workflowTriggeredCount = 0;
        
        for (QueuedSubmissionTask task : enhancedTasks.values()) {
            if (task.getStatus() == QueueTaskStatus.QUEUED || task.getStatus() == QueueTaskStatus.PROCESSING) {
                if (task.isHighPriority()) {
                    highPriorityCount++;
                } else if (task.isLowPriority()) {
                    lowPriorityCount++;
                } else {
                    normalPriorityCount++;
                }
                
                if (task.isWorkflowTriggered()) {
                    workflowTriggeredCount++;
                }
            }
        }
        
        enhancedStatus.setHighPriorityCount(highPriorityCount);
        enhancedStatus.setNormalPriorityCount(normalPriorityCount);
        enhancedStatus.setLowPriorityCount(lowPriorityCount);
        enhancedStatus.setWorkflowTriggeredCount(workflowTriggeredCount);
        enhancedStatus.setPriorityProcessingEnabled(priorityProcessingEnabled);
        
        return enhancedStatus;
    }
    
    @Override
    public void reorderQueueByPriority() {
        if (!priorityProcessingEnabled) {
            log.debug("优先级处理已禁用，跳过队列重排序");
            return;
        }
        
        log.info("开始按优先级重新排序队列");
        
        try {
            // 获取当前队列中的所有任务
            List<QueuedSubmissionTask> queuedTasks = enhancedTasks.values().stream()
                    .filter(task -> task.getStatus() == QueueTaskStatus.QUEUED)
                    .filter(task -> !pausedPriorities.contains(task.getPriority()))
                    .sorted(Comparator.comparingInt(QueuedSubmissionTask::getPriority)
                            .thenComparing(QueuedSubmissionTask::getQueuedAt))
                    .collect(Collectors.toList());
            
            log.info("队列重排序完成: 任务数={}, 高优先级={}, 普通优先级={}, 低优先级={}", 
                    queuedTasks.size(),
                    queuedTasks.stream().mapToInt(t -> t.isHighPriority() ? 1 : 0).sum(),
                    queuedTasks.stream().mapToInt(t -> !t.isHighPriority() && !t.isLowPriority() ? 1 : 0).sum(),
                    queuedTasks.stream().mapToInt(t -> t.isLowPriority() ? 1 : 0).sum());
            
        } catch (Exception e) {
            log.error("队列重排序失败", e);
        }
    }
    
    @Override
    public boolean setTaskPriority(String taskId, int priority) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return false;
        }
        
        QueuedSubmissionTask task = enhancedTasks.get(taskId);
        if (task == null) {
            log.warn("任务不存在，无法设置优先级: taskId={}", taskId);
            return false;
        }
        
        if (task.getStatus() == QueueTaskStatus.PROCESSING || task.getStatus() == QueueTaskStatus.COMPLETED) {
            log.warn("任务已在处理或已完成，无法修改优先级: taskId={}, status={}", taskId, task.getStatus());
            return false;
        }
        
        int oldPriority = task.getPriority();
        task.setPriority(priority);
        
        log.info("任务优先级已更新: taskId={}, oldPriority={}, newPriority={}", 
                taskId, QueuedSubmissionTask.Priority.fromValue(oldPriority).getDescription(),
                QueuedSubmissionTask.Priority.fromValue(priority).getDescription());
        
        // 重新排序队列
        if (priorityProcessingEnabled) {
            reorderQueueByPriority();
        }
        
        return true;
    }
    
    @Override
    public int getTaskPriority(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return -1;
        }
        
        QueuedSubmissionTask task = enhancedTasks.get(taskId);
        return task != null ? task.getPriority() : -1;
    }
    
    @Override
    public List<String> getTasksByPriorityRange(int minPriority, int maxPriority) {
        return enhancedTasks.values().stream()
                .filter(task -> task.getPriority() >= minPriority && task.getPriority() <= maxPriority)
                .filter(task -> task.getStatus() == QueueTaskStatus.QUEUED || task.getStatus() == QueueTaskStatus.PROCESSING)
                .map(QueuedSubmissionTask::getTaskId)
                .collect(Collectors.toList());
    }
    
    @Override
    public void pausePriorityLevel(int priority) {
        pausedPriorities.add(priority);
        log.info("暂停优先级处理: priority={}, description={}", 
                priority, QueuedSubmissionTask.Priority.fromValue(priority).getDescription());
    }
    
    @Override
    public void resumePriorityLevel(int priority) {
        pausedPriorities.remove(priority);
        log.info("恢复优先级处理: priority={}, description={}", 
                priority, QueuedSubmissionTask.Priority.fromValue(priority).getDescription());
        
        // 恢复后重新排序队列
        if (priorityProcessingEnabled) {
            reorderQueueByPriority();
        }
    }
    
    @Override
    public QueueStatistics getQueueStatistics() {
        QueueStatistics stats = new QueueStatistics();
        
        // 计算基础统计
        List<QueuedSubmissionTask> allTasks = new ArrayList<>(enhancedTasks.values());
        
        stats.setTotalTasks(allTasks.size());
        stats.setCompletedTasks((int) allTasks.stream().filter(t -> t.getStatus() == QueueTaskStatus.COMPLETED).count());
        stats.setFailedTasks((int) allTasks.stream().filter(t -> t.getStatus() == QueueTaskStatus.FAILED).count());
        stats.setRetryTasks((int) allTasks.stream().filter(t -> t.getRetryCount() > 0).count());
        stats.setWorkflowTriggeredTasks((int) allTasks.stream().filter(QueuedSubmissionTask::isWorkflowTriggered).count());
        
        // 计算成功率
        int processedTasks = stats.getCompletedTasks() + stats.getFailedTasks();
        if (processedTasks > 0) {
            stats.setSuccessRate((double) stats.getCompletedTasks() / processedTasks * 100);
        }
        
        // 计算平均处理时间
        double avgProcessingTime = allTasks.stream()
                .filter(t -> t.getStatus() == QueueTaskStatus.COMPLETED)
                .mapToLong(QueuedSubmissionTask::getProcessingDurationMinutes)
                .average()
                .orElse(0.0);
        stats.setAverageProcessingTimeMinutes(avgProcessingTime);
        
        // 计算今日处理任务数
        LocalDate today = LocalDate.now();
        int tasksProcessedToday = (int) allTasks.stream()
                .filter(t -> t.getCompletedAt() != null)
                .filter(t -> t.getCompletedAt().toLocalDate().equals(today))
                .count();
        stats.setTasksProcessedToday(tasksProcessedToday);
        
        return stats;
    }
    
    @Override
    public int cleanupCompletedTasks(int olderThanDays) {
        if (olderThanDays <= 0) {
            throw new IllegalArgumentException("清理天数必须大于0");
        }
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(olderThanDays);
        
        List<String> tasksToRemove = enhancedTasks.values().stream()
                .filter(task -> task.getStatus() == QueueTaskStatus.COMPLETED || task.getStatus() == QueueTaskStatus.FAILED)
                .filter(task -> task.getCompletedAt() != null && task.getCompletedAt().isBefore(cutoffTime))
                .map(QueuedSubmissionTask::getTaskId)
                .collect(Collectors.toList());
        
        for (String taskId : tasksToRemove) {
            enhancedTasks.remove(taskId);
        }
        
        log.info("清理已完成任务: 清理数量={}, 清理条件={}天前", tasksToRemove.size(), olderThanDays);
        
        return tasksToRemove.size();
    }
    
    /**
     * 启用优先级处理
     */
    public void enablePriorityProcessing() {
        this.priorityProcessingEnabled = true;
        log.info("启用优先级处理");
        reorderQueueByPriority();
    }
    
    /**
     * 禁用优先级处理
     */
    public void disablePriorityProcessing() {
        this.priorityProcessingEnabled = false;
        log.info("禁用优先级处理");
    }
    
    /**
     * 获取增强任务信息
     */
    public QueuedSubmissionTask getEnhancedTask(String taskId) {
        return enhancedTasks.get(taskId);
    }
    
    /**
     * 获取所有增强任务
     */
    public Map<String, QueuedSubmissionTask> getAllEnhancedTasks() {
        return new HashMap<>(enhancedTasks);
    }
    
    /**
     * 清空增强任务缓存（用于测试）
     */
    public void clearEnhancedTasks() {
        enhancedTasks.clear();
        pausedPriorities.clear();
        taskCounter.set(0);
        log.info("清空增强任务缓存");
    }
}