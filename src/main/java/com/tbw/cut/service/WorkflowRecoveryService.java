package com.tbw.cut.service;

import com.tbw.cut.entity.TaskRelation;

import java.util.List;

/**
 * 工作流恢复服务接口
 * 处理工作流启动失败的恢复逻辑
 */
public interface WorkflowRecoveryService {
    
    /**
     * 重试失败的工作流启动
     * @param downloadTaskId 下载任务ID
     * @param submissionTaskId 投稿任务ID
     * @return 是否重试成功
     */
    boolean retryFailedWorkflowStartup(Long downloadTaskId, String submissionTaskId);
    
    /**
     * 批量重试失败的工作流启动
     * @param maxRetryCount 最大重试次数
     * @return 重试成功的任务数量
     */
    int batchRetryFailedWorkflows(int maxRetryCount);
    
    /**
     * 检查下载是否完成
     * @param downloadTaskId 下载任务ID
     * @return 是否下载完成
     */
    boolean isDownloadCompleted(Long downloadTaskId);
    
    /**
     * 清理过期的工作流配置
     * @return 清理的配置数量
     */
    int cleanupExpiredConfigurations();
    
    /**
     * 恢复系统重启后的未完成任务
     * @return 恢复的任务数量
     */
    int recoverIncompleteTasksAfterRestart();
    
    /**
     * 获取工作流恢复统计信息
     * @return 恢复统计信息
     */
    WorkflowRecoveryStats getRecoveryStats();
    
    /**
     * 工作流恢复统计信息
     */
    class WorkflowRecoveryStats {
        private long pendingTasks;
        private long failedTasks;
        private long retryableTasks;
        private long expiredConfigurations;
        
        public WorkflowRecoveryStats(long pendingTasks, long failedTasks, 
                                   long retryableTasks, long expiredConfigurations) {
            this.pendingTasks = pendingTasks;
            this.failedTasks = failedTasks;
            this.retryableTasks = retryableTasks;
            this.expiredConfigurations = expiredConfigurations;
        }
        
        // Getters
        public long getPendingTasks() { return pendingTasks; }
        public long getFailedTasks() { return failedTasks; }
        public long getRetryableTasks() { return retryableTasks; }
        public long getExpiredConfigurations() { return expiredConfigurations; }
    }
}