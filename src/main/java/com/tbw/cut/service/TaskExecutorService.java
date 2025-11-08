package com.tbw.cut.service;

public interface TaskExecutorService {
    
    /**
     * 执行待处理的任务
     */
    void executePendingTasks();
    
    /**
     * 执行指定任务
     * @param taskId 任务ID
     */
    void executeTask(String taskId);
}