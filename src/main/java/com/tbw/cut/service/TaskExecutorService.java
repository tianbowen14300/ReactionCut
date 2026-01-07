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

    void videoUpload(String taskId);
    
    /**
     * 执行投稿任务（队列系统使用）
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean executeSubmissionTask(String taskId);
}