package com.tbw.cut.entity;

/**
 * 队列任务状态枚举
 */
public enum QueueTaskStatus {
    /**
     * 已排队，等待处理
     */
    QUEUED,
    
    /**
     * 正在处理
     */
    PROCESSING,
    
    /**
     * 处理完成
     */
    COMPLETED,
    
    /**
     * 处理失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED,
    
    /**
     * 暂停中（406错误等待重试）
     */
    PAUSED
}