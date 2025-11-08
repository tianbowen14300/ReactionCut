package com.tbw.cut.service;

public interface TaskExecutorService {
    
    /**
     * 执行待处理的任务
     */
    void executePendingTasks();
}