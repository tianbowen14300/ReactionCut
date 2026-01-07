package com.tbw.cut.service;

import com.tbw.cut.entity.QueuedTask;
import java.util.List;
import java.util.Optional;

/**
 * 队列持久化服务接口
 */
public interface QueuePersistenceService {
    
    /**
     * 将任务添加到队列
     * @param queuedTask 队列任务
     * @return 是否成功
     */
    boolean enqueue(QueuedTask queuedTask);
    
    /**
     * 从队列中获取下一个任务
     * @param queueId 队列ID
     * @return 下一个任务，如果队列为空则返回空
     */
    Optional<QueuedTask> dequeue(String queueId);
    
    /**
     * 查看队列中的下一个任务（不移除）
     * @param queueId 队列ID
     * @return 下一个任务，如果队列为空则返回空
     */
    Optional<QueuedTask> peek(String queueId);
    
    /**
     * 获取队列长度
     * @param queueId 队列ID
     * @return 队列长度
     */
    int getQueueLength(String queueId);
    
    /**
     * 获取队列中的所有任务
     * @param queueId 队列ID
     * @return 任务列表
     */
    List<QueuedTask> getAllQueuedTasks(String queueId);
    
    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param queuedTask 更新后的任务信息
     * @return 是否成功
     */
    boolean updateTask(String taskId, QueuedTask queuedTask);
    
    /**
     * 从队列中移除任务
     * @param taskId 任务ID
     * @param queueId 队列ID
     * @return 是否成功
     */
    boolean removeTask(String taskId, String queueId);
    
    /**
     * 根据任务ID获取任务
     * @param taskId 任务ID
     * @return 任务信息
     */
    Optional<QueuedTask> getTask(String taskId);
    
    /**
     * 获取任务在队列中的位置
     * @param taskId 任务ID
     * @param queueId 队列ID
     * @return 位置（从1开始），如果不在队列中返回-1
     */
    int getTaskPosition(String taskId, String queueId);
    
    /**
     * 清空队列
     * @param queueId 队列ID
     * @return 是否成功
     */
    boolean clearQueue(String queueId);
    
    /**
     * 检查队列是否存在
     * @param queueId 队列ID
     * @return 是否存在
     */
    boolean queueExists(String queueId);
    
    /**
     * 从持久存储恢复队列
     * @param queueId 队列ID
     * @return 恢复的任务数量
     */
    int restoreQueue(String queueId);
    
    /**
     * 持久化队列状态
     * @param queueId 队列ID
     * @return 是否成功
     */
    boolean persistQueue(String queueId);
}