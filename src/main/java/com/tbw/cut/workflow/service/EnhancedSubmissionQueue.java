package com.tbw.cut.workflow.service;

import com.tbw.cut.dto.QueuePosition;
import com.tbw.cut.dto.QueueStatus;
import com.tbw.cut.workflow.model.WorkflowInstance;

import java.util.List;

/**
 * 增强的投稿队列接口
 * 集成工作流完成后的投稿触发和队列优先级管理
 */
public interface EnhancedSubmissionQueue {
    
    /**
     * 工作流完成后自动入队投稿任务
     * 
     * @param workflowInstance 完成的工作流实例
     * @return 队列位置信息
     */
    QueuePosition enqueueFromWorkflow(WorkflowInstance workflowInstance);
    
    /**
     * 带优先级的任务入队
     * 
     * @param taskId 任务ID
     * @param priority 优先级（数值越小优先级越高）
     * @return 队列位置信息
     */
    QueuePosition enqueueWithPriority(String taskId, int priority);
    
    /**
     * 批量入队任务
     * 
     * @param taskIds 任务ID列表
     * @return 队列位置信息列表
     */
    List<QueuePosition> enqueueBatch(List<String> taskIds);
    
    /**
     * 获取增强的队列状态
     * 包含优先级信息和工作流集成状态
     * 
     * @return 增强的队列状态
     */
    EnhancedQueueStatus getEnhancedQueueStatus();
    
    /**
     * 根据优先级重新排序队列
     */
    void reorderQueueByPriority();
    
    /**
     * 设置任务优先级
     * 
     * @param taskId 任务ID
     * @param priority 新的优先级
     * @return 是否设置成功
     */
    boolean setTaskPriority(String taskId, int priority);
    
    /**
     * 获取任务的优先级
     * 
     * @param taskId 任务ID
     * @return 优先级，如果任务不存在返回-1
     */
    int getTaskPriority(String taskId);
    
    /**
     * 获取指定优先级范围内的任务
     * 
     * @param minPriority 最小优先级
     * @param maxPriority 最大优先级
     * @return 任务ID列表
     */
    List<String> getTasksByPriorityRange(int minPriority, int maxPriority);
    
    /**
     * 暂停指定优先级的任务处理
     * 
     * @param priority 要暂停的优先级
     */
    void pausePriorityLevel(int priority);
    
    /**
     * 恢复指定优先级的任务处理
     * 
     * @param priority 要恢复的优先级
     */
    void resumePriorityLevel(int priority);
    
    /**
     * 获取队列统计信息
     * 
     * @return 队列统计信息
     */
    QueueStatistics getQueueStatistics();
    
    /**
     * 清理已完成的任务记录
     * 
     * @param olderThanDays 清理多少天前的记录
     * @return 清理的任务数量
     */
    int cleanupCompletedTasks(int olderThanDays);
    
    /**
     * 增强的队列状态
     */
    class EnhancedQueueStatus extends QueueStatus {
        private int highPriorityCount;
        private int normalPriorityCount;
        private int lowPriorityCount;
        private int workflowTriggeredCount;
        private boolean priorityProcessingEnabled;
        
        // Getters and Setters
        public int getHighPriorityCount() {
            return highPriorityCount;
        }
        
        public void setHighPriorityCount(int highPriorityCount) {
            this.highPriorityCount = highPriorityCount;
        }
        
        public int getNormalPriorityCount() {
            return normalPriorityCount;
        }
        
        public void setNormalPriorityCount(int normalPriorityCount) {
            this.normalPriorityCount = normalPriorityCount;
        }
        
        public int getLowPriorityCount() {
            return lowPriorityCount;
        }
        
        public void setLowPriorityCount(int lowPriorityCount) {
            this.lowPriorityCount = lowPriorityCount;
        }
        
        public int getWorkflowTriggeredCount() {
            return workflowTriggeredCount;
        }
        
        public void setWorkflowTriggeredCount(int workflowTriggeredCount) {
            this.workflowTriggeredCount = workflowTriggeredCount;
        }
        
        public boolean isPriorityProcessingEnabled() {
            return priorityProcessingEnabled;
        }
        
        public void setPriorityProcessingEnabled(boolean priorityProcessingEnabled) {
            this.priorityProcessingEnabled = priorityProcessingEnabled;
        }
    }
    
    /**
     * 队列统计信息
     */
    class QueueStatistics {
        private int totalTasks;
        private int completedTasks;
        private int failedTasks;
        private int retryTasks;
        private double averageProcessingTimeMinutes;
        private double successRate;
        private int tasksProcessedToday;
        private int workflowTriggeredTasks;
        
        // Getters and Setters
        public int getTotalTasks() {
            return totalTasks;
        }
        
        public void setTotalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
        }
        
        public int getCompletedTasks() {
            return completedTasks;
        }
        
        public void setCompletedTasks(int completedTasks) {
            this.completedTasks = completedTasks;
        }
        
        public int getFailedTasks() {
            return failedTasks;
        }
        
        public void setFailedTasks(int failedTasks) {
            this.failedTasks = failedTasks;
        }
        
        public int getRetryTasks() {
            return retryTasks;
        }
        
        public void setRetryTasks(int retryTasks) {
            this.retryTasks = retryTasks;
        }
        
        public double getAverageProcessingTimeMinutes() {
            return averageProcessingTimeMinutes;
        }
        
        public void setAverageProcessingTimeMinutes(double averageProcessingTimeMinutes) {
            this.averageProcessingTimeMinutes = averageProcessingTimeMinutes;
        }
        
        public double getSuccessRate() {
            return successRate;
        }
        
        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }
        
        public int getTasksProcessedToday() {
            return tasksProcessedToday;
        }
        
        public void setTasksProcessedToday(int tasksProcessedToday) {
            this.tasksProcessedToday = tasksProcessedToday;
        }
        
        public int getWorkflowTriggeredTasks() {
            return workflowTriggeredTasks;
        }
        
        public void setWorkflowTriggeredTasks(int workflowTriggeredTasks) {
            this.workflowTriggeredTasks = workflowTriggeredTasks;
        }
    }
}