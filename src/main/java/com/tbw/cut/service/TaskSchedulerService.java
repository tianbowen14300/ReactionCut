package com.tbw.cut.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskSchedulerService {
    
    private final SubmissionTaskService submissionTaskService;
    private final TaskExecutorService taskExecutorService;
    
    public TaskSchedulerService(SubmissionTaskService submissionTaskService, 
                               TaskExecutorService taskExecutorService) {
        this.submissionTaskService = submissionTaskService;
        this.taskExecutorService = taskExecutorService;
    }
    
    /**
     * 定时扫描PENDING状态的任务并执行
     */
//    @Scheduled(fixedDelay = 5000) // 每30秒执行一次
    public void schedulePendingTasks() {
        taskExecutorService.executePendingTasks();
    }
}