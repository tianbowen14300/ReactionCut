package com.tbw.cut.service.download.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 视频下载时长日志记录器
 * 记录和统计视频下载的详细时长信息
 */
@Slf4j
@Component
public class DownloadTimeLogger {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 存储下载开始时间
    private final ConcurrentHashMap<Long, DownloadTimeRecord> downloadRecords = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong totalDownloads = new AtomicLong(0);
    private final AtomicLong totalDownloadTime = new AtomicLong(0);
    private final AtomicLong successfulDownloads = new AtomicLong(0);
    private final AtomicLong failedDownloads = new AtomicLong(0);
    
    /**
     * 记录下载开始
     * @param taskId 任务ID
     * @param videoTitle 视频标题
     * @param videoUrl 视频URL
     * @param fileSize 文件大小（字节）
     * @param segmentCount 分段数量
     */
    public void logDownloadStart(Long taskId, String videoTitle, String videoUrl, 
                                long fileSize, int segmentCount) {
        LocalDateTime startTime = LocalDateTime.now();
        DownloadTimeRecord record = new DownloadTimeRecord(
            taskId, videoTitle, videoUrl, fileSize, segmentCount, startTime
        );
        
        downloadRecords.put(taskId, record);
        totalDownloads.incrementAndGet();
        
        log.info("📥 [下载开始] 任务ID: {}, 视频: {}, 文件大小: {}, 分段数: {}, 开始时间: {}", 
            taskId, videoTitle, formatFileSize(fileSize), segmentCount, startTime.format(TIME_FORMATTER));
    }
    
