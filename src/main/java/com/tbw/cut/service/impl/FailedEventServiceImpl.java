package com.tbw.cut.service.impl;

import com.tbw.cut.event.DownloadCompletionEvent;
import com.tbw.cut.event.DownloadEventPublisher;
import com.tbw.cut.service.FailedEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 失败事件记录服务实现
 * 使用内存存储，生产环境应该使用数据库
 */
@Slf4j
@Service
public class FailedEventServiceImpl implements FailedEventService {
    
    @Autowired
    private DownloadEventPublisher downloadEventPublisher;
    
    // 使用内存存储，生产环境应该使用数据库
    private final Map<Long, FailedEventRecord> failedEvents = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Long recordFailedEvent(DownloadCompletionEvent event, String errorMessage) {
        try {
            Long recordId = idGenerator.getAndIncrement();
            
            FailedEventRecord record = new FailedEventRecord(
                    event.getDownloadTaskId(),
                    event.getSubmissionTaskId(),
                    event.getCompletedFilePaths(),
                    errorMessage
            );
            record.setId(recordId);
            
            failedEvents.put(recordId, record);
            
            log.info("Recorded failed event: recordId={}, downloadTaskId={}, submissionTaskId={}, error={}", 
                    recordId, event.getDownloadTaskId(), event.getSubmissionTaskId(), errorMessage);
            
            return recordId;
            
        } catch (Exception e) {
            log.error("Failed to record failed event", e);
            return null;
        }
    }
    
    @Override
    public List<FailedEventRecord> getAllFailedEvents() {
        try {
            return new ArrayList<>(failedEvents.values());
        } catch (Exception e) {
            log.error("Failed to get all failed events", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<FailedEventRecord> getRetryableFailedEvents(int maxRetryCount) {
        try {
            return failedEvents.values().stream()
                    .filter(record -> record.canRetry(maxRetryCount))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get retryable failed events", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean replayFailedEvent(Long recordId) {
        try {
            FailedEventRecord record = failedEvents.get(recordId);
            if (record == null) {
                log.warn("Failed event record not found: recordId={}", recordId);
                return false;
            }
            
            if (!record.canRetry(3)) { // 默认最大重试3次
                log.warn("Failed event cannot be retried: recordId={}, retryCount={}", 
                        recordId, record.getRetryCount());
                return false;
            }
            
            // 增加重试次数
            record.incrementRetryCount();
            
            // 重新发布事件
            DownloadCompletionEvent event = record.toDownloadCompletionEvent();
            downloadEventPublisher.publishDownloadCompletionEvent(
                    event.getDownloadTaskId(), 
                    event.getCompletedFilePaths()
            );
            
            log.info("Replayed failed event: recordId={}, downloadTaskId={}, submissionTaskId={}, retryCount={}", 
                    recordId, record.getDownloadTaskId(), record.getSubmissionTaskId(), record.getRetryCount());
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to replay failed event: recordId={}", recordId, e);
            return false;
        }
    }
    
    @Override
    public int batchReplayFailedEvents(int maxRetryCount) {
        try {
            List<FailedEventRecord> retryableEvents = getRetryableFailedEvents(maxRetryCount);
            int successCount = 0;
            
            for (FailedEventRecord record : retryableEvents) {
                try {
                    boolean success = replayFailedEvent(record.getId());
                    if (success) {
                        successCount++;
                    }
                    
                    // 添加延迟避免系统过载
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    log.error("Failed to replay event: recordId={}", record.getId(), e);
                }
            }
            
            log.info("Batch replay completed: {} out of {} events replayed successfully", 
                    successCount, retryableEvents.size());
            return successCount;
            
        } catch (Exception e) {
            log.error("Batch replay failed", e);
            return 0;
        }
    }
    
    @Override
    public boolean deleteFailedEventRecord(Long recordId) {
        try {
            FailedEventRecord removed = failedEvents.remove(recordId);
            boolean success = removed != null;
            
            if (success) {
                log.info("Deleted failed event record: recordId={}", recordId);
            } else {
                log.warn("Failed event record not found for deletion: recordId={}", recordId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to delete failed event record: recordId={}", recordId, e);
            return false;
        }
    }
    
    @Override
    public int cleanupExpiredFailedEvents(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            int cleanupCount = 0;
            
            List<Long> expiredRecordIds = failedEvents.values().stream()
                    .filter(record -> record.getFailedAt().isBefore(cutoffTime))
                    .map(FailedEventRecord::getId)
                    .collect(Collectors.toList());
            
            for (Long recordId : expiredRecordIds) {
                if (deleteFailedEventRecord(recordId)) {
                    cleanupCount++;
                }
            }
            
            log.info("Cleaned up {} expired failed event records (older than {} days)", 
                    cleanupCount, daysToKeep);
            return cleanupCount;
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired failed events", e);
            return 0;
        }
    }
    
    /**
     * 标记事件为已解决
     * 当事件重放成功后调用
     */
    public void markEventResolved(Long recordId) {
        try {
            FailedEventRecord record = failedEvents.get(recordId);
            if (record != null) {
                record.markResolved();
                log.info("Marked failed event as resolved: recordId={}", recordId);
            }
        } catch (Exception e) {
            log.error("Failed to mark event as resolved: recordId={}", recordId, e);
        }
    }
    
    /**
     * 获取失败事件统计信息
     */
    public FailedEventStats getFailedEventStats() {
        try {
            long totalEvents = failedEvents.size();
            long pendingEvents = failedEvents.values().stream()
                    .filter(record -> "PENDING".equals(record.getStatus()))
                    .count();
            long retryingEvents = failedEvents.values().stream()
                    .filter(record -> "RETRYING".equals(record.getStatus()))
                    .count();
            long resolvedEvents = failedEvents.values().stream()
                    .filter(record -> "RESOLVED".equals(record.getStatus()))
                    .count();
            
            return new FailedEventStats(totalEvents, pendingEvents, retryingEvents, resolvedEvents);
            
        } catch (Exception e) {
            log.error("Failed to get failed event stats", e);
            return new FailedEventStats(0, 0, 0, 0);
        }
    }
    
    /**
     * 失败事件统计信息
     */
    public static class FailedEventStats {
        private final long totalEvents;
        private final long pendingEvents;
        private final long retryingEvents;
        private final long resolvedEvents;
        
        public FailedEventStats(long totalEvents, long pendingEvents, 
                               long retryingEvents, long resolvedEvents) {
            this.totalEvents = totalEvents;
            this.pendingEvents = pendingEvents;
            this.retryingEvents = retryingEvents;
            this.resolvedEvents = resolvedEvents;
        }
        
        public long getTotalEvents() { return totalEvents; }
        public long getPendingEvents() { return pendingEvents; }
        public long getRetryingEvents() { return retryingEvents; }
        public long getResolvedEvents() { return resolvedEvents; }
    }
}