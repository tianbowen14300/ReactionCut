package com.tbw.cut.service;

import com.tbw.cut.dto.QueuePosition;
import com.tbw.cut.dto.QueueStatus;

/**
 * 投稿队列服务接口
 */
public interface SubmissionQueueService {
    
    /**
     * 将任务添加到队列
     * @param taskId 任务ID
     * @return 队列位置信息
     */
    QueuePosition enqueueTask(String taskId);
    
    /**
     * 获取队列状态
     * @return 队列状态
     */
    QueueStatus getQueueStatus();
    
    /**
     * 获取任务在队列中的位置
     * @param taskId 任务ID
     * @return 队列位置信息，如果任务不在队列中返回null
     */
    QueuePosition getTaskPosition(String taskId);
    
    /**
     * 取消排队的任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelQueuedTask(String taskId);
    
    /**
     * 检查任务是否在队列中
     * @param taskId 任务ID
     * @return 是否在队列中
     */
    boolean isTaskInQueue(String taskId);
    
    /**
     * 获取队列长度
     * @return 队列长度
     */
    int getQueueLength();
    
    /**
     * 清空队列
     * @return 是否成功
     */
    boolean clearQueue();
    
    /**
     * 启动队列处理
     */
    void startQueue();
    
    /**
     * 停止队列处理
     */
    void stopQueue();
    
    /**
     * 暂停队列处理
     */
    void pauseQueue();
    
    /**
     * 恢复队列处理
     */
    void resumeQueue();
}