    /**
     * 记录下载完成
     * @param taskId 任务ID
     * @param success 是否成功
     * @param errorMessage 错误信息（如果失败）
     * @param actualFileSize 实际下载文件大小
     */
    public void logDownloadComplete(Long taskId, boolean success, String errorMessage, long actualFileSize) {
        DownloadTimeRecord record = downloadRecords.remove(taskId);
        if (record == null) {
            log.warn("⚠️ 未找到任务ID {} 的下载记录", taskId);
            return;
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(record.getStartTime(), endTime);
        long durationMs = duration.toMillis();
        
        // 更新统计信息
        totalDownloadTime.addAndGet(durationMs);
        if (success) {
            successfulDownloads.incrementAndGet();
        } else {
            failedDownloads.incrementAndGet();
        }
        
        // 计算下载速度
        double speedMBps = actualFileSize > 0 && durationMs > 0 ? 
            (actualFileSize / 1024.0 / 1024.0) / (durationMs / 1000.0) : 0.0;
        
        if (success) {
            log.info("✅ [下载完成] 任务ID: {}, 视频: {}, 耗时: {}, 文件大小: {}, 平均速度: {:.2f} MB/s, 分段数: {}", 
                taskId, record.getVideoTitle(), formatDuration(duration), 
                formatFileSize(actualFileSize), speedMBps, record.getSegmentCount());
        } else {
            log.error("❌ [下载失败] 任务ID: {}, 视频: {}, 耗时: {}, 错误: {}", 
                taskId, record.getVideoTitle(), formatDuration(duration), errorMessage);
        }
        
        // 记录详细统计信息
        logDetailedStats(record, duration, success, speedMBps);
    }
    
    /**
     * 记录分段下载时长
     * @param taskId 任务ID
     * @param segmentIndex 分段索引
     * @param segmentSize 分段大小
     * @param durationMs 下载耗时（毫秒）
     */
    public void logSegmentDownloadTime(Long taskId, int segmentIndex, long segmentSize, long durationMs) {
        double speedMBps = segmentSize > 0 && durationMs > 0 ? 
            (segmentSize / 1024.0 / 1024.0) / (durationMs / 1000.0) : 0.0;
        
        log.debug("🔗 [分段下载] 任务ID: {}, 分段: {}, 大小: {}, 耗时: {}ms, 速度: {:.2f} MB/s", 
            taskId, segmentIndex, formatFileSize(segmentSize), durationMs, speedMBps);
    }
    
    /**
     * 记录文件合并时长
     * @param taskId 任务ID
     * @param segmentCount 分段数量
     * @param totalSize 总文件大小
     * @param mergeDurationMs 合并耗时（毫秒）
     */
    public void logFileMergeTime(Long taskId, int segmentCount, long totalSize, long mergeDurationMs) {
        log.info("🔧 [文件合并] 任务ID: {}, 分段数: {}, 总大小: {}, 合并耗时: {}ms", 
            taskId, segmentCount, formatFileSize(totalSize), mergeDurationMs);
    }
    
    /**
     * 记录队列等待时长
     * @param taskId 任务ID
     * @param waitDurationMs 等待时长（毫秒）
     */
    public void logQueueWaitTime(Long taskId, long waitDurationMs) {
        if (waitDurationMs > 1000) { // 只记录超过1秒的等待
            log.info("⏳ [队列等待] 任务ID: {}, 等待时长: {}ms", taskId, waitDurationMs);
        }
    }
    
    /**
     * 获取下载统计信息
     * @return 统计信息字符串
     */
    public String getDownloadStatistics() {
        long total = totalDownloads.get();
        long successful = successfulDownloads.get();
        long failed = failedDownloads.get();
        long avgTime = total > 0 ? totalDownloadTime.get() / total : 0;
        
        return String.format(
            "📊 [下载统计] 总下载: %d, 成功: %d, 失败: %d, 成功率: %.1f%%, 平均耗时: %dms",
            total, successful, failed, 
            total > 0 ? (successful * 100.0 / total) : 0.0, 
            avgTime
        );
    }
    
    /**
     * 记录详细统计信息
     */
    private void logDetailedStats(DownloadTimeRecord record, Duration duration, boolean success, double speedMBps) {
        // 每10个下载记录一次统计
        if (totalDownloads.get() % 10 == 0) {
            log.info(getDownloadStatistics());
        }
        
        // 记录性能异常情况
        if (success) {
            if (speedMBps < 0.1) {
                log.warn("🐌 [低速下载] 任务ID: {}, 速度: {:.3f} MB/s, 可能网络较慢", 
                    record.getTaskId(), speedMBps);
            } else if (speedMBps > 50) {
                log.info("🚀 [高速下载] 任务ID: {}, 速度: {:.2f} MB/s, 网络状况良好", 
                    record.getTaskId(), speedMBps);
            }
            
            if (duration.toMinutes() > 30) {
                log.warn("⏰ [长时下载] 任务ID: {}, 耗时: {}, 文件较大或网络较慢", 
                    record.getTaskId(), formatDuration(duration));
            }
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }
    
    /**
     * 格式化时长
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return String.format("%d.%03ds", seconds, duration.toMillis() % 1000);
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm %ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
    }
    
    /**
     * 下载时间记录内部类
     */
    private static class DownloadTimeRecord {
        private final Long taskId;
        private final String videoTitle;
        private final String videoUrl;
        private final long fileSize;
        private final int segmentCount;
        private final LocalDateTime startTime;
        
        public DownloadTimeRecord(Long taskId, String videoTitle, String videoUrl, 
                                 long fileSize, int segmentCount, LocalDateTime startTime) {
            this.taskId = taskId;
            this.videoTitle = videoTitle;
            this.videoUrl = videoUrl;
            this.fileSize = fileSize;
            this.segmentCount = segmentCount;
            this.startTime = startTime;
        }
        
        public Long getTaskId() { return taskId; }
        public String getVideoTitle() { return videoTitle; }
        public String getVideoUrl() { return videoUrl; }
        public long getFileSize() { return fileSize; }
        public int getSegmentCount() { return segmentCount; }
        public LocalDateTime getStartTime() { return startTime; }
    }
}