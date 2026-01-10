package com.tbw.cut.service;

import com.tbw.cut.event.DownloadCompletionEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失败事件记录服务接口
 * 记录和管理失败的事件
 */
public interface FailedEventService {
    
    /**
     * 记录失败的下载完成事件
     * @param event 失败的事件
     * @param errorMessage 错误信息
     * @return 记录ID
     */
    Long recordFailedEvent(DownloadCompletionEvent event, String errorMessage);
    
    /**
     * 获取所有失败的事件
     * @return 失败事件列表
     */
    List<FailedEventRecord> getAllFailedEvents();
    
    /**
     * 获取可重试的失败事件
     * @param maxRetryCount 最大重试次数
     * @return 可重试的失败事件列表
     */
    List<FailedEventRecord> getRetryableFailedEvents(int maxRetryCount);
    
    /**
     * 重放失败的事件
     * @param recordId 记录ID
     * @return 是否重放成功
     */
    boolean replayFailedEvent(Long recordId);
    
    /**
     * 批量重放失败的事件
     * @param maxRetryCount 最大重试次数
     * @return 重放成功的事件数量
     */
    int batchReplayFailedEvents(int maxRetryCount);
    
    /**
     * 删除失败事件记录
     * @param recordId 记录ID
     * @return 是否删除成功
     */
    boolean deleteFailedEventRecord(Long recordId);
    
    /**
     * 清理过期的失败事件记录
     * @param daysToKeep 保留天数
     * @return 清理的记录数量
     */
    int cleanupExpiredFailedEvents(int daysToKeep);
    
    /**
     * 失败事件记录
     */
    class FailedEventRecord {
        private Long id;
        private Long downloadTaskId;
        private String submissionTaskId;
        private List<String> completedFilePaths;
        private String errorMessage;
        private LocalDateTime failedAt;
        private LocalDateTime lastRetryAt;
        private Integer retryCount;
        private String status; // PENDING, RETRYING, RESOLVED, EXPIRED
        
        public FailedEventRecord() {}
        
        public FailedEventRecord(Long downloadTaskId, String submissionTaskId, 
                               List<String> completedFilePaths, String errorMessage) {
            this.downloadTaskId = downloadTaskId;
            this.submissionTaskId = submissionTaskId;
            this.completedFilePaths = completedFilePaths;
            this.errorMessage = errorMessage;
            this.failedAt = LocalDateTime.now();
            this.retryCount = 0;
            this.status = "PENDING";
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Long getDownloadTaskId() { return downloadTaskId; }
        public void setDownloadTaskId(Long downloadTaskId) { this.downloadTaskId = downloadTaskId; }
        
        public String getSubmissionTaskId() { return submissionTaskId; }
        public void setSubmissionTaskId(String submissionTaskId) { this.submissionTaskId = submissionTaskId; }
        
        public List<String> getCompletedFilePaths() { return completedFilePaths; }
        public void setCompletedFilePaths(List<String> completedFilePaths) { this.completedFilePaths = completedFilePaths; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getFailedAt() { return failedAt; }
        public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
        
        public LocalDateTime getLastRetryAt() { return lastRetryAt; }
        public void setLastRetryAt(LocalDateTime lastRetryAt) { this.lastRetryAt = lastRetryAt; }
        
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        /**
         * 增加重试次数
         */
        public void incrementRetryCount() {
            this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
            this.lastRetryAt = LocalDateTime.now();
            this.status = "RETRYING";
        }
        
        /**
         * 标记为已解决
         */
        public void markResolved() {
            this.status = "RESOLVED";
        }
        
        /**
         * 检查是否可以重试
         */
        public boolean canRetry(int maxRetryCount) {
            return this.retryCount < maxRetryCount && 
                   ("PENDING".equals(this.status) || "RETRYING".equals(this.status));
        }
        
        /**
         * 转换为下载完成事件
         */
        public DownloadCompletionEvent toDownloadCompletionEvent() {
            return DownloadCompletionEvent.builder()
                    .downloadTaskId(this.downloadTaskId)
                    .submissionTaskId(this.submissionTaskId)
                    .completedFilePaths(this.completedFilePaths)
                    .completionTime(LocalDateTime.now())
                    .build();
        }
    }
}