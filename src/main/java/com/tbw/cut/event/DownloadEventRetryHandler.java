package com.tbw.cut.event;

import com.tbw.cut.service.FailedEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 下载事件重试处理器
 * 
 * 处理下载完成事件发布失败的重试逻辑
 * 使用手动重试机制，避免引入spring-retry依赖
 */
@Component
@Slf4j
public class DownloadEventRetryHandler {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    @Autowired
    private DownloadEventPublisher downloadEventPublisher;
    
    @Autowired
    private FailedEventService failedEventService;
    
    /**
     * 带重试机制的事件发布
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePaths 文件路径列表
     */
    public void publishEventWithRetry(Long downloadTaskId, List<String> filePaths) {
        log.debug("尝试发布下载完成事件(带重试): downloadTaskId={}, fileCount={}", 
                downloadTaskId, filePaths != null ? filePaths.size() : 0);
        
        Exception lastException = null;
        long delay = INITIAL_DELAY_MS;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                downloadEventPublisher.publishDownloadCompletionEvent(downloadTaskId, filePaths);
                log.info("成功发布下载完成事件(重试机制): downloadTaskId={}, attempt={}", downloadTaskId, attempt);
                return; // 成功，直接返回
            } catch (Exception e) {
                lastException = e;
                log.warn("发布下载完成事件失败，尝试次数: {}/{}, downloadTaskId={}, error={}", 
                        attempt, MAX_RETRY_ATTEMPTS, downloadTaskId, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(delay);
                        delay = (long) (delay * BACKOFF_MULTIPLIER);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败，执行恢复逻辑
        recoverFromEventPublishFailure(lastException, downloadTaskId, filePaths);
    }
    
    /**
     * 带重试机制的事件发布（带元数据）
     * 
     * @param downloadTaskId 下载任务ID
     * @param filePaths 文件路径列表
     * @param metadata 附加元数据
     */
    public void publishEventWithRetry(Long downloadTaskId, List<String> filePaths, Map<String, Object> metadata) {
        log.debug("尝试发布下载完成事件(带重试和元数据): downloadTaskId={}, fileCount={}", 
                downloadTaskId, filePaths != null ? filePaths.size() : 0);
        
        Exception lastException = null;
        long delay = INITIAL_DELAY_MS;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                downloadEventPublisher.publishDownloadCompletionEvent(downloadTaskId, filePaths, metadata);
                log.info("成功发布下载完成事件(重试机制+元数据): downloadTaskId={}, attempt={}", downloadTaskId, attempt);
                return; // 成功，直接返回
            } catch (Exception e) {
                lastException = e;
                log.warn("发布下载完成事件失败，尝试次数: {}/{}, downloadTaskId={}, error={}", 
                        attempt, MAX_RETRY_ATTEMPTS, downloadTaskId, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(delay);
                        delay = (long) (delay * BACKOFF_MULTIPLIER);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败，执行恢复逻辑
        recoverFromEventPublishFailure(lastException, downloadTaskId, filePaths, metadata);
    }
    
    /**
     * 带重试机制的事件发布（使用事件对象）
     * 
     * @param event 下载完成事件对象
     */
    public void publishEventWithRetry(DownloadCompletionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("事件对象不能为空");
        }
        
        log.debug("尝试发布下载完成事件对象(带重试): {}", event);
        
        Exception lastException = null;
        long delay = INITIAL_DELAY_MS;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                downloadEventPublisher.publishDownloadCompletionEvent(
                        event.getDownloadTaskId(), 
                        event.getCompletedFilePaths(), 
                        event.getMetadata()
                );
                log.info("成功发布下载完成事件对象(重试机制): downloadTaskId={}, attempt={}", 
                        event.getDownloadTaskId(), attempt);
                return; // 成功，直接返回
            } catch (Exception e) {
                lastException = e;
                log.warn("发布下载完成事件对象失败，尝试次数: {}/{}, downloadTaskId={}, error={}", 
                        attempt, MAX_RETRY_ATTEMPTS, event.getDownloadTaskId(), e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(delay);
                        delay = (long) (delay * BACKOFF_MULTIPLIER);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败，执行恢复逻辑
        recoverFromEventPublishFailure(lastException, event);
    }
    
    /**
     * 重试失败后的恢复处理（基本版本）
     * 
     * @param ex 最后一次重试的异常
     * @param downloadTaskId 下载任务ID
     * @param filePaths 文件路径列表
     */
    private void recoverFromEventPublishFailure(Exception ex, Long downloadTaskId, List<String> filePaths) {
        log.error("事件发布最终失败，所有重试都已用尽: downloadTaskId={}, fileCount={}, error={}", 
                downloadTaskId, filePaths != null ? filePaths.size() : 0, 
                ex != null ? ex.getMessage() : "unknown", ex);
        
        try {
            // 创建下载完成事件对象
            DownloadCompletionEvent event = DownloadCompletionEvent.builder()
                    .downloadTaskId(downloadTaskId)
                    .completedFilePaths(filePaths)
                    .completionTime(LocalDateTime.now())
                    .build();
            
            // 记录失败事件到FailedEventService
            Long recordId = failedEventService.recordFailedEvent(event, ex != null ? ex.getMessage() : "unknown");
            
            if (recordId != null) {
                log.info("失败事件已记录，可稍后手动重试: recordId={}, downloadTaskId={}", 
                        recordId, downloadTaskId);
            } else {
                log.error("记录失败事件失败: downloadTaskId={}", downloadTaskId);
            }
            
        } catch (Exception recordException) {
            log.error("记录失败事件时发生异常: downloadTaskId={}", downloadTaskId, recordException);
            
            // 降级处理：记录到日志
            recordFailedEventForManualRetry(FailedEventRecord.builder()
                    .downloadTaskId(downloadTaskId)
                    .filePaths(filePaths)
                    .failureTime(LocalDateTime.now())
                    .errorMessage(ex != null ? ex.getMessage() : "unknown")
                    .retryCount(MAX_RETRY_ATTEMPTS)
                    .build());
        }
        
        // 发送告警通知（如果配置了告警系统）
        sendFailureAlert(downloadTaskId, ex);
    }
    
    /**
     * 重试失败后的恢复处理（带元数据版本）
     * 
     * @param ex 最后一次重试的异常
     * @param downloadTaskId 下载任务ID
     * @param filePaths 文件路径列表
     * @param metadata 附加元数据
     */
    private void recoverFromEventPublishFailure(Exception ex, Long downloadTaskId, List<String> filePaths, Map<String, Object> metadata) {
        log.error("事件发布最终失败(带元数据)，所有重试都已用尽: downloadTaskId={}, fileCount={}, error={}", 
                downloadTaskId, filePaths != null ? filePaths.size() : 0, 
                ex != null ? ex.getMessage() : "unknown", ex);
        
        // 创建失败事件记录
        FailedEventRecord failedRecord = FailedEventRecord.builder()
                .downloadTaskId(downloadTaskId)
                .filePaths(filePaths)
                .metadata(metadata)
                .failureTime(LocalDateTime.now())
                .errorMessage(ex != null ? ex.getMessage() : "unknown")
                .retryCount(MAX_RETRY_ATTEMPTS)
                .build();
        
        // 记录失败事件到持久化存储
        recordFailedEventForManualRetry(failedRecord);
        
        // 发送告警通知（如果配置了告警系统）
        sendFailureAlert(downloadTaskId, ex);
    }
    
    /**
     * 重试失败后的恢复处理（事件对象版本）
     * 
     * @param ex 最后一次重试的异常
     * @param event 下载完成事件对象
     */
    private void recoverFromEventPublishFailure(Exception ex, DownloadCompletionEvent event) {
        Long downloadTaskId = event != null ? event.getDownloadTaskId() : null;
        
        log.error("事件对象发布最终失败，所有重试都已用尽: downloadTaskId={}, error={}", 
                downloadTaskId, ex != null ? ex.getMessage() : "unknown", ex);
        
        if (event != null) {
            try {
                // 记录失败事件到FailedEventService
                Long recordId = failedEventService.recordFailedEvent(event, ex != null ? ex.getMessage() : "unknown");
                
                if (recordId != null) {
                    log.info("失败事件已记录，可稍后手动重试: recordId={}, downloadTaskId={}", 
                            recordId, downloadTaskId);
                } else {
                    log.error("记录失败事件失败: downloadTaskId={}", downloadTaskId);
                }
                
            } catch (Exception recordException) {
                log.error("记录失败事件时发生异常: downloadTaskId={}", downloadTaskId, recordException);
                
                // 降级处理：记录到日志
                recordFailedEventForManualRetry(FailedEventRecord.builder()
                        .downloadTaskId(event.getDownloadTaskId())
                        .filePaths(event.getCompletedFilePaths())
                        .metadata(event.getMetadata())
                        .failureTime(LocalDateTime.now())
                        .errorMessage(ex != null ? ex.getMessage() : "unknown")
                        .retryCount(MAX_RETRY_ATTEMPTS)
                        .originalEvent(event)
                        .build());
            }
        }
        
        // 发送告警通知（如果配置了告警系统）
        sendFailureAlert(downloadTaskId, ex);
    }
    
    /**
     * 记录失败事件以便后续手动重试
     * 
     * @param failedRecord 失败事件记录
     */
    private void recordFailedEventForManualRetry(FailedEventRecord failedRecord) {
        try {
            log.info("记录失败的下载完成事件以便后续处理: downloadTaskId={}, failureTime={}", 
                    failedRecord.getDownloadTaskId(), failedRecord.getFailureTime());
            
            // TODO: 实现失败事件的持久化存储
            // 可以存储到数据库、Redis或其他持久化存储中
            // failedEventService.recordFailedEvent(failedRecord);
            
            // 临时方案：记录到日志文件
            log.warn("FAILED_EVENT_RECORD: {}", failedRecord);
            
        } catch (Exception e) {
            log.error("记录失败事件时发生异常: downloadTaskId={}", 
                    failedRecord.getDownloadTaskId(), e);
        }
    }
    
    /**
     * 发送失败告警
     * 
     * @param downloadTaskId 下载任务ID
     * @param error 错误信息
     */
    private void sendFailureAlert(Long downloadTaskId, Exception error) {
        try {
            // TODO: 实现告警通知机制
            // 可以通过邮件、短信、钉钉等方式发送告警
            log.warn("发送下载完成事件发布失败告警: downloadTaskId={}, error={}", 
                    downloadTaskId, error.getMessage());
            
        } catch (Exception e) {
            log.error("发送失败告警时发生异常: downloadTaskId={}", downloadTaskId, e);
        }
    }
    
    /**
     * 失败事件记录
     */
    @lombok.Data
    @lombok.Builder
    public static class FailedEventRecord {
        private Long downloadTaskId;
        private List<String> filePaths;
        private Map<String, Object> metadata;
        private LocalDateTime failureTime;
        private String errorMessage;
        private int retryCount;
        private DownloadCompletionEvent originalEvent;
        
        @Override
        public String toString() {
            return String.format("FailedEventRecord{downloadTaskId=%d, fileCount=%d, failureTime=%s, retryCount=%d, error='%s'}", 
                    downloadTaskId, 
                    filePaths != null ? filePaths.size() : 0, 
                    failureTime, 
                    retryCount, 
                    errorMessage);
        }
    }
}