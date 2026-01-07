package com.tbw.cut.service;

import com.tbw.cut.dto.QueueStatus;

/**
 * 队列管理器接口
 */
public interface QueueManager {
    
    /**
     * 启动队列处理器
     */
    void startProcessing();
    
    /**
     * 停止队列处理器
     */
    void stopProcessing();
    
    /**
     * 处理下一个任务
     */
    void processNextTask();
    
    /**
     * 获取当前处理状态
     */
    QueueStatus.ProcessingStatus getCurrentStatus();
    
    /**
     * 获取队列状态
     */
    QueueStatus getQueueStatus();
    
    /**
     * 检查是否正在运行
     */
    boolean isRunning();
    
    /**
     * 检查是否正在处理任务
     */
    boolean isProcessing();
    
    /**
     * 获取当前正在处理的任务ID
     */
    String getCurrentTaskId();
    
    /**
     * 暂停处理（用于406错误等情况）
     */
    void pauseProcessing();
    
    /**
     * 恢复处理
     */
    void resumeProcessing();
    
    /**
     * 强制停止当前任务
     */
    void forceStopCurrentTask();
}